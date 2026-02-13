package edu.yu.marketmaker.memory.interfaces;

import edu.yu.marketmaker.model.ExternalOrder;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ExternalOrderRepository {
    Optional<ExternalOrder> getExternalOrder(UUID id);
    void putExternalOrder(UUID id, ExternalOrder order);
    Collection<ExternalOrder> getAllExternalOrders();
    void deleteExternalOrder(UUID id);
}

