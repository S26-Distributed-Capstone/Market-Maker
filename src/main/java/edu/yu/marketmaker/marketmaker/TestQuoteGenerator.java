package edu.yu.marketmaker.marketmaker;

import org.springframework.stereotype.Component;

import edu.yu.marketmaker.model.Fill;
import edu.yu.marketmaker.model.Position;
import edu.yu.marketmaker.model.Quote;

@Component
public class TestQuoteGenerator implements QuoteGenerator {

    @Override
    public Quote generateQuote(Position position, Fill lastFill) {
        return null;
    }
    

}
