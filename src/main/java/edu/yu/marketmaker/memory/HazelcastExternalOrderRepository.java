package edu.yu.marketmaker.memory;

import com.hazelcast.map.IMap;
import edu.yu.marketmaker.memory.interfaces.ExternalOrderRepository;
import edu.yu.marketmaker.model.ExternalOrder;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Hazelcast-backed implementation of ExternalOrderRepository.
 * Uses a distributed IMap for external order storage with write-through
 * persistence to PostgreSQL via the configured MapStore.
 */
@Repository
public class HazelcastExternalOrderRepository implements ExternalOrderRepository {

    private final IMap<UUID, ExternalOrder> externalOrdersMap;

    public HazelcastExternalOrderRepository(IMap<UUID, ExternalOrder> externalOrdersMap) {
        this.externalOrdersMap = externalOrdersMap;
    }

    @Override
    public Optional<ExternalOrder> getExternalOrder(UUID id) {
        return Optional.ofNullable(externalOrdersMap.get(id));
    }

    @Override
    public void putExternalOrder(UUID id, ExternalOrder order) {
        externalOrdersMap.put(id, order);
    }

    @Override
    public Collection<ExternalOrder> getAllExternalOrders() {
        return externalOrdersMap.values();
    }

    @Override
    public void deleteExternalOrder(UUID id) {
        externalOrdersMap.remove(id);
    }
}

