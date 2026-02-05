package edu.yu.marketmaker.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
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
     * Submit a buy order to the Exchange.
     * Calls POST /orders on Exchange API.
     *
     * @param symbol The symbol to buy (e.g., "AAPL")
     * @param quantity Number of units to buy
     * @param limitPrice Maximum price willing to pay
     * @return orderId if accepted, null if rejected
     */
    public String submitBuyOrder(String symbol, int quantity, BigDecimal limitPrice) {
        String orderId = UUID.randomUUID().toString();

        logger.info("Submitting BUY order: {} x {} @ {}", symbol, quantity, limitPrice);

        // TODO: Implement HTTP POST to {exchangeBaseUrl}/orders
        // Request body: {"order_id": orderId, "symbol": symbol, "side": "buy",
        //                "quantity": quantity, "limit_price": limitPrice}
        // Handle response: 200 OK (filled), 202 Accepted (partial), 400 Rejected

        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Submit a sell order to the Exchange.
     * Calls POST /orders on Exchange API.
     *
     * @param symbol The symbol to sell (e.g., "MSFT")
     * @param quantity Number of units to sell
     * @param limitPrice Minimum price willing to accept
     * @return orderId if accepted, null if rejected
     */
    public String submitSellOrder(String symbol, int quantity, BigDecimal limitPrice) {
        String orderId = UUID.randomUUID().toString();

        logger.info("Submitting SELL order: {} x {} @ {}", symbol, quantity, limitPrice);

        // TODO: Implement HTTP POST to {exchangeBaseUrl}/orders
        // Request body: {"order_id": orderId, "symbol": symbol, "side": "sell",
        //                "quantity": quantity, "limit_price": limitPrice}
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
        // - For each symbol, generate random buy or sell order
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
        boolean isBuy = random.nextBoolean();

        // Random quantity between 1 and 20
        int quantity = 1 + random.nextInt(20);

        // Random price around reference price (simplified: 100 +/- 5)
        BigDecimal referencePrice = new BigDecimal("100.00");
        double priceOffset = (random.nextDouble() * 10) - 5; // -5 to +5
        BigDecimal price = referencePrice.add(BigDecimal.valueOf(priceOffset))
                .setScale(2, BigDecimal.ROUND_HALF_UP);

        // Submit the order
        if (isBuy) {
            submitBuyOrder(symbol, quantity, price);
        } else {
            submitSellOrder(symbol, quantity, price);
        }
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