package com.ahd.trading_platform.marketdata.application.services;

import com.ahd.trading_platform.marketdata.application.dto.MarketDataRequest;
import com.ahd.trading_platform.marketdata.application.dto.MarketDataResponse;
import com.ahd.trading_platform.marketdata.application.usecases.FetchHistoricalDataUseCase;
import com.ahd.trading_platform.marketdata.domain.entities.MarketInstrument;
import com.ahd.trading_platform.marketdata.domain.repositories.MarketDataRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Application service that coordinates market data operations.
 * Serves as the main entry point for market data functionality.
 */
@Service
public class MarketDataApplicationService {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataApplicationService.class);
    
    private final FetchHistoricalDataUseCase fetchHistoricalDataUseCase;
    private final MarketDataRepository repository;
    
    public MarketDataApplicationService(
        FetchHistoricalDataUseCase fetchHistoricalDataUseCase,
        MarketDataRepository repository) {
        
        this.fetchHistoricalDataUseCase = fetchHistoricalDataUseCase;
        this.repository = repository;
    }
    
    /**
     * Fetches historical market data for the specified instruments
     */
    public CompletableFuture<MarketDataResponse> fetchHistoricalData(MarketDataRequest request) {
        String executionId = generateExecutionId();
        logger.info("Received historical data request: {} (execution: {})", request, executionId);
        
        return fetchHistoricalDataUseCase.execute(request, executionId);
    }
    
    /**
     * Fetches historical data for BTC and ETH (default instruments)
     */
    public CompletableFuture<MarketDataResponse> fetchBtcEthHistoricalData() {
        MarketDataRequest request = MarketDataRequest.forBtcEthHistorical();
        return fetchHistoricalData(request);
    }
    
    /**
     * Retrieves all available market instruments
     */
    public List<MarketInstrument> getAllInstruments() {
        logger.debug("Retrieving all market instruments");
        return repository.findAll();
    }
    
    /**
     * Retrieves a specific market instrument by symbol
     */
    public MarketInstrument getInstrument(String symbol) {
        logger.debug("Retrieving market instrument: {}", symbol);
        return repository.findBySymbol(symbol)
            .orElseThrow(() -> new IllegalArgumentException("Instrument not found: " + symbol));
    }
    
    /**
     * Checks if an instrument has sufficient data for analysis
     */
    public boolean hasInstrumentSufficientData(String symbol) {
        return repository.findBySymbol(symbol)
            .map(MarketInstrument::hasSufficientData)
            .orElse(false);
    }
    
    /**
     * Gets data point count for an instrument
     */
    public long getInstrumentDataCount(String symbol) {
        return repository.getDataPointCount(symbol);
    }
    
    private String generateExecutionId() {
        return "exec-" + UUID.randomUUID().toString().substring(0, 8);
    }
}