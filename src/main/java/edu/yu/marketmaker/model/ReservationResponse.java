package edu.yu.marketmaker.model;


import java.util.UUID;

public record ReservationResponse(
        UUID id,
        ReservationStatus status,
        int grantedQuantity
)
{

}
