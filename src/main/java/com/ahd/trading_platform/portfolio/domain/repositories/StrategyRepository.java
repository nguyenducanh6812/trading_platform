package com.ahd.trading_platform.portfolio.domain.repositories;

import com.ahd.trading_platform.portfolio.domain.entities.Strategy;
import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyCategory;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Strategy entities.
 * Manages the master list of available trading strategies.
 */
public interface StrategyRepository {

    /**
     * Finds all active strategies
     */
    List<Strategy> findAllActive();

    /**
     * Finds strategies by category
     */
    List<Strategy> findByCategory(StrategyCategory category);

    /**
     * Finds strategy by code
     */
    Optional<Strategy> findByCode(String code);

    /**
     * Finds strategy by ID
     */
    Optional<Strategy> findById(Long id);

    /**
     * Saves or updates a strategy
     */
    void save(Strategy strategy);

    /**
     * Deletes a strategy
     */
    void delete(Long id);
}
