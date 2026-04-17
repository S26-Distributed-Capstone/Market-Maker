package edu.yu.marketmaker.cluster;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Spring {@code @Configuration} that owns the ZooKeeper-side beans
 * required by every cluster component: a started {@link CuratorFramework}
 * client and a {@link ZkPaths} helper rooted at the configured base path.
 *
 * Active only on the {@code market-maker-node} profile so the rest of the
 * application (trading-state, exchange, etc.) does not pull in ZK at boot.
 */
@Configuration
@Profile("market-maker-node")
@EnableConfigurationProperties(ClusterProperties.class)
public class ClusterConfiguration {

    /**
     * Build the {@link ZkPaths} helper from the configured base path.
     *
     * @param props loaded cluster configuration
     * @return a path builder shared by every cluster bean
     */
    @Bean
    public ZkPaths zkPaths(ClusterProperties props) {
        return new ZkPaths(props.getZkBasePath());
    }

    /**
     * Build, start, and register the Curator client for shutdown via
     * {@code destroyMethod = "close"}. Uses an exponential backoff retry
     * policy so transient ZK hiccups don't tear the application down.
     *
     * @param props loaded cluster configuration
     * @return a started {@link CuratorFramework} ready for use
     */
    @Bean(destroyMethod = "close")
    public CuratorFramework curatorFramework(ClusterProperties props) {
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(props.getZookeeperConnect())
                .sessionTimeoutMs(props.getSessionTimeoutMs())
                .connectionTimeoutMs(props.getConnectionTimeoutMs())
                .retryPolicy(new ExponentialBackoffRetry(1_000, 5))
                .namespace(null)
                .build();
        client.start();
        return client;
    }
}
