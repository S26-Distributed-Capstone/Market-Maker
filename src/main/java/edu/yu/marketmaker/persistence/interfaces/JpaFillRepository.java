package edu.yu.marketmaker.persistence.interfaces;

import edu.yu.marketmaker.persistence.FillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * JPA Repository interface for FillEntity persistence.
 */
@Repository
public interface JpaFillRepository extends JpaRepository<FillEntity, UUID> {
}

