package edu.yu.marketmaker.marketmaker;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import edu.yu.marketmaker.model.Position;
import edu.yu.marketmaker.model.StateSnapshot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("market-maker-node")
public class MarketMaker implements ApplicationRunner {

    private final SnapshotTracker positionTracker;
    private final QuoteGenerator quoteGenerator;
    private final Map<String, Long> lastProcessedVersionBySymbol = new ConcurrentHashMap<>();

    public MarketMaker(SnapshotTracker positionTracker, QuoteGenerator quoteGenerator) {
        this.positionTracker = positionTracker;
        this.quoteGenerator = quoteGenerator;
    }

    private void handlePosition(StateSnapshot snapshot) {
        if (snapshot == null || snapshot.position() == null || snapshot.position().symbol() == null) {
            return;
        }
        if (!positionTracker.handlesSymbol(snapshot.position().symbol()) || !newVersion(snapshot.position())) {
            return;
        }
        quoteGenerator.generateQuote(snapshot.position(), snapshot.fill());
    }

    private boolean newVersion(Position position) {
        long incoming = position.version();
        boolean[] isNew = {false};
        lastProcessedVersionBySymbol.compute(position.symbol(), (k, prev) -> {
            if (prev == null || incoming > prev) {
                isNew[0] = true;
                return incoming;
            }
            return prev;
        });
        return isNew[0];
    }

    public boolean addSymbol(String symbol) {
        return positionTracker.addSymbol(symbol);
    }

    public boolean removeSymbol(String symbol) {
        boolean removed = positionTracker.removeSymbol(symbol);
        if (removed) {
            lastProcessedVersionBySymbol.remove(symbol);
        }
        return removed;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Subscribe once at startup so incoming snapshots are continuously processed.
        positionTracker.getPositions().subscribe(this::handlePosition);
    }
}
