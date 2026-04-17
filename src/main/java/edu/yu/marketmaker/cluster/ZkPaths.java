package edu.yu.marketmaker.cluster;

/**
 * Centralised builder for every ZooKeeper path the cluster uses.
 * Holding all paths in one type makes the znode layout easy to audit and
 * prevents drift between the components that read and write the same nodes.
 *
 * Layout under {@code base}:
 * <pre>
 *   {base}/election           — Curator LeaderLatch participants live here
 *   {base}/members            — parent of one ephemeral-sequential znode per live node
 *   {base}/members/n-XXXXX    — a single live node (id = "n-XXXXX")
 *   {base}/symbols            — persistent JSON array: the cluster-wide symbol list
 *   {base}/assignments        — parent of one persistent znode per node
 *   {base}/assignments/n-X    — JSON array of symbols this node should own
 * </pre>
 */
public final class ZkPaths {

    private final String base;

    /**
     * Build a ZkPaths rooted at {@code base}.
     *
     * @param base absolute znode path (must start with '/'), e.g. "/marketmaker"
     * @throws IllegalArgumentException if base is null, blank, or not absolute
     */
    public ZkPaths(String base) {
        if (base == null || base.isBlank() || !base.startsWith("/")) {
            throw new IllegalArgumentException("base path must start with '/'");
        }
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    /** @return the cluster's root znode path (no trailing slash). */
    public String base() {
        return base;
    }

    /** @return the LeaderLatch znode parent — Curator manages its children. */
    public String election() {
        return base + "/election";
    }

    /** @return the parent znode under which each node creates its ephemeral member entry. */
    public String members() {
        return base + "/members";
    }

    /**
     * @return the prefix passed to {@code create().withMode(EPHEMERAL_SEQUENTIAL).forPath(...)};
     *         ZooKeeper appends a monotonically increasing 10-digit suffix to it.
     */
    public String memberNodePrefix() {
        return members() + "/n-";
    }

    /** @return the persistent znode that stores the canonical symbol list as a JSON array. */
    public String symbols() {
        return base + "/symbols";
    }

    /** @return the parent znode under which per-node assignment lists are written. */
    public String assignments() {
        return base + "/assignments";
    }

    /**
     * @param nodeId the cluster member id (e.g. "n-0000000003")
     * @return the absolute znode path that holds that node's assigned symbol list
     */
    public String assignmentFor(String nodeId) {
        return assignments() + "/" + nodeId;
    }
}
