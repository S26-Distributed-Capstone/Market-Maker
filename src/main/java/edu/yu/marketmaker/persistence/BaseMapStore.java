package edu.yu.marketmaker.persistence;

import com.hazelcast.map.MapStore;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generic Hazelcast MapStore implementation that bridges Hazelcast IMap with JPA persistence.
 * This class eliminates boilerplate by providing a reusable implementation for all entity types.
 *
 * <p>Usage example:
 * <pre>{@code
 * BaseMapStore<UUID, Quote, QuoteEntity> mapStore = new BaseMapStore<>(
 *     jpaRepository,
 *     QuoteEntity::fromRecord,
 *     QuoteEntity::toRecord
 * );
 * }</pre>
 *
 * @param <K> the key type (e.g., UUID, String)
 * @param <R> the record/value type stored in Hazelcast (e.g., Quote, Fill)
 * @param <E> the JPA entity type (e.g., QuoteEntity, FillEntity)
 */
public class BaseMapStore<K, R, E extends ConvertibleEntity<R, K>> implements MapStore<K, R> {

    private final BaseJpaRepository<E, K> repository;
    private final Function<R, E> toEntity;
    private final Function<E, R> toRecord;

    /**
     * Creates a new BaseMapStore.
     *
     * @param repository the JPA repository for persistence operations
     * @param toEntity   function to convert a record to an entity (e.g., Entity::fromRecord)
     * @param toRecord   function to convert an entity to a record (e.g., Entity::toRecord)
     */
    public BaseMapStore(BaseJpaRepository<E, K> repository,
                        Function<R, E> toEntity,
                        Function<E, R> toRecord) {
        this.repository = repository;
        this.toEntity = toEntity;
        this.toRecord = toRecord;
    }

    // --- MapStore Write Methods ---

    @Override
    public void store(K key, R value) {
        E entity = toEntity.apply(value);
        repository.save(entity);
    }

    @Override
    public void storeAll(Map<K, R> map) {
        var entities = map.values().stream()
                .map(toEntity)
                .collect(Collectors.toList());
        repository.saveAll(entities);
    }

    @Override
    public void delete(K key) {
        repository.deleteById(key);
    }

    @Override
    public void deleteAll(Collection<K> keys) {
        repository.deleteAllById(keys);
    }

    // --- MapLoader Read Methods ---

    @Override
    public R load(K key) {
        return repository.findById(key)
                .map(toRecord)
                .orElse(null);
    }

    @Override
    public Map<K, R> loadAll(Collection<K> keys) {
        return repository.findAllById(keys).stream()
                .collect(Collectors.toMap(
                        ConvertibleEntity::getId,
                        toRecord
                ));
    }

    @Override
    public Iterable<K> loadAllKeys() {
        return repository.findAll().stream()
                .map(ConvertibleEntity::getId)
                .collect(Collectors.toList());
    }
}

