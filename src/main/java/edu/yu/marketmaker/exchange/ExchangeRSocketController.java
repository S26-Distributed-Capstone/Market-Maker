package edu.yu.marketmaker.exchange;

import edu.yu.marketmaker.memory.Repository;
import edu.yu.marketmaker.model.ExternalOrder;
import edu.yu.marketmaker.model.Quote;
import edu.yu.marketmaker.service.ServiceHealth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.util.Optional;

/**
 * RSocket TCP controller mirroring ExchangeService HTTP endpoints.
 *
 * Routes:
 *   quotes.get.{symbol}  →  GET  /quotes/{symbol}
 *   quotes.put.{symbol}  →  PUT  /quotes/{symbol}
 *   orders.submit        →  POST /orders
 *   health               →  GET  /health
 */
@Controller
@Profile("exchange")
public class ExchangeRSocketController {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRSocketController.class);

    private final Repository<String, Quote> quoteRepository;
    private final OrderDispatcher orderDispatcher;
    private final OrderValidator orderValidator;

    public ExchangeRSocketController(Repository<String, Quote> quoteRepository,
                                     OrderDispatcher orderDispatcher,
                                     OrderValidator orderValidator) {
        this.quoteRepository = quoteRepository;
        this.orderDispatcher = orderDispatcher;
        this.orderValidator = orderValidator;
    }

    // Request-Response: client sends symbol, gets Quote back
    @MessageMapping("quotes.get.{symbol}")
    public Quote getQuote(@DestinationVariable String symbol) {
        logger.debug("RSocket getQuote: {}", symbol);
        Optional<Quote> quote = quoteRepository.get(symbol);
        if (quote.isPresent()) {
            return quote.get();
        } else {
            throw new QuoteNotFoundException(symbol);
        }
    }

    // Fire-and-Forget: client sends symbol + Quote, no response
    @MessageMapping("quotes.put.{symbol}")
    public void putQuote(@DestinationVariable String symbol, Quote quote) {
        logger.debug("RSocket putQuote: {}", symbol);
        quoteRepository.put(quote);
    }

    // Fire-and-Forget: client sends ExternalOrder, no response
    @MessageMapping("orders.submit")
    public void submitOrder(ExternalOrder order) {
        logger.debug("RSocket submitOrder: {}", order);
        orderValidator.validateOrder(order);
        orderDispatcher.dispatchOrder(order);
    }

    // Request-Response: client requests health status
    @MessageMapping("health")
    public ServiceHealth getHealth() {
        return new ServiceHealth(true, 0, "Exchange Service");
    }
}