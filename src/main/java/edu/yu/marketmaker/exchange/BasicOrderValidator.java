package edu.yu.marketmaker.exchange;

import org.springframework.stereotype.Component;

import edu.yu.marketmaker.model.ExternalOrder;

@Component
public class BasicOrderValidator implements OrderValidator {

    @Override
    public void validateOrder(ExternalOrder order) throws OrderValidationException {
        if (order.quantity() <= 0 || order.limitPrice() <= 0) {
            throw new OrderValidationException("Invalid quantity or price");
        }
    }
    
}
