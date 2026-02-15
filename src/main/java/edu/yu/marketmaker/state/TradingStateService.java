package edu.yu.marketmaker.state;

import edu.yu.marketmaker.memory.Repository;
import edu.yu.marketmaker.model.Fill;
import edu.yu.marketmaker.model.Position;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Optional;


@RestController
@Profile("trading-state")
public class TradingStateService {

    private final Repository<String, Position> positionRepository;

    public TradingStateService(Repository<String, Position> positionRepository) {
        this.positionRepository = positionRepository;
    }

    /**
     * Method to submit a fill
     * @param fill
     */
    @PostMapping("/state/fills")
    void submitFill(@RequestBody Fill fill) {
        // TODO: Implement fill processing logic
    }

    /**
     * Get all current positions
     * @return a collection of positions
     */
    @GetMapping("/positions")
    Collection<Position> getAllPositions() {
        return positionRepository.getAll();
    }

    /**
     * Get a specific position based on inputted symbol
     * @param symbol
     * @return
     */
    @GetMapping("/positions/{symbol}")
    Optional<Position> getPosition(@PathVariable String symbol) {
        return positionRepository.get(symbol);
    }

}
