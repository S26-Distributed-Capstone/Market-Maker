package edu.yu.marketmaker.model;

import java.util.UUID;

public record Quote(String symbol, double bidPrice, int bidQuantity, double askPrice, int askQuantity, UUID quoteId, long expiresAt) implements Identifiable<String> {

    @Override
    public String getId() {
        return symbol;
    }
}
