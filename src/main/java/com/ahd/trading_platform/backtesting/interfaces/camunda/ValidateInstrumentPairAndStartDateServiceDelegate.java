package com.ahd.trading_platform.backtesting.interfaces.camunda;

import com.ahd.trading_platform.backtesting.domain.services.InstrumentPairValidator;
import com.ahd.trading_platform.backtesting.domain.valueobjects.BacktestPeriod;
import com.ahd.trading_platform.backtesting.domain.valueobjects.InstrumentPair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Camunda Service Delegate for validating instrument pairs and adjusting backtest start dates.
 * Ensures both instruments in the pair have sufficient historical data for the requested period.
 * Automatically adjusts the backtest start date if needed based on instrument trading history.
 */
@Component("validateInstrumentPairAndStartDateServiceDelegate")
@RequiredArgsConstructor
@Slf4j
public class ValidateInstrumentPairAndStartDateServiceDelegate implements JavaDelegate {
    
    private final InstrumentPairValidator instrumentPairValidator;
    
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        try {
            // Extract process variables
            String firstInstrument = getRequiredVariable(execution, ProcessVariables.FIRST_INSTRUMENT, String.class);
            String secondInstrument = getRequiredVariable(execution, ProcessVariables.SECOND_INSTRUMENT, String.class);
            String startDateStr = getRequiredVariable(execution, ProcessVariables.START_DATE, String.class);
            String endDateStr = getRequiredVariable(execution, ProcessVariables.END_DATE, String.class);
            
            String instrumentPairStr = firstInstrument + "-" + secondInstrument;
            log.info("Validating instrument pair {} for backtest period {} to {}", 
                instrumentPairStr, startDateStr, endDateStr);
            log.debug("Raw date strings received - start: '{}' (length: {}), end: '{}' (length: {})", 
                startDateStr, startDateStr.length(), endDateStr, endDateStr.length());
            
            // Parse domain objects
            InstrumentPair instrumentPair = InstrumentPair.fromString(instrumentPairStr);
            BacktestPeriod backtestPeriod = BacktestPeriod.fromDateStrings(startDateStr, endDateStr);
            
            // Execute instrument pair validation
            InstrumentPairValidator.InstrumentPairValidationResult result = 
                instrumentPairValidator.validateAndAdjustBacktestPeriod(instrumentPair, backtestPeriod);
            
            // Determine validation result based on whether adjustment is needed
            // If adjustment is needed, set validation to FALSE to trigger user review
            boolean isValidated = result.isValid() && !result.wasAdjusted();
            
            execution.setVariable(ProcessVariables.IS_INSTRUMENT_PAIR_AND_START_DATE_VALIDATED, isValidated);
            execution.setVariable(ProcessVariables.INSTRUMENT_PAIR_VALIDATION_MESSAGE, result.message());
            
            // Always set adjusted dates for user review (whether validation passes or fails)
            if (result.isValid() && result.wasAdjusted()) {
                // Set suggested adjusted dates for user review
                BacktestPeriod adjustedPeriod = result.getEffectivePeriod();
                String adjustedStartDate = adjustedPeriod.getStartLocalDate().toString();
                String adjustedEndDate = adjustedPeriod.getEndLocalDate().toString();
                
                execution.setVariable(ProcessVariables.ADJUSTED_START_DATE, adjustedStartDate);
                execution.setVariable(ProcessVariables.ADJUSTED_END_DATE, adjustedEndDate);
                
                log.info("Instrument pair validation requires adjustment for {}: {} → User review needed. Suggested dates: {} to {}", 
                    instrumentPairStr, result.message(), adjustedStartDate, adjustedEndDate);
            } else if (result.isValid()) {
                // No adjustment needed - set adjusted dates same as original
                execution.setVariable(ProcessVariables.ADJUSTED_START_DATE, startDateStr);
                execution.setVariable(ProcessVariables.ADJUSTED_END_DATE, endDateStr);
                
                log.info("Instrument pair validation successful without adjustment for {}: {}", 
                    instrumentPairStr, result.message());
            } else {
                // Validation failed - set original dates as adjusted dates
                execution.setVariable(ProcessVariables.ADJUSTED_START_DATE, startDateStr);
                execution.setVariable(ProcessVariables.ADJUSTED_END_DATE, endDateStr);
                
                log.warn("Instrument pair validation failed for {}: {}", 
                    instrumentPairStr, result.message());
            }
            
        } catch (Exception e) {
            String errorMessage = "Failed to validate instrument pair and start date: " + e.getMessage();
            log.error("Error in instrument pair validation: {}", errorMessage, e);
            
            // Set failure variables
            execution.setVariable(ProcessVariables.IS_INSTRUMENT_PAIR_AND_START_DATE_VALIDATED, false);
            execution.setVariable(ProcessVariables.INSTRUMENT_PAIR_VALIDATION_MESSAGE, errorMessage);
            
            // Try to set original dates as fallback
            try {
                String startDateStr = getRequiredVariable(execution, ProcessVariables.START_DATE, String.class);
                String endDateStr = getRequiredVariable(execution, ProcessVariables.END_DATE, String.class);
                execution.setVariable(ProcessVariables.ADJUSTED_START_DATE, startDateStr);
                execution.setVariable(ProcessVariables.ADJUSTED_END_DATE, endDateStr);
            } catch (Exception variableError) {
                log.warn("Could not set fallback dates for error recovery: {}", variableError.getMessage());
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
}