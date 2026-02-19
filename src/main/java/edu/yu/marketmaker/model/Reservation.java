package edu.yu.marketmaker.model;

import java.io.Serializable;
import java.util.UUID;

public record Reservation(UUID id, String symbol, int requested, int granted, ReservationStatus status) implements Identifiable<UUID>, Serializable {

    @Override
    public UUID getId() {
        return id;
    }
}