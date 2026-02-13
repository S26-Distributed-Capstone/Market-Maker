package edu.yu.marketmaker.memory;

import com.hazelcast.map.IMap;
import edu.yu.marketmaker.memory.interfaces.ReservationRepository;
import edu.yu.marketmaker.model.Reservation;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Hazelcast-backed implementation of ReservationRepository.
 * Uses a distributed IMap for reservation storage with write-through
 * persistence to PostgreSQL via the configured MapStore.
 */
@Repository
public class HazelcastReservationRepository implements ReservationRepository {

    private final IMap<UUID, Reservation> reservationsMap;

    public HazelcastReservationRepository(IMap<UUID, Reservation> reservationsMap) {
        this.reservationsMap = reservationsMap;
    }

    @Override
    public Optional<Reservation> getReservation(UUID id) {
        return Optional.ofNullable(reservationsMap.get(id));
    }

    @Override
    public void putReservation(Reservation reservation) {
        reservationsMap.put(reservation.id(), reservation);
    }

    @Override
    public Collection<Reservation> getAllReservations() {
        return reservationsMap.values();
    }

    @Override
    public void deleteReservation(UUID id) {
        reservationsMap.remove(id);
    }
}

