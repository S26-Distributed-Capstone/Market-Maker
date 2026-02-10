package edu.yu.marketmaker.model;

import java.util.UUID;

public record Quote(String symbol, int bidPrice, int bidQuantity, int askPrice, int askQuantity, UUID quoteId, long expiresAt) {
    
}
