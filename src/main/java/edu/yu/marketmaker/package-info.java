/**
 * Root package for the Market Maker application.
 *
 * <p>A Spring Boot application that acts as a market maker, managing order flow,
 * position tracking, exposure reservation, and exchange interactions with distributed
 * in-memory state backed by Hazelcast and JPA persistence.
 *
 * <p>Sub-packages:
 * <ul>
 *   <li>{@link edu.yu.marketmaker.config} - Spring and infrastructure configuration
 *       (Hazelcast, network discovery, map store setup).</li>
 *   <li>{@link edu.yu.marketmaker.exchange} - Exchange integration layer for order
 *       validation, dispatching, and fill processing.</li>
 *   <li>{@link edu.yu.marketmaker.exposurereservation} - Exposure reservation API and
 *       service for managing risk capacity before order submission and quoting.</li>
 *   <li>{@link edu.yu.marketmaker.external} - External order publishing and persistent
 *       HTTP connectivity for outbound order flow.</li>
 *   <li>{@link edu.yu.marketmaker.memory} - Generic distributed in-memory repository
 *       abstractions backed by Hazelcast.</li>
 *   <li>{@link edu.yu.marketmaker.model} - Domain model classes (orders, fills, positions,
 *       quotes, reservations, and related value types).</li>
 *   <li>{@link edu.yu.marketmaker.persistence} - JPA-based persistence infrastructure
 *       and Hazelcast {@code MapStore} integrations for durable storage.</li>
 *   <li>{@link edu.yu.marketmaker.service} - Core business services orchestrating
 *       market-making logic and state management.</li>
 *   <li>{@link edu.yu.marketmaker.state} - Trading state management and snapshot
 *       capabilities.</li>
 * </ul>
 *
 * @see edu.yu.marketmaker.MarketmakerApplication
 */
package edu.yu.marketmaker;
