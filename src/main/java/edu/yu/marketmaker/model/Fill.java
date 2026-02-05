package edu.yu.marketmaker.model;

enum Side{
    buy, sell
}

/**
 * A fill is the result of a successful execution against a quote.
 * @param orderId
 * @param symbol
 * @param side buy/sell
 * @param quantity
 * @param price
 * @param quoteId
 * @param createdAt timestamp
 */

public record Fill(long orderId, String symbol, Side side, int quantity, double price, long quoteId, long createdAt) {
}
