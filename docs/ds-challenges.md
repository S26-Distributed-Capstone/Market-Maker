# Market Maker Trading System: Technical Specification

## System Overview
This is a distributed market-making system designed to accept buy and sell requests and execute them against live quotes while strictly enforcing exposure limits. The system ensures correctness under concurrency and failure by coordinating several specialized services.

### Core Components
* **Exchange Service:** The gateway for external orders and quote maintenance.
* **Trading State Service:** The authoritative source for positions and fill consistency.
* **Exposure Reservation Service:** The gatekeeper for global risk limits.
* **Market Maker Nodes:** Distributed workers that generate and publish quotes.
* **External Order Publisher:** A simulation tool to stress-test the system.
* **Position Display UI:** A real-time visualization tool for monitoring net positions.

---

## Distributed Systems Challenges & Solutions

### 1. Naming & Communication
**Challenge:** Ensuring diverse services can discover and interact with each other reliably.
* **Implementation:**
    * **REST/HTTP:** Used for synchronous external orders and quote management via Spring Boot.
    * **RabbitMQ:** Facilitates asynchronous, decoupled inter-service communication (e.g., Exchange notifying Trading State of fills).
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



---

## Detailed Service Specifications

### Exchange Service
* **Role:** Entry point for orders and quotes.
* **APIs:**
    * `POST /orders`: Place an order (rejects if against expired quotes).
    * `PUT /quotes/{symbol}`: Publish or replace a quote.
    * `GET /quotes/{symbol}`: Retrieve active quote data.

### Trading State Service
* **Role:** Maintains authoritative positions and fill history.
* **Logic:** Consumes exchange events via RabbitMQ; ensures no duplicate fills; increments `position_version` per symbol.

### Exposure Reservation Service
* **Role:** Enforces a global net exposure limit of **±100**.
* **Operational Rules:**
    * **Quote-only Accounting:** Calculates exposure from active quotes rather than current positions.
    * **Automatic Cleanup:** Releases exposure capacity immediately upon quote expiration.
* **APIs:**
    * `POST /reservations`: Request capacity for a quote.
    * `POST /reservations/{id}/apply-fill`: Adjust reservation after a partial fill.

### Market Maker Nodes
* **Role:** Deterministic quote generation based on current positions.
* **Workflow:**
    1.  **Consume:** Listen for position updates from Trading State.
    2.  **Generate:** Calculate bid/ask prices and quantities.
    3.  **Reserve:** Secure exposure from the Exposure Service.
    4.  **Publish:** Push approved quotes to the Exchange.

---

## Scope & Deployment

| Feature | In Scope | Out of Scope |
| :--- | :--- | :--- |
| **Concurrency** | Atomic state updates & coordination | Auditing & Compliance |
| **Scalability** | Horizontal scaling via K3s | Regulatory reporting |
| **Fault Tolerance** | Leader election & state recovery | High-availability DB clusters (Black-boxed) |

### Technical Stack
* **Framework:** Spring Boot
* **Messaging:** RabbitMQ
* **Coordination:** Zookeeper
* **Deployment:** Docker, K3s
* **Database:** Spring Boot compatible SQL (PostgreSQL)