package com.ahd.trading_platform.forecasting.domain.repositories;

import com.ahd.trading_platform.forecasting.domain.valueobjects.DemeanDiffOCMasterData;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Domain repository contract for DemeanDiffOC master data.
 * Defines the interface for managing pre-calculated time series transformations
 * that can be reused across different forecasting models.
 */
public interface DemeanDiffOCMasterDataRepository {
    
    /**
     * Finds master data for the specified instrument and time range
     */
    List<DemeanDiffOCMasterData> findByInstrumentAndTimeRange(
        TradingInstrument instrument, 
        TimeRange timeRange,
        String calculationVersion
    );
    
    /**
     * Finds master data for the specified instrument starting from a timestamp
     */
    List<DemeanDiffOCMasterData> findByInstrumentFromTimestamp(
        TradingInstrument instrument,
        Instant fromTimestamp,
        String calculationVersion
    );
    
    /**
     * Checks if master data exists for the specified instrument and time range
     */
    boolean existsForInstrumentAndTimeRange(
        TradingInstrument instrument,
        TimeRange timeRange,
        String calculationVersion
    );
    
    /**
     * Gets the latest timestamp for which master data exists for the instrument
     */
    Optional<Instant> getLatestTimestampForInstrument(
        TradingInstrument instrument,
        String calculationVersion
    );
    
    /**
     * Saves a batch of master data points
     */
    List<DemeanDiffOCMasterData> saveAll(List<DemeanDiffOCMasterData> masterData);
    
    /**
     * Saves a single master data point
     */
    DemeanDiffOCMasterData save(DemeanDiffOCMasterData masterData);
    
    /**
     * Deletes master data for the specified instrument and calculation version
     */
    void deleteByInstrumentAndCalculationVersion(
        TradingInstrument instrument,
        String calculationVersion
    );
    
    /**
     * Counts master data points for the specified instrument and time range
     */
    long countByInstrumentAndTimeRange(
        TradingInstrument instrument,
        TimeRange timeRange,
        String calculationVersion
    );
    
    /**
     * Gets the count of available master data points for the instrument
     */
    long countByInstrument(TradingInstrument instrument, String calculationVersion);
    
    /**
     * Finds master data that needs to be recalculated due to algorithm updates
     */
    List<DemeanDiffOCMasterData> findObsoleteData(String currentCalculationVersion);
    
    /**
     * Gets all calculation versions available for the instrument
     */
    List<String> getAvailableCalculationVersions(TradingInstrument instrument);
    
    /**
     * Gets the most recent calculation version for the instrument
     */
    Optional<String> getLatestCalculationVersion(TradingInstrument instrument);
}