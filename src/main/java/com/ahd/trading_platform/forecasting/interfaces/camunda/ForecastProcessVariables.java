package com.ahd.trading_platform.forecasting.interfaces.camunda;

/**
 * Constants for Forecasting module Camunda process variables.
 * Contains only shared variable names that might be used across multiple workers or components.
 * Worker-specific values like topic names should remain in the worker for better readability.
 */
public final class ForecastProcessVariables {
    
    // Input variables - shared across multiple workers
    public static final String INSTRUMENT_CODES = "instrumentCodes";
    public static final String START_DATE = "startDate";
    public static final String END_DATE = "endDate";
    public static final String USE_DEFAULT_RANGE = "useDefaultRange";
    public static final String INCLUDE_CALCULATION_DETAILS = "includeCalculationDetails";
    
    // Output variables - returned by workers
    public static final String EXECUTION_ID = "executionId";
    public static final String TASK_COMPLETED = "taskCompleted";
    public static final String COMPLETED_AT = "completedAt";
    public static final String FORECAST_RESULTS = "forecastResults";
    public static final String EXPECTED_RETURNS = "expectedReturns";
    public static final String CONFIDENCE_LEVELS = "confidenceLevels";
    public static final String ERROR_MESSAGE = "errorMessage";
    
    // Forecasting-specific variables
    public static final String FORECAST_DATE = "forecastDate";
    public static final String MODEL_VERSION = "modelVersion";
    public static final String DATA_POINTS_USED = "dataPointsUsed";
    public static final String EXECUTION_TIME_MS = "executionTimeMs";
    
    private ForecastProcessVariables() {
        // Prevent instantiation
    }
}