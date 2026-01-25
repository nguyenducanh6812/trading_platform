package com.ahd.trading_platform.portfolio.infrastructure.persistence.repositories;

import com.ahd.trading_platform.portfolio.infrastructure.persistence.entities.StrategyParameterValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA Repository for StrategyParameterValueEntity.
 */
@Repository
public interface StrategyParameterValueJpaRepository extends JpaRepository<StrategyParameterValueEntity, Long> {

    /**
     * Finds all parameter values for a configuration
     */
    List<StrategyParameterValueEntity> findByConfigurationId(Long configurationId);

    /**
     * Deletes all parameter values for a configuration
     */
    void deleteByConfigurationId(Long configurationId);
}
