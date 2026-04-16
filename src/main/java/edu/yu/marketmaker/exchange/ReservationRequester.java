package edu.yu.marketmaker.exchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;

import edu.yu.marketmaker.model.Quote;
import edu.yu.marketmaker.model.ReservationResponse;

@Component
public class ReservationRequester {
    
    private final RSocketRequester requester;
    private final Logger logger;

    public ReservationRequester(RSocketRequester.Builder builder) {
        this.requester = builder.tcp("exposure-reservation", 7000);
        this.logger = LoggerFactory.getLogger(ReservationRequester.class);
    }

    public void sendReservation(Quote quote) {
        logger.info("Sending initial reservation for: {}", quote.symbol());
        requester.route("reservations").data(quote).retrieveMono(ReservationResponse.class).block();;
    }
}
