package edu.yu.marketmaker.marketmaker;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.yu.marketmaker.model.Fill;
import edu.yu.marketmaker.model.Position;
import edu.yu.marketmaker.model.Quote;
import edu.yu.marketmaker.model.Side;
import edu.yu.marketmaker.model.StateSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MarketMakerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Override with: -Dit.baseUrl=http://localhost:8080
    private static final String BASE_URL = System.getProperty("it.baseUrl", "http://localhost:8080");

    @Test
    void generatesQuoteForTrackedSymbol_andIgnoresDuplicateVersion() throws Exception {
        String symbol = "AAPL";
        UUID fillId = UUID.randomUUID();

        // 1) Add symbol to watch list
        SimpleResponse addSymbolResponse = post("/test/position-tracker/symbols/" + symbol);
        assertTrue(
                addSymbolResponse.statusCode() == 200
                        || addSymbolResponse.statusCode() == 201
                        || addSymbolResponse.statusCode() == 204,
                "Expected add-symbol success, got " + addSymbolResponse.statusCode()
                        + " body=" + addSymbolResponse.body()
        );

        // 2) Send first snapshot for version 1.
        Position position = new Position(symbol, 100, 1L, fillId);
        Fill fill = new Fill(fillId, symbol, Side.BUY, 100, 150.25, UUID.randomUUID(), System.currentTimeMillis());
        StateSnapshot snapshot = new StateSnapshot(position, fill);

        SimpleResponse snapshotResponse = postJson("/test/position-tracker/snapshots", MAPPER.writeValueAsString(snapshot));
        assertEquals(
                202,
                snapshotResponse.statusCode(),
                "Expected snapshot accepted (202), got " + snapshotResponse.statusCode() + " body=" + snapshotResponse.body()
        );

        // 3) Validate MarketMaker generated at least one quote for this symbol.
        int generatedCountAfterFirst = waitForGeneratedCountAtLeast(symbol, 1, 5000);
        assertTrue(generatedCountAfterFirst >= 1, "Expected at least one generated quote for symbol " + symbol);

        SimpleResponse lastQuoteResponse = get("/test/quote-generator/last/" + symbol);
        assertEquals(200, lastQuoteResponse.statusCode(), "Expected last quote endpoint 200");
        Quote lastQuote = MAPPER.readValue(lastQuoteResponse.body(), Quote.class);
        assertEquals(symbol, lastQuote.symbol(), "Expected generated quote symbol to match tracked symbol");

        // 4) Send duplicate version snapshot and verify quote count does not increase.
        SimpleResponse duplicateSnapshotResponse = postJson("/test/position-tracker/snapshots", MAPPER.writeValueAsString(snapshot));
        assertEquals(202, duplicateSnapshotResponse.statusCode(), "Expected duplicate snapshot accepted (202)");
        Thread.sleep(300);
        int generatedCountAfterDuplicate = getGeneratedCount(symbol);
        assertEquals(
                generatedCountAfterFirst,
                generatedCountAfterDuplicate,
                "Expected duplicate version snapshot to not generate an extra quote"
        );
    }

    private int waitForGeneratedCountAtLeast(String symbol, int minCount, long timeoutMillis) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            int count = getGeneratedCount(symbol);
            if (count >= minCount) {
                return count;
            }
            Thread.sleep(100);
        }
        return getGeneratedCount(symbol);
    }

    private int getGeneratedCount(String symbol) throws Exception {
        SimpleResponse countResponse = get("/test/quote-generator/count/" + symbol);
        assertEquals(200, countResponse.statusCode(), "Expected quote count endpoint 200");
        return Integer.parseInt(countResponse.body().trim());
    }

    private SimpleResponse postJson(String path, String json) throws Exception {
        return request("POST", path, json, "application/json");
    }

    private SimpleResponse get(String path) throws Exception {
        return request("GET", path, null, null);
    }

    private SimpleResponse post(String path) throws Exception {
        return request("POST", path, null, null);
    }

    private SimpleResponse request(String method, String path, String body, String contentType) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + path).openConnection(Proxy.NO_PROXY);
        connection.setRequestMethod(method);
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(5000);
        connection.setUseCaches(false);
        if (contentType != null) {
            connection.setRequestProperty("Content-Type", contentType);
        }

        if (body != null) {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Length", Integer.toString(bytes.length));
            try (OutputStream os = connection.getOutputStream()) {
                os.write(bytes);
            }
        }

        int status = connection.getResponseCode();
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String responseBody = "";
        if (stream != null) {
            try (InputStream is = stream) {
                responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        connection.disconnect();
        return new SimpleResponse(status, responseBody);
    }

    private record SimpleResponse(int statusCode, String body) {
    }
}
