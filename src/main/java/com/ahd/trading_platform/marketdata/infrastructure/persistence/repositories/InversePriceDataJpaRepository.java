package com.ahd.trading_platform.marketdata.infrastructure.persistence.repositories;

import com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.InversePriceDataEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for INVERSE market price data.
 * Supports multiple trading symbols in the INVERSE perpetual market.
 */
@Repository
public interface InversePriceDataJpaRepository extends JpaRepository<InversePriceDataEntity, Long> {

    /**
     * Finds price data for a specific symbol within a time range, ordered by timestamp
     */
    @Query("SELECT ip FROM InversePriceDataEntity ip WHERE ip.symbol = :symbol AND ip.timestamp >= :fromTime AND ip.timestamp <= :toTime ORDER BY ip.timestamp ASC")
    List<InversePriceDataEntity> findBySymbolAndTimestampBetweenOrderByTimestampAsc(
        @Param("symbol") String symbol,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime);

    /**
     * Finds the latest price data point for a specific symbol
     */
    Optional<InversePriceDataEntity> findTopBySymbolOrderByTimestampDesc(String symbol);

    /**
     * Finds the earliest price data point for a specific symbol
     */
    Optional<InversePriceDataEntity> findTopBySymbolOrderByTimestampAsc(String symbol);

    /**
     * Checks if price data exists for a specific symbol and timestamp
     */
    boolean existsBySymbolAndTimestamp(String symbol, Instant timestamp);

    /**
     * Counts data points for a specific symbol within a time range
     */
    @Query("SELECT COUNT(ip) FROM InversePriceDataEntity ip WHERE ip.symbol = :symbol AND ip.timestamp >= :fromTime AND ip.timestamp <= :toTime")
    long countBySymbolAndTimestampBetween(
        @Param("symbol") String symbol,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime);

    /**
     * Finds price data after a specific timestamp for a symbol (for incremental updates)
     */
    @Query("SELECT ip FROM InversePriceDataEntity ip WHERE ip.symbol = :symbol AND ip.timestamp > :afterTime ORDER BY ip.timestamp ASC")
    List<InversePriceDataEntity> findBySymbolAndTimestampAfterOrderByTimestampAsc(
        @Param("symbol") String symbol,
        @Param("afterTime") Instant afterTime);

    /**
     * Finds the highest price within a time range for a specific symbol
     */
    @Query("SELECT MAX(ip.highPrice) FROM InversePriceDataEntity ip WHERE ip.symbol = :symbol AND ip.timestamp >= :fromTime AND ip.timestamp <= :toTime")
    Optional<BigDecimal> findMaxHighPriceInTimeRange(
        @Param("symbol") String symbol,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime);

    /**
     * Finds the lowest price within a time range for a specific symbol
     */
    @Query("SELECT MIN(ip.lowPrice) FROM InversePriceDataEntity ip WHERE ip.symbol = :symbol AND ip.timestamp >= :fromTime AND ip.timestamp <= :toTime")
    Optional<BigDecimal> findMinLowPriceInTimeRange(
        @Param("symbol") String symbol,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime);

    /**
     * Calculates total volume within a time range for a specific symbol
     */
    @Query("SELECT COALESCE(SUM(ip.volume), 0) FROM InversePriceDataEntity ip WHERE ip.symbol = :symbol AND ip.timestamp >= :fromTime AND ip.timestamp <= :toTime")
    BigDecimal getTotalVolumeInTimeRange(
        @Param("symbol") String symbol,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime);

    /**
     * Deletes data points for a specific symbol older than specified timestamp
     */
    void deleteBySymbolAndTimestampBefore(String symbol, Instant timestamp);

    /**
     * Gets paginated data for large datasets for a specific symbol
     */
    @Query("SELECT ip FROM InversePriceDataEntity ip WHERE ip.symbol = :symbol AND ip.timestamp >= :fromTime AND ip.timestamp <= :toTime ORDER BY ip.timestamp ASC")
    Page<InversePriceDataEntity> findBySymbolAndTimestampBetween(
        @Param("symbol") String symbol,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime,
        Pageable pageable);

    /**
     * Finds only timestamps within a time range for gap detection for a specific symbol
     * This is much more efficient than loading full entities when we only need timestamps
     */
    @Query("SELECT ip.timestamp FROM InversePriceDataEntity ip WHERE ip.symbol = :symbol AND ip.timestamp >= :fromTime AND ip.timestamp <= :toTime ORDER BY ip.timestamp ASC")
    List<Instant> findTimestampsBySymbolAndDateRange(
        @Param("symbol") String symbol,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime);

    /**
     * Finds all distinct symbols in the INVERSE market
     */
    @Query("SELECT DISTINCT ip.symbol FROM InversePriceDataEntity ip ORDER BY ip.symbol")
    List<String> findAllDistinctSymbols();
}
