package edu.yu.marketmaker.exposurereservation;

import edu.yu.marketmaker.memory.Repository;
import edu.yu.marketmaker.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Service responsible for managing exposure limits and reservations.
 * Handles the logic for creating reservations against available capacity,
 * applying fills to reduce open exposure, and releasing unused reservations.
 */
public class ExposureReservationService {
    private final Logger logger;
    private final Repository<UUID, Reservation> reservations; // Map of id to reservation

    private static final int MAX_RESERVATION_LIMIT = 100; // Absolute max limit for the system

    public ExposureReservationService(Repository<UUID, Reservation> repo) {
        this.reservations = repo;
        this.logger = LoggerFactory.getLogger(ExposureReservationService.class);
    }

    /**
     * Creates a new exposure reservation.
     * Checks current usage against limits and grants full, partial, or no capacity.
     *
     * @param quote The requested quote.
     * @return The response containing the reservation ID and the actual granted quantity.
     */
    public synchronized ReservationResponse createReservation(Quote quote) {
        logger.info("Creating reservation for Symbol: {}, AskQty: {}", quote.symbol(), quote.askQuantity());

        // Calculate total currently granted exposure globally by summing all active reservations
        int currentUsage = reservations.getAll().stream()
                .mapToInt(Reservation::granted)
                .sum();

        logger.debug("Current global exposure usage: {}/{}", currentUsage, MAX_RESERVATION_LIMIT);

        // Calculate what is remaining
        int available = Math.max(0, MAX_RESERVATION_LIMIT - currentUsage);

        // Determine request size (using greater of bid/ask if bidirectional, or just ask as per previous code)
        int requestedQty = Math.max(0, quote.askQuantity());

        // Grant only what is available, never exceeding the limit
        int granted = Math.min(requestedQty, available);

        ReservationStatus status;
        if (granted == 0 && requestedQty > 0) status = ReservationStatus.DENIED;
        else if (granted < requestedQty) status = ReservationStatus.PARTIAL;
        else status = ReservationStatus.GRANTED;

        Reservation r = new Reservation(UUID.randomUUID(), quote.symbol(), requestedQty, granted, status);

        // Save the reservation to "lock in" this exposure usage for future requests
        reservations.put(r);

        logger.info("Reservation result: ID={}, Status={}, Granted={}", r.id(), status, granted);
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
    public synchronized int applyFill(UUID id, int filledQty) {
        logger.info("Applying fill: ID={}, FilledQty={}", id, filledQty);

        Optional<Reservation> r = reservations.get(id); // .get returns the object directly or null based on Repository interface convention used in previous context, adjusting if Optional is used
        if(r.isEmpty()) {
            logger.error("Failed to apply fill: Reservation ID {} not found", id);
            throw new RuntimeException("Reservation not found");
        }

        Reservation reservation = r.get();
        int freed = reduceGrantOnFill(reservation, filledQty);

        logger.info("Fill applied successfully: ID={}, FreedCapacity={}", id, freed);
        return freed;
    }

    /**
     * Releases all remaining capacity for a specific reservation.
     * This is typically called when a quote is cancelled.
     *
     * @param id The UUID of the reservation.
     * @return The total capacity freed.
     * @throws RuntimeException if the reservation is not found.
     */
    public synchronized int release(UUID id) {
        logger.info("Releasing reservation: ID={}", id);

        Optional<Reservation> r = reservations.get(id);
        if(r.isEmpty()) {
            logger.error("Failed to release: Reservation ID {} not found", id);
            throw new RuntimeException("Reservation not found");
        }

        Reservation reservation = r.get();
        int freed = releaseRemaining(reservation);

        logger.info("Reservation released: ID={}, FreedCapacity={}", id, freed);
        return freed;
    }

    /**
     * Reduces the granted (reserved) amount based on a fill.
     * If 100 units are reserved and 50 are filled, the reservation drops to 50.
     *
     * @param r The reservation to modify.
     * @param fillQty The quantity filled.
     * @return The amount of capacity to free (min of current granted and fill quantity).
     */
    private int reduceGrantOnFill(Reservation r, int fillQty) {
        int toFree = Math.min(r.granted(), fillQty);
        int newGranted = r.granted() - toFree;

        logger.debug("Reducing grant (Internal): ID={}, Granted={}, Fill={}, NewGranted={}, Freed={}",
                r.id(), r.granted(), fillQty, newGranted, toFree);

        // Update record
        Reservation updated = new Reservation(r.id(), r.symbol(), r.requested(), newGranted, r.status());
        reservations.put(updated);

        return toFree;
    }

    /**
     * Sets the granted amount to zero, effectively releasing the entire reservation.
     *
     * @param r The reservation to release.
     * @return The amount of capacity that was freed.
     */
    private int releaseRemaining(Reservation r) {
        int toFree = r.granted();

        logger.debug("Releasing remaining (Internal): ID={}, Granted={}, Freed={}", r.id(), r.granted(), toFree);

        // Update record
        Reservation updated = new Reservation(r.id(), r.symbol(), r.requested(), 0, r.status());
        reservations.put(updated);

        return toFree;
    }

    /**
     * Calculates the current system-wide exposure state.
     *
     * @return Snapshot of usage, capacity, and active count.
     */
    public ExposureState getExposureState() {
        int totalUsage = reservations.getAll().stream().mapToInt(Reservation::granted).sum();
        int activeCount = (int) reservations.getAll().stream().filter(r -> r.granted() > 0).count();

        logger.debug("Exposure state: Usage={}/{}, ActiveReservations={}", totalUsage, MAX_RESERVATION_LIMIT, activeCount);
        return new ExposureState(totalUsage, MAX_RESERVATION_LIMIT, activeCount);
    }
}
