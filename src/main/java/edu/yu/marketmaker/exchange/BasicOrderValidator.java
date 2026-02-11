package edu.yu.marketmaker.exchange;

import org.springframework.stereotype.Component;

import edu.yu.marketmaker.model.ExternalOrder;

@Component
public class BasicOrderValidator implements OrderValidator {

    @Override
    public void validateOrder(QuoteRepository repository, ExternalOrder order) throws OrderValidationException {
        if (order.symbol() == null || !repository.getQuote(order.symbol()).isPresent()) {
            throw new OrderValidationException("Invalid ticker");
        }
        if (order.quantity() <= 0 || order.limitPrice() <= 0) {
            throw new OrderValidationException("Invalid quantity or price");
        }
    }
    
}
