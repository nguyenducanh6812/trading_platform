package com.ahd.trading_platform.portfolio.infrastructure.persistence.repositories;

import com.ahd.trading_platform.portfolio.domain.entities.Portfolio;
import com.ahd.trading_platform.portfolio.domain.repositories.PortfolioRepository;
import com.ahd.trading_platform.portfolio.domain.valueobjects.PortfolioStatus;
import com.ahd.trading_platform.portfolio.infrastructure.persistence.entities.PortfolioEntity;
import com.ahd.trading_platform.portfolio.infrastructure.persistence.mappers.PortfolioEntityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of PortfolioRepository using JPA.
 */
@Repository
@Slf4j
public class PortfolioRepositoryImpl implements PortfolioRepository {

    private final PortfolioJpaRepository jpaRepository;
    private final PortfolioEntityMapper mapper;

    public PortfolioRepositoryImpl(
        PortfolioJpaRepository jpaRepository,
        PortfolioEntityMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Portfolio save(Portfolio portfolio) {
        log.debug("Saving portfolio: {}", portfolio.getId());

        PortfolioEntity entity;
        if (portfolio.getId() != null) {
            // Update existing entity to avoid Hibernate session conflicts
            entity = jpaRepository.findById(portfolio.getId())
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolio.getId()));
            mapper.updateEntity(entity, portfolio);
        } else {
            // Create new entity
            entity = mapper.toEntity(portfolio);
        }

        PortfolioEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Portfolio> findById(Long id) {
        log.debug("Finding portfolio by id: {}", id);
        return jpaRepository.findByIdWithPositionsAndTrades(id)
            .map(mapper::toDomain);
    }

    @Override
    public List<Portfolio> findByUserId(String userId) {
        log.debug("Finding portfolios for user: {}", userId);
        return jpaRepository.findByUserId(userId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Portfolio> findByUserIdAndStatus(String userId, PortfolioStatus status) {
        log.debug("Finding portfolios for user: {} with status: {}", userId, status);
        return jpaRepository.findByUserIdAndStatus(userId, status).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<Portfolio> findByUserIdAndName(String userId, String name) {
        log.debug("Finding portfolio for user: {} with name: {}", userId, name);
        return jpaRepository.findByUserId(userId).stream()
            .filter(p -> p.getName().equals(name))
            .findFirst()
            .map(mapper::toDomain);
    }

    @Override
    public boolean existsByUserIdAndName(String userId, String name) {
        log.debug("Checking if portfolio exists for user: {} with name: {}", userId, name);
        return jpaRepository.existsByUserIdAndName(userId, name);
    }

    @Override
    public void delete(Portfolio portfolio) {
        log.debug("Deleting portfolio: {}", portfolio.getId());
        PortfolioEntity entity = mapper.toEntity(portfolio);
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteById(Long id) {
        log.debug("Deleting portfolio by id: {}", id);
        jpaRepository.deleteById(id);
    }

    @Override
    public List<Portfolio> findAll() {
        log.debug("Finding all portfolios");
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public long countByUserId(String userId) {
        log.debug("Counting portfolios for user: {}", userId);
        return jpaRepository.findByUserId(userId).size();
    }
}
