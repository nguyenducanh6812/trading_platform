package com.ahd.trading_platform.backtesting.interfaces.camunda;

import com.ahd.trading_platform.backtesting.application.usecases.ValidatePredictionModelDataUseCase;
import com.ahd.trading_platform.backtesting.domain.valueobjects.BacktestPeriod;
import com.ahd.trading_platform.backtesting.domain.valueobjects.InstrumentPair;
import com.ahd.trading_platform.backtesting.domain.valueobjects.PredictionModelVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Camunda Service Delegate for validating prediction expected return data availability.
 * Implements Step 2 of the backtesting validation process.
 */
@Component("validatePredictionExpectedReturnDataServiceDelegate")
@RequiredArgsConstructor
@Slf4j
public class ValidatePredictionExpectedReturnDataServiceDelegate implements JavaDelegate {
    
    private final ValidatePredictionModelDataUseCase validatePredictionModelDataUseCase;
    
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        try {
            // Extract process variables
            String firstInstrument = getRequiredVariable(execution, ProcessVariables.FIRST_INSTRUMENT, String.class);
            String secondInstrument = getRequiredVariable(execution, ProcessVariables.SECOND_INSTRUMENT, String.class);
            String startDateStr = getRequiredVariable(execution, ProcessVariables.START_DATE, String.class);
            String endDateStr = getRequiredVariable(execution, ProcessVariables.END_DATE, String.class);
            String modelVersionStr = getRequiredVariable(execution, ProcessVariables.EXPECTED_RETURN_MODEL, String.class);
            
            String instrumentPairStr = firstInstrument + "-" + secondInstrument;
            log.info("Validating prediction model {} for instrument pair {} over backtest period {} to {}", 
                modelVersionStr, instrumentPairStr, startDateStr, endDateStr);
            log.debug("Raw date strings received - start: '{}' (length: {}), end: '{}' (length: {})", 
                startDateStr, startDateStr.length(), endDateStr, endDateStr.length());
            
            // Parse domain objects
            InstrumentPair instrumentPair = InstrumentPair.fromString(instrumentPairStr);
            BacktestPeriod backtestPeriod = BacktestPeriod.fromDateStrings(startDateStr, endDateStr);
            PredictionModelVersion modelVersion = new PredictionModelVersion(modelVersionStr);
            
            // Execute validation - just check if prediction data exists
            ValidatePredictionModelDataUseCase.PredictionModelValidationResult result = 
                validatePredictionModelDataUseCase.execute(instrumentPair, backtestPeriod, modelVersion);
            
            // Set process variables based on result
            execution.setVariable(ProcessVariables.IS_HAS_PREDICT_EXPECTED_RETURN, result.isValid());
            execution.setVariable(ProcessVariables.PREDICTION_VALIDATION_MESSAGE, result.message());
            
            if (result.isValid()) {
                log.info("Prediction data validation successful for {} with model {}: {}", 
                    instrumentPairStr, modelVersionStr, result.message());
            } else {
                log.warn("Prediction data validation failed for {} with model {}: {}", 
                    instrumentPairStr, modelVersionStr, result.message());
            }
            
        } catch (Exception e) {
            String errorMessage = "Failed to validate prediction model data: " + e.getMessage();
            log.error("Error in prediction model validation: {}", errorMessage, e);
            
            // Set failure variables
            execution.setVariable(ProcessVariables.IS_HAS_PREDICT_EXPECTED_RETURN, false);
            execution.setVariable(ProcessVariables.PREDICTION_VALIDATION_MESSAGE, errorMessage);
            
            // Re-throw to trigger Camunda error handling if needed
            throw new RuntimeException(errorMessage, e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> T getRequiredVariable(DelegateExecution execution, String variableName, Class<T> type) {
        Object value = execution.getVariable(variableName);
        if (value == null) {
            throw new IllegalArgumentException("Required process variable '" + variableName + "' is missing");
        }
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("Process variable '" + variableName + "' must be of type " + type.getSimpleName());
        }
        return (T) value;
    }
}