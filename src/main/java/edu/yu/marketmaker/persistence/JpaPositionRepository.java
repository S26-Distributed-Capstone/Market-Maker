package edu.yu.marketmaker.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPositionRepository extends JpaRepository<PositionEntity, String> {}