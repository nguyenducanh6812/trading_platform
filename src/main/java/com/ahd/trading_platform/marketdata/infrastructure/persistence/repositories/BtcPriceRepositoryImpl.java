package com.ahd.trading_platform.marketdata.infrastructure.persistence.repositories;

import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
import com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.BtcPriceEntity;
import com.ahd.trading_platform.marketdata.infrastructure.persistence.mappers.MarketDataMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Bitcoin-specific price repository implementation.
 * Handles persistence operations for BTC price data using dedicated BTC table.
 */
@Repository
public class BtcPriceRepositoryImpl implements AssetSpecificPriceRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(BtcPriceRepositoryImpl.class);
    private static final String ASSET_SYMBOL = "BTC";
    
    private final BtcPriceJpaRepository jpaRepository;
    private final MarketDataMapper mapper;
    
    public BtcPriceRepositoryImpl(BtcPriceJpaRepository jpaRepository, MarketDataMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    @Transactional
    public void saveAll(List<OHLCV> ohlcvData) {
        if (ohlcvData == null || ohlcvData.isEmpty()) {
            return;
        }
        
        logger.debug("Saving {} BTC price data points (insert if not exists)", ohlcvData.size());
        
        List<BtcPriceEntity> entities = mapper.toBtcEntities(ohlcvData);
        
        try {
            // Filter out existing data to avoid duplicate key errors
            List<BtcPriceEntity> newEntities = entities.stream()
                .filter(entity -> !jpaRepository.existsByTimestamp(entity.getTimestamp()))
                .toList();
            
            if (newEntities.isEmpty()) {
                logger.info("All {} BTC price data points already exist, skipping save", ohlcvData.size());
                return;
            }
            
            jpaRepository.saveAll(newEntities);
            logger.info("Successfully saved {} new BTC price data points (filtered {} duplicates)", 
                newEntities.size(), entities.size() - newEntities.size());
        } catch (Exception e) {
            logger.error("Failed to save BTC price data", e);
            throw new RuntimeException("Failed to save BTC price data: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<OHLCV> findByTimeRange(TimeRange timeRange) {
        if (timeRange == null) {
            return List.of();
        }
        
        logger.debug("Finding BTC price data for time range: {}", timeRange);
        
        List<BtcPriceEntity> entities = jpaRepository.findByTimestampBetweenOrderByTimestampAsc(
            timeRange.from(), timeRange.to());
        
        List<OHLCV> result = mapper.fromBtcEntities(entities);
        logger.debug("Found {} BTC price data points for time range", result.size());
        
        return result;
    }
    
    @Override
    public List<OHLCV> findAll() {
        logger.debug("Finding all BTC price data");
        
        List<BtcPriceEntity> entities = jpaRepository.findAll();
        entities.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
        
        List<OHLCV> result = mapper.fromBtcEntities(entities);
        logger.debug("Found {} total BTC price data points", result.size());
        
        return result;
    }
    
    @Override
    public Optional<OHLCV> findLatest() {
        logger.debug("Finding latest BTC price data point");
        
        return jpaRepository.findTopByOrderByTimestampDesc()
            .map(mapper::fromBtcEntity);
    }
    
    @Override
    public Optional<OHLCV> findEarliest() {
        logger.debug("Finding earliest BTC price data point");
        
        return jpaRepository.findTopByOrderByTimestampAsc()
            .map(mapper::fromBtcEntity);
    }
    
    @Override
    public long count() {
        long count = jpaRepository.count();
        logger.debug("BTC price data total count: {}", count);
        return count;
    }
    
    @Override
    public long countInTimeRange(TimeRange timeRange) {
        if (timeRange == null) {
            return 0L;
        }
        
        long count = jpaRepository.countByTimestampBetween(timeRange.from(), timeRange.to());
        logger.debug("BTC price data count in time range {}: {}", timeRange, count);
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
        logger.warn("Deleting all BTC price data");
        jpaRepository.deleteAll();
        logger.info("All BTC price data deleted");
    }
    
    @Override
    public String getAssetSymbol() {
        return ASSET_SYMBOL;
    }
    
    @Override
    public List<java.time.Instant> findTimestampsInRange(java.time.Instant from, java.time.Instant to) {
        if (from == null || to == null) {
            return List.of();
        }
        
        logger.debug("Finding BTC price data timestamps for time range: {} to {}", from, to);
        
        // Use JPQL query to get only timestamps for better performance
        List<java.time.Instant> timestamps = jpaRepository.findTimestampsByDateRange(from, to);
        
        logger.debug("Found {} BTC timestamp entries for time range", timestamps.size());
        return timestamps;
    }
}