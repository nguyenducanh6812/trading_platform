package com.ahd.trading_platform.backtesting.interfaces.camunda;

/**
 * Constants for Camunda process variables used in backtesting workflows.
 * Centralizes variable names to avoid typos and ensure consistency.
 */
public final class ProcessVariables {
    
    // Input variables (from process start form)
    public static final String FIRST_INSTRUMENT = "select_FirstInstrument";
    public static final String SECOND_INSTRUMENT = "select_SecondInstrument";
    public static final String START_DATE = "datetime_start_date";
    public static final String END_DATE = "datetime_end_date";
    public static final String EXPECTED_RETURN_MODEL = "arimaModelVersion";
    
    // Single validation step: Prediction model validation results  
    public static final String IS_HAS_PREDICT_EXPECTED_RETURN = "isHasPredictExpectedReturn";
    public static final String PREDICTION_VALIDATION_MESSAGE = "predictionValidationMessage";
    
    // Instrument pair validation results
    public static final String IS_INSTRUMENT_PAIR_AND_START_DATE_VALIDATED = "isInstrumentPairAndStartDateValidated";
    public static final String INSTRUMENT_PAIR_VALIDATION_MESSAGE = "instrumentPairValidationMessage";
    public static final String ADJUSTED_START_DATE = "adjustedStartDate";
    public static final String ADJUSTED_END_DATE = "adjustedEndDate";
    
    private ProcessVariables() {
        // Utility class - prevent instantiation
    }
}