package edu.yu.marketmaker.exposurereservation;

import edu.yu.marketmaker.memory.Repository;
import edu.yu.marketmaker.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ExposureReservationService
 *
 * From requirements.md:
 *   - Global exposure limit of ±100
 *   - Exposure usage computed from quantities of currently active quotes
 *   - Reservations are granted, reduced, or rejected
 *   - Exposure capacity must be released when quote is filled/replaced/expired
 *   - Capacity must not remain reserved for expired or replaced quotes
 *
 * From components.md:
 *   - Grants, reduces, or rejects exposure reservations
 *   - Updates reservations when a quote is partially or fully filled
 *   - Releases reservations when a quote is replaced or expires
 *   - Ensures capacity is not leaked after crashes, retries, or expirations
 */
class ExposureReservationServiceTest {

    private Repository<UUID, Reservation> reservationRepository;
    private ExposureReservationService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        reservationRepository = mock(Repository.class);
        when(reservationRepository.getAll()).thenReturn(Collections.emptyList());
        service = new ExposureReservationService(reservationRepository);
    }

    // Grant, full capacity available

    @Test
    void grantsFullyWhenCapacityAvailable() {
        Quote quote = makeQuote("AAPL", 10);

        ReservationResponse response = service.createReservation(quote);

        assertEquals(ReservationStatus.GRANTED, response.status());
        assertEquals(10, response.grantedQuantity());
        assertNotNull(response.id());
        verify(reservationRepository).put(any(Reservation.class));
    }

    @Test
    void grantsFullCapacityOfExactly100() {
        Quote quote = makeQuote("AAPL", 100);

        ReservationResponse response = service.createReservation(quote);

        assertEquals(ReservationStatus.GRANTED, response.status());
        assertEquals(100, response.grantedQuantity());
    }

    @Test
    void grantsZeroQuantityAsGranted() {
        Quote quote = makeQuote("AAPL", 0);

        ReservationResponse response = service.createReservation(quote);

        assertEquals(ReservationStatus.GRANTED, response.status());
        assertEquals(0, response.grantedQuantity());
    }

    // Partial, quote quantity is reduced deterministically to maximum allowed size

    @Test
    void grantsPartiallyWhenCapacityInsufficient() {
        Reservation existing = new Reservation(UUID.randomUUID(), "GOOG", 95, 95, ReservationStatus.GRANTED);
        when(reservationRepository.getAll()).thenReturn(List.of(existing));

        Quote quote = makeQuote("AAPL", 10);
        ReservationResponse response = service.createReservation(quote);

        assertEquals(ReservationStatus.PARTIAL, response.status());
        assertEquals(5, response.grantedQuantity(), "Only 5 of 100 remaining");
    }

    @Test
    void grantsPartiallyWithExactly1UnitRemaining() {
        Reservation existing = new Reservation(UUID.randomUUID(), "GOOG", 99, 99, ReservationStatus.GRANTED);
        when(reservationRepository.getAll()).thenReturn(List.of(existing));

        Quote quote = makeQuote("AAPL", 50);
        ReservationResponse response = service.createReservation(quote);

        assertEquals(ReservationStatus.PARTIAL, response.status());
        assertEquals(1, response.grantedQuantity());
    }

    // Denied, quote not publishe

    @Test
    void deniedWhenNoCapacityRemaining() {
        Reservation existing = new Reservation(UUID.randomUUID(), "GOOG", 100, 100, ReservationStatus.GRANTED);
        when(reservationRepository.getAll()).thenReturn(List.of(existing));

        Quote quote = makeQuote("AAPL", 10);
        ReservationResponse response = service.createReservation(quote);

        assertEquals(ReservationStatus.DENIED, response.status());
        assertEquals(0, response.grantedQuantity());
    }

    @Test
    void deniedWhenOverCapacity() {
        Reservation existing = new Reservation(UUID.randomUUID(), "GOOG", 120, 120, ReservationStatus.GRANTED);
        when(reservationRepository.getAll()).thenReturn(List.of(existing));

        Quote quote = makeQuote("AAPL", 10);
        ReservationResponse response = service.createReservation(quote);

        assertEquals(ReservationStatus.DENIED, response.status());
        assertEquals(0, response.grantedQuantity());
    }

    // Global capacity is shared across all symbols

    @Test
    void multipleSymbolsShareGlobalCapacity() {
        List<Reservation> existing = List.of(
                new Reservation(UUID.randomUUID(), "AAPL", 30, 30, ReservationStatus.GRANTED),
                new Reservation(UUID.randomUUID(), "GOOG", 40, 40, ReservationStatus.GRANTED)
        );
        when(reservationRepository.getAll()).thenReturn(existing);

        Quote quote = makeQuote("MSFT", 50);
        ReservationResponse response = service.createReservation(quote);

        assertEquals(ReservationStatus.PARTIAL, response.status());
        assertEquals(30, response.grantedQuantity());
    }

    @Test
    void releasedReservationsDoNotCountTowardCapacity() {
        // A reservation with granted=0 (released) should not consume capacity
        List<Reservation> existing = List.of(
                new Reservation(UUID.randomUUID(), "AAPL", 50, 0, ReservationStatus.GRANTED),
                new Reservation(UUID.randomUUID(), "GOOG", 30, 30, ReservationStatus.GRANTED)
        );
        when(reservationRepository.getAll()).thenReturn(existing);

        // Only 30 actually granted, so 70 available
        Quote quote = makeQuote("MSFT", 70);
        ReservationResponse response = service.createReservation(quote);

        assertEquals(ReservationStatus.GRANTED, response.status());
        assertEquals(70, response.grantedQuantity());
    }

    // Apply fill, Exposure reserved: 40, Fill occurs: sell 10, Remaining reserved exposure: 30

    @Test
    void applyFillReducesGrantedAmount() {
        UUID id = UUID.randomUUID();
        Reservation reservation = new Reservation(id, "AAPL", 40, 40, ReservationStatus.GRANTED);
        when(reservationRepository.get(id)).thenReturn(Optional.of(reservation));

        int freed = service.applyFill(id, 10);

        assertEquals(10, freed);
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).put(captor.capture());
        assertEquals(30, captor.getValue().granted());
    }

    @Test
    void applyFillFullyConsumesReservation() {
        UUID id = UUID.randomUUID();
        Reservation reservation = new Reservation(id, "AAPL", 20, 20, ReservationStatus.GRANTED);
        when(reservationRepository.get(id)).thenReturn(Optional.of(reservation));

        int freed = service.applyFill(id, 20);

        assertEquals(20, freed);
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).put(captor.capture());
        assertEquals(0, captor.getValue().granted());
    }

    @Test
    void applyFillClampsToGrantedAmount() {
        // Fill larger than what was granted, should not go negative
        UUID id = UUID.randomUUID();
        Reservation reservation = new Reservation(id, "AAPL", 10, 5, ReservationStatus.PARTIAL);
        when(reservationRepository.get(id)).thenReturn(Optional.of(reservation));

        int freed = service.applyFill(id, 20);

        assertEquals(5, freed, "Cannot free more than granted");
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).put(captor.capture());
        assertEquals(0, captor.getValue().granted());
    }

    @Test
    void applyFillThrowsWhenReservationNotFound() {
        UUID id = UUID.randomUUID();
        when(reservationRepository.get(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.applyFill(id, 5));
    }

    // Release, Exposure associated with an expired quote, must be released

    @Test
    void releaseFreesAllRemainingCapacity() {
        UUID id = UUID.randomUUID();
        Reservation reservation = new Reservation(id, "AAPL", 30, 30, ReservationStatus.GRANTED);
        when(reservationRepository.get(id)).thenReturn(Optional.of(reservation));

        int freed = service.release(id);

        assertEquals(30, freed);
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).put(captor.capture());
        assertEquals(0, captor.getValue().granted());
    }

    @Test
    void releaseAfterPartialFillFreesRemainder() {
        // Was 30, filled 10, now granted=20 -> release frees 20
        UUID id = UUID.randomUUID();
        Reservation reservation = new Reservation(id, "AAPL", 30, 20, ReservationStatus.GRANTED);
        when(reservationRepository.get(id)).thenReturn(Optional.of(reservation));

        int freed = service.release(id);

        assertEquals(20, freed);
    }

    @Test
    void releaseAlreadyEmptyReturnsZero() {
        UUID id = UUID.randomUUID();
        Reservation reservation = new Reservation(id, "AAPL", 30, 0, ReservationStatus.GRANTED);
        when(reservationRepository.get(id)).thenReturn(Optional.of(reservation));

        int freed = service.release(id);

        assertEquals(0, freed);
    }

    @Test
    void releaseThrowsWhenReservationNotFound() {
        UUID id = UUID.randomUUID();
        when(reservationRepository.get(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.release(id));
    }


    // Exposure state, debugging/verification endpoint

    @Test
    void exposureStateReflectsActiveReservations() {
        List<Reservation> reservations = List.of(
                new Reservation(UUID.randomUUID(), "AAPL", 20, 20, ReservationStatus.GRANTED),
                new Reservation(UUID.randomUUID(), "GOOG", 30, 15, ReservationStatus.PARTIAL),
                new Reservation(UUID.randomUUID(), "MSFT", 10, 0, ReservationStatus.DENIED)
        );
        when(reservationRepository.getAll()).thenReturn(reservations);

        ExposureState state = service.getExposureState();

        assertEquals(35, state.currentUsage(), "20 + 15 + 0 = 35");
        assertEquals(100, state.totalCapacity());
        assertEquals(2, state.activeReservations(), "Only 2 have granted > 0");
    }

    @Test
    void exposureStateEmptyWhenNoReservations() {
        ExposureState state = service.getExposureState();

        assertEquals(0, state.currentUsage());
        assertEquals(100, state.totalCapacity());
        assertEquals(0, state.activeReservations());
    }

    // Reservation is persisted, state survives restarts

    @Test
    void reservationIsPersistedOnCreate() {
        Quote quote = makeQuote("AAPL", 10);

        service.createReservation(quote);

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).put(captor.capture());

        Reservation saved = captor.getValue();
        assertEquals("AAPL", saved.symbol());
        assertEquals(10, saved.granted());
        assertEquals(10, saved.requested());
        assertEquals(ReservationStatus.GRANTED, saved.status());
    }

    @Test
    void deniedReservationIsAlsoPersisted() {
        Reservation existing = new Reservation(UUID.randomUUID(), "GOOG", 100, 100, ReservationStatus.GRANTED);
        when(reservationRepository.getAll()).thenReturn(List.of(existing));

        Quote quote = makeQuote("AAPL", 10);
        service.createReservation(quote);

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        // Two puts: one for existing (from mock setup) and one for new
        verify(reservationRepository).put(captor.capture());
        Reservation saved = captor.getValue();
        assertEquals(0, saved.granted());
        assertEquals(ReservationStatus.DENIED, saved.status());
    }


    private Quote makeQuote(String symbol, int askQty) {
        return new Quote(symbol, 99.0, 10, 101.0, askQty, UUID.randomUUID(),
                System.currentTimeMillis() + 30_000);
    }
}