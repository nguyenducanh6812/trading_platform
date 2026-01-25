package com.ahd.trading_platform.forecasting.interfaces.api;

import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;

import java.time.Instant;
import java.util.List;

/**
 * Port interface for Forecasting module operations.
 * Provides access to prediction data and forecasting capabilities for other modules.
 *
 * PUBLIC API: This interface is part of the module's public API and can be used
 * by other modules for cross-module communication.
 */
public interface ForecastingPort {

    /**
     * Counts successful predictions for a specific instrument, model version, and date range.
     *
     * @param instrument The trading instrument
     * @param modelVersion The prediction model version
     * @param startDate Start of the date range (inclusive)
     * @param endDate End of the date range (inclusive)
     * @return Number of successful predictions in the range
     */
    long countSuccessfulPredictions(
        TradingInstrument instrument,
        String modelVersion,
        Instant startDate,
        Instant endDate
    );

    /**
     * Finds successful predictions for a specific instrument, model version, and date range.
     *
     * @param instrument The trading instrument
     * @param modelVersion The prediction model version
     * @param startDate Start of the date range (inclusive)
     * @param endDate End of the date range (inclusive)
     * @return List of successful predictions sorted by forecast date
     */
    List<PredictionInfoDto> findSuccessfulPredictions(
        TradingInstrument instrument,
        String modelVersion,
        Instant startDate,
        Instant endDate
    );

    /**
     * Checks if sufficient predictions are available for the given instrument and date range.
     *
     * @param instrument The trading instrument
     * @param modelVersion The prediction model version
     * @param startDate Start of the date range (inclusive)
     * @param endDate End of the date range (inclusive)
     * @return true if complete prediction coverage exists, false otherwise
     */
    boolean hasCompletePredictionCoverage(
        TradingInstrument instrument,
        String modelVersion,
        Instant startDate,
        Instant endDate
    );
}
