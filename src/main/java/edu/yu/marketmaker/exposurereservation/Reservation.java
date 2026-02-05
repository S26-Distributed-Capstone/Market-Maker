package edu.yu.marketmaker.exposurereservation;

import java.util.UUID;

public class Reservation {
    public enum Status { GRANTED, PARTIAL, DENIED }

    private final UUID id;
    private final String symbol;
    private final long requested;
    private long granted;
    private Status status;

    public Reservation(String symbol, long requested, long granted, Status status) {
        this.id = UUID.randomUUID();
        this.symbol = symbol;
        this.requested = requested;
        this.granted = granted;
        this.status = status;
    }

    public long reduceGrantOnFill(long fillQty) {
        // If we filled X, we no longer need to reserve X.
        // We free min(granted, fillQty). Usually granted >= fillQty unless overfilled.
        long toFree = Math.min(this.granted, fillQty);
        this.granted -= toFree;
        return toFree;
    }

    public long releaseRemaining() {
        long freed = this.granted;
        this.granted = 0;
        return freed;
    }

    public UUID getId() { return id; }
    public String getSymbol() { return symbol; }
    public long getGranted() { return granted; }
    public Status getStatus() { return status; }
}

