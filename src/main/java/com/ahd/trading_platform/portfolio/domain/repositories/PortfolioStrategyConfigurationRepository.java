package com.ahd.trading_platform.portfolio.domain.repositories;

import com.ahd.trading_platform.portfolio.domain.entities.PortfolioStrategyConfiguration;
import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyCategory;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PortfolioStrategyConfiguration entities.
 * Manages strategy configurations applied to portfolios.
 */
public interface PortfolioStrategyConfigurationRepository {

    /**
     * Finds all strategy configurations for a portfolio
     */
    List<PortfolioStrategyConfiguration> findByPortfolioId(Long portfolioId);

    /**
     * Finds active strategy configurations for a portfolio
     */
    List<PortfolioStrategyConfiguration> findActiveByPortfolioId(Long portfolioId);

    /**
     * Finds strategy configurations by portfolio and category
     */
    List<PortfolioStrategyConfiguration> findByPortfolioIdAndCategory(
        Long portfolioId,
        StrategyCategory category
    );

    /**
     * Finds top-level (non-nested) strategy configurations for a portfolio
     */
    List<PortfolioStrategyConfiguration> findTopLevelByPortfolioId(Long portfolioId);

    /**
     * Finds nested strategy configurations (children of a parent configuration)
     */
    List<PortfolioStrategyConfiguration> findByParentConfigurationId(Long parentId);

    /**
     * Finds configuration by ID
     */
    Optional<PortfolioStrategyConfiguration> findById(Long id);

    /**
     * Saves or updates a strategy configuration
     */
    void save(PortfolioStrategyConfiguration configuration);

    /**
     * Saves multiple configurations in batch
     */
    void saveAll(List<PortfolioStrategyConfiguration> configurations);

    /**
     * Deletes a configuration
     */
    void delete(Long id);

    /**
     * Deletes all configurations for a portfolio
     */
    void deleteByPortfolioId(Long portfolioId);
}
