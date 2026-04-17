package edu.yu.marketmaker.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The cluster's symbol-to-node assignment engine.
 *
 * The Coordinator is dormant on every JVM until that JVM acquires leadership;
 * at that point it:
 * <ul>
 *   <li>Seeds the symbol znode from {@code symbols.txt} if it is empty.</li>
 *   <li>Begins watching {@code /marketmaker/members} (membership churn) and
 *       {@code /marketmaker/symbols} (operator-driven symbol changes).</li>
 *   <li>On any of those triggers, debounces briefly and then writes per-node
 *       assignment znodes computed by {@link EvenSplitStrategy}.</li>
 *   <li>Prunes assignment znodes left behind by departed nodes.</li>
 * </ul>
 *
 * Per the deployment design (dedicated leader): the leader writes itself an
 * empty assignment so it does not market-make while leading. When leadership
 * is lost, all watches are torn down, leaving the JVM free to re-enter the
 * worker pool on the next rebalance triggered by the new leader.
 */
@Component
@Profile("market-maker-node")
public class Coordinator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(Coordinator.class);
    private static final long DEBOUNCE_MS = 200;

    private final CuratorFramework curator;
    private final ZkPaths paths;
    private final ClusterNode clusterNode;
    private final ConfigStore configStore;
    private final ObjectMapper mapper = new ObjectMapper();

    private final AtomicBoolean leading = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "cluster-coordinator");
                t.setDaemon(true);
                return t;
            });
    private volatile ScheduledFuture<?> pendingRebalance;

    private CuratorCache symbolsCache;
    private CuratorCacheListener membersListener;
    private CuratorCacheListener symbolsListener;

    /**
     * Inject all collaborators.
     *
     * @param curator     started Curator client (used for assignment writes / prunes)
     * @param paths       znode-path helper
     * @param clusterNode this JVM's cluster identity / leadership status holder
     * @param configStore symbol-list façade (used for seeding and reads at rebalance time)
     */
    public Coordinator(CuratorFramework curator,
                       ZkPaths paths,
                       ClusterNode clusterNode,
                       ConfigStore configStore) {
        this.curator = curator;
        this.paths = paths;
        this.clusterNode = clusterNode;
        this.configStore = configStore;
    }

    /**
     * Spring Boot startup hook. Subscribes to leadership transitions on the
     * shared latch and immediately invokes {@link #onAcquireLeadership()} if
     * we are already leader by the time this runs (a benign race during boot).
     *
     * @param args command-line arguments (unused)
     */
    @Override
    public void run(ApplicationArguments args) {
        clusterNode.getLeaderLatch().addListener(new LeaderLatchListener() {
            @Override
            public void isLeader() {
                onAcquireLeadership();
            }

            @Override
            public void notLeader() {
                onLoseLeadership();
            }
        });
        if (clusterNode.getLeaderLatch().hasLeadership()) {
            onAcquireLeadership();
        }
    }

    /**
     * Transition this JVM into the active-leader state: seed the symbol list
     * if needed, attach watchers for membership and symbol-list changes, and
     * schedule the initial rebalance.
     *
     * Idempotent — guarded by an {@link AtomicBoolean} so duplicate
     * notifications cannot double-install watches.
     */
    private void onAcquireLeadership() {
        if (!leading.compareAndSet(false, true)) {
            return;
        }
        log.info("node {} acquired leadership", clusterNode.getNodeId());

        try {
            configStore.seedIfEmpty();
        } catch (Exception e) {
            log.error("failed to seed symbols on leadership acquisition", e);
        }

        this.symbolsCache = CuratorCache.build(curator, paths.symbols());
        this.symbolsListener = (type, oldData, data) -> scheduleRebalance("symbols change: " + type);
        this.symbolsCache.listenable().addListener(symbolsListener);
        this.symbolsCache.start();

        this.membersListener = (type, oldData, data) -> scheduleRebalance("members change: " + type);
        clusterNode.getMembersCache().listenable().addListener(membersListener);

        scheduleRebalance("initial rebalance on leadership acquisition");
    }

    /**
     * Transition out of the leader state: remove watchers, close the symbols
     * cache, and cancel any pending rebalance. Idempotent.
     */
    private void onLoseLeadership() {
        if (!leading.compareAndSet(true, false)) {
            return;
        }
        log.info("node {} lost leadership", clusterNode.getNodeId());

        if (symbolsCache != null) {
            if (symbolsListener != null) {
                symbolsCache.listenable().removeListener(symbolsListener);
            }
            symbolsCache.close();
            symbolsCache = null;
            symbolsListener = null;
        }
        if (membersListener != null) {
            clusterNode.getMembersCache().listenable().removeListener(membersListener);
            membersListener = null;
        }
        if (pendingRebalance != null) {
            pendingRebalance.cancel(false);
            pendingRebalance = null;
        }
    }

    /**
     * Coalesce a burst of triggers into a single rebalance. Cancels any
     * previously scheduled rebalance and re-schedules one {@link #DEBOUNCE_MS}
     * milliseconds out, so a flurry of member/symbol events runs the
     * computation only once.
     *
     * @param reason short human-readable trigger description (logged at rebalance time)
     */
    private synchronized void scheduleRebalance(String reason) {
        if (!leading.get()) return;
        if (pendingRebalance != null) {
            pendingRebalance.cancel(false);
        }
        pendingRebalance = scheduler.schedule(() -> doRebalance(reason), DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Compute the new assignment map and persist it to ZooKeeper. The leader
     * is given an empty list (dedicated-leader mode); the remaining live
     * members share the symbols via {@link EvenSplitStrategy#split}. Stale
     * znodes for departed nodes are pruned at the end.
     *
     * Failures are logged but do not propagate, so a transient ZK error does
     * not kill the scheduler thread.
     *
     * @param reason short human-readable trigger description (for logging)
     */
    private void doRebalance(String reason) {
        if (!leading.get()) return;
        try {
            String leaderId = clusterNode.getNodeId();
            Set<String> members = clusterNode.getLiveMembers();
            if (!members.contains(leaderId)) {
                members = new TreeSet<>(members);
                members.add(leaderId);
            }
            Set<String> workers = new TreeSet<>(members);
            workers.remove(leaderId);
            List<String> symbols = configStore.readSymbols();

            Map<String, List<String>> assignments = EvenSplitStrategy.split(workers, symbols);

            log.info("rebalance ({}): leader={} workers={} symbols={} assignments={}",
                    reason, leaderId, workers, symbols, assignments);

            writeAssignment(leaderId, List.of());
            for (Map.Entry<String, List<String>> e : assignments.entrySet()) {
                writeAssignment(e.getKey(), e.getValue());
            }
            pruneStaleAssignments(members);
        } catch (Exception e) {
            log.error("rebalance failed ({})", reason, e);
        }
    }

    /**
     * Create-or-update the per-node assignment znode with the given symbol list.
     *
     * @param nodeId  cluster member id whose assignment is being written
     * @param symbols the symbols this node should own
     * @throws Exception on serialisation or ZK failure
     */
    private void writeAssignment(String nodeId, List<String> symbols) throws Exception {
        byte[] bytes = mapper.writeValueAsBytes(new ArrayList<>(symbols));
        String path = paths.assignmentFor(nodeId);
        if (curator.checkExists().forPath(path) == null) {
            try {
                curator.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(path, bytes);
                return;
            } catch (KeeperException.NodeExistsException ignored) {
                // fall through to setData
            }
        }
        curator.setData().forPath(path, bytes);
    }

    /**
     * Delete assignment znodes whose owner is no longer in the live member set.
     * Tolerates races (e.g. another rebalance deleting the same node).
     *
     * @param liveMembers the current live member set (anything not in here is stale)
     * @throws Exception on a non-ignored ZK error
     */
    private void pruneStaleAssignments(Set<String> liveMembers) throws Exception {
        List<String> existing;
        try {
            existing = curator.getChildren().forPath(paths.assignments());
        } catch (KeeperException.NoNodeException e) {
            return;
        }
        Set<String> live = new HashSet<>(liveMembers);
        for (String id : existing) {
            if (!live.contains(id)) {
                try {
                    curator.delete().forPath(paths.assignmentFor(id));
                    log.info("pruned stale assignment znode for dead node {}", id);
                } catch (KeeperException.NoNodeException ignored) {
                    // raced with another deletion
                }
            }
        }
    }

    /**
     * Spring lifecycle hook invoked at context shutdown: relinquish the
     * leader role (closes watchers, cancels pending work) and shut down the
     * scheduler thread.
     */
    @PreDestroy
    public void shutdown() {
        onLoseLeadership();
        scheduler.shutdownNow();
    }
}
