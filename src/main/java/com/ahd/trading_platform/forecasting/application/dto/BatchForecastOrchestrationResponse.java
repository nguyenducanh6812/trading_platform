package com.ahd.trading_platform.forecasting.application.dto;

import java.util.Map;

/**
 * Response DTO for batch forecast orchestration.
 * Contains ONLY orchestration metadata, no business data like expectedReturn values.
 * Used for Camunda process flow control.
 */
public record BatchForecastOrchestrationResponse(
    int totalInstruments,
    int successfulForecasts,
    Map<String, String> failedInstruments,  // instrument -> error message
    boolean hasCriticalErrors,
    String arimaModelVersion,
    String executionId
) {
    
    public static BatchForecastOrchestrationResponse success(
            int totalInstruments,
            int successfulForecasts, 
            String arimaModelVersion,
            String executionId) {
        return new BatchForecastOrchestrationResponse(
            totalInstruments, 
            successfulForecasts, 
            Map.of(), 
            false, 
            arimaModelVersion, 
            executionId
        );
    }
    
    public static BatchForecastOrchestrationResponse withFailures(
            int totalInstruments,
            int successfulForecasts,
            Map<String, String> failedInstruments,
            boolean hasCriticalErrors,
            String arimaModelVersion,
            String executionId) {
        return new BatchForecastOrchestrationResponse(
            totalInstruments,
            successfulForecasts,
            failedInstruments,
            hasCriticalErrors,
            arimaModelVersion,
            executionId
        );
    }
    
    public String criticalErrorMessage() {
        return "ARIMA forecast failed due to critical errors: " + String.join("; ", failedInstruments.values());
    }
}