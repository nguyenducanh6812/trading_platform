package com.ahd.trading_platform.forecasting.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for ARIMA forecasting operations.
 * Supports two modes:
 * 1. Current Date Mode (isCurrentDate=true): Predict expected return for today
 * 2. Backtesting Mode (isCurrentDate=false): Calculate expected returns for historical date range
 * Used by REST API and internal service calls.
 */
public record ForecastRequest(
    
    @Schema(description = "Trading instrument code", example = "BTC", allowableValues = {"BTC", "ETH"})
    @NotNull(message = "Instrument code is required")
    @Pattern(regexp = "^(BTC|ETH)$", message = "Instrument code must be BTC or ETH")
    String instrumentCode,
    
    @Schema(description = "Start date for backtesting range (YYYY-MM-DD)", example = "2024-01-01")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Start date must be in YYYY-MM-DD format")
    String startDate,
    
    @Schema(description = "End date for backtesting range (YYYY-MM-DD)", example = "2024-12-31")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "End date must be in YYYY-MM-DD format")
    String endDate,
    
    @Schema(description = "True: predict for current date, False: backtest for date range", example = "true")
    Boolean isCurrentDate,
    
    @Schema(description = "Whether to include detailed calculation steps", example = "false")
    Boolean includeCalculationDetails
    
) {
    
    /**
     * Creates a current date forecast request (for live trading)
     */
    public static ForecastRequest forCurrentDate(String instrumentCode) {
        return new ForecastRequest(instrumentCode, null, null, true, false);
    }
    
    /**
     * Creates a backtesting forecast request (for historical analysis)
     */
    public static ForecastRequest forBacktesting(String instrumentCode, String startDate, String endDate) {
        return new ForecastRequest(instrumentCode, startDate, endDate, false, false);
    }
    
    /**
     * Creates a detailed current date forecast with calculation steps
     */
    public static ForecastRequest forCurrentDateDetailed(String instrumentCode) {
        return new ForecastRequest(instrumentCode, null, null, true, true);
    }
    
    /**
     * Gets the current date mode flag, defaulting to true if null
     */
    public boolean isCurrentDateMode() {
        return isCurrentDate == null || Boolean.TRUE.equals(isCurrentDate);
    }
    
    /**
     * Gets the include calculation details flag, defaulting to false if null
     */
    public boolean shouldIncludeCalculationDetails() {
        return Boolean.TRUE.equals(includeCalculationDetails);
    }
}