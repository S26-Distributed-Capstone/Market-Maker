package edu.yu.marketmaker.exposurereservation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class ExposureReservationAPI {

    private final ExposureReservationService service;

    public ExposureReservationAPI() {
        // In a real Spring app, use dependency injection (@Autowired)
        this.service = new ExposureReservationService(new InMemoryReservationRepository());
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> createReservation(@RequestBody ReservationRequest request) {
        return ResponseEntity.ok(service.createReservation(request));
    }

    @PostMapping("/reservations/{id}/apply-fill")
    public ResponseEntity<FreedCapacityResponse> applyFill(@PathVariable UUID id, @RequestBody ApplyFillRequest request) {
        long freed = service.applyFill(id, request.getFilledQuantity());
        return ResponseEntity.ok(new FreedCapacityResponse(freed));
    }

    @PostMapping("/reservations/{id}/release")
    public ResponseEntity<FreedCapacityResponse> release(@PathVariable UUID id) {
        long freed = service.release(id);
        return ResponseEntity.ok(new FreedCapacityResponse(freed));
    }

    @GetMapping("/exposure")
    public ResponseEntity<ExposureState> getExposure() {
        return ResponseEntity.ok(service.getExposureState());
    }
}
