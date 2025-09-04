package com.ahd.trading_platform.marketdata.infrastructure.persistence.repositories;

import com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.BtcPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for BTC price data.
 * Provides optimized queries for Bitcoin-specific price data operations.
 */
@Repository
public interface BtcPriceJpaRepository extends JpaRepository<BtcPriceEntity, Long> {
    
    /**
     * Finds price data within a time range, ordered by timestamp
     */
    @Query("SELECT bp FROM BtcPriceEntity bp WHERE bp.timestamp >= :fromTime AND bp.timestamp <= :toTime ORDER BY bp.timestamp ASC")
    List<BtcPriceEntity> findByTimestampBetweenOrderByTimestampAsc(
        @Param("fromTime") Instant fromTime, 
        @Param("toTime") Instant toTime);
    
    /**
     * Finds the latest price data point
     */
    Optional<BtcPriceEntity> findTopByOrderByTimestampDesc();
    
    /**
     * Finds the earliest price data point
     */
    Optional<BtcPriceEntity> findTopByOrderByTimestampAsc();
    
    /**
     * Checks if price data exists for a specific timestamp
     */
    boolean existsByTimestamp(Instant timestamp);
    
    /**
     * Counts data points within a time range
     */
    @Query("SELECT COUNT(bp) FROM BtcPriceEntity bp WHERE bp.timestamp >= :fromTime AND bp.timestamp <= :toTime")
    long countByTimestampBetween(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);
    
    /**
     * Finds price data after a specific timestamp (for incremental updates)
     */
    @Query("SELECT bp FROM BtcPriceEntity bp WHERE bp.timestamp > :afterTime ORDER BY bp.timestamp ASC")
    List<BtcPriceEntity> findByTimestampAfterOrderByTimestampAsc(@Param("afterTime") Instant afterTime);
    
    /**
     * Finds the highest price within a time range
     */
    @Query("SELECT MAX(bp.highPrice) FROM BtcPriceEntity bp WHERE bp.timestamp >= :fromTime AND bp.timestamp <= :toTime")
    Optional<java.math.BigDecimal> findMaxHighPriceInTimeRange(
        @Param("fromTime") Instant fromTime, 
        @Param("toTime") Instant toTime);
    
    /**
     * Finds the lowest price within a time range
     */
    @Query("SELECT MIN(bp.lowPrice) FROM BtcPriceEntity bp WHERE bp.timestamp >= :fromTime AND bp.timestamp <= :toTime")
    Optional<java.math.BigDecimal> findMinLowPriceInTimeRange(
        @Param("fromTime") Instant fromTime, 
        @Param("toTime") Instant toTime);
    
    /**
     * Calculates total volume within a time range
     */
    @Query("SELECT COALESCE(SUM(bp.volume), 0) FROM BtcPriceEntity bp WHERE bp.timestamp >= :fromTime AND bp.timestamp <= :toTime")
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
    @Query("SELECT bp FROM BtcPriceEntity bp WHERE bp.timestamp >= :fromTime AND bp.timestamp <= :toTime ORDER BY bp.timestamp ASC")
    org.springframework.data.domain.Page<BtcPriceEntity> findByTimestampBetween(
        @Param("fromTime") Instant fromTime, 
        @Param("toTime") Instant toTime,
        org.springframework.data.domain.Pageable pageable);
}