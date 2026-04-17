package edu.yu.marketmaker.cluster;

import jakarta.annotation.PreDestroy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The cluster-membership and identity primitive for a single market-maker JVM.
 *
 * On startup this bean:
 * <ol>
 *   <li>Ensures the cluster's base znodes exist in ZooKeeper.</li>
 *   <li>Creates an ephemeral-sequential znode under {@code /marketmaker/members/};
 *       its short name (e.g. {@code n-0000000004}) becomes the node's
 *       cluster-wide identity for the lifetime of the JVM.</li>
 *   <li>Joins a Curator {@link LeaderLatch} so exactly one node may be elected
 *       leader at any time.</li>
 *   <li>Starts a {@link CuratorCache} over {@code /marketmaker/members} that
 *       other components (notably the Coordinator) listen to.</li>
 * </ol>
 *
 * Liveness is provided entirely by ZooKeeper's session-based ephemeral nodes —
 * there is no separate heartbeat scheme — so when this JVM dies, ZK reaps both
 * the member znode and the latch entry on session timeout.
 */
@Component
@Profile("market-maker-node")
public class ClusterNode implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ClusterNode.class);

    private final CuratorFramework curator;
    private final ZkPaths paths;

    private volatile String nodeId;
    private volatile String memberZnodePath;
    private LeaderLatch leaderLatch;
    private CuratorCache membersCache;

    /**
     * Inject the started Curator client and the path helper.
     *
     * @param curator started {@link CuratorFramework} client
     * @param paths   znode-path helper for the cluster's base path
     */
    public ClusterNode(CuratorFramework curator, ZkPaths paths) {
        this.curator = curator;
        this.paths = paths;
    }

    /**
     * Spring Boot {@link ApplicationRunner} entry point. Performs the
     * one-shot membership registration and starts the leader latch.
     * Runs once after the application context has fully refreshed.
     *
     * @param args command-line arguments (unused)
     * @throws Exception if any ZooKeeper operation fails
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        ensurePath(paths.base());
        ensurePath(paths.members());
        ensurePath(paths.assignments());

        this.memberZnodePath = curator.create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(paths.memberNodePrefix(), new byte[0]);
        this.nodeId = memberZnodePath.substring(memberZnodePath.lastIndexOf('/') + 1);
        log.info("registered cluster member id={} at {}", nodeId, memberZnodePath);

        this.membersCache = CuratorCache.build(curator, paths.members());
        this.membersCache.start();

        this.leaderLatch = new LeaderLatch(curator, paths.election(), nodeId);
        this.leaderLatch.start();
        log.info("leader latch started for nodeId={}", nodeId);
    }

    /**
     * @return this JVM's cluster-wide id (the ephemeral-sequential suffix assigned by ZK)
     * @throws IllegalStateException if called before {@link #run(ApplicationArguments)}
     */
    public String getNodeId() {
        if (nodeId == null) {
            throw new IllegalStateException("cluster node not yet registered");
        }
        return nodeId;
    }

    /**
     * @return {@code true} if this JVM currently holds the leader latch
     */
    public boolean isLeader() {
        return leaderLatch != null && leaderLatch.hasLeadership();
    }

    /**
     * @return the underlying Curator {@link LeaderLatch}, exposed so other
     *         components can attach a {@code LeaderLatchListener} or query
     *         participants. Never null after {@link #run(ApplicationArguments)}.
     */
    public LeaderLatch getLeaderLatch() {
        return leaderLatch;
    }

    /**
     * @return the {@link CuratorCache} that mirrors {@code /marketmaker/members};
     *         the Coordinator attaches a listener here to detect membership churn.
     *         Never null after {@link #run(ApplicationArguments)}.
     */
    public CuratorCache getMembersCache() {
        return membersCache;
    }

    /**
     * Read the live member set directly from ZooKeeper (bypassing the cache).
     * Used by the Coordinator at rebalance time to get an authoritative snapshot.
     *
     * @return the set of live member ids, sorted lexicographically
     * @throws ClusterException if ZooKeeper cannot be queried
     */
    public Set<String> getLiveMembers() {
        Set<String> ids = new TreeSet<>();
        try {
            List<String> children = curator.getChildren().forPath(paths.members());
            ids.addAll(children);
        } catch (KeeperException.NoNodeException e) {
            return ids;
        } catch (Exception e) {
            throw new ClusterException("failed to list members", e);
        }
        return ids;
    }

    /**
     * Idempotently create a persistent znode at {@code path} (and its parents).
     * Tolerates concurrent creation by other nodes.
     *
     * @param path absolute znode path to ensure
     * @throws Exception if a non-ignored ZK error occurs
     */
    private void ensurePath(String path) throws Exception {
        if (curator.checkExists().forPath(path) == null) {
            try {
                curator.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
            } catch (KeeperException.NodeExistsException ignored) {
                // another node beat us to it; fine
            }
        }
    }

    /**
     * Spring lifecycle hook invoked at context shutdown. Closes the latch and
     * the members cache, then proactively deletes our member znode so peers
     * see us leave immediately rather than waiting for the ZK session timeout.
     */
    @PreDestroy
    public void shutdown() {
        log.info("cluster node shutting down");
        try {
            if (leaderLatch != null) {
                leaderLatch.close(LeaderLatch.CloseMode.NOTIFY_LEADER);
            }
        } catch (Exception e) {
            log.warn("error closing leader latch", e);
        }
        try {
            if (membersCache != null) {
                membersCache.close();
            }
        } catch (Exception e) {
            log.warn("error closing members cache", e);
        }
        try {
            if (memberZnodePath != null && curator.getZookeeperClient().isConnected()) {
                curator.delete().guaranteed().forPath(memberZnodePath);
            }
        } catch (Exception e) {
            log.debug("member znode delete on shutdown failed (likely already gone)", e);
        }
    }
}
