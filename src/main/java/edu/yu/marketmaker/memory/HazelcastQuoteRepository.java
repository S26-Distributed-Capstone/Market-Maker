package edu.yu.marketmaker.memory;

import com.hazelcast.map.IMap;
import edu.yu.marketmaker.memory.interfaces.QuoteRepository;
import edu.yu.marketmaker.model.Quote;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Hazelcast-backed implementation of QuoteRepository.
 * Uses a distributed IMap for quote storage with write-through
 * persistence to PostgreSQL via the configured MapStore.
 */
@Repository
public class HazelcastQuoteRepository implements QuoteRepository {

    private final IMap<UUID, Quote> quotesMap;

    public HazelcastQuoteRepository(IMap<UUID, Quote> quotesMap) {
        this.quotesMap = quotesMap;
    }

    @Override
    public Optional<Quote> getQuote(UUID quoteId) {
        return Optional.ofNullable(quotesMap.get(quoteId));
    }

    @Override
    public void putQuote(Quote quote) {
        quotesMap.put(quote.quoteId(), quote);
    }

    @Override
    public Collection<Quote> getAllQuotes() {
        return quotesMap.values();
    }

    @Override
    public void deleteQuote(UUID quoteId) {
        quotesMap.remove(quoteId);
    }
}

