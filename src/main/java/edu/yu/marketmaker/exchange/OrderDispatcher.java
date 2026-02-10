package edu.yu.marketmaker.exchange;

import edu.yu.marketmaker.model.ExternalOrder;

public interface OrderDispatcher {
    
    void dispatchOrder(ExternalOrder order);
}
