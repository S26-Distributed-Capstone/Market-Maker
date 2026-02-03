package edu.yu.marketmaker.exchange;

import java.util.Optional;

import edu.yu.marketmaker.model.Quote;

public interface QuoteRepository {
    
    Optional<Quote> getQuote(String token);
    void putQuote(Quote quote);
}
