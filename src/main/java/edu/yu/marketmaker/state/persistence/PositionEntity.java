package edu.yu.marketmaker.state.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "positions")
public class PositionEntity {
    @Id
    public String symbol;
    public int netQuantity;
    public long version;
    public UUID lastFillId;

    // Getters, Setters, and a conversion method to your 'Position' record

}