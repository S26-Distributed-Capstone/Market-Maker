/**
 * JPA-based persistence infrastructure for the Market Maker application.
 *
 * <p>This package provides the persistence layer used to back Hazelcast's distributed
 * in-memory storage with durable JPA/database storage via Hazelcast {@code MapStore}
 * integrations:
 * <ul>
 *   <li>{@link edu.yu.marketmaker.persistence.IdentifiableEntity} - Base JPA entity
 *       providing a common identity contract for all persistable domain objects.</li>
 *   <li>{@link edu.yu.marketmaker.persistence.BaseJpaRepository} - Base Spring Data
 *       JPA repository interface for {@code IdentifiableEntity} subclasses.</li>
 *   <li>{@link edu.yu.marketmaker.persistence.ExternalOrderEntity} - JPA entity
 *       representing a persisted {@link edu.yu.marketmaker.model.ExternalOrder}.</li>
 *   <li>{@link edu.yu.marketmaker.persistence.ExternalOrderMapStore} - Hazelcast
 *       {@code MapStore} bridging the {@code ExternalOrder} distributed map to JPA.</li>
 *   <li>{@link edu.yu.marketmaker.persistence.FillEntity} - JPA entity representing
 *       a persisted {@link edu.yu.marketmaker.model.Fill}.</li>
 *   <li>{@link edu.yu.marketmaker.persistence.FillMapStore} - Hazelcast {@code MapStore}
 *       bridging the {@code Fill} distributed map to JPA.</li>
 *   <li>{@link edu.yu.marketmaker.persistence.PositionEntity} - JPA entity representing
 *       a persisted {@link edu.yu.marketmaker.model.Position}.</li>
 *   <li>{@link edu.yu.marketmaker.persistence.PositionMapStore} - Hazelcast {@code MapStore}
 *       bridging the {@code Position} distributed map to JPA.</li>
 * </ul>
 */
package edu.yu.marketmaker.persistence;
