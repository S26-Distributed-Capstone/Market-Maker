package edu.yu.marketmaker.memory;

import edu.yu.marketmaker.model.Identifiable;

import java.util.Collection;
import java.util.Optional;

/**
 * Generic repository interface for entities that implement Identifiable.
 *
 * @param <K> the key type
 * @param <T> the entity type, must implement Identifiable<K>
 */
public interface Repository<K, T extends Identifiable<K>> {
    Optional<T> get(K id);
    void put(T entity);
    Collection<T> getAll();
    void delete(K id);
}
