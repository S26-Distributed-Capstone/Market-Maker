package edu.yu.marketmaker.marketmaker;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.yu.marketmaker.model.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Integration test for the MarketMaker ↔ ExposureReservation flow.
 *
 * <p>
 * Prerequisites: {@code docker compose up --build -d} must be running.
 * The compose file starts:
 * <ul>
 * <li><b>market-maker</b> on port 8080 — profiles: test-position-tracker,
 * production-quote-generator, market-maker-node</li>
 * <li><b>exposure-reservation</b> on port 8081 — profile:
 * exposure-reservation</li>
 * </ul>
 *
 * <p>
 * Flow under test:
 * <ol>
 * <li>Register a symbol with the TestPositionTracker</li>
 * <li>Submit a StateSnapshot (position + fill) via HTTP</li>
 * <li>MarketMaker receives the snapshot, ProductionQuoteGenerator generates a
 * quote
 * and requests an exposure reservation from the exposure-reservation service
 * via RSocket</li>
 * <li>Verify that the exposure-reservation service has recorded a reservation
 * by checking the /exposure endpoint shows non-zero usage</li>
 * </ol>
 */
public class MarketMakerTest {

    private static final String MARKET_MAKER_BASE = "http://localhost:9080";
    private static final String EXPOSURE_RESERVATION_BASE = "http://localhost:9081";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        System.out.println("=== MarketMaker ↔ ExposureReservation Integration Test ===\n");

        // Step 0: Wait for services to be healthy
        System.out.println("Waiting for services to be healthy...");
        waitForHealth(EXPOSURE_RESERVATION_BASE + "/health", "exposure-reservation");
        // market-maker has no /health endpoint; use TestPositionTracker liveness
        waitForHealth(MARKET_MAKER_BASE + "/test/position-tracker/received/NONE", "market-maker");
        System.out.println("All services healthy.\n");

        // Step 1: Verify initial exposure is zero
        System.out.println("--- Step 1: Checking initial exposure state ---");
        ExposureState initialState = getExposureState();
        System.out.println("Initial exposure: " + initialState);
        assertEqual(0, initialState.bidUsage(), "initial bidUsage");
        assertEqual(0, initialState.askUsage(), "initial askUsage");
        assertEqual(0, initialState.activeReservations(), "initial activeReservations");
        System.out.println("PASS: Initial exposure is zero.\n");

        // Step 2: Register symbol AAPL with the TestPositionTracker
        System.out.println("--- Step 2: Registering symbol AAPL ---");
        int code = httpPut(MARKET_MAKER_BASE + "/test/position-tracker/symbols/AAPL", null);
        System.out.println("PUT /test/position-tracker/symbols/AAPL → " + code);
        assertHttpSuccess(code, "Register symbol AAPL");
        System.out.println("PASS: Symbol AAPL registered.\n");

        // Step 3: Submit a StateSnapshot
        System.out.println("--- Step 3: Submitting StateSnapshot for AAPL ---");
        Fill fill = new Fill(
                UUID.randomUUID(),
                "AAPL",
                Side.BUY,
                5,
                150.00,
                UUID.randomUUID(),
                System.currentTimeMillis());
        Position position = new Position("AAPL", 5, 1, fill.orderId());
        StateSnapshot snapshot = new StateSnapshot(position, fill);

        String snapshotJson = MAPPER.writeValueAsString(snapshot);
        System.out.println("Snapshot JSON: " + snapshotJson);
        code = httpPost(MARKET_MAKER_BASE + "/test/position-tracker/snapshots", snapshotJson);
        System.out.println("POST /test/position-tracker/snapshots → " + code);
        assertHttpSuccess(code, "Submit snapshot");
        System.out.println("PASS: Snapshot submitted.\n");

        // Step 4: Wait for the async pipeline to process the snapshot
        System.out.println("--- Step 4: Waiting for async processing ---");
        Thread.sleep(5000); // Give time for MarketMaker → ProductionQuoteGenerator → RSocket →
                            // ExposureReservation

        // Step 5: Verify exposure reservation was created
        System.out.println("--- Step 5: Verifying exposure reservation ---");
        ExposureState finalState = getExposureState();
        System.out.println("Final exposure: " + finalState);

        if (finalState.bidUsage() > 0 || finalState.askUsage() > 0) {
            System.out.println("PASS: Exposure reservation created! BidUsage=" + finalState.bidUsage()
                    + ", AskUsage=" + finalState.askUsage()
                    + ", ActiveReservations=" + finalState.activeReservations());
        } else {
            throw new AssertionError("FAIL: Expected non-zero exposure usage but got bidUsage="
                    + finalState.bidUsage() + ", askUsage=" + finalState.askUsage());
        }

        // Step 6: Submit a second snapshot with a higher version to test quote update
        System.out.println("\n--- Step 6: Submitting second snapshot (version 2) ---");
        Fill fill2 = new Fill(
                UUID.randomUUID(),
                "AAPL",
                Side.SELL,
                3,
                150.50,
                UUID.randomUUID(),
                System.currentTimeMillis());
        Position position2 = new Position("AAPL", 2, 2, fill2.orderId());
        StateSnapshot snapshot2 = new StateSnapshot(position2, fill2);

        code = httpPost(MARKET_MAKER_BASE + "/test/position-tracker/snapshots",
                MAPPER.writeValueAsString(snapshot2));
        System.out.println("POST /test/position-tracker/snapshots → " + code);
        assertHttpSuccess(code, "Submit second snapshot");

        Thread.sleep(5000);

        ExposureState afterSecond = getExposureState();
        System.out.println("Exposure after second snapshot: " + afterSecond);
        if (afterSecond.activeReservations() > 0) {
            System.out.println("PASS: Reservation updated after second snapshot.");
        } else {
            throw new AssertionError("FAIL: Expected active reservations after second snapshot.");
        }

        System.out.println("\n=== ALL TESTS PASSED ===");
    }

    // --- Helper methods ---

    private static ExposureState getExposureState() throws Exception {
        URL url = URI.create(EXPOSURE_RESERVATION_BASE + "/exposure").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        int code = conn.getResponseCode();
        if (code != 200) {
            throw new RuntimeException("GET /exposure returned " + code);
        }
        String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        conn.disconnect();
        return MAPPER.readValue(body, ExposureState.class);
    }

    private static void waitForHealth(String url, String serviceName) throws Exception {
        int maxRetries = 60;
        for (int i = 1; i <= maxRetries; i++) {
            try {
                HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                int code = conn.getResponseCode();
                conn.disconnect();
                if (code == 200) {
                    System.out.println("  " + serviceName + " healthy (attempt " + i + ")");
                    return;
                }
            } catch (IOException ignored) {
                // Service not ready yet
            }
            System.out.println("  " + serviceName + " not ready, retrying (" + i + "/" + maxRetries + ")...");
            Thread.sleep(3000);
        }
        throw new RuntimeException(serviceName + " failed to become healthy after " + maxRetries + " retries");
    }

    private static int httpPut(String url, String jsonBody) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/json");
        if (jsonBody != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }
        }
        int code = conn.getResponseCode();
        conn.disconnect();
        return code;
    }

    private static int httpPost(String url, String jsonBody) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        if (jsonBody != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }
        }
        int code = conn.getResponseCode();
        conn.disconnect();
        return code;
    }

    private static void assertEqual(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }

    private static void assertHttpSuccess(int code, String label) {
        if (code < 200 || code >= 300) {
            throw new AssertionError(label + ": expected 2xx but got " + code);
        }
    }
}
