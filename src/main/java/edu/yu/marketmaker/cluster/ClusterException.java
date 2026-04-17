package edu.yu.marketmaker.cluster;

/**
 * Unchecked wrapper for failures originating in the cluster layer
 * (ZooKeeper I/O, serialization, seed-file reads, …). Using a single
 * runtime type keeps the cluster API free of checked-exception leakage
 * while still letting callers distinguish cluster faults from generic
 * {@link RuntimeException}s if they choose to.
 */
public class ClusterException extends RuntimeException {

    /**
     * Create a cluster exception with no underlying cause.
     *
     * @param message human-readable description of the failure
     */
    public ClusterException(String message) {
        super(message);
    }

    /**
     * Create a cluster exception that wraps a lower-level cause
     * (typically a {@code KeeperException}, {@code IOException},
     * or interrupted-thread error from Curator).
     *
     * @param message human-readable description of the failure
     * @param cause   the original throwable that triggered this failure
     */
    public ClusterException(String message, Throwable cause) {
        super(message, cause);
    }
}
