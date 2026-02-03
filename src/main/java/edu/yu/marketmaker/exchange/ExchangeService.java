package edu.yu.marketmaker.exchange;

import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import edu.yu.marketmaker.model.Quote;
import edu.yu.marketmaker.service.ServiceHealth;

@RestController
public class ExchangeService {

    private QuoteRepository quoteRepository;

    public ExchangeService(QuoteRepository quoteRepository) {
        this.quoteRepository = quoteRepository;
    }
    
    @GetMapping("/quotes/{symbol}")
    Quote getQuote(@PathVariable String symbol) {
        Optional<Quote> quote = quoteRepository.getQuote(symbol);
        if (quote.isPresent()) {
            return quote.get();
        } else {
            throw new QuoteNotFoundException(symbol);
        }
        
    }

    @GetMapping("/health")
    ServiceHealth getHealth() {
        return new ServiceHealth(true, 0, "Exchange Service");
    }
}
