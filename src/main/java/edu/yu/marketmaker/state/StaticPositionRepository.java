package edu.yu.marketmaker.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.yu.marketmaker.model.Position;

@Component
public class StaticPositionRepository implements PositionRepository {

    private final Map<String, Position> map;

    public StaticPositionRepository(Map<String, Position> map) {
        this.map = new HashMap<>(map);
    }

    public StaticPositionRepository() {
        this(generatePositions(new Random(5678)));
    }

    @Override
    public Optional<Position> getPosition(String token) {
        return Optional.ofNullable(map.get(token));
    }

    @Override
    public void putPosition(Position position) {
        map.put(position.symbol(), position);
    }

    @Override
    public java.util.Collection<Position> getAllPositions() {
        return map.values();
    }

    private static Map<String, Position> generatePositions(Random random) {
        String[] symbols = new String[]{"AAA", "BBB", "CCC", "DDD", "EEE", "FFF", "ABC", "DEF", "XYZ"};
        Map<String, Position> positions = new HashMap<>();
        for (int i = 0; i < symbols.length; i++) {
            positions.put(symbols[i], new Position(
                symbols[i],
                random.nextInt(201) - 100, // netQuantity: -100 to +100
                0L, // initial version
                new UUID(0, 0)  // initial lastFillId
            ));
        }
        return positions;
    }
}
