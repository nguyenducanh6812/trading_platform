package com.ahd.trading_platform.forecasting.domain.repositories;

import com.ahd.trading_platform.forecasting.domain.entities.ARIMAModel;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository contract for ARIMA models.
 * Defines the interface for ARIMA model persistence operations.
 */
public interface ARIMAModelRepository {
    
    /**
     * Finds the active ARIMA model for the specified instrument
     */
    Optional<ARIMAModel> findActiveModelByInstrument(TradingInstrument instrument);
    
    /**
     * Finds ARIMA model by instrument and specific model version
     */
    Optional<ARIMAModel> findByInstrumentAndVersion(TradingInstrument instrument, String modelVersion);
    
    /**
     * Finds all active ARIMA models
     */
    List<ARIMAModel> findAllActiveModels();
    
    /**
     * Finds all models (active and inactive) for the specified instrument
     */
    List<ARIMAModel> findAllModelsByInstrument(TradingInstrument instrument);
    
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
     * Checks if an active model exists for the instrument
     */
    boolean existsActiveModelForInstrument(TradingInstrument instrument);
    
    /**
     * Gets the latest model for each supported instrument
     */
    List<ARIMAModel> findLatestModelForEachInstrument();
}