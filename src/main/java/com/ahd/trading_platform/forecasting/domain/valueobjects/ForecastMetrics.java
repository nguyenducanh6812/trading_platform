package com.ahd.trading_platform.forecasting.domain.valueobjects;

import java.time.Duration;
import java.time.Instant;

/**
 * Value object containing metrics and metadata about the ARIMA forecasting execution.
 * Used for monitoring, performance analysis, and quality assessment.
 */
public record ForecastMetrics(
    int dataPointsUsed,
    int arOrder,                    // The 'p' parameter (number of AR lags)
    double meanSquaredError,
    double standardError,
    Duration executionTime,
    Instant dataRangeStart,
    Instant dataRangeEnd,
    String modelVersion
) {
    
    public ForecastMetrics {
        if (dataPointsUsed < 0) {
            throw new IllegalArgumentException("Data points used cannot be negative");
        }
        if (arOrder < 0) {
            throw new IllegalArgumentException("AR order cannot be negative");
        }
        if (meanSquaredError < 0) {
            throw new IllegalArgumentException("Mean squared error cannot be negative");
        }
        if (standardError < 0) {
            throw new IllegalArgumentException("Standard error cannot be negative");
        }
        if (executionTime == null || executionTime.isNegative()) {
            throw new IllegalArgumentException("Execution time must be positive");
        }
        if (dataRangeStart == null) {
            throw new IllegalArgumentException("Data range start cannot be null");
        }
        if (dataRangeEnd == null) {
            throw new IllegalArgumentException("Data range end cannot be null");
        }
        if (dataRangeStart.isAfter(dataRangeEnd)) {
            throw new IllegalArgumentException("Data range start must be before end");
        }
        if (modelVersion == null || modelVersion.trim().isEmpty()) {
            throw new IllegalArgumentException("Model version cannot be null or empty");
        }
    }
    
    /**
     * Creates metrics for a successful forecast execution
     */
    public static ForecastMetrics successful(
            int dataPointsUsed,
            int arOrder,
            double meanSquaredError,
            double standardError,
            Duration executionTime,
            Instant dataRangeStart,
            Instant dataRangeEnd,
            String modelVersion) {
        
        return new ForecastMetrics(
            dataPointsUsed, arOrder, meanSquaredError, standardError,
            executionTime, dataRangeStart, dataRangeEnd, modelVersion
        );
    }
    
    /**
     * Gets the data range duration in days
     */
    public long getDataRangeDays() {
        return Duration.between(dataRangeStart, dataRangeEnd).toDays();
    }
    
    /**
     * Calculates the average data points per day
     */
    public double getDataDensity() {
        long rangeDays = getDataRangeDays();
        return rangeDays > 0 ? (double) dataPointsUsed / rangeDays : 0.0;
    }
    
    /**
     * Gets execution time in milliseconds
     */
    public long getExecutionTimeMs() {
        return executionTime.toMillis();
    }
    
    /**
     * Checks if the model has sufficient data quality
     */
    public boolean hasSufficientQuality() {
        return dataPointsUsed >= 30 && // At least 30 data points
               arOrder >= 1 &&        // Valid AR model
               getDataRangeDays() >= 30 && // At least 30 days of data
               !Double.isNaN(meanSquaredError) && !Double.isNaN(standardError);
    }
    
    /**
     * Gets a performance summary
     */
    public String getPerformanceSummary() {
        return String.format("Processed %d points (%.1f days) in %dms - MSE: %.6f, SE: %.6f",
            dataPointsUsed,
            (double) getDataRangeDays(),
            getExecutionTimeMs(),
            meanSquaredError,
            standardError
        );
    }
}