package edu.yu.marketmaker.state;

import com.hazelcast.core.HazelcastException;
import edu.yu.marketmaker.memory.Repository;
import edu.yu.marketmaker.model.Fill;
import edu.yu.marketmaker.model.Position;

import edu.yu.marketmaker.model.Side;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;


@RestController
@Profile("trading-state")
public class TradingStateService {

    private final Repository<String, Position> positionRepository;
    private final Repository<UUID, Fill> fillRepository;

    /**
     * Hot multicast sink – every call to {@code submitFill} that results in a
     * position change emits one {@link StateSnapshot} here.  All active
     * {@code streamState} subscribers receive the event in real time.
     * {@code onBackpressureBuffer} keeps a small history so a slow subscriber
     * does not block the publisher.
     */
    private final Sinks.Many<StateSnapshot> positionSink =
            Sinks.many().multicast().onBackpressureBuffer();

    public TradingStateService(Repository<String, Position> positionRepository, Repository<UUID, Fill> fillRepository) {
        this.positionRepository = positionRepository;
        this.fillRepository = fillRepository;
    }

    /**
     * Method to submit a fill.
     * After persisting the fill and updating the position the new
     * {@link StateSnapshot} is broadcast to all active RSocket subscribers.
     *
     * @param fill the fill to record
     */
    @PostMapping("/state/fills")
    ResponseEntity<Void> submitFill(@RequestBody Fill fill) {
        try {
            Optional<Position> position = positionRepository.get(fill.symbol());
            if (position.isEmpty() && fill.side() == Side.SELL) { // reject if selling with no position
                return ResponseEntity.badRequest().build();
            }
            fillRepository.put(fill);
            int quantity = fill.side() == Side.BUY ? fill.quantity() : -fill.quantity();
            Position updatedPosition;
            if (position.isPresent()) {
                int newQuantity = position.get().netQuantity() + quantity;
                updatedPosition = new Position(fill.symbol(), newQuantity, position.get().version() + 1, fill.getId());
            } else {
                updatedPosition = new Position(fill.symbol(), quantity, 0, fill.getId());
            }
            positionRepository.put(updatedPosition);

            // Broadcast to all active streamState subscribers
            Collection<Fill> symbolFills = fillRepository.getAll().stream()
                    .filter(f -> f.symbol().equals(updatedPosition.symbol()))
                    .toList();
            positionSink.tryEmitNext(new StateSnapshot(updatedPosition, symbolFills));

            return ResponseEntity.ok().build();
        } catch (HazelcastException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all current positions.
     *
     * @return a collection of positions
     */
    @GetMapping("/positions")
    Collection<Position> getAllPositions() {
        return positionRepository.getAll();
    }

    /**
     * Get a specific position based on inputted symbol.
     *
     * @param symbol ticker symbol
     * @return the position, if present
     */
    @GetMapping("/positions/{symbol}")
    Optional<Position> getPosition(@PathVariable String symbol) {
        return positionRepository.get(symbol);
    }

    /**
     * RSocket request-stream endpoint.
     * Connect via TCP to port 7000 ({@code spring.rsocket.server.port}) and
     * send route {@code "state.stream"}.
     * <p>
     * The subscriber first receives a {@link StateSnapshot} for every position
     * that already exists at subscription time (the current snapshot), and then
     * continues to receive a new {@link StateSnapshot} every time a fill is
     * submitted that updates a position – i.e. the stream never completes while
     * the connection is open.
     *
     * @return a hot {@link Flux} of {@link StateSnapshot} items
     */
    @MessageMapping("state.stream")
    public Flux<StateSnapshot> streamPositions() {
        // Emit current state first, then keep streaming live updates
        Flux<StateSnapshot> currentState = Flux.fromIterable(positionRepository.getAll())
                .map(position -> {
                    Collection<Fill> fills = fillRepository.getAll().stream()
                            .filter(f -> f.symbol().equals(position.symbol()))
                            .toList();
                    return new StateSnapshot(position, fills);
                });

        return currentState.concatWith(positionSink.asFlux());
    }

    /**
     * A snapshot of a single position together with all fills that contributed
     * to it.  Sent as individual stream items to RSocket subscribers.
     *
     * @param position the current net position for a symbol
     * @param fills    all fills recorded for that symbol
     */
    public record StateSnapshot(Position position, Collection<Fill> fills) {}

}
