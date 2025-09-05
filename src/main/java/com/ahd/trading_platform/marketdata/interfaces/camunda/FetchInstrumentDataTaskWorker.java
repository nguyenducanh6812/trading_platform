package com.ahd.trading_platform.marketdata.interfaces.camunda;

import com.ahd.trading_platform.marketdata.application.dto.MarketDataRequest;
import com.ahd.trading_platform.marketdata.application.usecases.FetchHistoricalDataUseCase;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import com.ahd.trading_platform.shared.valueobjects.*;
import com.ahd.trading_platform.marketdata.domain.valueobjects.*;
import com.ahd.trading_platform.marketdata.domain.constants.TradingConstants;
import static com.ahd.trading_platform.marketdata.interfaces.camunda.ProcessVariables.*;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.Arrays;

/**
 * Camunda External Task Worker for the "fetch-instruments-data" topic.
 * 
 * DDD Architecture Role:
 * - Acts as a thin orchestration layer (like a controller)
 * - Extracts process variables and delegates to domain use cases
 * - Returns only orchestration data to process (success/failure, execution ID)
 * - Does NOT handle business data or complex logic
 * - Follows single responsibility: orchestration only
 */
@Component
@ExternalTaskSubscription(
    topicName = "fetch-instruments-data",
    processDefinitionKey = "Process_Fetch_Instrument_Data",
    lockDuration = 300000, // 5 minutes
    includeExtensionProperties = true,
    variableNames = {INSTRUMENT_CODES, START_DATE, END_DATE, LAUNCH_NEW_INSTRUMENTS, RESOURCE}
)
public class FetchInstrumentDataTaskWorker implements ExternalTaskHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(FetchInstrumentDataTaskWorker.class);
    
    private final FetchHistoricalDataUseCase fetchHistoricalDataUseCase;
    
    public FetchInstrumentDataTaskWorker(FetchHistoricalDataUseCase fetchHistoricalDataUseCase) {
        this.fetchHistoricalDataUseCase = fetchHistoricalDataUseCase;
    }
    
    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String processInstanceId = externalTask.getProcessInstanceId();
        String taskId = externalTask.getId();
        
        logger.info("Starting fetch-instruments-data orchestration. ProcessInstance: {}, TaskId: {}", 
            processInstanceId, taskId);
        
        try {
            // Extract orchestration input from process variables
            OrchestrationInput input = extractOrchestrationInput(externalTask);
            
            logger.info("Orchestrating data fetch for instruments: {} from {} to {} (ProcessInstance: {})", 
                input.instrumentCodes(), input.startDate(), input.endDate(), processInstanceId);
            
            // Create MarketDataRequest for use case
            List<String> instrumentCodeStrings = input.instrumentCodes().stream()
                .map(TradingInstrument::getCode)
                .collect(java.util.stream.Collectors.toList());
                
            MarketDataRequest request = new MarketDataRequest(
                instrumentCodeStrings,
                input.startDate(),
                input.endDate(),
                input.dataSource().getCode()
            );
            
            // Generate execution ID for tracking
            String executionId = "exec_" + processInstanceId + "_" + System.currentTimeMillis();
            
            // Delegate to domain use case (business logic handled in domain layer)
            // Note: This returns CompletableFuture, but we handle orchestration only
            fetchHistoricalDataUseCase.execute(request, executionId);
            
            // Return only orchestration data to process (no business data)
            Map<String, Object> orchestrationResult = Map.of(
                EXECUTION_ID, executionId,
                TASK_COMPLETED, true,
                COMPLETED_AT, System.currentTimeMillis(),
                INSTRUMENTS_REQUESTED, input.instrumentCodes().size(),
                LAUNCH_NEW_INSTRUMENTS, input.isLaunchNew(),
                DATA_SOURCE, input.dataSource().getCode()
            );
            
            externalTaskService.complete(externalTask, orchestrationResult);
            
            logger.info("Successfully orchestrated fetch-instruments-data task. ProcessInstance: {}, ExecutionId: {}", 
                processInstanceId, executionId);
                
        } catch (InvalidProcessVariablesException e) {
            // Handle process variable validation errors
            logger.warn("Invalid process variables in fetch-instruments-data task. ProcessInstance: {}, Error: {}", 
                processInstanceId, e.getMessage());
            externalTaskService.handleBpmnError(externalTask, "INVALID_PROCESS_VARIABLES", e.getMessage());
            
        } catch (Exception e) {
            // Handle unexpected technical errors
            logger.error("Technical failure in fetch-instruments-data orchestration. ProcessInstance: {}, TaskId: {}", 
                processInstanceId, taskId, e);
            
            String errorMessage = "Orchestration failure: " + e.getMessage();
            String errorDetails = "Failed to orchestrate instrument data fetch: " + e;
            
            int retries = externalTask.getRetries() != null ? externalTask.getRetries() - 1 : 2;
            long retryTimeout = 60000L; // 1 minute retry timeout
            
            externalTaskService.handleFailure(externalTask, errorMessage, errorDetails, retries, retryTimeout);
        }
    }
    
    /**
     * Extracts orchestration input from process variables.
     * Validates all required inputs and handles launchNewInstruments logic.
     */
    private OrchestrationInput extractOrchestrationInput(ExternalTask externalTask) 
            throws InvalidProcessVariablesException {
        try {
            // Validate and extract instrument codes (required)
            logger.info("All variables: {}", externalTask.getAllVariables());
            List<TradingInstrument> instrumentCodes = extractInstrumentCodes(externalTask);
            if (instrumentCodes == null || instrumentCodes.isEmpty()) {
                throw new InvalidProcessVariablesException(
                    "instrumentCodes is required and cannot be null or empty");
            }
            
            // Extract launchNewInstruments flag (default: false)
            Boolean launchNewInstruments = externalTask.getVariable(LAUNCH_NEW_INSTRUMENTS);
            boolean isLaunchNew = Boolean.TRUE.equals(launchNewInstruments);
            
            // Extract resource (data source) with validation
            String resource = externalTask.getVariable(RESOURCE);
            DataSourceType dataSourceType = (resource != null && !resource.trim().isEmpty()) ? 
                DataSourceType.fromCode(resource) : DataSourceType.getDefault();

            // Handle date range logic based on launchNewInstruments flag
            LocalDate startDate;
            LocalDate endDate;
            
            if (isLaunchNew) {
                // For new instruments, always use default historical range
                startDate = TradingConstants.HISTORICAL_START_DATE;
                endDate = LocalDate.now();
                logger.info("LaunchNewInstruments=true, using default date range: {} to {}", startDate, endDate);
            } else {
                // For regular fetch, startDate and endDate are required
                String startDateStr = externalTask.getVariable(START_DATE);
                String endDateStr = externalTask.getVariable(END_DATE);
                
                if (startDateStr == null || startDateStr.trim().isEmpty()) {
                    throw new InvalidProcessVariablesException(
                        "When launchNewInstruments=false, startDate is required and cannot be null or empty");
                }
                
                if (endDateStr == null || endDateStr.trim().isEmpty()) {
                    throw new InvalidProcessVariablesException(
                        "When launchNewInstruments=false, endDate is required and cannot be null or empty");
                }
                
                try {
                    startDate = LocalDate.parse(startDateStr);
                    endDate = LocalDate.parse(endDateStr);
                } catch (Exception parseException) {
                    throw new InvalidProcessVariablesException(
                        "Invalid date format. Expected format: YYYY-MM-DD. startDate: " + startDateStr + 
                        ", endDate: " + endDateStr, parseException);
                }
                
                // Validate date range
                if (startDate.isAfter(endDate)) {
                    throw new InvalidProcessVariablesException(
                        "startDate (" + startDate + ") cannot be after endDate (" + endDate + ")");
                }
                
                logger.info("LaunchNewInstruments=false, using provided date range: {} to {}", startDate, endDate);
            }
            
            // Create TimeRange using the fromDates static method
            TimeRange timeRange = TimeRange.fromDates(startDate, endDate);
            
            return new OrchestrationInput(instrumentCodes, timeRange, startDate, endDate, isLaunchNew, dataSourceType);
            
        } catch (InvalidProcessVariablesException e) {
            // Re-throw validation errors
            throw e;
        } catch (Exception e) {
            throw new InvalidProcessVariablesException(
                "Failed to extract process variables: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extracts instrument codes from process variables, handling both JSON string and List formats.
     * Camunda may send arrays as JSON strings, so we need to deserialize them properly.
     * Returns validated TradingInstrument enums for type safety.
     */
    @SuppressWarnings("unchecked")
    private List<TradingInstrument> extractInstrumentCodes(ExternalTask externalTask) throws InvalidProcessVariablesException {
        Object instrumentCodesVar = externalTask.getVariable(INSTRUMENT_CODES);
        
        if (instrumentCodesVar == null) {
            return null;
        }
        
        try {
            // First, extract raw string codes
            List<String> stringCodes;
            
            // If it's already a List, use it
            if (instrumentCodesVar instanceof List) {
                stringCodes = (List<String>) instrumentCodesVar;
            }
            // If it's a JSON string, parse it
            else if (instrumentCodesVar instanceof String) {
                stringCodes = parseJsonStringArray((String) instrumentCodesVar);
            }
            else {
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
    
    /**
     * Parses JSON string array format like ["BTC","ETH"] into List<String>.
     */
    private List<String> parseJsonStringArray(String jsonString) {
        // Remove outer quotes if present
        if (jsonString.startsWith("\"") && jsonString.endsWith("\"")) {
            jsonString = jsonString.substring(1, jsonString.length() - 1);
            // Unescape inner quotes
            jsonString = jsonString.replace("\\\"", "\"");
        }
        
        // Parse JSON array manually for simple case
        if (jsonString.startsWith("[") && jsonString.endsWith("]")) {
            String content = jsonString.substring(1, jsonString.length() - 1);
            if (content.trim().isEmpty()) {
                return List.of(); // Empty array
            }
            
            // Split by comma and clean up quotes
            String[] items = content.split(",");
            return Arrays.stream(items)
                .map(String::trim)
                .map(item -> item.startsWith("\"") && item.endsWith("\"") 
                    ? item.substring(1, item.length() - 1) 
                    : item)
                .toList();
        }
        
        // If it's a single string value, treat as single item list
        return List.of(jsonString);
    }
    
    /**
     * Input data extracted from process variables for orchestration.
     * Contains only the minimal data needed for business logic delegation.
     */
    private record OrchestrationInput(
        List<TradingInstrument> instrumentCodes,
        TimeRange timeRange,
        LocalDate startDate,
        LocalDate endDate,
        boolean isLaunchNew,
        DataSourceType dataSource
    ) {}
    
    /**
     * Exception for invalid process variables.
     * Used to distinguish process errors from technical errors.
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