package edu.yu.marketmaker.persistence.interfaces;

import edu.yu.marketmaker.persistence.QuoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * JPA Repository interface for QuoteEntity persistence.
 */
@Repository
public interface JpaQuoteRepository extends JpaRepository<QuoteEntity, UUID> {
}

