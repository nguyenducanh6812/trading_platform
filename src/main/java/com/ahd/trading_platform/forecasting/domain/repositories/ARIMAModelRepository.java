package com.ahd.trading_platform.forecasting.domain.repositories;

import com.ahd.trading_platform.forecasting.domain.entities.ARIMAModel;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository contract for ARIMA models.
 * Defines the interface for ARIMA model persistence operations.
 * Supports all trading symbols across all markets (SPOT, LINEAR, INVERSE, OPTION).
 */
public interface ARIMAModelRepository {

    /**
     * Finds the active ARIMA model for the specified symbol
     */
    Optional<ARIMAModel> findActiveModelBySymbol(String symbol);

    /**
     * Finds ARIMA model by symbol and specific model version
     */
    Optional<ARIMAModel> findBySymbolAndVersion(String symbol, String modelVersion);

    /**
     * Finds all active ARIMA models
     */
    List<ARIMAModel> findAllActiveModels();

    /**
     * Finds all models (active and inactive) for the specified symbol
     */
    List<ARIMAModel> findAllModelsBySymbol(String symbol);

    /**
     * Saves an ARIMA model
     */
    ARIMAModel save(ARIMAModel model);

    /**
     * Deletes an ARIMA model
     */
    void delete(ARIMAModel model);

    /**
     * Finds model by ID
     */
    Optional<ARIMAModel> findById(Long id);

    /**
     * Checks if an active model exists for the symbol
     */
    boolean existsActiveModelForSymbol(String symbol);

    /**
     * Gets the latest model for each supported symbol
     */
    List<ARIMAModel> findLatestModelForEachSymbol();
}