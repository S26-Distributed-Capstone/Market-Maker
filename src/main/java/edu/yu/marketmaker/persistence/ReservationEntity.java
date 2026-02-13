package edu.yu.marketmaker.persistence;

import edu.yu.marketmaker.model.Reservation;
import edu.yu.marketmaker.model.ReservationStatus;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * JPA Entity for Reservation records used by Hazelcast MapStore.
 */
@Entity
@Table(name = "reservations")
public class ReservationEntity {

    @Id
    private UUID id;
    private String symbol;
    private int requested;
    private int granted;
    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    // --- Constructors ---

    /**
     * No-args constructor required by JPA.
     */
    public ReservationEntity() {
    }

    /**
     * All-args constructor.
     */
    public ReservationEntity(UUID id, String symbol, int requested, int granted, ReservationStatus status) {
        this.id = id;
        this.symbol = symbol;
        this.requested = requested;
        this.granted = granted;
        this.status = status;
    }

    // --- Conversion Methods ---

    /**
     * Converts this JPA entity back into the immutable Reservation record.
     * @return A Reservation record.
     */
    public Reservation toRecord() {
        return new Reservation(this.id, this.symbol, this.requested, this.granted, this.status);
    }

    /**
     * Static helper to create an Entity from a Record.
     * @param reservation The reservation record.
     * @return A new ReservationEntity.
     */
    public static ReservationEntity fromRecord(Reservation reservation) {
        return new ReservationEntity(
                reservation.id(),
                reservation.symbol(),
                reservation.requested(),
                reservation.granted(),
                reservation.status()
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

    public int getRequested() {
        return requested;
    }

    public void setRequested(int requested) {
        this.requested = requested;
    }

    public int getGranted() {
        return granted;
    }

    public void setGranted(int granted) {
        this.granted = granted;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ReservationEntity{" +
                "id=" + id +
                ", symbol='" + symbol + '\'' +
                ", requested=" + requested +
                ", granted=" + granted +
                ", status=" + status +
                '}';
    }
}

