package com.ahd.trading_platform.forecasting.domain.valueobjects;

/**
 * Represents the five-step ARIMA forecasting process.
 * Each step builds upon the previous one to generate the final expected return prediction.
 */
public enum ForecastStep {
    STEP_0_PREPARE_DATA("Prepare Data - Calculate OC and Diff_OC"),
    STEP_1_AR_LAG_PREPARATION("AR Lag Data Preparation - Create autoregressive lag variables"), 
    STEP_2_PREDICTED_DIFFERENCE("Predicted Difference Calculation - Apply ARIMA coefficients"),
    STEP_3_PREDICTED_OC("Predicted OC Calculation - Convert differences to absolute values"),
    STEP_4_FINAL_RETURN("Final Return Prediction - Calculate expected return ratio");
    
    private final String description;
    
    ForecastStep(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getStepNumber() {
        return ordinal();
    }
}