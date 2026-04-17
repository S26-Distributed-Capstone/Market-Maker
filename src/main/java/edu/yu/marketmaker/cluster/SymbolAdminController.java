package edu.yu.marketmaker.cluster;

import org.apache.curator.framework.recipes.leader.Participant;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Operator-facing HTTP API for inspecting cluster state and mutating the
 * cluster's symbol list at runtime.
 *
 * Endpoints:
 * <ul>
 *   <li>{@code GET  /cluster/status}  — answered by every node, returns this
 *       node's view of membership, leadership, and the current symbol list.</li>
 *   <li>{@code POST /cluster/symbols} — leader-only; adds a symbol.</li>
 *   <li>{@code DELETE /cluster/symbols/{symbol}} — leader-only; removes a symbol.</li>
 * </ul>
 *
 * Mutation endpoints called against a non-leader return HTTP 503 with an
 * {@code X-Leader} header naming the current leader's id, so an operator
 * (or a thin client wrapper) can retry against the right node.
 */
@RestController
@RequestMapping("/cluster")
@Profile("market-maker-node")
public class SymbolAdminController {

    private final ClusterNode clusterNode;
    private final ConfigStore configStore;

    /**
     * Inject the cluster identity bean and the symbol-list façade.
     *
     * @param clusterNode this JVM's identity / leadership status
     * @param configStore symbol-list reader/writer backed by ZooKeeper
     */
    public SymbolAdminController(ClusterNode clusterNode, ConfigStore configStore) {
        this.clusterNode = clusterNode;
        this.configStore = configStore;
    }

    /** Request body for {@code POST /cluster/symbols}. */
    public record SymbolRequest(String symbol) {}

    /**
     * Response body for {@code GET /cluster/status}.
     *
     * @param nodeId   this node's cluster id (e.g. "n-0000000003")
     * @param leader   {@code true} if this node currently holds leadership
     * @param leaderId id of the current leader (may be null during a transition)
     * @param members  ids of all live cluster members at the moment of the call
     * @param symbols  the current symbol list as stored in ZooKeeper
     */
    public record StatusView(String nodeId,
                             boolean leader,
                             String leaderId,
                             Set<String> members,
                             List<String> symbols) {}

    /**
     * Snapshot the cluster state as visible from this node. Available on
     * every node (handy for round-robin debugging).
     *
     * @return the current cluster status view
     */
    @GetMapping("/status")
    public StatusView status() {
        return new StatusView(
                clusterNode.getNodeId(),
                clusterNode.isLeader(),
                currentLeaderId(),
                clusterNode.getLiveMembers(),
                configStore.readSymbols()
        );
    }

    /**
     * Append a symbol to the cluster's symbol list. Must be invoked against
     * the current leader; the resulting znode change triggers a rebalance.
     *
     * @param req JSON body containing the symbol to add
     * @return 200 with the resulting symbol list, 400 on missing symbol,
     *         or 503 with an {@code X-Leader} header if this node is not the leader
     */
    @PostMapping("/symbols")
    public ResponseEntity<?> addSymbol(@RequestBody SymbolRequest req) {
        ResponseEntity<?> leaderGuard = requireLeader();
        if (leaderGuard != null) return leaderGuard;
        if (req == null || req.symbol() == null || req.symbol().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "symbol required"));
        }
        boolean added = configStore.addSymbol(req.symbol());
        return ResponseEntity.ok(Map.of(
                "symbol", req.symbol().trim().toUpperCase(),
                "added", added,
                "symbols", configStore.readSymbols()
        ));
    }

    /**
     * Remove a symbol from the cluster's symbol list. Must be invoked against
     * the current leader; the resulting znode change triggers a rebalance.
     *
     * @param symbol the ticker to remove (path variable)
     * @return 200 with the resulting symbol list, or 503 with an {@code X-Leader}
     *         header if this node is not the leader
     */
    @DeleteMapping("/symbols/{symbol}")
    public ResponseEntity<?> removeSymbol(@PathVariable String symbol) {
        ResponseEntity<?> leaderGuard = requireLeader();
        if (leaderGuard != null) return leaderGuard;
        boolean removed = configStore.removeSymbol(symbol);
        return ResponseEntity.ok(Map.of(
                "symbol", symbol.trim().toUpperCase(),
                "removed", removed,
                "symbols", configStore.readSymbols()
        ));
    }

    /**
     * Guard helper for mutation endpoints.
     *
     * @return {@code null} if this node is the leader (caller may proceed),
     *         or a 503 response naming the current leader otherwise
     */
    private ResponseEntity<?> requireLeader() {
        if (clusterNode.isLeader()) return null;
        String leader = currentLeaderId();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("X-Leader", leader == null ? "" : leader)
                .body(Map.of(
                        "error", "not leader",
                        "leader", leader == null ? "" : leader,
                        "thisNode", clusterNode.getNodeId()
                ));
    }

    /**
     * Look up the current leader's participant id from the Curator
     * {@link org.apache.curator.framework.recipes.leader.LeaderLatch}.
     *
     * @return the leader's id, or {@code null} if not currently determinable
     *         (e.g. mid-failover or ZK unreachable)
     */
    private String currentLeaderId() {
        try {
            Collection<Participant> participants = clusterNode.getLeaderLatch().getParticipants();
            if (participants == null) return null;
            return participants.stream()
                    .filter(Participant::isLeader)
                    .map(Participant::getId)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
