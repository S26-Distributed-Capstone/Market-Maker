package edu.yu.marketmaker.persistence.interfaces;

import edu.yu.marketmaker.persistence.PositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * interface for mapping our position to entity for persistence
 */
@Repository
public interface JpaPositionRepository extends JpaRepository<PositionEntity, String> {}