package edu.yu.marketmaker.exchange;

import edu.yu.marketmaker.model.ExternalOrder;

public interface OrderValidator {
    
    void validateOrder(QuoteRepository repository, ExternalOrder order) throws OrderValidationException;
}
