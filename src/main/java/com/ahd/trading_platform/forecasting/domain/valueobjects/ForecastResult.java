package com.ahd.trading_platform.forecasting.domain.valueobjects;

import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;

import java.time.Instant;
import java.util.List;

/**
 * Value object representing the final result of ARIMA forecasting process.
 * Contains the predicted return and all intermediate calculations for audit and monitoring.
 */
public record ForecastResult(
    TradingInstrument instrument,
    Instant forecastDate,
    double expectedReturn,
    double confidenceLevel,
    List<TimeSeriesCalculation> calculations,
    ForecastMetrics metrics,
    Instant calculatedAt
) {
    
    public ForecastResult {
        if (instrument == null) {
            throw new IllegalArgumentException("Trading instrument cannot be null");
        }
        if (forecastDate == null) {
            throw new IllegalArgumentException("Forecast date cannot be null");
        }
        if (calculations == null || calculations.isEmpty()) {
            throw new IllegalArgumentException("Calculations cannot be null or empty");
        }
        if (metrics == null) {
            throw new IllegalArgumentException("Forecast metrics cannot be null");
        }
        if (calculatedAt == null) {
            throw new IllegalArgumentException("Calculated at timestamp cannot be null");
        }
        
        // Validate confidence level
        if (confidenceLevel < 0.0 || confidenceLevel > 1.0) {
            throw new IllegalArgumentException("Confidence level must be between 0.0 and 1.0");
        }
    }
    
    /**
     * Creates a successful forecast result
     */
    public static ForecastResult successful(
            TradingInstrument instrument,
            Instant forecastDate,
            double expectedReturn,
            double confidenceLevel,
            List<TimeSeriesCalculation> calculations,
            ForecastMetrics metrics) {
        
        return new ForecastResult(
            instrument, forecastDate, expectedReturn, confidenceLevel,
            List.copyOf(calculations), metrics, Instant.now()
        );
    }
    
    /**
     * Gets the final calculation (last step)
     */
    public TimeSeriesCalculation getFinalCalculation() {
        return calculations.get(calculations.size() - 1);
    }
    
    /**
     * Gets calculations for a specific forecast step
     */
    public List<TimeSeriesCalculation> getCalculationsForStep(ForecastStep step) {
        return calculations.stream()
            .filter(calc -> calc.getCurrentStep() == step)
            .toList();
    }
    
    /**
     * Checks if the forecast result is reliable based on confidence level
     */
    public boolean isReliable() {
        return confidenceLevel >= 0.7 && // At least 70% confidence
               metrics.dataPointsUsed() >= 30 && // Sufficient historical data
               !Double.isNaN(expectedReturn) && !Double.isInfinite(expectedReturn);
    }
    
    /**
     * Gets a summary description of the forecast
     */
    public String getSummary() {
        return String.format("ARIMA forecast for %s: %.4f%% expected return (%.1f%% confidence) using %d data points",
            instrument.getCode(),
            expectedReturn * 100,
            confidenceLevel * 100,
            metrics.dataPointsUsed()
        );
    }
}