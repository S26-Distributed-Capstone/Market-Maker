package edu.yu.marketmaker.exposurereservation;

import java.util.UUID;

/**
 * DTO representing a request to reserve exposure capacity.
 */
class ReservationRequest {
    private String symbol;
    private long quantity;
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public long getQuantity() { return quantity; }
    public void setQuantity(long quantity) { this.quantity = quantity; }
}

/**
 * DTO representing the response to a reservation request.
 */
class ReservationResponse {
    private UUID id;
    private Reservation.Status status;
    private long grantedQuantity;

    public ReservationResponse(UUID id, Reservation.Status status, long grantedQuantity) {
        this.id = id;
        this.status = status;
        this.grantedQuantity = grantedQuantity;
    }
    public UUID getId() { return id; }
    public Reservation.Status getStatus() { return status; }
    public long getGrantedQuantity() { return grantedQuantity; }
}

/**
 * DTO representing a request to apply a fill to a reservation.
 */
class ApplyFillRequest {
    private long filledQuantity;
    public long getFilledQuantity() { return filledQuantity; }
    public void setFilledQuantity(long filledQuantity) { this.filledQuantity = filledQuantity; }
}

/**
 * DTO representing the capacity freed by an operation.
 */
class FreedCapacityResponse {
    private long freedCapacity;
    public FreedCapacityResponse(long freedCapacity) { this.freedCapacity = freedCapacity; }
    public long getFreedCapacity() { return freedCapacity; }
}

/**
 * DTO representing the global exposure state snapshot.
 */
class ExposureState {
    private long currentUsage;
    private long totalCapacity;
    private int activeReservations;

    public ExposureState(long currentUsage, long totalCapacity, int activeReservations) {
        this.currentUsage = currentUsage;
        this.totalCapacity = totalCapacity;
        this.activeReservations = activeReservations;
    }
    public long getCurrentUsage() { return currentUsage; }
    public long getTotalCapacity() { return totalCapacity; }
    public int getActiveReservations() { return activeReservations; }
}
