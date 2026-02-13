package edu.yu.marketmaker.memory;

import com.hazelcast.map.IMap;
import edu.yu.marketmaker.memory.interfaces.FillRepository;
import edu.yu.marketmaker.model.Fill;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Hazelcast-backed implementation of FillRepository.
 * Uses a distributed IMap for fill storage with write-through
 * persistence to PostgreSQL via the configured MapStore.
 */
@Repository
public class HazelcastFillRepository implements FillRepository {

    private final IMap<UUID, Fill> fillsMap;

    public HazelcastFillRepository(IMap<UUID, Fill> fillsMap) {
        this.fillsMap = fillsMap;
    }

    @Override
    public Optional<Fill> getFill(UUID orderId) {
        return Optional.ofNullable(fillsMap.get(orderId));
    }

    @Override
    public void putFill(Fill fill) {
        fillsMap.put(fill.orderId(), fill);
    }

    @Override
    public Collection<Fill> getAllFills() {
        return fillsMap.values();
    }

    @Override
    public void deleteFill(UUID orderId) {
        fillsMap.remove(orderId);
    }
}

