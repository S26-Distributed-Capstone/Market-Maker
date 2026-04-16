# Workflows

## Submitting External Orders
```mermaid
sequenceDiagram
  participant user as User
  participant exchange as Exchange Service
  participant state as Trading State Service
  user->>exchange: Submit order
  alt Order Validation Exception
    exchange->>user: Report failure
  else Order is Valid
    exchange->>exchange: Generate fill
    exchange->>state: Send fill
    exchange->>user: Report success
  end
```

### Order submitted against an expired quote
```mermaid
sequenceDiagram
  participant user as External Order Publisher
  participant exchange as Exchange Service
  user->>exchange: Submit order (POST /orders)
  exchange->>exchange: Look up active quote
  exchange->>exchange: Check expiresAt < currentTime
  Note over exchange: Quote has expired
  exchange->>user: Reject: "Quote is expired"
```
**Outcome:** The exchange checks `expiresAt` before matching. Expired quotes are never filled. The publisher receives a 400 error and may retry later when a fresh quote is available.

### Order quantity exceeds remaining quote quantity (partial fill)
```mermaid
sequenceDiagram
  participant user as External Order Publisher
  participant exchange as Exchange Service
  participant state as Trading State Service
  user->>exchange: Submit order (qty=15)
  exchange->>exchange: Active quote has remaining qty=10
  exchange->>exchange: Partial fill: adjustedQty = min(15, 10) = 10
  exchange->>exchange: Quote remaining qty → 0
  exchange->>state: Send fill (qty=10)
  exchange->>user: Report success (partial fill)
```
**Outcome:** The order is partially filled against the remaining quote quantity. The fill reflects only the actually executed quantity. The quote's remaining quantity is decremented accordingly.

### Concurrent orders on same quote
```mermaid
sequenceDiagram
  participant user1 as Publisher Thread 1
  participant user2 as Publisher Thread 2
  participant exchange as Exchange Service
  participant state as Trading State Service
  user1->>exchange: Submit BUY order (qty=8)
  user2->>exchange: Submit BUY order (qty=8)
  Note over exchange: Quote askQuantity = 10
  exchange->>exchange: Thread 1: adjustedQty = min(8, 10) = 8
  exchange->>exchange: Thread 1: update quote askQty = 2
  exchange->>state: Send fill (qty=8)
  exchange->>exchange: Thread 2: adjustedQty = min(8, 2) = 2
  exchange->>exchange: Thread 2: update quote askQty = 0
  exchange->>state: Send fill (qty=2)
  Note over exchange: Without synchronization on quote updates,
  Note over exchange: race conditions could cause over-fills
```
**Outcome:** If the exchange does not synchronize access to the quote's remaining quantity, two concurrent orders could both read the same remaining quantity and over-fill. Proper locking or atomic operations on the quote are needed to prevent this.

---

## Updating Quote
```mermaid
sequenceDiagram
  participant state as Trading State Service
  participant maker as Market Maker Node
  participant reservation as Reservation Service
  participant exchange as Exchange Service
  state->>maker: Send position update
  maker->>maker: Generate new quote
  maker->>reservation: Update reservation
  reservation->>maker: Send actual reservation
  maker->>exchange: Send updated quote
```

### Reservation is partially granted
```mermaid
sequenceDiagram
  participant state as Trading State Service
  participant maker as Market Maker Node
  participant reservation as Exposure Reservation Service
  participant exchange as Exchange Service
  state->>maker: Position update (position = -80)
  maker->>maker: Generate quote: bid=10, ask=10
  maker->>reservation: Request reservation (ask=10)
  reservation->>reservation: Available capacity = 5
  reservation->>maker: Partial grant (granted=5, status=PARTIAL)
  maker->>maker: Reduce ask quantity to 5
  maker->>exchange: Publish quote (bid=10, ask=5)
  exchange->>exchange: Activate reduced quote
```
**Outcome:** When insufficient exposure capacity exists, the reservation service grants only what is available. The market maker deterministically reduces the quote quantity to match and publishes a smaller quote. This prevents exposure limit violations while still providing some liquidity.

### Reservation denied entirely
```mermaid
sequenceDiagram
  participant state as Trading State Service
  participant maker as Market Maker Node
  participant reservation as Exposure Reservation Service
  state->>maker: Position update
  maker->>maker: Generate quote: bid=10, ask=10
  maker->>reservation: Request reservation (ask=10)
  reservation->>reservation: Available capacity = 0
  reservation->>maker: Denied (granted=0, status=DENIED)
  Note over maker: Cannot publish quote
  Note over maker: Allow existing quote to expire
  Note over maker: Retry on next position update
```
**Outcome:** No exposure capacity is available. The market maker cannot publish a quote for this symbol. The existing quote (if any) expires naturally. The market maker will retry when it receives the next position update or when capacity becomes available.

### Stale position update arrives after a newer one
```mermaid
sequenceDiagram
  participant state as Trading State Service
  participant maker as Market Maker Node
  state->>maker: Position update (version=5)
  maker->>maker: Process and publish quote for version 5
  state->>maker: Position update (version=4, delayed/reordered)
  maker->>maker: Check: version 4 < last seen version 5
  maker->>maker: Discard stale update
  Note over maker: Quote from version 5 remains active
```
**Outcome:** The market maker tracks the last processed position version per symbol. Updates with older versions are discarded to prevent stale quotes from overriding newer state.

### Quote expires before it can be refreshed
```mermaid
sequenceDiagram
  participant maker as Market Maker Node
  participant reservation as Exposure Reservation Service
  participant exchange as Exchange Service
  Note over exchange: Active quote TTL = 30s
  Note over exchange: 30 seconds pass with no refresh
  exchange->>exchange: Quote expires, no longer fillable
  maker->>maker: Detect quote expiration
  maker->>reservation: Release reservation (POST /reservations/{id}/release)
  reservation->>reservation: Free capacity
  maker->>maker: Generate fresh quote
  maker->>reservation: Request new reservation
  reservation->>maker: Granted
  maker->>exchange: Publish new quote (PUT /quotes/{symbol})
```
**Outcome:** When a quote expires, the exchange stops filling it. The market maker detects this and releases the associated reservation, then publishes a fresh quote. This is normal lifecycle behavior.

---

## Streaming Position Data Updates
```mermaid
sequenceDiagram
  participant frontend as Frontend
  participant state as Trading State Service
  participant exchange as Exchange Service
  frontend->>state: Create connection
  loop Until connection is closed
    exchange->>state: Send fill
    state->>state: Generate position
    state->>frontend: Send position update
  end
  frontend->>state: Close connection
```

### UI connects but no positions exist yet
```mermaid
sequenceDiagram
  participant frontend as Position Display UI
  participant state as Trading State Service
  frontend->>state: Connect (RSocket state.stream)
  state->>state: No positions in repository
  state->>frontend: Empty initial snapshot
  Note over frontend: UI shows empty state / "No positions"
  Note over state: Later, first fill arrives
  state->>state: Create first position
  state->>frontend: Send StateSnapshot (first position)
  Note over frontend: UI updates to show new position
```
**Outcome:** The UI handles the empty state gracefully and updates dynamically as positions are created.

### Multiple UI clients connected simultaneously
```mermaid
sequenceDiagram
  participant ui1 as UI Client 1
  participant ui2 as UI Client 2
  participant state as Trading State Service
  ui1->>state: Connect (RSocket state.stream)
  ui2->>state: Connect (RSocket state.stream)
  state->>ui1: Current position snapshot
  state->>ui2: Current position snapshot
  Note over state: Fill arrives
  state->>state: Update position
  state->>ui1: StateSnapshot broadcast
  state->>ui2: StateSnapshot broadcast
  Note over state: Both clients see the same data
```
**Outcome:** The multicast sink broadcasts to all connected subscribers. All UI clients see consistent, real-time position data.
