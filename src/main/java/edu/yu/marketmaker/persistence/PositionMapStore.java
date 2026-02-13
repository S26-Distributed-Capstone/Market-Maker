package edu.yu.marketmaker.persistence;

import com.hazelcast.map.MapStore;
import edu.yu.marketmaker.model.Position;
import edu.yu.marketmaker.persistence.interfaces.JpaPositionRepository;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hazelcast MapStore implementation for Position records.
 * This class bridges the Hazelcast IMap with PostgreSQL persistence
 * by converting between Position records and PositionEntity objects.
 */
public class PositionMapStore implements MapStore<String, Position> {

    private final JpaPositionRepository repository;

    public PositionMapStore(JpaPositionRepository repository) {
        this.repository = repository;
    }

    // --- MapStore Write Methods ---

    @Override
    public void store(String key, Position position) {
        PositionEntity entity = PositionEntity.fromRecord(position);
        repository.save(entity);
    }

    @Override
    public void storeAll(Map<String, Position> map) {
        var entities = map.values().stream()
                .map(PositionEntity::fromRecord)
                .collect(Collectors.toList());
        repository.saveAll(entities);
    }

    @Override
    public void delete(String key) {
        repository.deleteById(key);
    }

    @Override
    public void deleteAll(Collection<String> keys) {
        repository.deleteAllById(keys);
    }

    // --- MapLoader Read Methods ---

    @Override
    public Position load(String key) {
        return repository.findById(key)
                .map(PositionEntity::toRecord)
                .orElse(null);
    }

    @Override
    public Map<String, Position> loadAll(Collection<String> keys) {
        return repository.findAllById(keys).stream()
                .collect(Collectors.toMap(
                        PositionEntity::getSymbol,
                        PositionEntity::toRecord
                ));
    }

    @Override
    public Iterable<String> loadAllKeys() {
        return repository.findAll().stream()
                .map(PositionEntity::getSymbol)
                .collect(Collectors.toList());
    }
}

