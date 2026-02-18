package edu.yu.marketmaker.persistence;

/**
 * Interface for JPA entities that can convert to/from record types.
 * Extends IdentifiableEntity to also provide key extraction.
 *
 * @param <R> the record type this entity converts to/from
 * @param <K> the key/ID type
 */
public interface ConvertibleEntity<R, K> extends IdentifiableEntity<K> {

    /**
     * Converts this entity to its corresponding record.
     * @return the record representation of this entity
     */
    R toRecord();
}

