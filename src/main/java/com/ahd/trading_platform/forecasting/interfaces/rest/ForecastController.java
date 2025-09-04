package com.ahd.trading_platform.forecasting.interfaces.rest;

import com.ahd.trading_platform.forecasting.application.dto.ForecastRequest;
import com.ahd.trading_platform.forecasting.application.dto.ForecastResponse;
import com.ahd.trading_platform.forecasting.application.usecases.ExecuteARIMAForecastUseCase;
import com.ahd.trading_platform.forecasting.infrastructure.repositories.ARIMAMasterDataLoader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for ARIMA forecasting operations.
 * Provides endpoints for executing forecasts, checking health, and managing models.
 */
@RestController
@RequestMapping("/api/v1/forecasting")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "ARIMA Forecasting", description = "ARIMA-based financial forecasting operations")
public class ForecastController {
    
    private final ExecuteARIMAForecastUseCase executeARIMAForecastUseCase;
    private final ARIMAMasterDataLoader arimaMasterDataLoader;
    
    @Operation(
        summary = "Execute ARIMA forecast",
        description = "Executes ARIMA forecast for specified trading instruments and returns expected returns"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Forecast executed successfully",
            content = @Content(schema = @Schema(implementation = ForecastResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error during forecast execution")
    })
    @PostMapping("/execute")
    public ResponseEntity<ForecastResponse> executeForecast(
            @Parameter(description = "Forecast request parameters")
            @Valid @RequestBody ForecastRequest request) {
        
        log.info("Received ARIMA forecast request for instrument: {}", request.instrumentCode());
        
        try {
            ForecastResponse response = executeARIMAForecastUseCase.execute(request);
            
            if (response.isSuccessful()) {
                log.info("ARIMA forecast completed successfully for {}: expected return = {:.4f}%", 
                    request.instrumentCode(), response.getExpectedReturnPercent());
                return ResponseEntity.ok(response);
            } else {
                log.warn("ARIMA forecast failed for {}: {}", request.instrumentCode(), response.errorMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Unexpected error during ARIMA forecast for {}: {}", request.instrumentCode(), e.getMessage(), e);
            ForecastResponse errorResponse = ForecastResponse.failure(
                "error-" + System.currentTimeMillis(),
                request.instrumentCode(),
                "Unexpected error: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @Operation(
        summary = "Execute synchronous ARIMA forecast",
        description = "Executes ARIMA forecast synchronously and returns result immediately"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Forecast executed successfully",
            content = @Content(schema = @Schema(implementation = ForecastResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error during forecast execution")
    })
    @PostMapping("/execute-sync")
    public ResponseEntity<ForecastResponse> executeForecastSync(
            @Parameter(description = "Forecast request parameters")
            @Valid @RequestBody ForecastRequest request) {
        
        log.info("Received synchronous ARIMA forecast request for instrument: {}", request.instrumentCode());
        return executeForecast(request); // Same implementation for now
    }
    
    @Operation(
        summary = "Execute simple forecast",
        description = "Executes ARIMA forecast with default parameters for a single instrument"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Forecast executed successfully",
            content = @Content(schema = @Schema(implementation = ForecastResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid instrument code"),
        @ApiResponse(responseCode = "500", description = "Internal server error during forecast execution")
    })
    @PostMapping("/execute/{instrumentCode}")
    public ResponseEntity<ForecastResponse> executeSimpleForecast(
            @Parameter(description = "Trading instrument code (BTC or ETH)", example = "BTC")
            @PathVariable String instrumentCode) {
        
        log.info("Received simple ARIMA forecast request for instrument: {}", instrumentCode);
        
        // Create simple request with default parameters
        ForecastRequest request = ForecastRequest.simple(instrumentCode.toUpperCase());
        return executeForecast(request);
    }
    
    @Operation(
        summary = "Get forecasting service health",
        description = "Returns health status of the forecasting service and loaded ARIMA models"
    )
    @ApiResponse(responseCode = "200", description = "Health status retrieved successfully")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "service", "ARIMA Forecasting Service",
            "timestamp", System.currentTimeMillis(),
            "models", arimaMasterDataLoader.getCacheStatistics()
        );
        
        return ResponseEntity.ok(health);
    }
    
    @Operation(
        summary = "Get ARIMA model statistics",
        description = "Returns detailed statistics about loaded ARIMA models"
    )
    @ApiResponse(responseCode = "200", description = "Model statistics retrieved successfully")
    @GetMapping("/models/statistics")
    public ResponseEntity<Map<String, Object>> getModelStatistics() {
        Map<String, Object> stats = arimaMasterDataLoader.getCacheStatistics();
        return ResponseEntity.ok(stats);
    }
    
    @Operation(
        summary = "Reload ARIMA models",
        description = "Reloads all ARIMA models from master data files (admin operation)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Models reloaded successfully"),
        @ApiResponse(responseCode = "500", description = "Failed to reload models")
    })
    @PostMapping("/master-data/reload")
    public ResponseEntity<Map<String, Object>> reloadMasterData() {
        log.info("Received request to reload ARIMA master data");
        
        try {
            arimaMasterDataLoader.reloadAllModels();
            
            Map<String, Object> response = Map.of(
                "status", "SUCCESS",
                "message", "ARIMA models reloaded successfully",
                "timestamp", System.currentTimeMillis(),
                "models", arimaMasterDataLoader.getCacheStatistics()
            );
            
            log.info("ARIMA master data reloaded successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to reload ARIMA master data: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = Map.of(
                "status", "FAILED",
                "message", "Failed to reload ARIMA models: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @Operation(
        summary = "Get forecast execution info",
        description = "Returns information about a specific forecast execution (placeholder for future implementation)"
    )
    @ApiResponse(responseCode = "501", description = "Not implemented yet")
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<Map<String, Object>> getExecutionInfo(
            @Parameter(description = "Forecast execution ID")
            @PathVariable String executionId) {
        
        // Placeholder for future implementation with persistent execution tracking
        Map<String, Object> response = Map.of(
            "message", "Execution tracking not yet implemented",
            "executionId", executionId,
            "status", "NOT_IMPLEMENTED"
        );
        
        return ResponseEntity.status(501).body(response);
    }
    
    @Operation(
        summary = "List recent forecast executions",
        description = "Returns list of recent forecast executions (placeholder for future implementation)"
    )
    @ApiResponse(responseCode = "501", description = "Not implemented yet")
    @GetMapping("/executions")
    public ResponseEntity<Map<String, Object>> listExecutions(
            @Parameter(description = "Number of executions to return", example = "10")
            @RequestParam(defaultValue = "10") int limit) {
        
        // Placeholder for future implementation with persistent execution tracking
        Map<String, Object> response = Map.of(
            "message", "Execution history not yet implemented",
            "limit", limit,
            "status", "NOT_IMPLEMENTED"
        );
        
        return ResponseEntity.status(501).body(response);
    }
}