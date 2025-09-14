package com.ahd.trading_platform.forecasting.interfaces.camunda;

import com.ahd.trading_platform.forecasting.application.dto.BatchForecastOrchestrationResponse;
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
 * 
 * Architecture:
 * - Thin orchestration layer following DDD best practices
 * - Extracts input data from process variables
 * - Delegates ALL business logic to domain use cases/services
 * - Handles BPMN errors appropriately
 * - Returns only process flow variables (no business data)
 * 
 * Does NOT:
 * - Handle business logic (loops, error analysis, etc.)
 * - Process business data
 * - Make business decisions
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
    
    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String taskId = externalTask.getId();
        String processInstanceId = externalTask.getProcessInstanceId();
        
        log.info("Starting ARIMA forecast orchestration: taskId={}, processInstanceId={}", taskId, processInstanceId);
        
        try {
            // Extract orchestration input from process variables
            OrchestrationInput input = extractOrchestrationInput(externalTask);
            
            // Delegate business logic to use case (returns orchestration data only)
            BatchForecastOrchestrationResponse batchResult =
                executeBatchForecasts(input);
            
            // Handle critical errors with BPMN error
            if (batchResult.hasCriticalErrors()) {
                log.error("ARIMA forecast failed due to critical errors: taskId={}, errors={}", 
                    taskId, batchResult.criticalErrorMessage());
                externalTaskService.handleBpmnError(externalTask, "ARIMA_CRITICAL_ERROR", batchResult.criticalErrorMessage());
                return;
            }
            
            // Create orchestration output (process variables only)
            Map<String, Object> orchestrationOutput = createOrchestrationOutput(input, batchResult);
            
            // Complete task with orchestration data only
            externalTaskService.complete(externalTask, orchestrationOutput);
            
            log.info("ARIMA forecast orchestration completed: taskId={}, successful={}/{}", 
                taskId, batchResult.successfulForecasts(), batchResult.totalInstruments());
                
        } catch (InvalidProcessVariablesException e) {
            log.error("Invalid process variables: taskId={}, error={}", taskId, e.getMessage());
            externalTaskService.handleBpmnError(externalTask, "INVALID_PROCESS_VARIABLES", e.getMessage());
            
        } catch (Exception e) {
            log.error("Technical failure in ARIMA forecast orchestration: taskId={}", taskId, e);
            
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
                // For current date mode, use default model version; for backtesting, it's required
                if (isCurrentDate) {
                    arimaModelVersion = "20250904"; // Default current model version
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
    
    /**
     * Executes batch forecasts by delegating to use case.
     * Returns orchestration response without business data.
     */
    private BatchForecastOrchestrationResponse executeBatchForecasts(OrchestrationInput input) {
        // Extract instrument codes as strings
        List<String> instrumentCodes = input.instrumentCodes().stream()
            .map(TradingInstrument::getCode)
            .toList();
        
        // Delegate batch processing to use case (all business logic handled there)
        return executeARIMAForecastUseCase.executeBatch(
            instrumentCodes,
            input.startDate(),
            input.endDate(),
            input.isCurrentDate(),
            input.includeCalculationDetails(),
            input.arimaModelVersion()
        );
    }
    
    
    private Map<String, Object> createOrchestrationOutput(
            OrchestrationInput input, 
            com.ahd.trading_platform.forecasting.application.dto.BatchForecastOrchestrationResponse batchResult) {
        
        Map<String, Object> variables = new HashMap<>();
        
        // Orchestration metadata
        variables.put(EXECUTION_ID, batchResult.executionId());
        variables.put(TASK_COMPLETED, true);
        variables.put(COMPLETED_AT, System.currentTimeMillis());
        
        // Summary data for next steps (no detailed business data like expectedReturn values)
        variables.put("successfulForecasts", batchResult.successfulForecasts());
        variables.put("totalInstruments", batchResult.totalInstruments());
        variables.put("arimaModelVersion", batchResult.arimaModelVersion());
        
        // Error information if any failures occurred
        if (!batchResult.failedInstruments().isEmpty()) {
            variables.put(ERROR_MESSAGE, String.join("; ", batchResult.failedInstruments().values()));
            variables.put("failedInstruments", batchResult.failedInstruments().keySet());
        }
        
        // Success indicators
        variables.put("allForecastsSuccessful", batchResult.failedInstruments().isEmpty());
        variables.put("hasPartialFailures", !batchResult.failedInstruments().isEmpty() && batchResult.successfulForecasts() > 0);
        
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