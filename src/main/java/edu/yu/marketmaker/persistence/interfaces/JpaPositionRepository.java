package edu.yu.marketmaker.persistence.interfaces;

import edu.yu.marketmaker.persistence.BaseJpaRepository;
import edu.yu.marketmaker.persistence.PositionEntity;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository interface for PositionEntity persistence.
 * Spring Data JPA requires concrete interfaces to create proxy implementations.
 */
@Repository
public interface JpaPositionRepository extends BaseJpaRepository<PositionEntity, String> {
}

