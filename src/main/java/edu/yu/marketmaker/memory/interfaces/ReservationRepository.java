package edu.yu.marketmaker.memory.interfaces;

import edu.yu.marketmaker.model.Reservation;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {
    Optional<Reservation> getReservation(UUID id);
    void putReservation(Reservation reservation);
    Collection<Reservation> getAllReservations();
    void deleteReservation(UUID id);
}

