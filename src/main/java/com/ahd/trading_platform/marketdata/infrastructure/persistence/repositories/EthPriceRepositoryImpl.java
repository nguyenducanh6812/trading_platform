package com.ahd.trading_platform.marketdata.infrastructure.persistence.repositories;

import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
import com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.EthPriceEntity;
import com.ahd.trading_platform.marketdata.infrastructure.persistence.mappers.MarketDataMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Ethereum-specific price repository implementation.
 * Handles persistence operations for ETH price data using dedicated ETH table.
 */
@Repository
public class EthPriceRepositoryImpl implements AssetSpecificPriceRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(EthPriceRepositoryImpl.class);
    private static final String ASSET_SYMBOL = "ETH";
    
    private final EthPriceJpaRepository jpaRepository;
    private final MarketDataMapper mapper;
    
    public EthPriceRepositoryImpl(EthPriceJpaRepository jpaRepository, MarketDataMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    @Transactional
    public void saveAll(List<OHLCV> ohlcvData) {
        if (ohlcvData == null || ohlcvData.isEmpty()) {
            return;
        }
        
        logger.debug("Saving {} ETH price data points", ohlcvData.size());
        
        List<EthPriceEntity> entities = mapper.toEthEntities(ohlcvData);
        
        try {
            jpaRepository.saveAll(entities);
            logger.info("Successfully saved {} ETH price data points", ohlcvData.size());
        } catch (Exception e) {
            logger.error("Failed to save ETH price data", e);
            throw new RuntimeException("Failed to save ETH price data: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<OHLCV> findByTimeRange(TimeRange timeRange) {
        if (timeRange == null) {
            return List.of();
        }
        
        logger.debug("Finding ETH price data for time range: {}", timeRange);
        
        List<EthPriceEntity> entities = jpaRepository.findByTimestampBetweenOrderByTimestampAsc(
            timeRange.from(), timeRange.to());
        
        List<OHLCV> result = mapper.fromEthEntities(entities);
        logger.debug("Found {} ETH price data points for time range", result.size());
        
        return result;
    }
    
    @Override
    public List<OHLCV> findAll() {
        logger.debug("Finding all ETH price data");
        
        List<EthPriceEntity> entities = jpaRepository.findAll();
        entities.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
        
        List<OHLCV> result = mapper.fromEthEntities(entities);
        logger.debug("Found {} total ETH price data points", result.size());
        
        return result;
    }
    
    @Override
    public Optional<OHLCV> findLatest() {
        logger.debug("Finding latest ETH price data point");
        
        return jpaRepository.findTopByOrderByTimestampDesc()
            .map(mapper::fromEthEntity);
    }
    
    @Override
    public Optional<OHLCV> findEarliest() {
        logger.debug("Finding earliest ETH price data point");
        
        return jpaRepository.findTopByOrderByTimestampAsc()
            .map(mapper::fromEthEntity);
    }
    
    @Override
    public long count() {
        long count = jpaRepository.count();
        logger.debug("ETH price data total count: {}", count);
        return count;
    }
    
    @Override
    public long countInTimeRange(TimeRange timeRange) {
        if (timeRange == null) {
            return 0L;
        }
        
        long count = jpaRepository.countByTimestampBetween(timeRange.from(), timeRange.to());
        logger.debug("ETH price data count in time range {}: {}", timeRange, count);
        return count;
    }
    
    @Override
    public boolean hasDataInTimeRange(TimeRange timeRange) {
        if (timeRange == null) {
            return false;
        }
        
        long count = countInTimeRange(timeRange);
        return count > 0;
    }
    
    @Override
    @Transactional
    public void deleteAll() {
        logger.warn("Deleting all ETH price data");
        jpaRepository.deleteAll();
        logger.info("All ETH price data deleted");
    }
    
    @Override
    public String getAssetSymbol() {
        return ASSET_SYMBOL;
    }
}