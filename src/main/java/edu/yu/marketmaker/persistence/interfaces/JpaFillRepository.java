package edu.yu.marketmaker.persistence.interfaces;

import edu.yu.marketmaker.persistence.BaseJpaRepository;
import edu.yu.marketmaker.persistence.FillEntity;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * JPA Repository interface for FillEntity persistence.
 * Spring Data JPA requires concrete interfaces to create proxy implementations.
 */
@Repository
public interface JpaFillRepository extends BaseJpaRepository<FillEntity, UUID> {
}

