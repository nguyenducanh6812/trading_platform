package com.ahd.trading_platform.forecasting.domain.valueobjects;

import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import com.ahd.trading_platform.forecasting.domain.constants.ForecastingConstants;
import org.springframework.modulith.NamedInterface;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Domain value object representing an expected return prediction result.
 * Contains all the information needed to store ARIMA forecast results.
 * Exposed as a named interface for cross-module access.
 */
@NamedInterface("prediction-data")
public record ExpectedReturnPrediction(
    String executionId,
    TradingInstrument instrument,
    Instant forecastDate,
    BigDecimal expectedReturn,
    BigDecimal confidenceLevel,
    String modelVersion,
    String predictionStatus,
    String summary,
    Integer dataPointsUsed,
    Integer arOrder,
    BigDecimal meanSquaredError,
    BigDecimal standardError,
    Long executionTimeMs,
    Boolean hasSufficientQuality,
    String errorMessage,
    BigDecimal predictDiffOC,
    BigDecimal predictOC,
    Instant createdAt
) {
    
    /**
     * Creates a successful prediction
     */
    public static ExpectedReturnPrediction successful(
            String executionId,
            TradingInstrument instrument,
            Instant forecastDate,
            BigDecimal expectedReturn,
            BigDecimal confidenceLevel,
            String modelVersion,
            String summary,
            Integer dataPointsUsed,
            Integer arOrder,
            BigDecimal meanSquaredError,
            BigDecimal standardError,
            Long executionTimeMs,
            Boolean hasSufficientQuality,
            BigDecimal predictDiffOC,
            BigDecimal predictOC) {

        return new ExpectedReturnPrediction(
            executionId, instrument, forecastDate, expectedReturn, confidenceLevel,
            modelVersion, "SUCCESS", summary, dataPointsUsed, arOrder,
            meanSquaredError, standardError, executionTimeMs,
            hasSufficientQuality, null, predictDiffOC, predictOC, Instant.now()
        );
    }
    
    /**
     * Creates a failed prediction
     */
    public static ExpectedReturnPrediction failed(
            String executionId,
            TradingInstrument instrument,
            Instant forecastDate,
            String modelVersion,
            String errorMessage) {
        
        return new ExpectedReturnPrediction(
            executionId, instrument, forecastDate, BigDecimal.ZERO, BigDecimal.ZERO,
            modelVersion, "FAILED", "Forecast execution failed", null, null,
            null, null, null, null, errorMessage, null, null, Instant.now()
        );
    }
    
    /**
     * Checks if the prediction was successful
     */
    public boolean isSuccessful() {
        return "SUCCESS".equals(predictionStatus);
    }
    
    /**
     * Gets expected return as percentage
     */
    public double getExpectedReturnPercent() {
        return expectedReturn != null ? expectedReturn.doubleValue() * 100.0 : 0.0;
    }
    
    /**
     * Gets confidence level as percentage
     */
    public double getConfidencePercent() {
        return confidenceLevel != null ? confidenceLevel.doubleValue() * 100.0 : 0.0;
    }
    
    /**
     * Gets a unique key for this prediction
     */
    public String getUniqueKey() {
        return String.format("%s_%s_%s",
            instrument.getCode(),
            forecastDate.toEpochMilli(),
            modelVersion);
    }

    /**
     * Calculated getter for data range start (backward compatibility).
     * Returns the start date of the historical data range used for this prediction.
     *
     * @return The start date of the historical data range (forecastDate - TOTAL_PREPARATION_DAYS)
     */
    public Instant dataRangeStart() {
        return forecastDate.minus(ForecastingConstants.ARIMA_MODEL_PREPARATION_DAYS, ChronoUnit.DAYS);
    }

    /**
     * Calculated getter for data range end (backward compatibility).
     * Returns the end date of the historical data range used for this prediction.
     *
     * @return The end date of the historical data range (same as forecastDate)
     */
    public Instant dataRangeEnd() {
        return forecastDate;
    }
}