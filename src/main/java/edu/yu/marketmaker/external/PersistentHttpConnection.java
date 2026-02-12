package edu.yu.marketmaker.external;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class PersistentHttpConnection {

    private final HttpClient client;
    private final URI targetUri;

    public PersistentHttpConnection(URI targetUri) {
        // HttpClient manages the persistent TCP connection pool automatically
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.targetUri = targetUri;
    }

    /**
     * Sends a separate POST request for each order.
     * The HttpClient reuses the underlying TCP connection (Keep-Alive).
     */
    public void sendData(String jsonBody) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(targetUri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // Send asynchronously to avoid blocking the generator loop
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() >= 400) {
                        System.err.println("Order rejected: " + response.statusCode() + " " + response.body());
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Network error: " + ex.getMessage());
                    return null;
                });
    }
}