package edu.yu.marketmaker.persistence.interfaces;

import edu.yu.marketmaker.persistence.ExternalOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * JPA Repository interface for ExternalOrderEntity persistence.
 */
@Repository
public interface JpaExternalOrderRepository extends JpaRepository<ExternalOrderEntity, UUID> {
}

