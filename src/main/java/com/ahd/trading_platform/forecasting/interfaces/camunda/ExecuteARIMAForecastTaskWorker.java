package com.ahd.trading_platform.forecasting.interfaces.camunda;

import com.ahd.trading_platform.forecasting.application.dto.ForecastRequest;
import com.ahd.trading_platform.forecasting.application.dto.ForecastResponse;
import com.ahd.trading_platform.forecasting.application.usecases.ExecuteARIMAForecastUseCase;
import com.ahd.trading_platform.forecasting.application.services.ForecastResultPersistenceService;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ahd.trading_platform.forecasting.interfaces.camunda.ForecastProcessVariables.*;

/**
 * Camunda external task worker for executing ARIMA forecasts.
 * Follows the thin orchestration layer pattern - delegates business logic to domain use cases.
 */
@Component
@ExternalTaskSubscription(
    topicName = "predict-expected-return-arima-diffoc",
    lockDuration = 300000L,  // 5 minutes for forecast execution
    includeExtensionProperties = true,
    variableNames = {INSTRUMENT_CODES, START_DATE, END_DATE, IS_CURRENT_DATE, INCLUDE_CALCULATION_DETAILS, ARIMA_MODEL_VERSION}
)
@RequiredArgsConstructor
@Slf4j
public class ExecuteARIMAForecastTaskWorker implements ExternalTaskHandler {
    
    private final ExecuteARIMAForecastUseCase executeARIMAForecastUseCase;
    private final ObjectMapper objectMapper;
    private final ForecastResultPersistenceService persistenceService;
    
    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String taskId = externalTask.getId();
        String processInstanceId = externalTask.getProcessInstanceId();
        
        log.info("Starting ARIMA forecast task execution: taskId={}, processInstanceId={}", taskId, processInstanceId);
        
        try {
            // Extract orchestration input
            OrchestrationInput input = extractOrchestrationInput(externalTask);
            
            // Execute forecasts for all requested instruments
            Map<String, Boolean> forecastSuccess = new HashMap<>();
            Map<String, String> forecastErrors = new HashMap<>();
            int successfulForecasts = 0;
            
            for (TradingInstrument instrument : input.instrumentCodes()) {
                // Create forecast request for this instrument
                ForecastRequest request = new ForecastRequest(
                    instrument.getCode(),
                    input.startDate(),
                    input.endDate(),
                    input.isCurrentDate(),
                    input.includeCalculationDetails()
                );
                
                // Execute forecast using domain use case
                ForecastResponse response = executeARIMAForecastUseCase.execute(request, input.arimaModelVersion());
                
                // Collect orchestration results only (no business data)
                if (response.isSuccessful()) {
                    forecastSuccess.put(instrument.getCode(), true);
                    successfulForecasts++;
                } else {
                    forecastSuccess.put(instrument.getCode(), false);
                    forecastErrors.put(instrument.getCode(), response.errorMessage());
                }
            }
            
            // Create orchestration output (no business data)
            Map<String, Object> orchestrationOutput = createOrchestrationOutput(
                input, forecastSuccess, forecastErrors, successfulForecasts, input.arimaModelVersion());
            
            // Complete task with orchestration data only
            externalTaskService.complete(externalTask, orchestrationOutput);
            
            log.info("ARIMA forecast task completed successfully: taskId={}, instruments={}", 
                taskId, input.instrumentCodes().size());
                
        } catch (InvalidProcessVariablesException e) {
            log.error("ARIMA forecast task failed due to invalid process variables: taskId={}, error={}", taskId, e.getMessage());
            externalTaskService.handleBpmnError(externalTask, "INVALID_PROCESS_VARIABLES", e.getMessage());
            
        } catch (Exception e) {
            log.error("ARIMA forecast task failed unexpectedly: taskId={}, error={}", taskId, e.getMessage(), e);
            
            int retries = externalTask.getRetries() != null ? externalTask.getRetries() - 1 : 2;
            externalTaskService.handleFailure(externalTask, e.getMessage(), e.toString(), retries, 60000L);
        }
    }
    
    private OrchestrationInput extractOrchestrationInput(ExternalTask externalTask) throws InvalidProcessVariablesException {
        try {
            // Extract instrument codes (required)
            List<TradingInstrument> instrumentCodes = extractInstrumentCodes(externalTask);
            if (instrumentCodes == null || instrumentCodes.isEmpty()) {
                throw new InvalidProcessVariablesException("instrumentCodes is required and cannot be empty");
            }
            
            // Extract mode and date parameters
            String startDate = externalTask.getVariable(START_DATE);
            String endDate = externalTask.getVariable(END_DATE);
            Boolean isCurrentDate = externalTask.getVariable(IS_CURRENT_DATE);
            Boolean includeCalculationDetails = externalTask.getVariable(INCLUDE_CALCULATION_DETAILS);
            String arimaModelVersion = externalTask.getVariable(ARIMA_MODEL_VERSION);
            
            // Default values
            if (isCurrentDate == null) isCurrentDate = Boolean.TRUE;  // Default to current date mode
            if (includeCalculationDetails == null) includeCalculationDetails = Boolean.FALSE;
            if (arimaModelVersion == null) {
                // For current date mode, use current date as model version; for backtesting, it's required
                if (isCurrentDate) {
                    arimaModelVersion = persistenceService.getCurrentArimaModelVersion();
                } else {
                    throw new InvalidProcessVariablesException(
                        "arimaModelVersion is required for backtesting mode (isCurrentDate=false)");
                }
            }
            
            // Validate parameters based on mode
            if (!isCurrentDate) {
                // Backtesting mode - start and end dates are required
                if (startDate == null || endDate == null) {
                    throw new InvalidProcessVariablesException(
                        "startDate and endDate are required when isCurrentDate=false (backtesting mode)");
                }
            }
            
            return new OrchestrationInput(instrumentCodes, startDate, endDate, isCurrentDate, includeCalculationDetails, arimaModelVersion);
            
        } catch (Exception e) {
            throw new InvalidProcessVariablesException("Failed to extract process variables: " + e.getMessage(), e);
        }
    }
    
    private List<TradingInstrument> extractInstrumentCodes(ExternalTask externalTask) throws InvalidProcessVariablesException {
        Object instrumentCodesVar = externalTask.getVariable(INSTRUMENT_CODES);
        
        if (instrumentCodesVar == null) {
            throw new InvalidProcessVariablesException("instrumentCodes variable is missing");
        }
        
        try {
            // Extract raw string codes
            List<String> stringCodes;
            
            // Handle different input formats
            if (instrumentCodesVar instanceof List) {
                stringCodes = (List<String>) instrumentCodesVar;
            } else if (instrumentCodesVar instanceof String) {
                stringCodes = parseJsonStringArray((String) instrumentCodesVar);
            } else {
                throw new InvalidProcessVariablesException(
                    "instrumentCodes must be a List<String> or JSON string, but was: " + instrumentCodesVar.getClass());
            }
            
            // Convert to TradingInstrument enums with validation
            return TradingInstrument.fromCodes(stringCodes);
            
        } catch (Exception e) {
            throw new InvalidProcessVariablesException(
                "Failed to parse instrumentCodes from: " + instrumentCodesVar + " - " + e.getMessage(), e);
        }
    }
    
    private List<String> parseJsonStringArray(String jsonString) throws Exception {
        TypeReference<List<String>> typeRef = new TypeReference<>() {};
        return objectMapper.readValue(jsonString, typeRef);
    }
    
    private Map<String, Object> createOrchestrationOutput(
            OrchestrationInput input, 
            Map<String, Boolean> forecastSuccess,
            Map<String, String> forecastErrors,
            int successfulForecasts,
            String arimaModelVersion) {
        
        Map<String, Object> variables = new HashMap<>();
        
        // Orchestration metadata
        variables.put(EXECUTION_ID, java.util.UUID.randomUUID().toString());
        variables.put(TASK_COMPLETED, true);
        variables.put(COMPLETED_AT, System.currentTimeMillis());
        
        // Summary data for next steps (no detailed business data)
        variables.put("successfulForecasts", successfulForecasts);
        variables.put("totalInstruments", input.instrumentCodes().size());
        variables.put("arimaModelVersion", arimaModelVersion);
        
        // Error information if any failures occurred
        if (!forecastErrors.isEmpty()) {
            variables.put(ERROR_MESSAGE, String.join("; ", forecastErrors.values()));
            variables.put("failedInstruments", forecastErrors.keySet());
        }
        
        // Success indicators
        variables.put("allForecastsSuccessful", forecastErrors.isEmpty());
        variables.put("hasPartialFailures", !forecastErrors.isEmpty() && successfulForecasts > 0);
        
        return variables;
    }
    
    /**
     * Record representing orchestration input parameters
     */
    private record OrchestrationInput(
        List<TradingInstrument> instrumentCodes,
        String startDate,
        String endDate,
        Boolean isCurrentDate,
        Boolean includeCalculationDetails,
        String arimaModelVersion
    ) {}
    
    /**
     * Exception for invalid process variables
     */
    public static class InvalidProcessVariablesException extends Exception {
        public InvalidProcessVariablesException(String message) {
            super(message);
        }
        
        public InvalidProcessVariablesException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}