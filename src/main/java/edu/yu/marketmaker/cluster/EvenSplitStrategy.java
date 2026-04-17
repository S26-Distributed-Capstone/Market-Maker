package edu.yu.marketmaker.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Pure assignment policy: distribute a set of symbols across a set of worker
 * nodes as evenly as possible, deterministically. The strategy is intentionally
 * stateless and side-effect-free so it can be unit-tested in isolation; the
 * Coordinator wraps it in I/O.
 *
 * "Even split" here means symbols are dealt out round-robin across
 * sorted-by-id workers — assignments differ in size by at most one, and
 * the mapping is a deterministic function of the (workers, symbols) inputs.
 */
public final class EvenSplitStrategy {

    /** Utility class — not instantiable. */
    private EvenSplitStrategy() {}

    /**
     * Distribute {@code symbols} across {@code workers} as evenly as possible.
     * Both inputs are sorted before assignment, so the output is deterministic
     * for any given pair of input sets (regardless of iteration order).
     *
     * @param workers worker node ids eligible to receive assignments (leader excluded by caller)
     * @param symbols symbols to assign across the workers
     * @return map from worker id to assigned symbols; every worker gets an entry
     *         (possibly empty). If {@code workers} is empty the returned map is empty.
     */
    public static Map<String, List<String>> split(Collection<String> workers, Collection<String> symbols) {
        List<String> sortedWorkers = new ArrayList<>(new TreeSet<>(workers));
        List<String> sortedSymbols = new ArrayList<>(new TreeSet<>(symbols));

        Map<String, List<String>> out = new LinkedHashMap<>();
        for (String w : sortedWorkers) {
            out.put(w, new ArrayList<>());
        }
        if (sortedWorkers.isEmpty() || sortedSymbols.isEmpty()) {
            return out;
        }
        for (int i = 0; i < sortedSymbols.size(); i++) {
            String worker = sortedWorkers.get(i % sortedWorkers.size());
            out.get(worker).add(sortedSymbols.get(i));
        }
        return out;
    }

    /**
     * Identify workers whose assignment is identical between two rebalance
     * snapshots — used by callers that want to skip writing unchanged
     * assignment znodes (currently informational; the Coordinator writes
     * unconditionally for simplicity).
     *
     * @param previous the prior assignment map (may omit workers, which are treated as having no symbols)
     * @param next     the new assignment map under consideration
     * @return the set of worker ids whose assigned list did not change
     */
    public static Set<String> unchangedWorkers(Map<String, List<String>> previous, Map<String, List<String>> next) {
        Set<String> unchanged = new TreeSet<>();
        for (Map.Entry<String, List<String>> e : next.entrySet()) {
            List<String> prev = previous.getOrDefault(e.getKey(), Collections.emptyList());
            if (prev.equals(e.getValue())) {
                unchanged.add(e.getKey());
            }
        }
        return unchanged;
    }
}
