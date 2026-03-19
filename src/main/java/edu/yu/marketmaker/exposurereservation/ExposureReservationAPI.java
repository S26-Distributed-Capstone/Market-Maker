package edu.yu.marketmaker.exposurereservation;

import edu.yu.marketmaker.memory.Repository;
import edu.yu.marketmaker.model.*;

import edu.yu.marketmaker.service.ServiceHealth;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for the Exposure Reservation API.
 * Provides endpoints to reserve exposure capacity, manage fills, and release reservations.
 */
@RestController
@Profile("exposure-reservation")
public class ExposureReservationAPI {

    private final ExposureReservationService service;

    public ExposureReservationAPI(Repository<UUID, Reservation> repo) {
        this.service = new ExposureReservationService(repo);
    }

    /**
     * POST /reservations
     * Requests exposure capacity for a quote on both bid and ask sides.
     *
     * @param quote The quote containing symbol, bid quantity, and ask quantity.
     * @return A ReservationResponse with granted quantities per side and status (GRANTED, PARTIAL, DENIED).
     */
    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> createReservation(@RequestBody Quote quote) {
        return ResponseEntity.ok(service.createReservation(quote));
    }

    /**
     * TCP/RSocket: request-response route {@code "reservations"}.
     * Creates a reservation with the same behavior as POST /reservations.
     */
    @MessageMapping("reservations")
    public ReservationResponse createReservationMessage(@Payload Quote quote) {
        return service.createReservation(quote);
    }

    /**
     * POST /reservations/{id}/apply-fill
     * Updates an existing reservation after a partial or full fill occurs.
     * Reduces the reserved exposure on the appropriate side by the filled amount.
     *
     * @param id   The UUID of the reservation.
     * @param fill The fill containing quantity and side (BUY or SELL).
     * @return A response indicating the capacity that was freed up by this fill.
     */
    @PostMapping("/reservations/{id}/apply-fill")
    public ResponseEntity<FreedCapacityResponse> applyFill(@PathVariable UUID id, @RequestBody Fill fill) {
        int freed = service.applyFill(id, fill.quantity(), fill.side());
        return ResponseEntity.ok(new FreedCapacityResponse(freed));
    }

    /**
     * TCP/RSocket: request-response route {@code "reservations.{id}.apply-fill"}.
     * Applies a fill with the same behavior as POST /reservations/{id}/apply-fill.
     */
    @MessageMapping("reservations.{id}.apply-fill")
    public FreedCapacityResponse applyFillMessage(@DestinationVariable UUID id, @Payload Fill fill) {
        int freed = service.applyFill(id, fill.quantity(), fill.side());
        return new FreedCapacityResponse(freed);
    }

    /**
     * POST /reservations/{id}/release
     * Manually releases a reservation on both sides, typically used when a quote
     * is replaced, canceled, or expires.
     *
     * @param id The UUID of the reservation to release.
     * @return A response indicating the total capacity that was freed.
     */
    @PostMapping("/reservations/{id}/release")
    public ResponseEntity<FreedCapacityResponse> release(@PathVariable UUID id) {
        int freed = service.release(id);
        return ResponseEntity.ok(new FreedCapacityResponse(freed));
    }

    /**
     * TCP/RSocket: request-response route {@code "reservations.{id}.release"}.
     * Releases a reservation with the same behavior as POST /reservations/{id}/release.
     */
    @MessageMapping("reservations.{id}.release")
    public FreedCapacityResponse releaseMessage(@DestinationVariable UUID id) {
        int freed = service.release(id);
        return new FreedCapacityResponse(freed);
    }

    /**
     * GET /exposure
     * Retrieves the current global exposure state with bid and ask usage.
     *
     * @return The current exposure state including bid/ask usage, total capacity, and active reservation count.
     */
    @GetMapping("/exposure")
    public ResponseEntity<ExposureState> getExposure() {
        return ResponseEntity.ok(service.getExposureState());
    }

    /**
     * TCP/RSocket: request-response route {@code "exposure"}.
     * Retrieves exposure with the same behavior as GET /exposure.
     */
    @MessageMapping("exposure")
    public ExposureState getExposureMessage() {
        return service.getExposureState();
    }

    /**
     * GET /health
     * Health check endpoint to verify the service is running and responsive.
     *
     * @return A ServiceHealth object indicating the health status of the service.
     */
    @GetMapping("/health")
    public ResponseEntity<ServiceHealth> getHealth() {
        return ResponseEntity.ok(new ServiceHealth(true, 0, "Exposure Reservation Service"));
    }

    /**
     * TCP/RSocket: request-response route {@code "health"}.
     * Returns service health with the same behavior as GET /health.
     */
    @MessageMapping("health")
    public ServiceHealth getHealthMessage() {
        return new ServiceHealth(true, 0, "Exposure Reservation Service");
    }
}
