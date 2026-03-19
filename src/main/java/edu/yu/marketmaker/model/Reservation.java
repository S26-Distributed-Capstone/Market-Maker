package edu.yu.marketmaker.model;

import java.io.Serializable;
import java.util.UUID;

public record Reservation(UUID id, String symbol, int requestedBid, int grantedBid, int requestedAsk, int grantedAsk, ReservationStatus status) implements Identifiable<UUID>, Serializable {

    @Override
    public UUID getId() {
        return id;
    }
}