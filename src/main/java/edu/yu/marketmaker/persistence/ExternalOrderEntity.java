package edu.yu.marketmaker.persistence;

import edu.yu.marketmaker.model.ExternalOrder;
import edu.yu.marketmaker.model.Side;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * JPA Entity for ExternalOrder records used by Hazelcast MapStore.
 * Note: ExternalOrder doesn't have a natural ID, so we generate one.
 */
@Entity
@Table(name = "external_orders")
public class ExternalOrderEntity {

    @Id
    private UUID id;
    private String symbol;
    private int quantity;
    private double limitPrice;
    @Enumerated(EnumType.STRING)
    private Side side;

    // --- Constructors ---

    /**
     * No-args constructor required by JPA.
     */
    public ExternalOrderEntity() {
    }

    /**
     * All-args constructor.
     */
    public ExternalOrderEntity(UUID id, String symbol, int quantity, double limitPrice, Side side) {
        this.id = id;
        this.symbol = symbol;
        this.quantity = quantity;
        this.limitPrice = limitPrice;
        this.side = side;
    }

    // --- Conversion Methods ---

    /**
     * Converts this JPA entity back into the immutable ExternalOrder record.
     * @return An ExternalOrder record.
     */
    public ExternalOrder toRecord() {
        return new ExternalOrder(this.id, this.symbol, this.quantity, this.limitPrice, this.side);
    }

    /**
     * Static helper to create an Entity from a Record.
     * @param order The external order record.
     * @return A new ExternalOrderEntity.
     */
    public static ExternalOrderEntity fromRecord(ExternalOrder order) {
        return new ExternalOrderEntity(
                order.id(),
                order.symbol(),
                order.quantity(),
                order.limitPrice(),
                order.side()
        );
    }

    // --- Getters and Setters ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getLimitPrice() {
        return limitPrice;
    }

    public void setLimitPrice(double limitPrice) {
        this.limitPrice = limitPrice;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    @Override
    public String toString() {
        return "ExternalOrderEntity{" +
                "id=" + id +
                ", symbol='" + symbol + '\'' +
                ", quantity=" + quantity +
                ", limitPrice=" + limitPrice +
                ", side=" + side +
                '}';
    }
}

