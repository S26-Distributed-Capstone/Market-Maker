package edu.yu.marketmaker.marketmaker;

import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;

import edu.yu.marketmaker.model.StateSnapshot;
import reactor.core.publisher.Flux;

@Component
public class PositionTracker {
    
    private final RSocketRequester requester;

    public PositionTracker(RSocketRequester.Builder rsocketRequesterBuilder) {
        this.requester = rsocketRequesterBuilder.tcp("trading-state", 7000);
    }

    public Flux<StateSnapshot> getPositions() {
        return requester.route("state.stream").retrieveFlux(StateSnapshot.class);
    }
}
