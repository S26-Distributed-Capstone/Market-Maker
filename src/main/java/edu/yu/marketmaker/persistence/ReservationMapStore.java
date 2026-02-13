package edu.yu.marketmaker.persistence;

import com.hazelcast.map.MapStore;
import edu.yu.marketmaker.model.Reservation;
import edu.yu.marketmaker.persistence.interfaces.JpaReservationRepository;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Hazelcast MapStore implementation for Reservation records.
 * This class bridges the Hazelcast IMap with PostgreSQL persistence
 * by converting between Reservation records and ReservationEntity objects.
 */
public class ReservationMapStore implements MapStore<UUID, Reservation> {

    private final JpaReservationRepository repository;

    public ReservationMapStore(JpaReservationRepository repository) {
        this.repository = repository;
    }

    // --- MapStore Write Methods ---

    @Override
    public void store(UUID key, Reservation reservation) {
        ReservationEntity entity = ReservationEntity.fromRecord(reservation);
        repository.save(entity);
    }

    @Override
    public void storeAll(Map<UUID, Reservation> map) {
        var entities = map.values().stream()
                .map(ReservationEntity::fromRecord)
                .collect(Collectors.toList());
        repository.saveAll(entities);
    }

    @Override
    public void delete(UUID key) {
        repository.deleteById(key);
    }

    @Override
    public void deleteAll(Collection<UUID> keys) {
        repository.deleteAllById(keys);
    }

    // --- MapLoader Read Methods ---

    @Override
    public Reservation load(UUID key) {
        return repository.findById(key)
                .map(ReservationEntity::toRecord)
                .orElse(null);
    }

    @Override
    public Map<UUID, Reservation> loadAll(Collection<UUID> keys) {
        return repository.findAllById(keys).stream()
                .collect(Collectors.toMap(
                        ReservationEntity::getId,
                        ReservationEntity::toRecord
                ));
    }

    @Override
    public Iterable<UUID> loadAllKeys() {
        return repository.findAll().stream()
                .map(ReservationEntity::getId)
                .collect(Collectors.toList());
    }
}

