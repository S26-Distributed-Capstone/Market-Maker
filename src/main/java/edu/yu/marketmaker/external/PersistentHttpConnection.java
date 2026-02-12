package edu.yu.marketmaker.external;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class PersistentHttpConnection {

    private final HttpClient client;
    private final PipedOutputStream outputStream;
    private final PipedInputStream inputStream;
    private CompletableFuture<HttpResponse<String>> responseFuture;

    public PersistentHttpConnection(URI targetUri) throws IOException {
        this.client = HttpClient.newBuilder().build();

        // Connect the pipe: what we write to 'outputStream' appears in 'inputStream'
        this.outputStream = new PipedOutputStream();
        this.inputStream = new PipedInputStream(outputStream);

        // Prepare the request with a streaming body
        HttpRequest request = HttpRequest.newBuilder()
                .uri(targetUri)
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> inputStream))
                .header("Content-Type", "application/octet-stream")
                .build();

        // Start the request asynchronously
        this.responseFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Call this method whenever you have new data to send.
     */
    public synchronized void sendData(String data) throws IOException {
        outputStream.write(data.getBytes(StandardCharsets.UTF_8));
        outputStream.flush(); // Forces the data into the network buffer
    }

    /**
     * Call this when you are finished to close the connection properly.
     */
    public void close() throws IOException {
        outputStream.close();
    }

    public CompletableFuture<HttpResponse<String>> getResponse() {
        return responseFuture;
    }
}