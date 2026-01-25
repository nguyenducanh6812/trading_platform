package com.ahd.trading_platform.forecasting.infrastructure.repositories;

import com.ahd.trading_platform.forecasting.domain.entities.ARIMAModel;

import java.util.Optional;

/**
 * Repository interface for ARIMA model entities.
 * Abstracts the persistence layer for ARIMA models.
 * Supports all trading symbols across all markets (SPOT, LINEAR, INVERSE, OPTION).
 */
public interface ARIMAModelRepository {

    /**
     * Finds ARIMA model by trading symbol
     */
    Optional<ARIMAModel> findBySymbol(String symbol);

    /**
     * Saves or updates an ARIMA model
     */
    ARIMAModel save(ARIMAModel model);

    /**
     * Checks if ARIMA model exists for the given symbol
     */
    boolean existsBySymbol(String symbol);

    /**
     * Reloads all ARIMA models from master data files
     */
    void reloadAllModels();
}