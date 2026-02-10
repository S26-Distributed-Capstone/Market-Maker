package edu.yu.marketmaker.model;

import edu.yu.marketmaker.exposurereservation.Reservation;

import java.util.UUID;

public record ReservationResponse(
        UUID id,
        Reservation.Status status,
        long grantedQuantity
)
{

}
