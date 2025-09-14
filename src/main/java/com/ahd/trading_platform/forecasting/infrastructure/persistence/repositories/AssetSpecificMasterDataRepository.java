package com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories;

import com.ahd.trading_platform.forecasting.domain.valueobjects.DemeanDiffOCMasterData;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Interface for asset-specific master data repositories.
 * Abstracts the specific implementation (BTC, ETH, etc.) behind a common interface.
 * Similar to AssetSpecificPriceRepository but for pre-calculated master data.
 */
public interface AssetSpecificMasterDataRepository {
    
    /**
     * Finds master data within a time range
     */
    List<DemeanDiffOCMasterData> findByTimeRange(TimeRange timeRange);
    
    /**
     * Finds master data starting from a timestamp
     */
    List<DemeanDiffOCMasterData> findFromTimestamp(Instant fromTimestamp);
    
    /**
     * Checks if master data exists for a time range
     */
    boolean existsForTimeRange(TimeRange timeRange);
    
    /**
     * Gets the latest timestamp for which master data exists
     */
    Optional<Instant> getLatestTimestamp();
    
    /**
     * Saves a batch of master data points
     */
    List<DemeanDiffOCMasterData> saveAll(List<DemeanDiffOCMasterData> masterData);
    
    /**
     * Saves a single master data point
     */
    DemeanDiffOCMasterData save(DemeanDiffOCMasterData masterData);
    
    /**
     * Updates or inserts master data point (upsert operation)
     * If a record with the same timestamp exists, it will be updated.
     * If not, a new record will be created.
     */
    DemeanDiffOCMasterData upsert(DemeanDiffOCMasterData masterData);
    
    /**
     * Deletes all master data for this asset
     */
    void deleteAll();
    
    /**
     * Counts master data points within a time range
     */
    long countByTimeRange(TimeRange timeRange);
    
    /**
     * Gets the count of available master data points
     */
    long count();
    
    /**
     * Returns the asset symbol this repository handles
     */
    String getAssetSymbol();
    
    /**
     * Finds timestamps of existing master data within a time range
     * Used for detecting data gaps
     */
    List<Instant> findTimestampsInRange(Instant from, Instant to);
    
    /**
     * Finds master data with calculated differences within a time range
     */
    List<DemeanDiffOCMasterData> findByTimeRangeWithDifferences(TimeRange timeRange);
    
    /**
     * Counts master data points with calculated differences within a time range
     */
    long countByTimeRangeWithDifferences(TimeRange timeRange);
}