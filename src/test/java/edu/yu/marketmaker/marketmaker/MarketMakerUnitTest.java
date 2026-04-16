package edu.yu.marketmaker.marketmaker;

import edu.yu.marketmaker.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for MarketMaker
 *
 * From components.md:
 *   - Routes incoming position snapshots to the quote generator
 *   - Filters snapshots to only managed symbols
 *   - Deduplicates by version to prevent reprocessing the same position state
 *   - Passes the fill from each snapshot to the quote generator for inventory skew
 */
class MarketMakerUnitTest {

    private SnapshotTracker snapshotTracker;
    private QuoteGenerator quoteGenerator;
    private MarketMaker marketMaker;

    @BeforeEach
    void setUp() {
        snapshotTracker = mock(SnapshotTracker.class);
        quoteGenerator = mock(QuoteGenerator.class);
        marketMaker = new MarketMaker(snapshotTracker, quoteGenerator);
    }

    private Position makePosition(String symbol, int netQty, long version) {
        return new Position(symbol, netQty, version, UUID.randomUUID());
    }

    private void runWith(StateSnapshot... snapshots) throws Exception {
        when(snapshotTracker.getPositions()).thenReturn(Flux.just(snapshots));
        marketMaker.run(null);
    }

    // --- Symbol filtering ---

    @Test
    void callsGenerateQuoteForManagedSymbol() throws Exception {
        Position pos = makePosition("AAPL", 0, 1L);
        StateSnapshot snapshot = new StateSnapshot(pos, null);

        when(snapshotTracker.handlesSymbol("AAPL")).thenReturn(true);
        runWith(snapshot);

        verify(quoteGenerator).generateQuote(pos, null);
    }

    @Test
    void doesNotCallGenerateQuoteForUnmanagedSymbol() throws Exception {
        Position pos = makePosition("AAPL", 0, 1L);
        StateSnapshot snapshot = new StateSnapshot(pos, null);

        when(snapshotTracker.handlesSymbol("AAPL")).thenReturn(false);
        runWith(snapshot);

        verify(quoteGenerator, never()).generateQuote(any(), any());
    }

    // --- Null safety ---

    @Test
    void ignoresSnapshotWithNullPosition() throws Exception {
        StateSnapshot snapshot = new StateSnapshot(null, null);
        when(snapshotTracker.getPositions()).thenReturn(Flux.just(snapshot));

        marketMaker.run(null);

        verify(quoteGenerator, never()).generateQuote(any(), any());
    }

    @Test
    void ignoresSnapshotWithNullSymbol() throws Exception {
        Position pos = new Position(null, 0, 1L, null);
        StateSnapshot snapshot = new StateSnapshot(pos, null);
        when(snapshotTracker.getPositions()).thenReturn(Flux.just(snapshot));

        marketMaker.run(null);

        verify(quoteGenerator, never()).generateQuote(any(), any());
    }

    // --- Version deduplication ---

    @Test
    void processesFirstSnapshotForSymbol() throws Exception {
        Position pos = makePosition("AAPL", 0, 1L);

        when(snapshotTracker.handlesSymbol("AAPL")).thenReturn(true);
        runWith(new StateSnapshot(pos, null));

        verify(quoteGenerator, times(1)).generateQuote(pos, null);
    }

    @Test
    void doesNotProcessSameVersionTwice() throws Exception {
        Position pos1 = makePosition("AAPL", 0, 5L);
        Position pos2 = makePosition("AAPL", 0, 5L);

        when(snapshotTracker.handlesSymbol("AAPL")).thenReturn(true);
        runWith(new StateSnapshot(pos1, null), new StateSnapshot(pos2, null));

        verify(quoteGenerator, times(1)).generateQuote(any(), any());
    }

    @Test
    void processesNewerVersionAfterOlder() throws Exception {
        Position pos1 = makePosition("AAPL", 0, 1L);
        Position pos2 = makePosition("AAPL", 5, 2L);

        when(snapshotTracker.handlesSymbol("AAPL")).thenReturn(true);
        runWith(new StateSnapshot(pos1, null), new StateSnapshot(pos2, null));

        verify(quoteGenerator, times(2)).generateQuote(any(), any());
    }

    @Test
    void skipsOlderVersionAfterNewerHasBeenProcessed() throws Exception {
        Position newer = makePosition("AAPL", 0, 10L);
        Position older = makePosition("AAPL", 0, 5L);

        when(snapshotTracker.handlesSymbol("AAPL")).thenReturn(true);
        runWith(new StateSnapshot(newer, null), new StateSnapshot(older, null));

        verify(quoteGenerator, times(1)).generateQuote(any(), any());
    }

    // --- Multiple symbols are independent ---

    @Test
    void processesEachManagedSymbolIndependently() throws Exception {
        Position aaplPos = makePosition("AAPL", 0, 1L);
        Position googPos = makePosition("GOOG", 0, 1L);

        when(snapshotTracker.handlesSymbol("AAPL")).thenReturn(true);
        when(snapshotTracker.handlesSymbol("GOOG")).thenReturn(true);
        runWith(new StateSnapshot(aaplPos, null), new StateSnapshot(googPos, null));

        verify(quoteGenerator, times(2)).generateQuote(any(), any());
    }

    @Test
    void versionDeduplicationIsTrackedPerSymbol() throws Exception {
        // Version 1 for both symbols should each be processed once
        Position aaplV1 = makePosition("AAPL", 0, 1L);
        Position googV1 = makePosition("GOOG", 0, 1L);
        Position aaplV1Duplicate = makePosition("AAPL", 0, 1L);

        when(snapshotTracker.handlesSymbol("AAPL")).thenReturn(true);
        when(snapshotTracker.handlesSymbol("GOOG")).thenReturn(true);
        when(snapshotTracker.getPositions()).thenReturn(Flux.just(
                new StateSnapshot(aaplV1, null),
                new StateSnapshot(googV1, null),
                new StateSnapshot(aaplV1Duplicate, null)
        ));
        marketMaker.run(null);

        verify(quoteGenerator, times(2)).generateQuote(any(), any());
    }

    @Test
    void unmanagedSymbolDoesNotBlockVersionTrackingForOtherSymbols() throws Exception {
        Position aaplPos = makePosition("AAPL", 0, 1L);
        Position googPos = makePosition("GOOG", 0, 1L);

        when(snapshotTracker.handlesSymbol("AAPL")).thenReturn(false);
        when(snapshotTracker.handlesSymbol("GOOG")).thenReturn(true);
        runWith(new StateSnapshot(aaplPos, null), new StateSnapshot(googPos, null));

        verify(quoteGenerator, times(1)).generateQuote(any(), any());
    }

    // --- Fill is passed through ---

    @Test
    void passesFillFromSnapshotToQuoteGenerator() throws Exception {
        Fill fill = new Fill(UUID.randomUUID(), "AAPL", Side.BUY, 10, 100.0, UUID.randomUUID(), System.currentTimeMillis());
        Position pos = makePosition("AAPL", 0, 1L);
        StateSnapshot snapshot = new StateSnapshot(pos, fill);

        when(snapshotTracker.handlesSymbol("AAPL")).thenReturn(true);
        runWith(snapshot);

        verify(quoteGenerator).generateQuote(pos, fill);
    }

    @Test
    void passesNullFillWhenSnapshotHasNoFill() throws Exception {
        Position pos = makePosition("AAPL", 0, 1L);
        StateSnapshot snapshot = new StateSnapshot(pos, null);

        when(snapshotTracker.handlesSymbol("AAPL")).thenReturn(true);
        runWith(snapshot);

        verify(quoteGenerator).generateQuote(pos, null);
    }
}
