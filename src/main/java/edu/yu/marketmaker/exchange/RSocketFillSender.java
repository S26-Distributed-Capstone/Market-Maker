package edu.yu.marketmaker.exchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;

import edu.yu.marketmaker.model.Fill;

@Component
public class RSocketFillSender implements FillSender {
    
    private static final Logger logger = LoggerFactory.getLogger(RSocketFillSender.class);
    private final RSocketRequester requester;

    public RSocketFillSender(RSocketRequester.Builder rsocketRequesterBuilder) {
        this.requester = rsocketRequesterBuilder.tcp("trading-state", 7000);
    }

    @Override
    public void sendFill(Fill fill) {
        logger.info("Sending {} fill: {} x {} @ {}",
            fill.side(), fill.symbol(), fill.quantity(), fill.price());
        requester.route("state.fills").data(fill).send().subscribe();
    }
}
