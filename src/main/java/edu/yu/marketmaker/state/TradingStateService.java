package edu.yu.marketmaker.state;

import com.hazelcast.core.HazelcastException;
import edu.yu.marketmaker.memory.Repository;
import edu.yu.marketmaker.model.Fill;
import edu.yu.marketmaker.model.Position;

import edu.yu.marketmaker.model.Side;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;


@RestController
@Profile("trading-state")
public class TradingStateService {

    private final Repository<String, Position> positionRepository;
    private final Repository<UUID, Fill> fillRepository;

    public TradingStateService(Repository<String, Position> positionRepository, Repository<UUID, Fill> fillRepository) {
        this.positionRepository = positionRepository;
        this.fillRepository = fillRepository;
    }

    /**
     * Method to submit a fill
     * @param fill
     */
    @PostMapping("/state/fills")
    ResponseEntity<Void> submitFill(@RequestBody Fill fill) {
        try {
            Optional<Position> position = positionRepository.get(fill.symbol());
            if(position.isEmpty() && fill.side() == Side.SELL){
                return ResponseEntity.badRequest().build();
            }
            fillRepository.put(fill);
            int quantity = fill.side() == Side.BUY ? fill.quantity() : -fill.quantity();
            if (position.isPresent()) {
                int newQuantity = position.get().netQuantity() + quantity;
                positionRepository.put(new Position(fill.symbol(), newQuantity, position.get().version()+1, fill.getId()));
            }else{
                positionRepository.put(new Position(fill.symbol(), quantity, 0, fill.getId()));
            }
            return ResponseEntity.ok().build();
        } catch (HazelcastException e) {
            return ResponseEntity.internalServerError().build();
        }
    }


    /**
     * Get all current positions
     * @return a collection of positions
     */
    @GetMapping("/positions")
    Collection<Position> getAllPositions() {
        return positionRepository.getAll();
    }

    /**
     * Get a specific position based on inputted symbol
     * @param symbol
     * @return
     */
    @GetMapping("/positions/{symbol}")
    Optional<Position> getPosition(@PathVariable String symbol) {
        return positionRepository.get(symbol);
    }

}
