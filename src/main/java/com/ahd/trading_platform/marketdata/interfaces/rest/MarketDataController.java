package com.ahd.trading_platform.marketdata.interfaces.rest;

import com.ahd.trading_platform.marketdata.application.dto.MarketDataRequest;
import com.ahd.trading_platform.marketdata.application.dto.MarketDataResponse;
import com.ahd.trading_platform.marketdata.application.services.MarketDataApplicationService;
import com.ahd.trading_platform.marketdata.domain.entities.MarketInstrument;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for market data operations.
 * Provides HTTP endpoints for triggering market data workflows and retrieving data.
 */
@RestController
@RequestMapping("/api/v1/market-data")
@Tag(name = "Market Data", description = "Market data operations and queries")
public class MarketDataController {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataController.class);
    
    private final MarketDataApplicationService applicationService;
    
    public MarketDataController(MarketDataApplicationService applicationService) {
        this.applicationService = applicationService;
    }
    
    /**
     * Triggers historical data fetch process
     */
    @PostMapping("/fetch-historical")
    @Operation(
        summary = "Fetch historical market data", 
        description = "Initiates the process to fetch historical price data for specified instruments"
    )
    @ApiResponse(responseCode = "202", description = "Request accepted and processing started")
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    public ResponseEntity<CompletableFuture<MarketDataResponse>> fetchHistoricalData(
        @Valid @RequestBody MarketDataRequest request) {
        
        logger.info("Received historical data fetch request: {}", request);
        
        CompletableFuture<MarketDataResponse> futureResponse = applicationService.fetchHistoricalData(request);
        
        return ResponseEntity.accepted().body(futureResponse);
    }
    
    /**
     * Triggers historical data fetch for BTC and ETH (default instruments)
     */
    @PostMapping("/fetch-btc-eth-historical")
    @Operation(
        summary = "Fetch BTC and ETH historical data",
        description = "Initiates the process to fetch historical price data for Bitcoin and Ethereum from March 15, 2021 to current date"
    )
    @ApiResponse(responseCode = "202", description = "Request accepted and processing started")
    public ResponseEntity<CompletableFuture<MarketDataResponse>> fetchBtcEthHistoricalData() {
        
        logger.info("Received BTC/ETH historical data fetch request");
        
        CompletableFuture<MarketDataResponse> futureResponse = applicationService.fetchBtcEthHistoricalData();
        
        return ResponseEntity.accepted().body(futureResponse);
    }
    
    /**
     * Retrieves all available market instruments
     */
    @GetMapping("/instruments")
    @Operation(
        summary = "Get all market instruments",
        description = "Retrieves a list of all available market instruments with their metadata"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved instruments")
    public ResponseEntity<List<MarketInstrument>> getAllInstruments() {
        
        logger.debug("Retrieving all market instruments");
        
        List<MarketInstrument> instruments = applicationService.getAllInstruments();
        
        return ResponseEntity.ok(instruments);
    }
    
    /**
     * Retrieves a specific market instrument by symbol
     */
    @GetMapping("/instruments/{symbol}")
    @Operation(
        summary = "Get market instrument by symbol",
        description = "Retrieves detailed information about a specific market instrument"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved instrument")
    @ApiResponse(responseCode = "404", description = "Instrument not found")
    public ResponseEntity<MarketInstrument> getInstrument(
        @Parameter(description = "Instrument symbol (e.g., BTC, ETH)", example = "BTC")
        @PathVariable String symbol) {
        
        logger.debug("Retrieving market instrument: {}", symbol);
        
        try {
            MarketInstrument instrument = applicationService.getInstrument(symbol);
            return ResponseEntity.ok(instrument);
        } catch (IllegalArgumentException e) {
            logger.warn("Instrument not found: {}", symbol);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Checks if an instrument has sufficient data for analysis
     */
    @GetMapping("/instruments/{symbol}/data-sufficiency")
    @Operation(
        summary = "Check data sufficiency",
        description = "Checks if the instrument has sufficient data points and quality for analysis"
    )
    @ApiResponse(responseCode = "200", description = "Data sufficiency status retrieved")
    public ResponseEntity<DataSufficiencyResponse> checkDataSufficiency(
        @Parameter(description = "Instrument symbol", example = "BTC")
        @PathVariable String symbol) {
        
        logger.debug("Checking data sufficiency for: {}", symbol);
        
        boolean hasSufficientData = applicationService.hasInstrumentSufficientData(symbol);
        long dataPointCount = applicationService.getInstrumentDataCount(symbol);
        
        DataSufficiencyResponse response = new DataSufficiencyResponse(
            symbol, hasSufficientData, dataPointCount,
            hasSufficientData ? "SUFFICIENT" : "INSUFFICIENT"
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Checks the health status of the market data service"
    )
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<HealthResponse> healthCheck() {
        
        logger.debug("Market data service health check");
        
        // Basic health check - could be enhanced with more sophisticated checks
        HealthResponse response = new HealthResponse(
            "UP", "Market Data Service", 
            System.currentTimeMillis(),
            "All systems operational"
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Response DTO for data sufficiency check
     */
    public record DataSufficiencyResponse(
        String symbol,
        boolean sufficient,
        long dataPointCount,
        String status
    ) {}
    
    /**
     * Response DTO for health check
     */
    public record HealthResponse(
        String status,
        String service,
        long timestamp,
        String message
    ) {}
}