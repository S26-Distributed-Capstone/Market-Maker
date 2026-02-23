package edu.yu.marketmaker.exchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import edu.yu.marketmaker.model.Fill;

@Component
public class TestFillSender implements FillSender {

    private static final Logger logger = LoggerFactory.getLogger(TestFillSender.class);

    @Override
    public void sendFill(Fill fill) {
        logger.info("Sending {} fill: {} x {} @ {}",
            fill.side(), fill.symbol(), fill.quantity(), fill.price());
    }
    
}
