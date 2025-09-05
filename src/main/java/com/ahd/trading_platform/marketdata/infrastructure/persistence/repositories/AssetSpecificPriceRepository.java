package com.ahd.trading_platform.marketdata.infrastructure.persistence.repositories;

import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;

import java.util.List;
import java.util.Optional;

/**
 * Interface for asset-specific price data repositories.
 * Abstracts the specific implementation (BTC, ETH, etc.) behind a common interface.
 */
public interface AssetSpecificPriceRepository {
    
    /**
     * Saves historical OHLCV data for the asset
     */
    void saveAll(List<OHLCV> ohlcvData);
    
    /**
     * Finds historical data within a time range
     */
    List<OHLCV> findByTimeRange(TimeRange timeRange);
    
    /**
     * Finds all historical data for the asset
     */
    List<OHLCV> findAll();
    
    /**
     * Finds the latest price data point
     */
    Optional<OHLCV> findLatest();
    
    /**
     * Finds the earliest price data point
     */
    Optional<OHLCV> findEarliest();
    
    /**
     * Counts total data points
     */
    long count();
    
    /**
     * Counts data points within a time range
     */
    long countInTimeRange(TimeRange timeRange);
    
    /**
     * Checks if data exists for a time range
     */
    boolean hasDataInTimeRange(TimeRange timeRange);
    
    /**
     * Deletes all data for the asset
     */
    void deleteAll();
    
    /**
     * Returns the asset symbol this repository handles
     */
    String getAssetSymbol();
}