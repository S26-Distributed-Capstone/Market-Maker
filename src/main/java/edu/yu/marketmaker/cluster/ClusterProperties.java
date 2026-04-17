package edu.yu.marketmaker.cluster;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring {@code @ConfigurationProperties} bean carrying every operator-tunable
 * value that the cluster code reads. All settings are sourced from
 * {@code application-market-maker-node.properties} (and may be overridden by
 * environment variables, e.g. {@code ZOOKEEPER_CONNECT}).
 *
 * Defaults are picked so that the application boots cleanly inside the
 * Compose stack without requiring any explicit configuration.
 */
@ConfigurationProperties(prefix = "cluster")
public class ClusterProperties {

    private String zookeeperConnect = "zookeeper:2181";
    private String zkBasePath = "/marketmaker";
    private String symbolsSeedFile = "/config/symbols.txt";
    private int sessionTimeoutMs = 10_000;
    private int connectionTimeoutMs = 5_000;

    /** @return the ZooKeeper connection string ({@code host:port[,host:port...]}). */
    public String getZookeeperConnect() {
        return zookeeperConnect;
    }

    /** @param zookeeperConnect the ZooKeeper connection string. */
    public void setZookeeperConnect(String zookeeperConnect) {
        this.zookeeperConnect = zookeeperConnect;
    }

    /** @return the absolute znode path that the cluster roots itself under. */
    public String getZkBasePath() {
        return zkBasePath;
    }

    /** @param zkBasePath the absolute znode path the cluster roots itself under. */
    public void setZkBasePath(String zkBasePath) {
        this.zkBasePath = zkBasePath;
    }

    /** @return absolute filesystem path to the seed file used to bootstrap the symbol list. */
    public String getSymbolsSeedFile() {
        return symbolsSeedFile;
    }

    /** @param symbolsSeedFile absolute filesystem path to the symbol seed file. */
    public void setSymbolsSeedFile(String symbolsSeedFile) {
        this.symbolsSeedFile = symbolsSeedFile;
    }

    /** @return ZK session timeout in ms (controls how quickly ephemeral znodes vanish on death). */
    public int getSessionTimeoutMs() {
        return sessionTimeoutMs;
    }

    /** @param sessionTimeoutMs ZK session timeout in ms. */
    public void setSessionTimeoutMs(int sessionTimeoutMs) {
        this.sessionTimeoutMs = sessionTimeoutMs;
    }

    /** @return ZK connection (TCP) timeout in ms — distinct from the session timeout. */
    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    /** @param connectionTimeoutMs ZK connection (TCP) timeout in ms. */
    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }
}
