package edu.yu.marketmaker.exposurereservation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for managing exposure limits and reservations.
 * Handles the logic for creating reservations against available capacity,
 * applying fills to reduce open exposure, and releasing unused reservations.
 */
public class ExposureReservationService {
    private final ReservationRepository repo;
    // Simple global capacity per symbol mock
    private final Map<String, Long> capacityConfig = new ConcurrentHashMap<>();
    private static final long DEFAULT_CAPACITY = 10000;

    public ExposureReservationService(ReservationRepository repo) {
        this.repo = repo;
    }

    /**
     * Creates a new exposure reservation.
     * Checks current usage against limits and grants full, partial, or no capacity.
     *
     * @param req The reservation request details.
     * @return The response containing the reservation ID and the actual granted quantity.
     */
    public synchronized ReservationResponse createReservation(ReservationRequest req) {
        long limit = capacityConfig.getOrDefault(req.getSymbol(), DEFAULT_CAPACITY);
        long currentUsage = repo.findAll().stream()
                .filter(r -> r.getSymbol().equals(req.getSymbol()))
                .mapToLong(Reservation::getGranted)
                .sum();

        long available = Math.max(0, limit - currentUsage);
        long granted = Math.min(req.getQuantity(), available);

        Reservation.Status status;
        if (granted == 0) status = Reservation.Status.DENIED;
        else if (granted < req.getQuantity()) status = Reservation.Status.PARTIAL;
        else status = Reservation.Status.GRANTED;

        Reservation r = new Reservation(req.getSymbol(), req.getQuantity(), granted, status);
        repo.save(r);
        return new ReservationResponse(r.getId(), r.getStatus(), r.getGranted());
    }

    /**
     * Applies a fill to an active reservation.
     * When a trade executes, the reserved exposure is converted to actual position (not tracked here),
     * so we free up the reserved capacity corresponding to the fill.
     *
     * @param id The UUID of the reservation.
     * @param filledQty The quantity that was filled.
     * @return The amount of capacity freed by this operation.
     * @throws RuntimeException if the reservation is not found.
     */
    public synchronized long applyFill(UUID id, long filledQty) {
        Reservation r = repo.findById(id).orElseThrow(() -> new RuntimeException("Reservation not found"));
        return r.reduceGrantOnFill(filledQty);
    }

    /**
     * Releases all remaining capacity for a specific reservation.
     * This is typically called when a quote is cancelled.
     *
     * @param id The UUID of the reservation.
     * @return The total capacity freed.
     * @throws RuntimeException if the reservation is not found.
     */
    public synchronized long release(UUID id) {
        Reservation r = repo.findById(id).orElseThrow(() -> new RuntimeException("Reservation not found"));
        return r.releaseRemaining();
    }

    /**
     * Calculates the current system-wide exposure state.
     *
     * @return Snapshot of usage, capacity, and active count.
     */
    public ExposureState getExposureState() {
        long totalUsage = repo.findAll().stream().mapToLong(Reservation::getGranted).sum();
        int count = (int) repo.findAll().stream().filter(r -> r.getGranted() > 0).count();
        // Assuming single symbol for simple debugging or sum of all default capacities
        return new ExposureState(totalUsage, DEFAULT_CAPACITY, count);
    }
}
