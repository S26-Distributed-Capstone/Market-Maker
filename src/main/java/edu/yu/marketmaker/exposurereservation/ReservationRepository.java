package edu.yu.marketmaker.exposurereservation;

import edu.yu.marketmaker.model.Reservation;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {
    void save(Reservation reservation);
    Optional<Reservation> findById(UUID id);
    Collection<Reservation> findAll();
}