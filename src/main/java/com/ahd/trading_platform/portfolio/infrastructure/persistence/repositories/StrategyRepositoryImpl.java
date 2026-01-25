package com.ahd.trading_platform.portfolio.infrastructure.persistence.repositories;

import com.ahd.trading_platform.portfolio.domain.entities.Strategy;
import com.ahd.trading_platform.portfolio.domain.repositories.StrategyRepository;
import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyCategory;
import com.ahd.trading_platform.portfolio.infrastructure.persistence.mappers.StrategyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of StrategyRepository using JPA.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
@Transactional
public class StrategyRepositoryImpl implements StrategyRepository {

    private final StrategyJpaRepository jpaRepository;
    private final StrategyMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<Strategy> findAllActive() {
        log.debug("Finding all active strategies");
        return jpaRepository.findByActiveTrue().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Strategy> findByCategory(StrategyCategory category) {
        log.debug("Finding strategies by category: {}", category);
        return jpaRepository.findByCategoryAndActiveTrue(category).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Strategy> findByCode(String code) {
        log.debug("Finding strategy by code: {}", code);
        return jpaRepository.findByCodeIgnoreCase(code)
            .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Strategy> findById(Long id) {
        log.debug("Finding strategy by ID: {}", id);
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public void save(Strategy strategy) {
        log.debug("Saving strategy: {}", strategy.getCode());
        var entity = mapper.toEntity(strategy);
        jpaRepository.save(entity);
        log.info("Saved strategy: {}", strategy.getCode());
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting strategy with ID: {}", id);
        jpaRepository.deleteById(id);
    }
}
