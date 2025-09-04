package com.ahd.trading_platform.forecasting.application.dto;

import com.ahd.trading_platform.forecasting.domain.valueobjects.ForecastStep;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for ARIMA forecasting operations.
 * Contains forecast results and execution metadata.
 */
public record ForecastResponse(
    
    @Schema(description = "Unique execution identifier")
    String executionId,
    
    @Schema(description = "Trading instrument code", example = "BTC")
    String instrumentCode,
    
    @Schema(description = "Predicted expected return as decimal", example = "0.0234")
    double expectedReturn,
    
    @Schema(description = "Prediction confidence level (0.0 to 1.0)", example = "0.85")
    double confidenceLevel,
    
    @Schema(description = "Date for which the forecast applies")
    Instant forecastDate,
    
    @Schema(description = "When the forecast was calculated")
    Instant calculatedAt,
    
    @Schema(description = "Forecast execution status", allowableValues = {"SUCCESS", "FAILED", "PARTIAL"})
    String status,
    
    @Schema(description = "Human-readable forecast summary")
    String summary,
    
    @Schema(description = "Execution metrics and metadata")
    ForecastExecutionMetrics metrics,
    
    @Schema(description = "Detailed calculation steps (optional)")
    List<CalculationStepDto> calculationSteps,
    
    @Schema(description = "Error message if forecast failed")
    String errorMessage
    
) {
    
    /**
     * Creates a successful forecast response
     */
    public static ForecastResponse success(
            String executionId,
            String instrumentCode,
            double expectedReturn,
            double confidenceLevel,
            Instant forecastDate,
            String summary,
            ForecastExecutionMetrics metrics) {
        
        return new ForecastResponse(
            executionId, instrumentCode, expectedReturn, confidenceLevel,
            forecastDate, Instant.now(), "SUCCESS", summary, metrics, null, null
        );
    }
    
    /**
     * Creates a successful forecast response with detailed calculation steps
     */
    public static ForecastResponse successWithDetails(
            String executionId,
            String instrumentCode,
            double expectedReturn,
            double confidenceLevel,
            Instant forecastDate,
            String summary,
            ForecastExecutionMetrics metrics,
            List<CalculationStepDto> calculationSteps) {
        
        return new ForecastResponse(
            executionId, instrumentCode, expectedReturn, confidenceLevel,
            forecastDate, Instant.now(), "SUCCESS", summary, metrics, calculationSteps, null
        );
    }
    
    /**
     * Creates a failed forecast response
     */
    public static ForecastResponse failure(String executionId, String instrumentCode, String errorMessage) {
        return new ForecastResponse(
            executionId, instrumentCode, 0.0, 0.0, null, Instant.now(),
            "FAILED", "Forecast execution failed", null, null, errorMessage
        );
    }
    
    /**
     * Gets the expected return as percentage
     */
    public double getExpectedReturnPercent() {
        return expectedReturn * 100.0;
    }
    
    /**
     * Gets the confidence level as percentage
     */
    public double getConfidencePercent() {
        return confidenceLevel * 100.0;
    }
    
    /**
     * Checks if the forecast was successful
     */
    public boolean isSuccessful() {
        return "SUCCESS".equals(status);
    }
    
    /**
     * Checks if detailed calculation steps are included
     */
    public boolean hasCalculationDetails() {
        return calculationSteps != null && !calculationSteps.isEmpty();
    }
}

