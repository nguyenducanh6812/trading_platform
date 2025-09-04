package com.ahd.trading_platform.forecasting.infrastructure.repositories;

import com.ahd.trading_platform.forecasting.domain.entities.ARIMAModel;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;

import java.util.Optional;

/**
 * Repository interface for ARIMA model entities.
 * Abstracts the persistence layer for ARIMA models.
 */
public interface ARIMAModelRepository {
    
    /**
     * Finds ARIMA model by trading instrument
     */
    Optional<ARIMAModel> findByInstrument(TradingInstrument instrument);
    
    /**
     * Saves or updates an ARIMA model
     */
    ARIMAModel save(ARIMAModel model);
    
    /**
     * Checks if ARIMA model exists for the given instrument
     */
    boolean existsByInstrument(TradingInstrument instrument);
    
    /**
     * Reloads all ARIMA models from master data files
     */
    void reloadAllModels();
}