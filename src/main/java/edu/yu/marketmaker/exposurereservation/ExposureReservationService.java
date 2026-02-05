package edu.yu.marketmaker.exposurereservation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ExposureReservationService {
    private final ReservationRepository repo;
    // Simple global capacity per symbol mock
    private final Map<String, Long> capacityConfig = new ConcurrentHashMap<>();
    private static final long DEFAULT_CAPACITY = 10000;

    public ExposureReservationService(ReservationRepository repo) {
        this.repo = repo;
    }

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

    public synchronized long applyFill(UUID id, long filledQty) {
        Reservation r = repo.findById(id).orElseThrow(() -> new RuntimeException("Reservation not found"));
        return r.reduceGrantOnFill(filledQty);
    }

    public synchronized long release(UUID id) {
        Reservation r = repo.findById(id).orElseThrow(() -> new RuntimeException("Reservation not found"));
        return r.releaseRemaining();
    }

    public ExposureState getExposureState() {
        long totalUsage = repo.findAll().stream().mapToLong(Reservation::getGranted).sum();
        int count = (int) repo.findAll().stream().filter(r -> r.getGranted() > 0).count();
        // Assuming single symbol for simple debugging or sum of all default capacities
        return new ExposureState(totalUsage, DEFAULT_CAPACITY, count);
    }
}

