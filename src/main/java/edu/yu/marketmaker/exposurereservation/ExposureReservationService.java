package edu.yu.marketmaker.exposurereservation;

import edu.yu.marketmaker.model.*;

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
     * @param quote The requested quote.
     * @return The response containing the reservation ID and the actual granted quantity.
     */
    public synchronized ReservationResponse createReservation(Quote quote) {
        long limit = capacityConfig.getOrDefault(quote.symbol(), DEFAULT_CAPACITY);
        long currentUsage = repo.findAll().stream()
                .filter(r -> r.symbol().equals(quote.symbol()))
                .mapToLong(Reservation::granted)
                .sum();

        long available = Math.max(0, limit - currentUsage);
        long granted = Math.min(quote.askQuantity(), available);

        ReservationStatus status;
        if (granted == 0 && quote.askQuantity() > 0) status = ReservationStatus.DENIED;
        else if (granted < quote.askQuantity()) status = ReservationStatus.PARTIAL;
        else status = ReservationStatus.GRANTED;

        Reservation r = new Reservation(UUID.randomUUID(), quote.symbol(), quote.askQuantity(), granted, status);
        repo.save(r);

        return new ReservationResponse(r.id(), r.status(), r.granted());
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
        return reduceGrantOnFill(r, filledQty);
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
        return releaseRemaining(r);
    }

    /**
     * Reduces the granted (reserved) amount based on a fill.
     * If 100 units are reserved and 50 are filled, the reservation drops to 50.
     *
     * @param r The reservation to modify.
     * @param fillQty The quantity filled.
     * @return The amount of capacity to free (min of current granted and fill quantity).
     */
    private long reduceGrantOnFill(Reservation r, long fillQty) {
        long toFree = Math.min(r.granted(), fillQty);
        long newGranted = r.granted() - toFree;

        // Update record
        Reservation updated = new Reservation(r.id(), r.symbol(), r.requested(), newGranted, r.status());
        repo.save(updated);

        return toFree;
    }

    /**
     * Sets the granted amount to zero, effectively releasing the entire reservation.
     *
     * @param r The reservation to release.
     * @return The amount of capacity that was freed.
     */
    private long releaseRemaining(Reservation r) {
        long toFree = r.granted();

        // Update record
        Reservation updated = new Reservation(r.id(), r.symbol(), r.requested(), 0, r.status());
        repo.save(updated);

        return toFree;
    }

    /**
     * Calculates the current system-wide exposure state.
     *
     * @return Snapshot of usage, capacity, and active count.
     */
    public ExposureState getExposureState() {
        long totalUsage = repo.findAll().stream().mapToLong(Reservation::granted).sum();
        int activeCount = (int) repo.findAll().stream().filter(r -> r.granted() > 0).count();
        // Summing default capacity for illustration, essentially assuming 1 symbol or simplified view
        return new ExposureState(totalUsage, DEFAULT_CAPACITY, activeCount);
    }
}
