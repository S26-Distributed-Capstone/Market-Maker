package edu.yu.marketmaker.cluster;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link EvenSplitStrategy}. The strategy is intentionally
 * a pure function so it can be exercised without ZooKeeper, Spring, or any
 * other infrastructure — these tests pin down the behaviours the Coordinator
 * relies on at runtime: even sharing, deterministic output, and well-defined
 * empty-input handling.
 */
class EvenSplitStrategyTest {

    /**
     * When the symbol count divides the worker count, every worker should
     * receive exactly the same number of symbols.
     */
    @Test
    void splitsEvenlyWhenDivisible() {
        Map<String, List<String>> out = EvenSplitStrategy.split(
                List.of("n1", "n2"),
                List.of("AAPL", "MSFT", "GOOG", "TSLA")
        );
        assertEquals(2, out.get("n1").size());
        assertEquals(2, out.get("n2").size());
    }

    /**
     * The output must be a deterministic function of the input sets — input
     * iteration order should not matter and the total symbol count must be
     * preserved, even when the split is uneven.
     */
    @Test
    void distributesRemainderDeterministically() {
        Map<String, List<String>> a = EvenSplitStrategy.split(
                List.of("n3", "n1", "n2"),
                List.of("D", "A", "C", "B")
        );
        Map<String, List<String>> b = EvenSplitStrategy.split(
                List.of("n1", "n2", "n3"),
                List.of("A", "B", "C", "D")
        );
        assertEquals(a, b);
        int total = a.values().stream().mapToInt(List::size).sum();
        assertEquals(4, total);
    }

    /**
     * With no workers there is nowhere to put symbols — the strategy returns
     * an empty map rather than throwing, so the Coordinator can call it
     * during transient single-node startup states.
     */
    @Test
    void emptyWorkersReturnsEmptyMap() {
        Map<String, List<String>> out = EvenSplitStrategy.split(List.of(), List.of("AAPL"));
        assertTrue(out.isEmpty());
    }

    /**
     * With workers but no symbols, every worker must still appear in the
     * map (with an empty list) so the Coordinator can write empty-assignment
     * znodes that drain the workers cleanly.
     */
    @Test
    void emptySymbolsYieldsEmptyListsPerWorker() {
        Map<String, List<String>> out = EvenSplitStrategy.split(List.of("n1", "n2"), List.of());
        assertEquals(Set.of("n1", "n2"), out.keySet());
        assertTrue(out.get("n1").isEmpty());
        assertTrue(out.get("n2").isEmpty());
    }

    /**
     * {@link EvenSplitStrategy#unchangedWorkers} must return only the workers
     * whose assigned list is element-wise identical between snapshots — a
     * worker whose list grew or shrank (even by one symbol) is changed.
     */
    @Test
    void unchangedWorkersIdentifiesStableAssignments() {
        Map<String, List<String>> prev = Map.of(
                "n1", List.of("A", "B"),
                "n2", List.of("C")
        );
        Map<String, List<String>> next = Map.of(
                "n1", List.of("A", "B"),
                "n2", List.of("C", "D")
        );
        assertEquals(Set.of("n1"), EvenSplitStrategy.unchangedWorkers(prev, next));
    }
}
