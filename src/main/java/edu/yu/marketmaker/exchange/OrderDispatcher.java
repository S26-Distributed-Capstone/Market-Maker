package edu.yu.marketmaker.exchange;

import edu.yu.marketmaker.model.ExternalOrder;

/**
 * Interface for a component that handles an external service
 */
public interface OrderDispatcher {
    
    /**
     * Handle an external order
     * @param order the external order to handle
     */
    void dispatchOrder(ExternalOrder order);
}
