package com.ahd.trading_platform.forecasting.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * DTO representing forecast execution metrics
 */
public record ForecastExecutionMetrics(
    
    @Schema(description = "Number of historical data points used")
    int dataPointsUsed,
    
    @Schema(description = "ARIMA model AR order (number of lags)")
    int arOrder,
    
    @Schema(description = "Model mean squared error")
    double meanSquaredError,
    
    @Schema(description = "Model standard error") 
    double standardError,
    
    @Schema(description = "Execution time in milliseconds")
    long executionTimeMs,
    
    @Schema(description = "Historical data range start date")
    Instant dataRangeStart,
    
    @Schema(description = "Historical data range end date")
    Instant dataRangeEnd,
    
    @Schema(description = "ARIMA model version used")
    String modelVersion,
    
    @Schema(description = "Data quality assessment")
    boolean hasSufficientQuality
    
) {}