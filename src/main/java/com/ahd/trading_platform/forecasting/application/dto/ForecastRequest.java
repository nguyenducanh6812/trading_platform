package com.ahd.trading_platform.forecasting.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for ARIMA forecasting operations.
 * Used by REST API and internal service calls.
 */
public record ForecastRequest(
    
    @Schema(description = "Trading instrument code", example = "BTC", allowableValues = {"BTC", "ETH"})
    @NotNull(message = "Instrument code is required")
    @Pattern(regexp = "^(BTC|ETH)$", message = "Instrument code must be BTC or ETH")
    String instrumentCode,
    
    @Schema(description = "Start date for historical data (YYYY-MM-DD)", example = "2024-01-01")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Start date must be in YYYY-MM-DD format")
    String startDate,
    
    @Schema(description = "End date for historical data (YYYY-MM-DD)", example = "2024-12-31")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "End date must be in YYYY-MM-DD format")
    String endDate,
    
    @Schema(description = "Whether to use default date range", example = "false")
    Boolean useDefaultRange,
    
    @Schema(description = "Whether to include detailed calculation steps", example = "false")
    Boolean includeCalculationDetails
    
) {
    
    /**
     * Creates a simple forecast request for a single instrument
     */
    public static ForecastRequest simple(String instrumentCode) {
        return new ForecastRequest(instrumentCode, null, null, true, false);
    }
    
    /**
     * Creates a detailed forecast request with calculation steps
     */
    public static ForecastRequest detailed(String instrumentCode, String startDate, String endDate) {
        return new ForecastRequest(instrumentCode, startDate, endDate, false, true);
    }
    
    /**
     * Gets the use default range flag, defaulting to false if null
     */
    public boolean shouldUseDefaultRange() {
        return Boolean.TRUE.equals(useDefaultRange);
    }
    
    /**
     * Gets the include calculation details flag, defaulting to false if null
     */
    public boolean shouldIncludeCalculationDetails() {
        return Boolean.TRUE.equals(includeCalculationDetails);
    }
}