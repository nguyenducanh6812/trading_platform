package com.ahd.trading_platform.marketdata.infrastructure.persistence.repositories;

import com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.EthPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for ETH price data.
 * Provides optimized queries for Ethereum-specific price data operations.
 */
@Repository
public interface EthPriceJpaRepository extends JpaRepository<EthPriceEntity, Long> {
    
    /**
     * Finds price data within a time range, ordered by timestamp
     */
    @Query("SELECT ep FROM EthPriceEntity ep WHERE ep.timestamp >= :fromTime AND ep.timestamp <= :toTime ORDER BY ep.timestamp ASC")
    List<EthPriceEntity> findByTimestampBetweenOrderByTimestampAsc(
        @Param("fromTime") Instant fromTime, 
        @Param("toTime") Instant toTime);
    
    /**
     * Finds the latest price data point
     */
    Optional<EthPriceEntity> findTopByOrderByTimestampDesc();
    
    /**
     * Finds the earliest price data point
     */
    Optional<EthPriceEntity> findTopByOrderByTimestampAsc();
    
    /**
     * Checks if price data exists for a specific timestamp
     */
    boolean existsByTimestamp(Instant timestamp);
    
    /**
     * Counts data points within a time range
     */
    @Query("SELECT COUNT(ep) FROM EthPriceEntity ep WHERE ep.timestamp >= :fromTime AND ep.timestamp <= :toTime")
    long countByTimestampBetween(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);
    
    /**
     * Finds price data after a specific timestamp (for incremental updates)
     */
    @Query("SELECT ep FROM EthPriceEntity ep WHERE ep.timestamp > :afterTime ORDER BY ep.timestamp ASC")
    List<EthPriceEntity> findByTimestampAfterOrderByTimestampAsc(@Param("afterTime") Instant afterTime);
    
    /**
     * Finds the highest price within a time range
     */
    @Query("SELECT MAX(ep.highPrice) FROM EthPriceEntity ep WHERE ep.timestamp >= :fromTime AND ep.timestamp <= :toTime")
    Optional<java.math.BigDecimal> findMaxHighPriceInTimeRange(
        @Param("fromTime") Instant fromTime, 
        @Param("toTime") Instant toTime);
    
    /**
     * Finds the lowest price within a time range
     */
    @Query("SELECT MIN(ep.lowPrice) FROM EthPriceEntity ep WHERE ep.timestamp >= :fromTime AND ep.timestamp <= :toTime")
    Optional<java.math.BigDecimal> findMinLowPriceInTimeRange(
        @Param("fromTime") Instant fromTime, 
        @Param("toTime") Instant toTime);
    
    /**
     * Calculates total volume within a time range
     */
    @Query("SELECT COALESCE(SUM(ep.volume), 0) FROM EthPriceEntity ep WHERE ep.timestamp >= :fromTime AND ep.timestamp <= :toTime")
    java.math.BigDecimal getTotalVolumeInTimeRange(
        @Param("fromTime") Instant fromTime, 
        @Param("toTime") Instant toTime);
    
    /**
     * Deletes data points older than specified timestamp
     */
    void deleteByTimestampBefore(Instant timestamp);
    
    /**
     * Gets paginated data for large datasets
     */
    @Query("SELECT ep FROM EthPriceEntity ep WHERE ep.timestamp >= :fromTime AND ep.timestamp <= :toTime ORDER BY ep.timestamp ASC")
    org.springframework.data.domain.Page<EthPriceEntity> findByTimestampBetween(
        @Param("fromTime") Instant fromTime, 
        @Param("toTime") Instant toTime,
        org.springframework.data.domain.Pageable pageable);
    
    /**
     * Finds only timestamps within a time range for gap detection
     * This is much more efficient than loading full entities when we only need timestamps
     */
    @Query("SELECT ep.timestamp FROM EthPriceEntity ep WHERE ep.timestamp >= :fromTime AND ep.timestamp <= :toTime ORDER BY ep.timestamp ASC")
    List<Instant> findTimestampsByDateRange(
        @Param("fromTime") Instant fromTime, 
        @Param("toTime") Instant toTime);
}