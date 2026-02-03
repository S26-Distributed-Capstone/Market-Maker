package edu.yu.marketmaker.model;

public record Quote(String symbol, int bidPrice, int bidQuantity, int askPrice, int askQuantity, long quoteId, long expiresAt) {
    
}
