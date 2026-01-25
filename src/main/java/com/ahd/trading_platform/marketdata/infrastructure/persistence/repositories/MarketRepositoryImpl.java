package com.ahd.trading_platform.marketdata.infrastructure.persistence.repositories;

import com.ahd.trading_platform.marketdata.domain.entities.Market;
import com.ahd.trading_platform.marketdata.domain.repositories.MarketRepository;
import com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.MarketEntity;
import com.ahd.trading_platform.marketdata.infrastructure.persistence.mappers.MarketMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of MarketRepository using JPA.
 * Handles persistence operations for Market reference data.
 */
@Repository
public class MarketRepositoryImpl implements MarketRepository {

    private static final Logger logger = LoggerFactory.getLogger(MarketRepositoryImpl.class);

    private final MarketJpaRepository marketJpaRepository;
    private final MarketMapper mapper;

    public MarketRepositoryImpl(MarketJpaRepository marketJpaRepository, MarketMapper mapper) {
        this.marketJpaRepository = marketJpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Market> findByCode(String code) {
        logger.debug("Finding market by code: {}", code);
        return marketJpaRepository.findByCodeIgnoreCase(code)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Market> findById(Long id) {
        logger.debug("Finding market by id: {}", id);
        return marketJpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Market> findAll() {
        logger.debug("Finding all markets");
        return marketJpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void save(Market market) {
        logger.debug("Saving market: {}", market.getCode());

        // Check if market already exists
        Optional<MarketEntity> existingEntityOpt =
            marketJpaRepository.findByCodeIgnoreCase(market.getCode());

        if (existingEntityOpt.isPresent()) {
            // Update existing market
            MarketEntity existingEntity = existingEntityOpt.get();
            existingEntity.setName(market.getName());
            existingEntity.setDescription(market.getDescription());
            marketJpaRepository.save(existingEntity);
            logger.info("Updated market: {}", market.getCode());
        } else {
            // Create new market
            MarketEntity newEntity = mapper.toEntity(market);
            marketJpaRepository.save(newEntity);
            logger.info("Created new market: {}", market.getCode());
        }
    }
}
