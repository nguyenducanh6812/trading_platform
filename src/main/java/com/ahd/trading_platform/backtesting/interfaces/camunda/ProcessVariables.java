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
    public static final String EXPECTED_RETURN_MODEL = "select_expected_return_model";
    
    // Single validation step: Prediction model validation results  
    public static final String IS_HAS_PREDICT_EXPECTED_RETURN = "isHasPredictExpectedReturn";
    public static final String PREDICTION_VALIDATION_MESSAGE = "predictionValidationMessage";
    
    private ProcessVariables() {
        // Utility class - prevent instantiation
    }
}