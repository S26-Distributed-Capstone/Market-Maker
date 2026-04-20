package edu.yu.marketmaker.cluster;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end cluster test: builds the compose stack (3-node ZK ensemble + 7
 * market-maker nodes + postgres), waits for convergence, kills the elected
 * leader, and verifies that a new leader is elected and the dead node is
 * evicted from the members set.
 *
 * Opt-in: requires {@code -Dcluster.it=true} on the mvn command line and
 * docker running locally.
 */
@EnabledIfSystemProperty(named = "cluster.it", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClusterIntegrationTest {

    /** Host port -> compose service name, per compose.yml. */
    private static final SortedMap<Integer, String> PORT_TO_SERVICE;
    static {
        SortedMap<Integer, String> m = new TreeMap<>();
        m.put(8081, "market-maker-node-1");
        m.put(8082, "market-maker-node-2");
        m.put(8083, "market-maker-node-3");
        m.put(8084, "market-maker-node-4");
        m.put(8085, "market-maker-node-5");
        m.put(8086, "market-maker-node-6");
        m.put(8087, "market-maker-node-7");
        PORT_TO_SERVICE = Collections.unmodifiableSortedMap(m);
    }

    private static final Set<String> SEED_SYMBOLS = new TreeSet<>(List.of(
            "AAPL", "MSFT", "GOOG", "TSLA", "NVDA", "AMZN", "META"));

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path PROJECT_ROOT = Path.of(".").toAbsolutePath().normalize();

    @BeforeAll
    static void bootStack() throws Exception {
        System.out.println("[IT] cleaning any prior stack...");
        runDocker(TimeUnit.MINUTES.toMillis(2), "compose", "down", "-v", "--remove-orphans");

        System.out.println("[IT] docker compose build market-maker-node-1 (first run may take several minutes)...");
        int buildRc = runDocker(TimeUnit.MINUTES.toMillis(15),
                "compose", "build", "market-maker-node-1");
        assertEquals(0, buildRc, "docker compose build failed");

        System.out.println("[IT] docker compose up -d...");
        List<String> upCmd = new ArrayList<>(List.of("compose", "up", "-d"));
        upCmd.add("zookeeper1");
        upCmd.add("zookeeper2");
        upCmd.add("zookeeper3");
        upCmd.add("postgres");
        upCmd.addAll(PORT_TO_SERVICE.values());
        int rc = runDocker(TimeUnit.MINUTES.toMillis(5), upCmd.toArray(String[]::new));
        assertEquals(0, rc, "docker compose up failed");

        System.out.println("[IT] waiting for all 7 nodes to converge (leader elected, members=7)...");
        awaitCondition(Duration.ofMinutes(4), ClusterIntegrationTest::allNodesConverged,
                "cluster did not converge within 4 minutes");
        System.out.println("[IT] cluster up.");
    }

    @AfterAll
    static void teardownStack() throws Exception {
        System.out.println("[IT] docker compose down -v");
        runDocker(TimeUnit.MINUTES.toMillis(2), "compose", "down", "-v");
    }

    @Test
    @Order(1)
    void clusterConvergesOnSingleLeaderAndFullSymbolList() throws Exception {
        Map<Integer, JsonNode> statuses = statusFromEachNode(-1);
        assertEquals(PORT_TO_SERVICE.size(), statuses.size(), "not all nodes responded");

        Set<String> leadersSeen = new HashSet<>();
        Set<String> membersUnion = new HashSet<>();
        for (JsonNode s : statuses.values()) {
            leadersSeen.add(s.path("leaderId").asText(null));
            JsonNode m = s.path("members");
            for (int i = 0; i < m.size(); i++) membersUnion.add(m.get(i).asText());
        }
        assertEquals(1, leadersSeen.size(), "nodes disagree on leader: " + leadersSeen);
        assertNotNull(leadersSeen.iterator().next(), "leader is null");
        assertEquals(PORT_TO_SERVICE.size(), membersUnion.size(),
                "expected " + PORT_TO_SERVICE.size() + " members, got " + membersUnion);

        int leaderCount = 0;
        for (JsonNode s : statuses.values()) {
            if (s.path("leader").asBoolean(false)) leaderCount++;
        }
        assertEquals(1, leaderCount, "expected exactly one node to report leader=true");

        JsonNode symbolsNode = statuses.values().iterator().next().path("symbols");
        Set<String> symbols = new TreeSet<>();
        for (int i = 0; i < symbolsNode.size(); i++) symbols.add(symbolsNode.get(i).asText());
        assertEquals(SEED_SYMBOLS, symbols, "seed symbol list mismatch");
    }

    @Test
    @Order(2)
    void leaderFailoverElectsNewLeaderAndEvictsOld() throws Exception {
        Map<Integer, JsonNode> before = statusFromEachNode(-1);
        String oldLeader = before.values().iterator().next().path("leaderId").asText();

        int leaderPort = before.entrySet().stream()
                .filter(e -> oldLeader.equals(e.getValue().path("nodeId").asText()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "couldn't locate host port for leader nodeId=" + oldLeader));
        String leaderService = PORT_TO_SERVICE.get(leaderPort);

        System.out.println("[IT] killing leader service=" + leaderService + " (nodeId=" + oldLeader + ")");
        assertEquals(0, runDocker(TimeUnit.MINUTES.toMillis(1),
                        "compose", "kill", "-s", "SIGKILL", leaderService),
                "docker compose kill failed");

        awaitCondition(Duration.ofSeconds(90), () -> {
            Map<Integer, JsonNode> now = statusFromEachNode(leaderPort);
            if (now.size() != PORT_TO_SERVICE.size() - 1) return false;
            Set<String> leadersSeen = new HashSet<>();
            Set<String> membersUnion = new HashSet<>();
            for (JsonNode s : now.values()) {
                String lid = s.path("leaderId").asText(null);
                if (lid == null || lid.equals(oldLeader)) return false;
                leadersSeen.add(lid);
                JsonNode m = s.path("members");
                for (int i = 0; i < m.size(); i++) membersUnion.add(m.get(i).asText());
            }
            return leadersSeen.size() == 1
                    && !membersUnion.contains(oldLeader)
                    && membersUnion.size() == PORT_TO_SERVICE.size() - 1;
        }, "new leader not elected and old leader not evicted within 90s");

        Map<Integer, JsonNode> after = statusFromEachNode(leaderPort);
        String newLeader = after.values().iterator().next().path("leaderId").asText();
        System.out.println("[IT] new leader=" + newLeader + " (old=" + oldLeader + ")");
        assertTrue(!newLeader.equals(oldLeader), "new leader must differ from old");
    }

    // ---------- helpers ----------

    private static boolean allNodesConverged() {
        Map<Integer, JsonNode> statuses = statusFromEachNode(-1);
        if (statuses.size() != PORT_TO_SERVICE.size()) return false;
        Set<String> leaders = new HashSet<>();
        for (JsonNode s : statuses.values()) {
            String lid = s.path("leaderId").asText(null);
            if (lid == null) return false;
            if (s.path("members").size() != PORT_TO_SERVICE.size()) return false;
            leaders.add(lid);
        }
        return leaders.size() == 1;
    }

    /** Fetch {@code /cluster/status} from every node except {@code excludedPort}. Unreachable nodes are skipped. */
    private static Map<Integer, JsonNode> statusFromEachNode(int excludedPort) {
        Map<Integer, JsonNode> out = new LinkedHashMap<>();
        for (int port : PORT_TO_SERVICE.keySet()) {
            if (port == excludedPort) continue;
            try {
                out.put(port, status(port));
            } catch (Exception ignored) {
                // node not yet up, or just killed
            }
        }
        return out;
    }

    private static JsonNode status(int port) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/cluster/status"))
                .timeout(Duration.ofSeconds(3))
                .GET().build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("non-200 from port " + port + ": " + resp.statusCode());
        }
        return JSON.readTree(resp.body());
    }

    private static int runDocker(long timeoutMs, String... args) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add("docker");
        Collections.addAll(cmd, args);
        ProcessBuilder pb = new ProcessBuilder(cmd)
                .directory(PROJECT_ROOT.toFile())
                .redirectErrorStream(true)
                .inheritIO();
        Process p = pb.start();
        if (!p.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
            p.destroyForcibly();
            throw new AssertionError("timeout running: " + String.join(" ", cmd));
        }
        return p.exitValue();
    }

    private static void awaitCondition(Duration timeout, BooleanSupplier condition, String failureMessage) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) return;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new AssertionError(failureMessage);
    }
}
