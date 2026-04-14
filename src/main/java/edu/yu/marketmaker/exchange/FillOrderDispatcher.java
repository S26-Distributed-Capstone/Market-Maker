package edu.yu.marketmaker.exchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import edu.yu.marketmaker.memory.Repository;
import edu.yu.marketmaker.model.ExternalOrder;
import edu.yu.marketmaker.model.Fill;
import edu.yu.marketmaker.model.Quote;
import edu.yu.marketmaker.model.Side;

@Component
public class FillOrderDispatcher implements OrderDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(FillOrderDispatcher.class);

    private final Repository<String, Quote> quoteRepository;
    private final FillSender fillSender;

    public FillOrderDispatcher(Repository<String, Quote> repository, FillSender fillSender) {
        this.quoteRepository = repository;
        this.fillSender = fillSender;
    }

    @Override
    public void dispatchOrder(ExternalOrder order) {
        logger.info("Dispatching {} order: {} x {} @ {}",
            order.side(), order.symbol(), order.quantity(), order.limitPrice());
        Quote quote = quoteRepository.get(order.symbol()).orElseThrow(() -> new OrderValidationException("Quote " + order.symbol() + " does not exist"));
        long timestamp = System.currentTimeMillis();
        if (timestamp >= quote.expiresAt()) {
            throw new OrderValidationException("Quote " + order.symbol() + " is expired");
        }
        double price = 0.0;
        switch (order.side()) {
            case BUY:
                if (order.limitPrice() < quote.askPrice()) {
                    throw new OrderValidationException("Limit price too low to cross ask");
                } else {
                    price = quote.askPrice();
                }
                break;
            case SELL:
                if (order.limitPrice() > quote.bidPrice()) {
                    throw new OrderValidationException("Limit price too high to cross bid");
                } else {
                    price = quote.bidPrice();
                }
        }
        int adjustedQuantity = adjustQuantity(quote, order);
        if (adjustedQuantity == 0) {
            throw new OrderValidationException("Order could not be filled");
        }
        Fill fill = new Fill(order.id(), order.symbol(), order.side(), adjustedQuantity, price, quote.quoteId(), timestamp);
        fillSender.sendFill(fill);
    }

    private int adjustQuantity(Quote quote, ExternalOrder order) {
        int adjustedQuantity = Math.min(order.side() == Side.BUY ? quote.askQuantity() : quote.bidQuantity(), order.quantity());
        quoteRepository.put(updateQuote(quote, order.side(), adjustedQuantity));
        return adjustedQuantity;
    }

    private Quote updateQuote(Quote quote, Side side, int amount) {
        return new Quote(
                quote.symbol(),
                quote.bidPrice(),
                side == Side.SELL ? quote.bidQuantity() - amount : quote.bidQuantity(),
                quote.askPrice(),
                side == Side.BUY ? quote.askQuantity() - amount : quote.askQuantity(),
                quote.quoteId(),
                quote.expiresAt()
        );
    }
}