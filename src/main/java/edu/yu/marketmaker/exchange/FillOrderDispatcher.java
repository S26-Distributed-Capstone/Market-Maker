package edu.yu.marketmaker.exchange;

import org.springframework.stereotype.Component;

import edu.yu.marketmaker.memory.Repository;
import edu.yu.marketmaker.model.ExternalOrder;
import edu.yu.marketmaker.model.Fill;
import edu.yu.marketmaker.model.Quote;
import edu.yu.marketmaker.model.Side;

@Component
public class FillOrderDispatcher implements OrderDispatcher {

    private final Repository<String, Quote> quoteRepository;

    public FillOrderDispatcher(Repository<String, Quote> repository) {
        this.quoteRepository = repository;
    }

    @Override
    public void dispatchOrder(ExternalOrder order) {
        Quote quote = quoteRepository.get(order.symbol()).orElseThrow(() -> new OrderValidationException("Quote " + order.symbol() + " does not exist"));
        long timestamp = System.currentTimeMillis();
        if (timestamp >= quote.expiresAt()) {
            throw new OrderValidationException("Quote " + order.symbol() + " is expired");
        }
        double price = 0.0;
        switch (order.side()) {
            case BUY:
                if (order.limitPrice() > quote.bidPrice()) {
                    throw new OrderValidationException("Limit price is too low");
                } else {
                    price = quote.bidPrice();
                }
                break;
            case SELL:
                if (order.limitPrice() < quote.askPrice()) {
                    throw new OrderValidationException("Limit price is too high");
                } else {
                    price = quote.askPrice();
                }
        }
        int adjustedQuantity = adjustQuantity(quote, order);
        if (adjustedQuantity == 0) {
            throw new OrderValidationException("Order could not be filled");
        }
        Fill fill = new Fill(order.id(), order.symbol(), order.side(), adjustedQuantity, price, null, timestamp);
        // TODO: send fill to trading service
    }

    private int adjustQuantity(Quote quote, ExternalOrder order) {
        int adjustedQuantity = Math.min(order.side() == Side.BUY ? quote.bidQuantity() : quote.askQuantity(), order.quantity());
        quoteRepository.put(updateQuote(quote, order.side(), adjustedQuantity));
        return adjustedQuantity;
    }

    private Quote updateQuote(Quote quote, Side side, int amount) {
        return new Quote(
            quote.symbol(),
            quote.bidPrice(),
            side == Side.BUY ? quote.bidQuantity() - amount : quote.bidQuantity(),
            quote.askPrice(),
            side == Side.SELL ? quote.askQuantity() - amount : quote.askQuantity(),
            quote.quoteId(),
            quote.expiresAt() 
        );
    }
    
}
