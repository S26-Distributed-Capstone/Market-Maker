package edu.yu.marketmaker.exchange;

import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import edu.yu.marketmaker.memory.Repository;
import edu.yu.marketmaker.model.ExternalOrder;
import edu.yu.marketmaker.model.Quote;
import edu.yu.marketmaker.service.ServiceHealth;

@RestController
@Profile("exchange")
public class ExchangeService {

    private Repository<String, Quote> quoteRepository;
    private OrderDispatcher orderDispatcher;

    public ExchangeService(Repository<String, Quote> quoteRepository, OrderDispatcher orderDispatcher) {
        this.quoteRepository = quoteRepository;
        this.orderDispatcher = orderDispatcher;
    }
    
    @GetMapping("/quotes/{symbol}")
    Quote getQuote(@PathVariable String symbol) {
        Optional<Quote> quote = quoteRepository.get(symbol);
        if (quote.isPresent()) {
            return quote.get();
        } else {
            throw new QuoteNotFoundException(symbol);
        }
        
    }

    @PutMapping("/quotes/{symbol}")
    void putQuote(@PathVariable String symbol, @RequestBody Quote quote) {
        quoteRepository.put(quote);
    }

    @PostMapping("/orders")
    void submitOrder(@RequestBody ExternalOrder order) {
        orderDispatcher.dispatchOrder(order);
    }

    @GetMapping("/health")
    ServiceHealth getHealth() {
        return new ServiceHealth(true, 0, "Exchange Service");
    }
}
