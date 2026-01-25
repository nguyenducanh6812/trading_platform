package com.ahd.trading_platform.portfolio.infrastructure.persistence.repositories;

import com.ahd.trading_platform.portfolio.domain.entities.PortfolioStrategyConfiguration;
import com.ahd.trading_platform.portfolio.domain.repositories.PortfolioStrategyConfigurationRepository;
import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyCategory;
import com.ahd.trading_platform.portfolio.infrastructure.persistence.entities.PortfolioStrategyConfigurationEntity;
import com.ahd.trading_platform.portfolio.infrastructure.persistence.entities.StrategyParameterValueEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of PortfolioStrategyConfigurationRepository using JPA.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PortfolioStrategyConfigurationRepositoryImpl implements PortfolioStrategyConfigurationRepository {

    private final PortfolioStrategyConfigurationJpaRepository configJpaRepository;
    private final StrategyParameterValueJpaRepository paramValueJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioStrategyConfiguration> findByPortfolioId(Long portfolioId) {
        log.debug("Finding all configurations for portfolio: {}", portfolioId);
        return configJpaRepository.findByPortfolioId(portfolioId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioStrategyConfiguration> findActiveByPortfolioId(Long portfolioId) {
        log.debug("Finding active configurations for portfolio: {}", portfolioId);
        return configJpaRepository.findByPortfolioIdAndActiveTrue(portfolioId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioStrategyConfiguration> findByPortfolioIdAndCategory(
        Long portfolioId,
        StrategyCategory category
    ) {
        log.debug("Finding configurations for portfolio {} and category {}", portfolioId, category);
        return configJpaRepository.findByPortfolioIdAndCategory(portfolioId, category).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioStrategyConfiguration> findTopLevelByPortfolioId(Long portfolioId) {
        log.debug("Finding top-level configurations for portfolio: {}", portfolioId);
        return configJpaRepository.findByPortfolioIdAndParentConfigurationIdIsNull(portfolioId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioStrategyConfiguration> findByParentConfigurationId(Long parentId) {
        log.debug("Finding nested configurations for parent: {}", parentId);
        return configJpaRepository.findByParentConfigurationId(parentId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PortfolioStrategyConfiguration> findById(Long id) {
        log.debug("Finding configuration by ID: {}", id);
        return configJpaRepository.findById(id)
            .map(this::toDomain);
    }

    @Override
    public void save(PortfolioStrategyConfiguration configuration) {
        log.debug("Saving configuration for portfolio {} with strategy {}",
            configuration.getPortfolioId(), configuration.getStrategyCode());

        // Save configuration entity
        PortfolioStrategyConfigurationEntity entity = toEntity(configuration);
        PortfolioStrategyConfigurationEntity savedEntity = configJpaRepository.save(entity);

        // Delete old parameter values if updating
        if (savedEntity.getId() != null) {
            paramValueJpaRepository.deleteByConfigurationId(savedEntity.getId());
        }

        // Save parameter values
        List<StrategyParameterValueEntity> paramValues = configuration.getParameterValues().entrySet().stream()
            .map(entry -> new StrategyParameterValueEntity(
                null,
                savedEntity.getId(),
                entry.getKey(),
                entry.getValue()
            ))
            .collect(Collectors.toList());

        paramValueJpaRepository.saveAll(paramValues);

        log.info("Saved configuration ID {} for portfolio {}", savedEntity.getId(), configuration.getPortfolioId());
    }

    @Override
    public void saveAll(List<PortfolioStrategyConfiguration> configurations) {
        log.debug("Batch saving {} configurations", configurations.size());
        configurations.forEach(this::save);
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting configuration ID: {}", id);
        paramValueJpaRepository.deleteByConfigurationId(id);
        configJpaRepository.deleteById(id);
    }

    @Override
    public void deleteByPortfolioId(Long portfolioId) {
        log.info("Deleting all configurations for portfolio: {}", portfolioId);

        // Find all configurations first
        List<PortfolioStrategyConfigurationEntity> configs = configJpaRepository.findByPortfolioId(portfolioId);

        // Delete parameter values for each
        configs.forEach(config -> paramValueJpaRepository.deleteByConfigurationId(config.getId()));

        // Delete configurations
        configJpaRepository.deleteByPortfolioId(portfolioId);
    }

    /**
     * Maps entity to domain, loading parameter values
     */
    private PortfolioStrategyConfiguration toDomain(PortfolioStrategyConfigurationEntity entity) {
        // Load parameter values
        List<StrategyParameterValueEntity> paramValues =
            paramValueJpaRepository.findByConfigurationId(entity.getId());

        Map<String, String> parameters = paramValues.stream()
            .collect(Collectors.toMap(
                StrategyParameterValueEntity::getParameterCode,
                StrategyParameterValueEntity::getParameterValue
            ));

        return new PortfolioStrategyConfiguration(
            entity.getId(),
            entity.getPortfolioId(),
            entity.getStrategyCode(),
            entity.getCategory(),
            parameters,
            entity.getParentConfigurationId(),
            entity.getActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    /**
     * Maps domain to entity (without parameter values)
     */
    private PortfolioStrategyConfigurationEntity toEntity(PortfolioStrategyConfiguration domain) {
        PortfolioStrategyConfigurationEntity entity = new PortfolioStrategyConfigurationEntity();
        entity.setId(domain.getId());
        entity.setPortfolioId(domain.getPortfolioId());
        entity.setStrategyCode(domain.getStrategyCode());
        entity.setCategory(domain.getCategory());
        entity.setParentConfigurationId(domain.getParentConfigurationId());
        entity.setActive(domain.isActive());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
