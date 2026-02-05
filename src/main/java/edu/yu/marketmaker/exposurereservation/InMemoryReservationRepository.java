package edu.yu.marketmaker.exposurereservation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryReservationRepository implements ReservationRepository {
    private final Map<UUID, Reservation> store = new ConcurrentHashMap<>();

    @Override
    public void save(Reservation reservation) {
        store.put(reservation.getId(), reservation);
    }

    @Override
    public Optional<Reservation> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Collection<Reservation> findAll() {
        return store.values();
    }
}

