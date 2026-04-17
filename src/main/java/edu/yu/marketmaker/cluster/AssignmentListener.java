package edu.yu.marketmaker.cluster;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.yu.marketmaker.marketmaker.MarketMaker;
import jakarta.annotation.PreDestroy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The worker-side glue between ZooKeeper and the local {@link MarketMaker}.
 *
 * Each JVM watches exactly one znode — its own assignment under
 * {@code /marketmaker/assignments/<this-node-id>} — and translates every
 * change into add/remove calls on the local {@link MarketMaker}. The class
 * tracks which symbols have been accepted locally so subsequent updates can
 * compute a precise diff (avoiding spurious add/remove pairs).
 *
 * On the leader, the assignment znode is set to an empty list, so this
 * component naturally drains the local market-maker — there is no special
 * "I'm the leader" branch needed in MarketMaker itself.
 */
@Component
@Profile("market-maker-node")
public class AssignmentListener implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AssignmentListener.class);
    private static final TypeReference<List<String>> LIST_OF_STRING = new TypeReference<>() {};

    private final CuratorFramework curator;
    private final ZkPaths paths;
    private final ClusterNode clusterNode;
    private final MarketMaker marketMaker;
    private final ObjectMapper mapper = new ObjectMapper();

    private final Set<String> currentAssigned = Collections.synchronizedSet(new HashSet<>());
    private CuratorCache cache;

    /**
     * Inject all collaborators.
     *
     * @param curator     started Curator client (used for the initial znode read)
     * @param paths       znode-path helper
     * @param clusterNode supplies this JVM's id (which znode to watch)
     * @param marketMaker the local market-maker that ultimately owns each symbol
     */
    public AssignmentListener(CuratorFramework curator,
                              ZkPaths paths,
                              ClusterNode clusterNode,
                              MarketMaker marketMaker) {
        this.curator = curator;
        this.paths = paths;
        this.clusterNode = clusterNode;
        this.marketMaker = marketMaker;
    }

    /**
     * Spring Boot startup hook. Registers a {@link CuratorCache} listener on
     * the per-node assignment znode and immediately applies whatever value
     * happens to be present at startup (or an empty list if the znode does
     * not exist yet).
     *
     * @param args command-line arguments (unused)
     */
    @Override
    public void run(ApplicationArguments args) {
        String myPath = paths.assignmentFor(clusterNode.getNodeId());
        this.cache = CuratorCache.build(curator, myPath);
        this.cache.listenable().addListener((type, oldData, data) -> {
            byte[] bytes = (data != null) ? data.getData() : null;
            applyDesired(parse(bytes));
        });
        this.cache.start();
        try {
            byte[] initial = curator.getData().forPath(myPath);
            applyDesired(parse(initial));
        } catch (KeeperException.NoNodeException ignored) {
            applyDesired(List.of());
        } catch (Exception e) {
            log.warn("initial assignment read failed for {}", myPath, e);
        }
    }

    /**
     * Decode the JSON payload of the assignment znode into a list of symbols.
     * Robust to nulls, empty payloads, and parse errors (logged then treated
     * as empty so a malformed znode cannot wedge the worker).
     *
     * @param bytes raw znode data
     * @return the parsed symbol list, or an empty list on null/empty/error input
     */
    private List<String> parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return List.of();
        }
        try {
            return mapper.readValue(bytes, LIST_OF_STRING);
        } catch (Exception e) {
            log.error("failed to parse assignment payload; treating as empty", e);
            return List.of();
        }
    }

    /**
     * Reconcile the locally-tracked symbol set against the desired list:
     * remove any symbol no longer assigned, then add any newly assigned
     * symbol. Both operations defer to the {@link MarketMaker} API so this
     * class never touches market-making state directly.
     *
     * Synchronised so overlapping ZK callbacks cannot interleave their diffs.
     *
     * @param desiredList the freshly read assignment list from ZooKeeper
     */
    private synchronized void applyDesired(List<String> desiredList) {
        Set<String> desired = new TreeSet<>(desiredList);
        Set<String> toRemove = new TreeSet<>(currentAssigned);
        toRemove.removeAll(desired);
        Set<String> toAdd = new TreeSet<>(desired);
        toAdd.removeAll(currentAssigned);

        for (String s : toRemove) {
            if (marketMaker.removeSymbol(s)) {
                currentAssigned.remove(s);
                log.info("released symbol {}", s);
            }
        }
        for (String s : toAdd) {
            if (marketMaker.addSymbol(s)) {
                currentAssigned.add(s);
                log.info("accepted symbol {}", s);
            }
        }
    }

    /**
     * Spring lifecycle hook invoked at context shutdown — closes the
     * underlying {@link CuratorCache} so its background thread terminates.
     */
    @PreDestroy
    public void shutdown() {
        if (cache != null) {
            cache.close();
        }
    }
}
