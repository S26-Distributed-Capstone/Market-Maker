package edu.yu.marketmaker.model;

/**
 * An external order is a request submitted to the exchange.
 * Represents a buy or sell order with specified quantity and limit price.
 * @param symbol ticker symbol
 * @param quantity number of units to trade
 * @param limitPrice maximum price for buy, minimum price for sell
 * @param side buy or sell
 */
public record ExternalOrder(String symbol, int quantity, double limitPrice, Side side) {
}