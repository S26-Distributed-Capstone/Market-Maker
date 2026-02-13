package edu.yu.marketmaker.config;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import edu.yu.marketmaker.model.Position;
import edu.yu.marketmaker.state.persistence.JpaPositionRepository;
import edu.yu.marketmaker.state.persistence.PositionMapStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hazelcast configuration for the Market Maker application.
 * Configures an embedded Hazelcast instance with a MapStore
 * backed by PostgreSQL for Position data persistence.
 */
@Configuration
public class HazelcastConfig {

    private static final String POSITIONS_MAP_NAME = "positions";

    @Bean
    public HazelcastInstance hazelcastInstance(JpaPositionRepository repository) {
        Config config = new Config();
        config.setInstanceName("market-maker-hazelcast");

        // Configure the positions map with MapStore
        MapConfig positionsMapConfig = createPositionsMapConfig(repository);
        config.addMapConfig(positionsMapConfig);

        // Network configuration for embedded mode
        configureNetwork(config);

        return Hazelcast.newHazelcastInstance(config);
    }

    private MapConfig createPositionsMapConfig(JpaPositionRepository repository) {
        MapConfig mapConfig = new MapConfig(POSITIONS_MAP_NAME);

        // Configure MapStore
        MapStoreConfig mapStoreConfig = new MapStoreConfig();
        mapStoreConfig.setImplementation(new PositionMapStore(repository));
        mapStoreConfig.setEnabled(true);

        // Write-behind configuration for better performance
        // Writes are batched and flushed to DB asynchronously
        mapStoreConfig.setWriteDelaySeconds(0); // 0 for write-through, >0 for write-behind
        mapStoreConfig.setWriteBatchSize(100);
        mapStoreConfig.setWriteCoalescing(true);

        // Load all keys on startup
        mapStoreConfig.setInitialLoadMode(MapStoreConfig.InitialLoadMode.EAGER);

        mapConfig.setMapStoreConfig(mapStoreConfig);

        // Eviction policy - keep positions in memory
        EvictionConfig evictionConfig = new EvictionConfig();
        evictionConfig.setEvictionPolicy(EvictionPolicy.NONE);
        mapConfig.setEvictionConfig(evictionConfig);

        // Backup configuration
        mapConfig.setBackupCount(1);
        mapConfig.setAsyncBackupCount(0);

        return mapConfig;
    }

    private void configureNetwork(Config config) {
        NetworkConfig networkConfig = config.getNetworkConfig();

        // Join configuration - using multicast for development
        // For production, consider TCP-IP or Kubernetes discovery
        JoinConfig joinConfig = networkConfig.getJoin();
        joinConfig.getMulticastConfig().setEnabled(true);
        joinConfig.getTcpIpConfig().setEnabled(false);
    }

    /**
     * Provides the positions IMap for dependency injection.
     */
    @Bean
    public com.hazelcast.map.IMap<String, Position> positionsMap(HazelcastInstance hazelcastInstance) {
        return hazelcastInstance.getMap(POSITIONS_MAP_NAME);
    }
}

