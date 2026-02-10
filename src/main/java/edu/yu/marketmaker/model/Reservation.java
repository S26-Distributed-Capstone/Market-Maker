package edu.yu.marketmaker.model;

import java.util.UUID;

public record Reservation (UUID id, String symbol, long requested, long granted, ReservationStatus status) {

}