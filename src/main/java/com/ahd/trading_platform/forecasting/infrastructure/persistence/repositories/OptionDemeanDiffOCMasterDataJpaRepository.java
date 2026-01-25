package com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories;

import com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.OptionDemeanDiffOCMasterDataEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for OPTION market Demean_Diff_OC master data.
 * Supports multiple trading symbols in the OPTIONS market.
 */
@Repository
public interface OptionDemeanDiffOCMasterDataJpaRepository extends JpaRepository<OptionDemeanDiffOCMasterDataEntity, Long> {

    /**
     * Finds master data for a specific symbol within a time range, ordered by timestamp
     */
    @Query("SELECT od FROM OptionDemeanDiffOCMasterDataEntity od WHERE od.symbol = :symbol AND od.timestamp >= :fromTime AND od.timestamp <= :toTime ORDER BY od.timestamp ASC")
    List<OptionDemeanDiffOCMasterDataEntity> findBySymbolAndTimestampBetweenOrderByTimestampAsc(
        @Param("symbol") String symbol,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime);

    /**
     * Finds the latest master data point for a specific symbol
     */
    Optional<OptionDemeanDiffOCMasterDataEntity> findTopBySymbolOrderByTimestampDesc(String symbol);

    /**
     * Finds the earliest master data point for a specific symbol
     */
    Optional<OptionDemeanDiffOCMasterDataEntity> findTopBySymbolOrderByTimestampAsc(String symbol);

    /**
     * Finds master data by specific symbol and timestamp
     */
    Optional<OptionDemeanDiffOCMasterDataEntity> findBySymbolAndTimestamp(String symbol, Instant timestamp);

    /**
     * Checks if master data exists for a specific symbol and timestamp
     */
    boolean existsBySymbolAndTimestamp(String symbol, Instant timestamp);

    /**
     * Counts data points for a specific symbol within a time range
     */
    @Query("SELECT COUNT(od) FROM OptionDemeanDiffOCMasterDataEntity od WHERE od.symbol = :symbol AND od.timestamp >= :fromTime AND od.timestamp <= :toTime")
    long countBySymbolAndTimestampBetween(
        @Param("symbol") String symbol,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime);

    /**
     * Finds master data after a specific timestamp for a symbol (for incremental updates)
     */
    @Query("SELECT od FROM OptionDemeanDiffOCMasterDataEntity od WHERE od.symbol = :symbol AND od.timestamp > :afterTime ORDER BY od.timestamp ASC")
    List<OptionDemeanDiffOCMasterDataEntity> findBySymbolAndTimestampAfterOrderByTimestampAsc(
        @Param("symbol") String symbol,
        @Param("afterTime") Instant afterTime);

    /**
     * Finds only timestamps within a time range for gap detection for a specific symbol
     * This is much more efficient than loading full entities when we only need timestamps
     */
    @Query("SELECT od.timestamp FROM OptionDemeanDiffOCMasterDataEntity od WHERE od.symbol = :symbol AND od.timestamp >= :fromTime AND od.timestamp <= :toTime ORDER BY od.timestamp ASC")
    List<Instant> findTimestampsBySymbolAndDateRange(
        @Param("symbol") String symbol,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime);

    /**
     * Finds master data with calculated differences within a time range for a specific symbol
     */
    @Query("SELECT od FROM OptionDemeanDiffOCMasterDataEntity od WHERE od.symbol = :symbol AND od.timestamp >= :fromTime AND od.timestamp <= :toTime AND od.hasDifferences = true ORDER BY od.timestamp ASC")
    List<OptionDemeanDiffOCMasterDataEntity> findBySymbolAndTimestampBetweenAndHasDifferencesOrderByTimestampAsc(
        @Param("symbol") String symbol,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime);

    /**
     * Counts master data points with calculated differences within a time range for a specific symbol
     */
    @Query("SELECT COUNT(od) FROM OptionDemeanDiffOCMasterDataEntity od WHERE od.symbol = :symbol AND od.timestamp >= :fromTime AND od.timestamp <= :toTime AND od.hasDifferences = true")
    long countBySymbolAndTimestampBetweenAndHasDifferences(
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
    @Query("SELECT od FROM OptionDemeanDiffOCMasterDataEntity od WHERE od.symbol = :symbol AND od.timestamp >= :fromTime AND od.timestamp <= :toTime ORDER BY od.timestamp ASC")
    Page<OptionDemeanDiffOCMasterDataEntity> findBySymbolAndTimestampBetween(
        @Param("symbol") String symbol,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime,
        Pageable pageable);

    /**
     * Finds all distinct symbols in the OPTION market
     */
    @Query("SELECT DISTINCT od.symbol FROM OptionDemeanDiffOCMasterDataEntity od ORDER BY od.symbol")
    List<String> findAllDistinctSymbols();
}
