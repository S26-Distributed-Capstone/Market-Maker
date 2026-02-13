package edu.yu.marketmaker.memory.interfaces;

import edu.yu.marketmaker.model.Fill;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface FillRepository {
    Optional<Fill> getFill(UUID orderId);
    void putFill(Fill fill);
    Collection<Fill> getAllFills();
    void deleteFill(UUID orderId);
}

