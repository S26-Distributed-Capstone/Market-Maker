package edu.yu.marketmaker.marketmaker;

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

    public MarketMaker(PositionTracker positionTracker, QuoteGenerator quoteGenerator) {
        this.positionTracker = positionTracker;
        this.quoteGenerator = quoteGenerator;
    }

    private void handlePosition(StateSnapshot snapshot) {
        if (!handlesSymbol(snapshot.position().symbol()) || !newVersion(snapshot.position())) {
            return;
        }
        Quote quote = quoteGenerator.generateQuote(snapshot.position(), snapshot.fill());
        // TODO: update reservations
        // TODO: send new quote to exchange
    }

    private boolean handlesSymbol(String symbol) {
        return false;
    }

    private boolean newVersion(Position position) {
        return false;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        positionTracker.getPositions().doOnNext(this::handlePosition);
    }
}
