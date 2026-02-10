package edu.yu.marketmaker.exposurereservation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for the Exposure Reservation API.
 * Provides endpoints to reserve exposure capacity, managing fills, and releasing reservations.
 */
@RestController
public class ExposureReservationAPI {

    private final ExposureReservationService service;

    public ExposureReservationAPI() {
        // In a real Spring app, use dependency injection (@Autowired)
        this.service = new ExposureReservationService(new InMemoryReservationRepository());
    }

    /**
     * POST /reservations
     * Requests exposure capacity for a quote.
     *
     * @param request The reservation request containing symbol and quantity.
     * @return A ReservationResponse with the granted quantity and status (GRANTED, PARTIAL, DENIED).
     */
    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> createReservation(@RequestBody ReservationRequest request) {
        return ResponseEntity.ok(service.createReservation(request));
    }

    /**
     * POST /reservations/{id}/apply-fill
     * Updates an existing reservation after a partial or full fill occurs.
     * Reduces the reserved exposure by the filled amount.
     *
     * @param id The UUID of the reservation.
     * @param request The request containing the filled quantity.
     * @return A response indicating the capacity that was freed up by this fill.
     */
    @PostMapping("/reservations/{id}/apply-fill")
    public ResponseEntity<FreedCapacityResponse> applyFill(@PathVariable UUID id, @RequestBody ApplyFillRequest request) {
        long freed = service.applyFill(id, request.getFilledQuantity());
        return ResponseEntity.ok(new FreedCapacityResponse(freed));
    }

    /**
     * POST /reservations/{id}/release
     * Manually releases a reservation, typically used when a quote is replaced or cancelled
     * without being filled.
     *
     * @param id The UUID of the reservation to release.
     * @return A response indicating the total remaining capacity that was freed.
     */
    @PostMapping("/reservations/{id}/release")
    public ResponseEntity<FreedCapacityResponse> release(@PathVariable UUID id) {
        long freed = service.release(id);
        return ResponseEntity.ok(new FreedCapacityResponse(freed));
    }

    /**
     * GET /exposure
     * Retrieves the current global exposure state.
     * Useful for debugging and monitoring usage versus capacity.
     *
     * @return The current exposure state including usage, total capacity, and active reservation count.
     */
    @GetMapping("/exposure")
    public ResponseEntity<ExposureState> getExposure() {
        return ResponseEntity.ok(service.getExposureState());
    }
}
