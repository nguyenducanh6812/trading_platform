package com.ahd.trading_platform.forecasting.domain.repositories;

import com.ahd.trading_platform.forecasting.domain.valueobjects.DemeanDiffOCMasterData;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Domain repository contract for DemeanDiffOC master data operations.
 *
 * This interface belongs to the domain layer and defines the contract for persisting
 * and retrieving pre-calculated Demean_Diff_OC master data. Implementations should
 * route to the appropriate market-specific storage (SPOT, LINEAR, INVERSE, OPTION).
 *
 * Following DDD principles, this interface:
 * - Lives in the domain layer (not infrastructure)
 * - Works with domain objects (DemeanDiffOCMasterData, TimeRange)
 * - Hides infrastructure details from the application layer
 */
public interface MasterDataRepository {

    /**
     * Finds master data within a time range for a specific symbol
     */
    List<DemeanDiffOCMasterData> findByTimeRange(String symbol, TimeRange timeRange);

    /**
     * Finds master data starting from a timestamp for a specific symbol
     */
    List<DemeanDiffOCMasterData> findFromTimestamp(String symbol, Instant fromTimestamp);

    /**
     * Checks if master data exists for a specific symbol within a time range
     */
    boolean existsForTimeRange(String symbol, TimeRange timeRange);

    /**
     * Gets the latest timestamp for which master data exists for a specific symbol
     */
    Optional<Instant> getLatestTimestamp(String symbol);

    /**
     * Saves a batch of master data points for a specific symbol
     */
    List<DemeanDiffOCMasterData> saveAll(String symbol, List<DemeanDiffOCMasterData> masterData);

    /**
     * Saves a single master data point for a specific symbol
     */
    DemeanDiffOCMasterData save(String symbol, DemeanDiffOCMasterData masterData);

    /**
     * Counts master data points within a time range for a specific symbol
     */
    long countByTimeRange(String symbol, TimeRange timeRange);

    /**
     * Finds timestamps of existing master data within a time range for a specific symbol
     * Useful for detecting data gaps
     */
    List<Instant> findTimestampsInRange(String symbol, Instant from, Instant to);

    /**
     * Finds master data with calculated differences within a time range for a specific symbol
     */
    List<DemeanDiffOCMasterData> findByTimeRangeWithDifferences(String symbol, TimeRange timeRange);

    /**
     * Counts master data points with calculated differences within a time range for a specific symbol
     */
    long countByTimeRangeWithDifferences(String symbol, TimeRange timeRange);
}
