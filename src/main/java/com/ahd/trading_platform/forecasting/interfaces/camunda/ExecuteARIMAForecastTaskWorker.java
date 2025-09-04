package com.ahd.trading_platform.forecasting.interfaces.camunda;

import com.ahd.trading_platform.forecasting.application.dto.ForecastRequest;
import com.ahd.trading_platform.forecasting.application.dto.ForecastResponse;
import com.ahd.trading_platform.forecasting.application.usecases.ExecuteARIMAForecastUseCase;
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
    topicName = "execute-arima-forecast",
    lockDuration = 300000L,  // 5 minutes for forecast execution
    includeExtensionProperties = true,
    variableNames = {INSTRUMENT_CODES, START_DATE, END_DATE, USE_DEFAULT_RANGE, INCLUDE_CALCULATION_DETAILS}
)
@RequiredArgsConstructor
@Slf4j
public class ExecuteARIMAForecastTaskWorker implements ExternalTaskHandler {
    
    private final ExecuteARIMAForecastUseCase executeARIMAForecastUseCase;
    private final ObjectMapper objectMapper;
    
    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String taskId = externalTask.getId();
        String processInstanceId = externalTask.getProcessInstanceId();
        
        log.info("Starting ARIMA forecast task execution: taskId={}, processInstanceId={}", taskId, processInstanceId);
        
        try {
            // Extract orchestration input
            OrchestrationInput input = extractOrchestrationInput(externalTask);
            
            // Execute forecasts for all requested instruments
            Map<String, ForecastResponse> forecastResults = new HashMap<>();
            Map<String, Double> expectedReturns = new HashMap<>();
            Map<String, Double> confidenceLevels = new HashMap<>();
            
            for (TradingInstrument instrument : input.instrumentCodes()) {
                // Create forecast request for this instrument
                ForecastRequest request = new ForecastRequest(
                    instrument.getCode(),
                    input.startDate(),
                    input.endDate(),
                    input.useDefaultRange(),
                    input.includeCalculationDetails()
                );
                
                // Execute forecast using domain use case
                ForecastResponse response = executeARIMAForecastUseCase.execute(request);
                
                // Collect results
                forecastResults.put(instrument.getCode(), response);
                if (response.isSuccessful()) {
                    expectedReturns.put(instrument.getCode(), response.expectedReturn());
                    confidenceLevels.put(instrument.getCode(), response.confidenceLevel());
                }
            }
            
            // Create orchestration output
            Map<String, Object> orchestrationOutput = createOrchestrationOutput(input, forecastResults, expectedReturns, confidenceLevels);
            
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
            
            // Extract optional date range parameters
            String startDate = (String) externalTask.getVariable(START_DATE);
            String endDate = (String) externalTask.getVariable(END_DATE);
            Boolean useDefaultRange = (Boolean) externalTask.getVariable(USE_DEFAULT_RANGE);
            Boolean includeCalculationDetails = (Boolean) externalTask.getVariable(INCLUDE_CALCULATION_DETAILS);
            
            // Default values
            if (useDefaultRange == null) useDefaultRange = Boolean.TRUE;
            if (includeCalculationDetails == null) includeCalculationDetails = Boolean.FALSE;
            
            return new OrchestrationInput(instrumentCodes, startDate, endDate, useDefaultRange, includeCalculationDetails);
            
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
            Map<String, ForecastResponse> forecastResults,
            Map<String, Double> expectedReturns,
            Map<String, Double> confidenceLevels) {
        
        Map<String, Object> variables = new HashMap<>();
        
        // Orchestration metadata
        variables.put(EXECUTION_ID, java.util.UUID.randomUUID().toString());
        variables.put(TASK_COMPLETED, true);
        variables.put(COMPLETED_AT, System.currentTimeMillis());
        
        // Forecast results summary (for Analytics module consumption)
        variables.put(EXPECTED_RETURNS, expectedReturns);
        variables.put(CONFIDENCE_LEVELS, confidenceLevels);
        
        // Check for any failures
        boolean hasFailures = forecastResults.values().stream()
            .anyMatch(response -> !response.isSuccessful());
        
        if (hasFailures) {
            List<String> errorMessages = forecastResults.values().stream()
                .filter(response -> !response.isSuccessful())
                .map(ForecastResponse::errorMessage)
                .toList();
            variables.put(ERROR_MESSAGE, String.join("; ", errorMessages));
        }
        
        // Additional metadata for monitoring
        variables.put("forecastedInstruments", forecastResults.keySet());
        variables.put("successfulForecasts", expectedReturns.size());
        variables.put("totalInstruments", input.instrumentCodes().size());
        
        return variables;
    }
    
    /**
     * Record representing orchestration input parameters
     */
    private record OrchestrationInput(
        List<TradingInstrument> instrumentCodes,
        String startDate,
        String endDate,
        Boolean useDefaultRange,
        Boolean includeCalculationDetails
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