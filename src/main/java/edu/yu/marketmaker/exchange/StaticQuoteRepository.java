package edu.yu.marketmaker.exchange;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import edu.yu.marketmaker.memory.Repository;
import edu.yu.marketmaker.model.Quote;

@Component
@Profile("testing")
public class StaticQuoteRepository implements Repository<String, Quote> {

    private final Map<String, Quote> map;

    public StaticQuoteRepository(Map<String, Quote> map) {
        this.map = new HashMap<>(map);
    }

    public StaticQuoteRepository() {
        this(generateQuotes(new Random(1234)));
    }

    private static Map<String, Quote> generateQuotes(Random random) {
        String[] symbols = new String[]{"AAA", "BBB", "CCC", "DDD", "EEE", "FFF", "ABC", "DEF", "XYZ"};
        Map<String, Quote> quotes = new HashMap<>();
        for (int i = 0; i < symbols.length; i++) {
            quotes.put(symbols[i], new Quote(symbols[i],
                random.nextInt(100), random.nextInt(100), random.nextInt(100), random.nextInt(100),
                UUID.randomUUID(), random.nextLong(1000)
            ));
        }
        return quotes;
    }

    @Override
    public Optional<Quote> get(String id) {
        return Optional.ofNullable(map.get(id));
    }

    @Override
    public void put(Quote entity) {
        map.put(entity.getId(), entity);
    }

    @Override
    public Collection<Quote> getAll() {
        return map.values();
    }

    @Override
    public void delete(String id) {
        map.remove(id);
    }
    
}
