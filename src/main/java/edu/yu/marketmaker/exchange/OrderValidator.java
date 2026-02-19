package edu.yu.marketmaker.exchange;

import edu.yu.marketmaker.model.ExternalOrder;

public interface OrderValidator {
    
    void validateOrder(ExternalOrder order) throws OrderValidationException;
}
