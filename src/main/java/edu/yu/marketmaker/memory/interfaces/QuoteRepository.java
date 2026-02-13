package edu.yu.marketmaker.memory.interfaces;

import edu.yu.marketmaker.model.Quote;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface QuoteRepository {
    Optional<Quote> getQuote(UUID quoteId);
    void putQuote(Quote quote);
    Collection<Quote> getAllQuotes();
    void deleteQuote(UUID quoteId);
}
