package edu.yu.marketmaker.persistence.interfaces;

import edu.yu.marketmaker.persistence.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * JPA Repository interface for ReservationEntity persistence.
 */
@Repository
public interface JpaReservationRepository extends JpaRepository<ReservationEntity, UUID> {
}

