package edu.yu.marketmaker.exchange;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.springframework.stereotype.Component;

import edu.yu.marketmaker.model.Quote;

@Component
public class StaticQuoteRepository implements QuoteRepository {

    private final Map<String, Quote> map;

    public StaticQuoteRepository(Map<String, Quote> map) {
        this.map = new HashMap<>(map);
    }

    public StaticQuoteRepository() {
        this(generateQuotes(new Random(1234)));
    }

    @Override
    public Optional<Quote> getQuote(String token) {
        return Optional.ofNullable(map.get(token));
    }

    @Override
    public void putQuote(Quote quote) {
    }

    private static Map<String, Quote> generateQuotes(Random random) {
        String[] symbols = new String[]{"AAA", "BBB", "CCC", "DDD", "EEE", "FFF", "ABC", "DEF", "XYZ"};
        Map<String, Quote> quotes = new HashMap<>();
        for (int i = 0; i < symbols.length; i++) {
            quotes.put(symbols[i], new Quote(symbols[i],
                random.nextInt(100), random.nextInt(100), random.nextInt(100), random.nextInt(100),
                random.nextLong(1000), random.nextLong(1000)
            ));
        }
        return quotes;
    }
    
}
