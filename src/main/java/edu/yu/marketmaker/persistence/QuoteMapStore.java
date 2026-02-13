package edu.yu.marketmaker.persistence;

import com.hazelcast.map.MapStore;
import edu.yu.marketmaker.model.Quote;
import edu.yu.marketmaker.persistence.interfaces.JpaQuoteRepository;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Hazelcast MapStore implementation for Quote records.
 * This class bridges the Hazelcast IMap with PostgreSQL persistence
 * by converting between Quote records and QuoteEntity objects.
 */
public class QuoteMapStore implements MapStore<UUID, Quote> {

    private final JpaQuoteRepository repository;

    public QuoteMapStore(JpaQuoteRepository repository) {
        this.repository = repository;
    }

    // --- MapStore Write Methods ---

    @Override
    public void store(UUID key, Quote quote) {
        QuoteEntity entity = QuoteEntity.fromRecord(quote);
        repository.save(entity);
    }

    @Override
    public void storeAll(Map<UUID, Quote> map) {
        var entities = map.values().stream()
                .map(QuoteEntity::fromRecord)
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
    public Quote load(UUID key) {
        return repository.findById(key)
                .map(QuoteEntity::toRecord)
                .orElse(null);
    }

    @Override
    public Map<UUID, Quote> loadAll(Collection<UUID> keys) {
        return repository.findAllById(keys).stream()
                .collect(Collectors.toMap(
                        QuoteEntity::getQuoteId,
                        QuoteEntity::toRecord
                ));
    }

    @Override
    public Iterable<UUID> loadAllKeys() {
        return repository.findAll().stream()
                .map(QuoteEntity::getQuoteId)
                .collect(Collectors.toList());
    }
}
