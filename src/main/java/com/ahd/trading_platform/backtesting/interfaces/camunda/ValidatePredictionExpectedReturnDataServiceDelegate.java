package com.ahd.trading_platform.backtesting.interfaces.camunda;

import com.ahd.trading_platform.backtesting.application.usecases.ValidatePredictionModelDataUseCase;
import com.ahd.trading_platform.backtesting.domain.valueobjects.BacktestPeriod;
import com.ahd.trading_platform.backtesting.domain.valueobjects.InstrumentPair;
import com.ahd.trading_platform.backtesting.domain.valueobjects.MissingPredictionRange;
import com.ahd.trading_platform.backtesting.domain.valueobjects.PredictionModelVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.List;

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
                
                // Set variables for FetchInstrumentDataTaskWorker to use missing date ranges
                // Use the sophisticated missing range analysis to set precise date ranges
                if (!result.missingRanges().isEmpty()) {
                    setMissingRangeVariables(execution, result.missingRanges());
                } else {
                    // Fallback to original logic if no ranges provided
                    List<String> missingInstruments = result.missingInstruments();
                    String instrumentCodesJson = "[\"" + String.join("\", \"", missingInstruments) + "\"]";
                    execution.setVariable("instrumentCodes", instrumentCodesJson);
                    execution.setVariable("startDate", startDateStr);
                    execution.setVariable("endDate", endDateStr);
                }
                
                log.info("Set process variables for data preparation based on missing prediction ranges");
            }
            
        } catch (Exception e) {
            String errorMessage = "Failed to validate prediction model data: " + e.getMessage();
            log.error("Error in prediction model validation: {}", errorMessage, e);
            
            // Set failure variables
            execution.setVariable(ProcessVariables.IS_HAS_PREDICT_EXPECTED_RETURN, false);
            execution.setVariable(ProcessVariables.PREDICTION_VALIDATION_MESSAGE, errorMessage);
            
            // Even on error, set variables for potential data preparation if dates are available
            try {
                String firstInstrument = getRequiredVariable(execution, ProcessVariables.FIRST_INSTRUMENT, String.class);
                String secondInstrument = getRequiredVariable(execution, ProcessVariables.SECOND_INSTRUMENT, String.class);
                String startDateStr = getRequiredVariable(execution, ProcessVariables.START_DATE, String.class);
                String endDateStr = getRequiredVariable(execution, ProcessVariables.END_DATE, String.class);
                
                // For error recovery, assume both instruments might be missing
                List<String> fallbackInstruments = List.of(firstInstrument, secondInstrument);
                // Convert to JSON string to avoid Java serialization issues
                String instrumentCodesJson = "[\"" + String.join("\", \"", fallbackInstruments) + "\"]";
                execution.setVariable("instrumentCodes", instrumentCodesJson);
                execution.setVariable("startDate", startDateStr);
                execution.setVariable("endDate", endDateStr);
                
                log.info("Set process variables for error recovery: instrumentCodes={}, startDate={}, endDate={}", 
                    instrumentCodesJson, startDateStr, endDateStr);
            } catch (Exception variableError) {
                log.warn("Could not extract variables for error recovery: {}", variableError.getMessage());
            }
            
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
    
    /**
     * Sets process variables based on missing prediction ranges.
     * Uses the most optimal date range from the analysis to minimize data fetching.
     */
    private void setMissingRangeVariables(DelegateExecution execution, List<MissingPredictionRange> missingRanges) {
        // Extract unique instruments that need data
        List<String> missingInstruments = missingRanges.stream()
            .map(MissingPredictionRange::getInstrumentCode)
            .distinct()
            .collect(java.util.stream.Collectors.toList());
        
        // Convert to JSON string for process variables
        String instrumentCodesJson = "[\"" + String.join("\", \"", missingInstruments) + "\"]";
        
        // Find the overall date range that covers all missing ranges
        // This ensures we fetch data for the minimum required period
        String earliestStartDate = missingRanges.stream()
            .map(range -> range.getStartLocalDate().toString())
            .min(String::compareTo)
            .orElse(null);
            
        String latestEndDate = missingRanges.stream()
            .map(range -> range.getEndLocalDate().toString())
            .max(String::compareTo)
            .orElse(null);
        
        // Set process variables
        execution.setVariable("instrumentCodes", instrumentCodesJson);
        execution.setVariable("startDate", earliestStartDate);
        execution.setVariable("endDate", latestEndDate);
        
        // Add detailed missing range information for potential use by other tasks
        String missingRangesDescription = missingRanges.stream()
            .map(MissingPredictionRange::getDescription)
            .collect(java.util.stream.Collectors.joining("; "));
        execution.setVariable("missingRangesDescription", missingRangesDescription);
        
        log.info("Set optimized process variables for {} missing ranges: instruments={}, dateRange={} to {}, details: {}", 
            missingRanges.size(), instrumentCodesJson, earliestStartDate, latestEndDate, missingRangesDescription);
    }
}