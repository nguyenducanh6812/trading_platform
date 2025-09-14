package com.ahd.trading_platform.forecasting.domain.valueobjects;

import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain value object representing an expected return prediction result.
 * Contains all the information needed to store ARIMA forecast results.
 */
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
    Instant dataRangeStart,
    Instant dataRangeEnd,
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
            Instant dataRangeStart,
            Instant dataRangeEnd,
            Boolean hasSufficientQuality,
            BigDecimal predictDiffOC,
            BigDecimal predictOC) {
        
        return new ExpectedReturnPrediction(
            executionId, instrument, forecastDate, expectedReturn, confidenceLevel,
            modelVersion, "SUCCESS", summary, dataPointsUsed, arOrder,
            meanSquaredError, standardError, executionTimeMs, dataRangeStart, dataRangeEnd,
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
            null, null, null, null, null, null, errorMessage, null, null, Instant.now()
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
}