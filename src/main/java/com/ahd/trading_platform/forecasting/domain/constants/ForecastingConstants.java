package com.ahd.trading_platform.forecasting.domain.constants;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Constants used across the forecasting module.
 * Centralizes configuration values to improve maintainability.
 */
public final class ForecastingConstants {
    
    private ForecastingConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    // ARIMA Model Configuration
    public static final int DEFAULT_ARIMA_P_ORDER = 30; // 30 AR coefficients
    public static final int DEFAULT_ARIMA_D_ORDER = 0;  // No differencing (using diff_oc directly)
    public static final int DEFAULT_ARIMA_Q_ORDER = 0;  // No moving average terms
    
    // Statistical Configuration
    public static final int CALCULATION_PRECISION = 10;
    public static final BigDecimal MINIMUM_CONFIDENCE_THRESHOLD = BigDecimal.valueOf(0.70);
    public static final BigDecimal MAXIMUM_CONFIDENCE_THRESHOLD = BigDecimal.valueOf(0.99);
    
    // Data Quality Requirements
    public static final int MIN_DATA_POINTS_FOR_FORECAST = 50; // Minimum historical data needed
    public static final int RECOMMENDED_DATA_POINTS_FOR_FORECAST = 100; // Recommended minimum
    public static final double MAX_MISSING_DATA_RATIO = 0.05; // 5% max missing data points
    
    // Forecast Configuration  
    public static final int DEFAULT_FORECAST_LOOKBACK_DAYS = 30;
    public static final int MAX_FORECAST_LOOKBACK_DAYS = 365;
    public static final LocalDate MIN_FORECAST_DATE = LocalDate.of(2021, 1, 1);
    
    // Performance Configuration
    public static final long FORECAST_EXECUTION_TIMEOUT_MS = 600_000L; // 10 minutes
    public static final long LONG_RUNNING_EXECUTION_THRESHOLD_MS = 300_000L; // 5 minutes
    public static final int MAX_CONCURRENT_FORECASTS = 5;
    
    // Coefficient Validation
    public static final BigDecimal MIN_VALID_COEFFICIENT = BigDecimal.valueOf(-2.0);
    public static final BigDecimal MAX_VALID_COEFFICIENT = BigDecimal.valueOf(2.0);
    public static final BigDecimal COEFFICIENT_WARNING_THRESHOLD = BigDecimal.valueOf(1.5);
    
    // Process Integration
    public static final String DEFAULT_CAMUNDA_FORECAST_SOURCE = "CAMUNDA_PROCESS";
    public static final String DEFAULT_API_FORECAST_SOURCE = "REST_API";
    public static final String DEFAULT_AUTO_FORECAST_SOURCE = "AUTO_TRIGGER";
    
    // Error Handling
    public static final int DEFAULT_FORECAST_RETRY_COUNT = 2;
    public static final long DEFAULT_RETRY_DELAY_MS = 60_000L; // 1 minute
    
    // Data Storage
    public static final int FORECAST_RESULT_BATCH_SIZE = 100;
    public static final int CLEANUP_RETENTION_DAYS = 90; // Keep forecast data for 90 days
    
    // Quality Thresholds
    public static final double HIGH_QUALITY_SUCCESS_RATE = 0.90; // 90%+
    public static final double MEDIUM_QUALITY_SUCCESS_RATE = 0.70; // 70%+
    public static final double LOW_QUALITY_SUCCESS_RATE = 0.50; // 50%+
    
    // Monitoring and Alerting
    public static final double ALERT_LOW_SUCCESS_RATE = 0.60; // Alert if success rate < 60%
    public static final int ALERT_MAX_RUNNING_EXECUTIONS = 10; // Alert if too many running
    public static final long ALERT_STUCK_EXECUTION_THRESHOLD_MS = 1_800_000L; // 30 minutes
    
    // Analytics Integration
    public static final String ANALYTICS_EVENT_TYPE = "FORECAST_COMPLETED";
    public static final String PORTFOLIO_OPTIMIZATION_TRIGGER_EVENT = "PORTFOLIO_OPTIMIZATION_READY";
}