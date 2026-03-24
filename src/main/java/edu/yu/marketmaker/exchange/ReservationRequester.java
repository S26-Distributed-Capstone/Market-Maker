package edu.yu.marketmaker.exchange;

import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;

import edu.yu.marketmaker.model.Quote;

@Component
public class ReservationRequester {
    
    private final RSocketRequester requester;

    public ReservationRequester(RSocketRequester.Builder builder) {
        this.requester = builder.tcp("exposure-reservation", 7000);
    }

    public void sendReservation(Quote quote) {
        requester.route("reservations").data(quote).send().subscribe();
    }
}
