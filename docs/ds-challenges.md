# Market Maker Trading System: Technical Specification

## Distributed Systems Challenges & Solutions

### 1. Naming & Communication
**Challenge:** Ensuring diverse services can discover and interact with each other reliably.
* **Implementation:**
    * **REST/HTTP:** Used for synchronous external orders and quote management via Spring Boot.
    * **TCP:** Facilitates asynchronous, decoupled inter-service communication (e.g., Exchange notifying Trading State of fills).
    * **WebSockets:** Used by the UI and Market Maker nodes for real-time position updates.



### 2. Coordination
**Challenge:** Managing the interaction between autonomous nodes to avoid race conditions.
* **Implementation:**
    * **Leader Election:** Powered by **Zookeeper** to ensure only one Market Maker node manages a specific ticker symbol at a time.
    * **Version Monotony:** The Trading State Service attaches a `position_version` to every update. Consumers (UI and Market Makers) use this to ignore stale or out-of-order data.

### 3. Scheduling
**Challenge:** Efficiently distributing tasks across hardware resources.
* **Implementation:**
    * **Ticker Sharding:** Workload is split by symbol across the Market Maker cluster.
    * **Orchestration:** Managed via **K3s (Kubernetes)** to handle container deployment, scaling, and resource allocation.

### 4. Replication & Consistency
**Challenge:** Maintaining a single "Source of Truth" across distributed databases.
* **Implementation:**
    * **Atomic Reservations:** The Exposure service ensures all reservation requests are atomic and persistent.
    * **Idempotency:** Unique request IDs prevent duplicate fills or reservations from corrupting state during network retries.



### 5. Availability & Fault Tolerance
**Challenge:** Maintaining system uptime during individual component failures.
* **Implementation:**
    * **State Rebuild:** The Exposure service can rebuild its global totals by scanning active transactions in the database upon startup.
    * **Fail-Safe Expiration:** The Exchange automatically rejects trades against expired quotes if a Market Maker node stops refreshing.