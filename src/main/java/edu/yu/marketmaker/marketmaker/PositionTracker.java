package edu.yu.marketmaker.marketmaker;

import org.springframework.context.annotation.Profile;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;

import edu.yu.marketmaker.model.StateSnapshot;
import reactor.core.publisher.Flux;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("!test-position-tracker")
public class PositionTracker implements SnapshotTracker {

    private final RSocketRequester requester;
    private final Set<String> trackedSymbols;

    public PositionTracker(RSocketRequester.Builder rsocketRequesterBuilder) {
        this.requester = rsocketRequesterBuilder.tcp("trading-state", 7000);
        this.trackedSymbols = ConcurrentHashMap.newKeySet();
    }

    public Flux<StateSnapshot> getPositions() {
        return requester.route("state.stream")
                .retrieveFlux(StateSnapshot.class)
                .filter(snapshot -> snapshot.position() != null
                        && trackedSymbols.contains(snapshot.position().symbol()));
    }

    public boolean addSymbol(String symbol) {
        return trackedSymbols.add(symbol);
    }

    public boolean removeSymbol(String symbol) {
        return trackedSymbols.remove(symbol);
    }

    public boolean handlesSymbol(String symbol) {
        return trackedSymbols.contains(symbol);
    }
}
