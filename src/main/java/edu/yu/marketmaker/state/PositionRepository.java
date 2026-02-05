package edu.yu.marketmaker.state;

import edu.yu.marketmaker.model.Position;

import java.util.Collection;
import java.util.Optional;

public interface PositionRepository {
    Optional<Position> getPosition(String token);
    void putPosition(Position position);
    Collection<Position> getAllPositions();
}
