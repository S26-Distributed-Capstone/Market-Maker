package edu.yu.marketmaker.marketmaker;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import edu.yu.marketmaker.model.Position;
import edu.yu.marketmaker.model.Quote;
import edu.yu.marketmaker.model.StateSnapshot;

@Component
@Profile("market-maker-node")
public class MarketMaker implements ApplicationRunner {

    private final PositionTracker positionTracker;
    private final QuoteGenerator quoteGenerator;
    private final Set<String> handledSymbols;

    public MarketMaker(PositionTracker positionTracker, QuoteGenerator quoteGenerator) {
        this.positionTracker = positionTracker;
        this.quoteGenerator = quoteGenerator;
        this.handledSymbols = new HashSet<>();
    }

    private void handlePosition(StateSnapshot snapshot) {
        if (!handlesSymbol(snapshot.position().symbol()) || !newVersion(snapshot.position())) {
            return;
        }
        Quote quote = quoteGenerator.generateQuote(snapshot.position(), snapshot.fill());
    }

    private boolean handlesSymbol(String symbol) {
        return handledSymbols.contains(symbol);
    }

    private boolean newVersion(Position position) {
        return false;
    }

    public boolean addSymbol(String symbol) {
        return handledSymbols.add(symbol);
    }

    public boolean removeSymbol(String symbol) {
        return handledSymbols.remove(symbol);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        positionTracker.getPositions().doOnNext(this::handlePosition);
    }
}
