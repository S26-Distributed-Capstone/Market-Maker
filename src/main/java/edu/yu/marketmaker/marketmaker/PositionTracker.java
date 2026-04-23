package edu.yu.marketmaker.marketmaker;

import edu.yu.marketmaker.ha.LeaderAwareRSocketClient;
import edu.yu.marketmaker.model.StateSnapshot;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Component
@Profile("market-maker-node")
public class PositionTracker {

    private final LeaderAwareRSocketClient client;

    public PositionTracker(LeaderAwareRSocketClient client) {
        this.client = client;
    }

    public Flux<StateSnapshot> getPositions() {
        // .repeatWhen reconnects on leader change: if the current leader dies,
        // the stream errors, we resume with empty, then repeat (which re-resolves
        // the leader via the registry cache).
        return client.requestStream("trading-state", "state.stream", StateSnapshot.class)
                .onErrorResume(e -> Flux.empty())
                .repeatWhen(signals -> signals.delayElements(Duration.ofSeconds(2)));
    }
}