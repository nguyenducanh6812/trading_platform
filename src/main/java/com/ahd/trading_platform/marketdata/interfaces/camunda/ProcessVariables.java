package com.ahd.trading_platform.marketdata.interfaces.camunda;

/**
 * Constants for Camunda process variables.
 * Contains only shared variable names that might be used across multiple workers or components.
 * Worker-specific values like topic names and lock durations should remain in the worker for better readability.
 */
public final class ProcessVariables {
    
    private ProcessVariables() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    // Input variables - shared across multiple workers
    public static final String INSTRUMENT_CODES = "instrumentCodes";
    public static final String START_DATE = "startDate";
    public static final String END_DATE = "endDate";
    public static final String LAUNCH_NEW_INSTRUMENTS = "launchNewInstruments";
    public static final String RESOURCE = "resource";
    
    // Output variables
    public static final String EXECUTION_ID = "executionId";
    public static final String TASK_COMPLETED = "taskCompleted";
    public static final String COMPLETED_AT = "completedAt";
    public static final String INSTRUMENTS_REQUESTED = "instrumentsRequested";
    public static final String INSTRUMENTS_PROCESSED = "instrumentsProcessed";
    public static final String DATA_SOURCE = "dataSource";
}