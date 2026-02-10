package edu.yu.marketmaker.external;

import edu.yu.marketmaker.model.ExternalOrder;
import edu.yu.marketmaker.model.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.UUID;
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

    public ExternalOrderPublisher(String exchangeBaseUrl) {
        this.exchangeBaseUrl = exchangeBaseUrl;
        this.symbols = Arrays.asList("AAPL", "MSFT", "GOOG", "TSLA");
        this.random = new Random();
    }

    /**
     * Submit an order to the Exchange.
     * Calls POST /orders on Exchange API.
     *
     * @param order The ExternalOrder to submit
     * @return orderId if accepted, null if rejected
     */
    public String submitOrder(ExternalOrder order) {
        String orderId = UUID.randomUUID().toString();

        logger.info("Submitting {} order: {} x {} @ {}",
                order.side(), order.symbol(), order.quantity(), order.limitPrice());

        // TODO: Implement HTTP POST to {exchangeBaseUrl}/orders
        // Request body: {"order_id": orderId, "symbol": order.symbol(),
        //                "side": order.side(), "quantity": order.quantity(),
        //                "limit_price": order.limitPrice()}
        // Handle response: 200 OK (filled), 202 Accepted (partial), 400 Rejected

        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Generate and submit random orders continuously.
     * Creates concurrent load by submitting orders for multiple symbols simultaneously.
     */
    public void startGeneratingOrders() {
        logger.info("Starting order generation for symbols: {}", symbols);

        // TODO: Implement continuous order generation loop
        // - For each symbol, generate random ExternalOrder
        // - Random quantity between 1-20
        // - Random price near reference price (e.g., 100 +/- 5)
        // - Sleep between orders to control rate (e.g., 2 orders/second)
        // - Use multiple threads for concurrency across symbols

        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Generate a random order for the given symbol.
     * Helper method to create random buy/sell orders for testing.
     *
     * @param symbol Symbol to generate order for
     */
    private void generateRandomOrder(String symbol) {
        // Random buy or sell
        Side side = random.nextBoolean() ? Side.buy : Side.sell;

        // Random quantity between 1 and 20
        int quantity = 1 + random.nextInt(20);

        // Random price around reference price (simplified: 100 +/- 5)
        double referencePrice = 100.00;
        double priceOffset = (random.nextDouble() * 10) - 5; // -5 to +5
        double limitPrice = Math.round((referencePrice + priceOffset) * 100.0) / 100.0; // Round to 2 decimals

        // Create and submit the order
        ExternalOrder order = new ExternalOrder(symbol, quantity, limitPrice, side);
        submitOrder(order);
    }

    /**
     * Stop generating orders.
     */
    public void stop() {
        logger.info("Stopping order generation");

        // TODO: Implement graceful shutdown
        // - Stop all order generation threads
        // - Wait for in-flight requests to complete

        throw new UnsupportedOperationException("Not implemented yet");
    }
}