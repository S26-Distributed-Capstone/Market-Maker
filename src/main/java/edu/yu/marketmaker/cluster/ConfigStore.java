package edu.yu.marketmaker.cluster;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Read/write façade over the {@code /marketmaker/symbols} znode, which
 * holds the cluster's authoritative ticker list as a JSON array.
 *
 * Responsibilities:
 * <ul>
 *   <li>Serialise/deserialise the symbol list to/from ZooKeeper.</li>
 *   <li>Provide atomic add/remove operations (synchronised at the JVM level —
 *       only the leader mutates the znode at runtime).</li>
 *   <li>On first leadership acquisition, seed the znode from a mounted
 *       {@code symbols.txt} file so the cluster comes up with a sensible
 *       default rather than an empty book.</li>
 * </ul>
 *
 * Symbols are normalised to upper-case on the way in.
 */
@Component
@Profile("market-maker-node")
public class ConfigStore {

    private static final Logger log = LoggerFactory.getLogger(ConfigStore.class);
    private static final TypeReference<List<String>> LIST_OF_STRING = new TypeReference<>() {};

    private final CuratorFramework curator;
    private final ZkPaths paths;
    private final ClusterProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Construct the config store with its ZK and configuration dependencies.
     *
     * @param curator started Curator client
     * @param paths   path helper for the symbols znode
     * @param props   cluster configuration (used for the seed-file location)
     */
    public ConfigStore(CuratorFramework curator, ZkPaths paths, ClusterProperties props) {
        this.curator = curator;
        this.paths = paths;
        this.props = props;
    }

    /**
     * Read the current symbol list from ZooKeeper.
     *
     * @return the symbol list, or an empty list if the znode is missing or empty
     * @throws ClusterException if the znode exists but cannot be read or parsed
     */
    public List<String> readSymbols() {
        try {
            byte[] bytes = curator.getData().forPath(paths.symbols());
            if (bytes == null || bytes.length == 0) {
                return List.of();
            }
            return mapper.readValue(bytes, LIST_OF_STRING);
        } catch (KeeperException.NoNodeException e) {
            return List.of();
        } catch (Exception e) {
            throw new ClusterException("failed to read symbols znode", e);
        }
    }

    /**
     * Replace the symbol list in ZooKeeper with {@code symbols}, deduplicating
     * while preserving insertion order. Creates the znode if it does not exist.
     *
     * @param symbols the new symbol list to persist
     * @throws ClusterException on serialisation or ZK failure
     */
    public void writeSymbols(List<String> symbols) {
        List<String> normalized = new ArrayList<>(new LinkedHashSet<>(symbols));
        byte[] bytes;
        try {
            bytes = mapper.writeValueAsBytes(normalized);
        } catch (IOException e) {
            throw new ClusterException("failed to serialize symbols", e);
        }
        try {
            Stat stat = curator.checkExists().forPath(paths.symbols());
            if (stat == null) {
                curator.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(paths.symbols(), bytes);
            } else {
                curator.setData().forPath(paths.symbols(), bytes);
            }
        } catch (Exception e) {
            throw new ClusterException("failed to write symbols znode", e);
        }
    }

    /**
     * Append a symbol to the persistent list. No-op if already present.
     *
     * @param symbol the ticker to add (case-insensitive; whitespace trimmed)
     * @return {@code true} if the symbol was newly added; {@code false} if it was already present
     * @throws IllegalArgumentException if the symbol is null or blank
     */
    public synchronized boolean addSymbol(String symbol) {
        String normalized = normalize(symbol);
        List<String> current = readSymbols();
        if (current.contains(normalized)) {
            return false;
        }
        List<String> updated = new ArrayList<>(current);
        updated.add(normalized);
        writeSymbols(updated);
        return true;
    }

    /**
     * Remove a symbol from the persistent list. No-op if not present.
     *
     * @param symbol the ticker to remove (case-insensitive; whitespace trimmed)
     * @return {@code true} if the symbol was present and removed; {@code false} otherwise
     * @throws IllegalArgumentException if the symbol is null or blank
     */
    public synchronized boolean removeSymbol(String symbol) {
        String normalized = normalize(symbol);
        List<String> current = readSymbols();
        if (!current.contains(normalized)) {
            return false;
        }
        List<String> updated = new ArrayList<>(current);
        updated.remove(normalized);
        writeSymbols(updated);
        return true;
    }

    /**
     * Bootstrap the symbol znode from {@code cluster.symbols-seed-file}
     * if the znode is currently empty. Idempotent — safe to call on every
     * leadership acquisition.
     */
    public void seedIfEmpty() {
        List<String> existing = readSymbols();
        if (!existing.isEmpty()) {
            log.info("symbols znode already populated with {} entries; skipping seed", existing.size());
            return;
        }
        List<String> seed = readSeedFile();
        if (seed.isEmpty()) {
            log.info("seed file {} is empty or missing; initializing empty symbol list", props.getSymbolsSeedFile());
            writeSymbols(Collections.emptyList());
            return;
        }
        log.info("seeding symbols znode with {} entries from {}", seed.size(), props.getSymbolsSeedFile());
        writeSymbols(seed);
    }

    /**
     * Parse the seed file, ignoring blank lines and lines beginning with {@code '#'}.
     *
     * @return distinct, normalised symbols from the file (empty if the file does not exist)
     * @throws ClusterException if the file exists but cannot be read
     */
    private List<String> readSeedFile() {
        Path path = Path.of(props.getSymbolsSeedFile());
        if (!Files.exists(path)) {
            return List.of();
        }
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8).stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .filter(line -> !line.startsWith("#"))
                    .map(ConfigStore::normalize)
                    .distinct()
                    .toList();
        } catch (IOException e) {
            throw new ClusterException("failed to read seed file " + path, e);
        }
    }

    /**
     * Trim and upper-case a symbol, rejecting null/blank input.
     *
     * @param symbol the raw input
     * @return the canonical form used everywhere in ZK
     * @throws IllegalArgumentException if the input is null or blank after trimming
     */
    private static String normalize(String symbol) {
        if (symbol == null) {
            throw new IllegalArgumentException("symbol must not be null");
        }
        String trimmed = symbol.trim().toUpperCase();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        return trimmed;
    }
}
