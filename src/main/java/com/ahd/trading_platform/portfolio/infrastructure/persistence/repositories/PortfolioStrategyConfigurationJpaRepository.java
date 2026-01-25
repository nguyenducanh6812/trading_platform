package com.ahd.trading_platform.portfolio.infrastructure.persistence.repositories;

import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyCategory;
import com.ahd.trading_platform.portfolio.infrastructure.persistence.entities.PortfolioStrategyConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA Repository for PortfolioStrategyConfigurationEntity.
 */
@Repository
public interface PortfolioStrategyConfigurationJpaRepository extends JpaRepository<PortfolioStrategyConfigurationEntity, Long> {

    /**
     * Finds all configurations for a portfolio
     */
    List<PortfolioStrategyConfigurationEntity> findByPortfolioId(Long portfolioId);

    /**
     * Finds active configurations for a portfolio
     */
    List<PortfolioStrategyConfigurationEntity> findByPortfolioIdAndActiveTrue(Long portfolioId);

    /**
     * Finds configurations by portfolio and category
     */
    List<PortfolioStrategyConfigurationEntity> findByPortfolioIdAndCategory(
        Long portfolioId,
        StrategyCategory category
    );

    /**
     * Finds top-level configurations (non-nested)
     */
    List<PortfolioStrategyConfigurationEntity> findByPortfolioIdAndParentConfigurationIdIsNull(Long portfolioId);

    /**
     * Finds nested configurations by parent ID
     */
    List<PortfolioStrategyConfigurationEntity> findByParentConfigurationId(Long parentId);

    /**
     * Deletes all configurations for a portfolio
     */
    void deleteByPortfolioId(Long portfolioId);
}
