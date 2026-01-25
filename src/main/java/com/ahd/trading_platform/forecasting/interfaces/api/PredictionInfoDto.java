package com.ahd.trading_platform.forecasting.interfaces.api;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Data Transfer Object for prediction information exposed to other modules.
 * Provides essential prediction data without exposing domain value objects.
 */
public record PredictionInfoDto(
    Instant forecastDate,
    BigDecimal expectedReturn,
    BigDecimal confidenceLevel,
    String modelVersion,
    String predictionStatus,
    int arOrder,
    BigDecimal meanSquaredError,
    int dataPointsUsed
) {

    /**
     * Check if the prediction was successful.
     */
    public boolean isSuccessful() {
        return "SUCCESS".equalsIgnoreCase(predictionStatus);
    }

    /**
     * Check if the prediction has high confidence.
     */
    public boolean hasHighConfidence() {
        return confidenceLevel != null && confidenceLevel.compareTo(new BigDecimal("0.8")) >= 0;
    }
}
