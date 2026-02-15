package edu.yu.marketmaker.external;

import edu.yu.marketmaker.model.ExternalOrder;
import edu.yu.marketmaker.model.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.util.Random;
import java.util.List;
import java.util.Arrays;

/**
 * External Order Publisher
 *
 * Generates and submits random buy/sell orders to the Exchange API.
 * Purpose: Drive trading activity and test system under load and concurrency.
 *
 * Key Characteristics:
 * - Stateless: Does NOT track positions, quotes, or any internal state
 * - Concurrent: Issues orders across multiple symbols simultaneously
 * - Adversarial: Generates orders that may hit expired quotes or bad prices
 */
public class ExternalOrderPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ExternalOrderPublisher.class);

    // Configuration
    private final String exchangeBaseUrl;
    private final List<String> symbols;
    private final Random random;
    private PersistentHttpConnection httpConnection;
    private boolean isShutdown;

    public ExternalOrderPublisher(String exchangeBaseUrl) throws IOException {
        this.exchangeBaseUrl = exchangeBaseUrl;
        this.symbols = Arrays.asList("AAPL", "MSFT", "GOOG", "TSLA");
        this.random = new Random();
        this.isShutdown = false;
    }

    /**
     * Submit an order to the Exchange.
     * Calls POST /orders on Exchange API.
     *
     * @param order The ExternalOrder to submit
     */
    public void submitOrder(ExternalOrder order) {
        if (this.httpConnection == null) {
            throw new IllegalStateException("HTTP connection not initialized");
        }

        logger.info("Submitting {} order: {} x {} @ {}",
                order.side(), order.symbol(), order.quantity(), order.limitPrice());

        ObjectMapper mapper = JsonMapper
                .builder()
                .findAndAddModules()
                .build();
        String serializedOrder = mapper.writeValueAsString(order);

        this.httpConnection.sendData(serializedOrder);
    }

    /**
     * Generate and submit random orders continuously.
     * Creates a concurrent load by submitting orders for multiple symbols simultaneously.
     */
    public void startGeneratingOrders() throws IOException {
        logger.info("Starting order generation for symbols: {}", symbols);

        this.httpConnection = new PersistentHttpConnection(URI.create(this.exchangeBaseUrl));

        // TODO: Implement continuous order generation loop
        // - For each symbol, generate random ExternalOrder
        // - Random quantity between 1-20
        // - Random price near reference price (e.g., 100 +/- 5)
        // - Sleep between orders to control rate (e.g., 2 orders/second)
        // - Use multiple threads for concurrency across symbols

        while (!this.isShutdown) {
            for (int i=0; i < this.random.nextInt(20); i++) { // Generate 10 orders per batch
                for (String symbol : symbols) {
                    generateRandomOrder(symbol);
                }
            }
            try {
                Thread.sleep(500); // Sleep for 0.5 seconds between batches
            } catch (InterruptedException e) {
                logger.warn("Order generation interrupted: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Generate a random order for the given symbol.
     * Helper method to create random buy/sell orders for testing.
     *
     * @param symbol Symbol to generate order for
     */
    private void generateRandomOrder(String symbol) {
        // Random buy or sell
        Side side = random.nextBoolean() ? Side.BUY : Side.SELL;

        // Random quantity between 1 and 20
        int quantity = 1 + random.nextInt(20);

        // Random price around reference price (simplified: 100 +/- 5)
        double referencePrice = 100.00;
        double priceOffset = (random.nextDouble() * 10) - 5; // -5 to +5
        double limitPrice = Math.round((referencePrice + priceOffset) * 100.0) / 100.0; // Round to 2 decimals

        // Create and submit the order
        ExternalOrder order = new ExternalOrder(java.util.UUID.randomUUID(), symbol, quantity, limitPrice, side);
        submitOrder(order);
    }

    /**
     * Stop generating orders.
     */
    public void stop() throws IOException {
        logger.info("Stopping order generation");

        // TODO: Implement graceful shutdown
        // - Stop all order generation threads
        // - Wait for in-flight requests to complete

        this.isShutdown = true;
    }
}