package com.ahd.trading_platform.marketdata.infrastructure.persistence.repositories;

import com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.MarketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA Repository for MarketEntity.
 * Provides standard CRUD operations for Market reference data.
 */
@Repository
public interface MarketJpaRepository extends JpaRepository<MarketEntity, Long> {

    /**
     * Finds a market by code (case-insensitive)
     */
    Optional<MarketEntity> findByCodeIgnoreCase(String code);

    /**
     * Checks if a market exists by code
     */
    boolean existsByCodeIgnoreCase(String code);
}
