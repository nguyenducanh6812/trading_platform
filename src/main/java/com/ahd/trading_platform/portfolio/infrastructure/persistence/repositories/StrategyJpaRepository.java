package com.ahd.trading_platform.portfolio.infrastructure.persistence.repositories;

import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyCategory;
import com.ahd.trading_platform.portfolio.infrastructure.persistence.entities.StrategyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for StrategyEntity.
 */
@Repository
public interface StrategyJpaRepository extends JpaRepository<StrategyEntity, Long> {

    /**
     * Finds strategy by code
     */
    Optional<StrategyEntity> findByCodeIgnoreCase(String code);

    /**
     * Finds all active strategies
     */
    List<StrategyEntity> findByActiveTrue();

    /**
     * Finds strategies by category
     */
    List<StrategyEntity> findByCategory(StrategyCategory category);

    /**
     * Finds active strategies by category
     */
    List<StrategyEntity> findByCategoryAndActiveTrue(StrategyCategory category);

    /**
     * Checks if strategy exists by code
     */
    boolean existsByCodeIgnoreCase(String code);
}
