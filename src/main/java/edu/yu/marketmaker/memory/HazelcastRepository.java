package edu.yu.marketmaker.memory;

import com.hazelcast.map.IMap;
import edu.yu.marketmaker.model.Identifiable;

import java.util.Collection;
import java.util.Optional;

/**
 * Generic Hazelcast-backed implementation of Repository.
 * Uses a distributed IMap for storage with write-through
 * persistence to the database via the configured MapStore.
 *
 * @param <K> the key type
 * @param <T> the entity type, must implement Identifiable<K>
 */
public class HazelcastRepository<K, T extends Identifiable<K>> implements Repository<K, T> {

    private final IMap<K, T> map;

    public HazelcastRepository(IMap<K, T> map) {
        this.map = map;
    }

    @Override
    public Optional<T> get(K id) {
        return Optional.ofNullable(map.get(id));
    }

    @Override
    public void put(T entity) {
        map.put(entity.getId(), entity);
    }

    @Override
    public Collection<T> getAll() {
        return map.values();
    }

    @Override
    public void delete(K id) {
        map.remove(id);
    }

    /**
     * Returns the underlying IMap for advanced operations.
     * @return the Hazelcast IMap
     */
    protected IMap<K, T> getMap() {
        return map;
    }
}

