package com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories;

import com.ahd.trading_platform.forecasting.domain.valueobjects.ExpectedReturnPrediction;
import org.springframework.modulith.NamedInterface;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Interface for asset-specific expected return prediction repositories.
 * Abstracts the specific implementation (BTC, ETH, etc.) behind a common interface.
 * Exposed as a named interface for cross-module access.
 */
@NamedInterface("prediction-repositories")
public interface AssetSpecificPredictionRepository {
    
    /**
     * Saves an expected return prediction
     */
    ExpectedReturnPrediction save(ExpectedReturnPrediction prediction);
    
    /**
     * Updates or inserts an expected return prediction (upsert operation)
     * If a prediction with the same forecast date and model version exists, it will be updated.
     * If not, a new prediction will be created.
     */
    ExpectedReturnPrediction upsert(ExpectedReturnPrediction prediction);
    
    /**
     * Saves multiple predictions
     */
    List<ExpectedReturnPrediction> saveAll(List<ExpectedReturnPrediction> predictions);
    
    /**
     * Finds predictions within a date range
     */
    List<ExpectedReturnPrediction> findByDateRange(Instant startDate, Instant endDate);
    
    /**
     * Finds predictions by model version
     */
    List<ExpectedReturnPrediction> findByModelVersion(String modelVersion);
    
    /**
     * Finds prediction for specific date and model version
     */
    Optional<ExpectedReturnPrediction> findByDateAndModelVersion(Instant forecastDate, String modelVersion);
    
    /**
     * Finds latest prediction for a model version
     */
    Optional<ExpectedReturnPrediction> findLatestByModelVersion(String modelVersion);
    
    /**
     * Finds successful predictions only
     */
    List<ExpectedReturnPrediction> findSuccessfulPredictions();
    
    /**
     * Finds predictions by execution ID
     */
    List<ExpectedReturnPrediction> findByExecutionId(String executionId);
    
    /**
     * Counts predictions by model version
     */
    long countByModelVersion(String modelVersion);
    
    /**
     * Checks if prediction exists for date and model version
     */
    boolean existsByDateAndModelVersion(Instant forecastDate, String modelVersion);
    
    /**
     * Deletes predictions older than specified date
     */
    void deleteOlderThan(Instant cutoffDate);
    
    /**
     * Returns the asset symbol this repository handles
     */
    String getAssetSymbol();
}