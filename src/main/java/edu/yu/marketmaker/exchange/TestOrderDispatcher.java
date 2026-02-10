package edu.yu.marketmaker.exchange;

import org.springframework.stereotype.Component;

import edu.yu.marketmaker.model.ExternalOrder;

@Component
public class TestOrderDispatcher implements OrderDispatcher {

    @Override
    public void dispatchOrder(ExternalOrder order) {
        // TODO: add logging
        System.out.println(order);
    }
    
}
