package com.ahd.trading_platform.forecasting.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.ahd.trading_platform.forecasting.domain.constants.ForecastingConstants;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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

    @Schema(description = "ARIMA model version used")
    String modelVersion,

    @Schema(description = "Data quality assessment")
    boolean hasSufficientQuality,

    @Schema(description = "Forecast date this metrics relates to")
    Instant forecastDate

) {

    /**
     * Calculated getter for data range start (backward compatibility).
     */
    @Schema(description = "Historical data range start date (calculated)")
    public Instant dataRangeStart() {
        return forecastDate.minus(ForecastingConstants.ARIMA_MODEL_PREPARATION_DAYS, ChronoUnit.DAYS);
    }

    /**
     * Calculated getter for data range end (backward compatibility).
     */
    @Schema(description = "Historical data range end date (calculated)")
    public Instant dataRangeEnd() {
        return forecastDate;
    }
}