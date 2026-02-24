package edu.yu.marketmaker.state;

import edu.yu.marketmaker.model.Fill;
import edu.yu.marketmaker.model.Position;
import edu.yu.marketmaker.model.Side;
import edu.yu.marketmaker.state.TradingStateService.StateSnapshot;
import io.rsocket.transport.netty.client.TcpClientTransport;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import reactor.core.Disposable;

import java.time.Duration;
import java.util.UUID;

/**
 * Simple standalone client to test the RSocket/TCP endpoints
 * exposed by {@link TradingStateService}.
 * <p>
 * Run the {@code main} method while the trading-state-service is up
 * (docker compose up). It connects to {@code localhost:7000} and exercises
 * each RSocket route.
 */
public class RSocketTestClient {

    private static final String HOST = "localhost";
    private static final int PORT = 7000;

    public static void main(String[] args) throws InterruptedException {

        // Build strategies with Jackson JSON codecs
        RSocketStrategies strategies = RSocketStrategies.builder()
                .encoder(new JacksonJsonEncoder())
                .decoder(new JacksonJsonDecoder())
                .build();

        // Build a requester that connects over TCP
        RSocketRequester requester = RSocketRequester.builder()
                .rsocketStrategies(strategies)
                .transport(TcpClientTransport.create(HOST, PORT));

        System.out.println("=== Connected to RSocket server at " + HOST + ":" + PORT + " ===\n");

        // -------------------------------------------------------
        // 1. Submit a fill  (request-response  →  state.fills)
        // -------------------------------------------------------
        Fill fill = new Fill(
                UUID.randomUUID(),
                "AAPL",
                Side.BUY,
                100,
                150.25,
                UUID.randomUUID(),
                System.currentTimeMillis()
        );

        System.out.println(">>> Submitting fill: " + fill);
        requester.route("state.fills")
                .data(fill)
                .retrieveMono(Void.class)
                .doOnSuccess(v -> System.out.println("<<< Fill submitted successfully\n"))
                .doOnError(e -> System.err.println("<<< Fill error: " + e.getMessage()))
                .block(Duration.ofSeconds(5));

        // -------------------------------------------------------
        // 2. Get a single position  (request-response  →  positions.AAPL)
        // -------------------------------------------------------
        System.out.println(">>> Getting position for AAPL");
        Position position = requester.route("positions.AAPL")
                .retrieveMono(Position.class)
                .doOnError(e -> System.err.println("<<< Position error: " + e.getMessage()))
                .block(Duration.ofSeconds(5));
        System.out.println("<<< Position: " + position + "\n");

        // -------------------------------------------------------
        // 3. Get all positions  (request-stream  →  positions)
        // -------------------------------------------------------
        System.out.println(">>> Getting all positions");
        requester.route("positions")
                .retrieveFlux(Position.class)
                .doOnNext(p -> System.out.println("    " + p))
                .doOnError(e -> System.err.println("<<< All-positions error: " + e.getMessage()))
                .blockLast(Duration.ofSeconds(5));
        System.out.println();

        // -------------------------------------------------------
        // 4. Stream live updates  (request-stream  →  state.stream)
        //    Runs for 15 seconds so you can submit fills from
        //    another terminal and watch them arrive here.
        // -------------------------------------------------------
        System.out.println(">>> Subscribing to state.stream (will listen for 15 s) ...");
        Disposable subscription = requester.route("state.stream")
                .retrieveFlux(StateSnapshot.class)
                .doOnNext(snap -> System.out.println("    Live update: " + snap))
                .doOnError(e -> System.err.println("<<< Stream error: " + e.getMessage()))
                .subscribe();

        // Submit a second fill while the stream is open so we can see it arrive
        Thread.sleep(2000);
        Fill fill2 = new Fill(
                UUID.randomUUID(),
                "AAPL",
                Side.SELL,
                50,
                151.00,
                UUID.randomUUID(),
                System.currentTimeMillis()
        );
        System.out.println("\n>>> Submitting second fill while stream is open: " + fill2);
        requester.route("state.fills")
                .data(fill2)
                .retrieveMono(Void.class)
                .doOnSuccess(v -> System.out.println("<<< Second fill submitted\n"))
                .block(Duration.ofSeconds(5));

        // Let the stream run a bit longer
        Thread.sleep(13_000);
        subscription.dispose();

        System.out.println("\n=== Done ===");
        requester.dispose();
    }
}

