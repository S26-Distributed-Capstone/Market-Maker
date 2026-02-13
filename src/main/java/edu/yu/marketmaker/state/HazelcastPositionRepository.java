package edu.yu.marketmaker.state;

import com.hazelcast.map.IMap;
import edu.yu.marketmaker.model.Position;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

/**
 * Hazelcast-backed implementation of PositionRepository.
 * Uses a distributed IMap for position storage with write-through
 * persistence to PostgreSQL via the configured MapStore.
 */
@Repository
public class HazelcastPositionRepository implements PositionRepository {

    private final IMap<String, Position> positionsMap;

    public HazelcastPositionRepository(IMap<String, Position> positionsMap) {
        this.positionsMap = positionsMap;
    }

    @Override
    public Optional<Position> getPosition(String symbol) {
        return Optional.ofNullable(positionsMap.get(symbol));
    }

    @Override
    public void putPosition(Position position) {
        positionsMap.put(position.symbol(), position);
    }

    @Override
    public Collection<Position> getAllPositions() {
        return positionsMap.values();
    }
}

