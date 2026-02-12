package edu.yu.marketmaker.model;

import java.util.UUID;

public record Reservation (UUID id, String symbol, int requested, int granted, ReservationStatus status) {

}