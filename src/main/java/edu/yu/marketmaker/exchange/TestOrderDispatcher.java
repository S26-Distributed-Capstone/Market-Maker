package edu.yu.marketmaker.exchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import edu.yu.marketmaker.model.ExternalOrder;

@Component
public class TestOrderDispatcher implements OrderDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(TestOrderDispatcher.class);

    @Override
    public void dispatchOrder(ExternalOrder order) {
        logger.info("Dispatching {} order: {} x {} @ {}",
            order.side(), order.symbol(), order.quantity(), order.limitPrice());
    }
    
}
