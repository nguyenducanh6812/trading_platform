package com.ahd.trading_platform.marketdata.infrastructure.persistence.repositories;

import com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.OptionPriceDataEntity;
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
 * JPA Repository for OPTION market price data.
 * Supports multiple trading symbols in the OPTIONS market.
 */
@Repository
public interface OptionPriceDataJpaRepository extends JpaRepository<OptionPriceDataEntity, Long> {

    /**
     * Finds price data for a specific symbol within a time range, ordered by timestamp
     */
    @Query("SELECT op FROM OptionPriceDataEntity op WHERE op.symbol = :symbol AND op.timestamp >= :fromTime AND op.timestamp <= :toTime ORDER BY op.timestamp ASC")
    List<OptionPriceDataEntity> findBySymbolAndTimestampBetweenOrderByTimestampAsc(
        @Param("symbol") String symbol,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime);

    /**
     * Finds the latest price data point for a specific symbol
     */
    Optional<OptionPriceDataEntity> findTopBySymbolOrderByTimestampDesc(String symbol);

    /**
     * Finds the earliest price data point for a specific symbol
     */
    Optional<OptionPriceDataEntity> findTopBySymbolOrderByTimestampAsc(String symbol);

    /**
     * Checks if price data exists for a specific symbol and timestamp
     */
    boolean existsBySymbolAndTimestamp(String symbol, Instant timestamp);

    /**
     * Counts data points for a specific symbol within a time range
     */
    @Query("SELECT COUNT(op) FROM OptionPriceDataEntity op WHERE op.symbol = :symbol AND op.timestamp >= :fromTime AND op.timestamp <= :toTime")
    long countBySymbolAndTimestampBetween(
        @Param("symbol") String symbol,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime);

    /**
     * Finds price data after a specific timestamp for a symbol (for incremental updates)
     */
    @Query("SELECT op FROM OptionPriceDataEntity op WHERE op.symbol = :symbol AND op.timestamp > :afterTime ORDER BY op.timestamp ASC")
    List<OptionPriceDataEntity> findBySymbolAndTimestampAfterOrderByTimestampAsc(
        @Param("symbol") String symbol,
        @Param("afterTime") Instant afterTime);

    /**
     * Finds the highest price within a time range for a specific symbol
     */
    @Query("SELECT MAX(op.highPrice) FROM OptionPriceDataEntity op WHERE op.symbol = :symbol AND op.timestamp >= :fromTime AND op.timestamp <= :toTime")
    Optional<BigDecimal> findMaxHighPriceInTimeRange(
        @Param("symbol") String symbol,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime);

    /**
     * Finds the lowest price within a time range for a specific symbol
     */
    @Query("SELECT MIN(op.lowPrice) FROM OptionPriceDataEntity op WHERE op.symbol = :symbol AND op.timestamp >= :fromTime AND op.timestamp <= :toTime")
    Optional<BigDecimal> findMinLowPriceInTimeRange(
        @Param("symbol") String symbol,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime);

    /**
     * Calculates total volume within a time range for a specific symbol
     */
    @Query("SELECT COALESCE(SUM(op.volume), 0) FROM OptionPriceDataEntity op WHERE op.symbol = :symbol AND op.timestamp >= :fromTime AND op.timestamp <= :toTime")
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
    @Query("SELECT op FROM OptionPriceDataEntity op WHERE op.symbol = :symbol AND op.timestamp >= :fromTime AND op.timestamp <= :toTime ORDER BY op.timestamp ASC")
    Page<OptionPriceDataEntity> findBySymbolAndTimestampBetween(
        @Param("symbol") String symbol,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime,
        Pageable pageable);

    /**
     * Finds only timestamps within a time range for gap detection for a specific symbol
     * This is much more efficient than loading full entities when we only need timestamps
     */
    @Query("SELECT op.timestamp FROM OptionPriceDataEntity op WHERE op.symbol = :symbol AND op.timestamp >= :fromTime AND op.timestamp <= :toTime ORDER BY op.timestamp ASC")
    List<Instant> findTimestampsBySymbolAndDateRange(
        @Param("symbol") String symbol,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime);

    /**
     * Finds all distinct symbols in the OPTION market
     */
    @Query("SELECT DISTINCT op.symbol FROM OptionPriceDataEntity op ORDER BY op.symbol")
    List<String> findAllDistinctSymbols();
}
