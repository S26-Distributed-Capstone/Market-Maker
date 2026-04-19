package edu.yu.marketmaker.cluster;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Operator-tunable cluster settings. Loaded from
 * {@code application-market-maker-node.properties}, overridable via env vars
 * (e.g. {@code ZOOKEEPER_CONNECT}). Defaults work out-of-the-box in the
 * Compose stack.
 */
@ConfigurationProperties(prefix = "cluster")
public class ClusterProperties {

    private String zookeeperConnect = "zookeeper:2181";
    private String zkBasePath = "/marketmaker";
    private String symbolsSeedFile = "/config/symbols.txt";
    private int sessionTimeoutMs = 10_000;
    private int connectionTimeoutMs = 5_000;

    /** @return the ZK connection string ({@code host:port[,host:port...]}). */
    public String getZookeeperConnect() {
        return zookeeperConnect;
    }

    public void setZookeeperConnect(String zookeeperConnect) {
        this.zookeeperConnect = zookeeperConnect;
    }

    /** @return the cluster's root znode path. */
    public String getZkBasePath() {
        return zkBasePath;
    }

    public void setZkBasePath(String zkBasePath) {
        this.zkBasePath = zkBasePath;
    }

    /** @return path to the symbol seed file. */
    public String getSymbolsSeedFile() {
        return symbolsSeedFile;
    }

    public void setSymbolsSeedFile(String symbolsSeedFile) {
        this.symbolsSeedFile = symbolsSeedFile;
    }

    /** @return ZK session timeout in ms (governs how fast ephemeral znodes expire on node death). */
    public int getSessionTimeoutMs() {
        return sessionTimeoutMs;
    }

    public void setSessionTimeoutMs(int sessionTimeoutMs) {
        this.sessionTimeoutMs = sessionTimeoutMs;
    }

    /** @return ZK TCP connection timeout in ms (distinct from session timeout). */
    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }
}
