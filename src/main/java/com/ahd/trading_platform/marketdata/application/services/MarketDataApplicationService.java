package com.ahd.trading_platform.marketdata.application.services;

import com.ahd.trading_platform.marketdata.application.dto.AvailableInstrumentsResponse;
import com.ahd.trading_platform.marketdata.application.dto.GetInstrumentsByMarketRequest;
import com.ahd.trading_platform.marketdata.application.dto.InstrumentsByMarketResponse;
import com.ahd.trading_platform.marketdata.application.dto.MarketDataRequest;
import com.ahd.trading_platform.marketdata.application.dto.MarketDataResponse;
import com.ahd.trading_platform.marketdata.application.dto.MarketResponse;
import com.ahd.trading_platform.marketdata.application.usecases.FetchHistoricalDataUseCase;
import com.ahd.trading_platform.marketdata.application.usecases.GetAllInstrumentsUseCase;
import com.ahd.trading_platform.marketdata.application.usecases.GetAllMarketsUseCase;
import com.ahd.trading_platform.marketdata.application.usecases.GetInstrumentsByMarketUseCase;
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
    private final GetAllInstrumentsUseCase getAllInstrumentsUseCase;
    private final GetAllMarketsUseCase getAllMarketsUseCase;
    private final GetInstrumentsByMarketUseCase getInstrumentsByMarketUseCase;
    private final MarketDataRepository repository;

    public MarketDataApplicationService(
        FetchHistoricalDataUseCase fetchHistoricalDataUseCase,
        GetAllInstrumentsUseCase getAllInstrumentsUseCase,
        GetAllMarketsUseCase getAllMarketsUseCase,
        GetInstrumentsByMarketUseCase getInstrumentsByMarketUseCase,
        MarketDataRepository repository) {

        this.fetchHistoricalDataUseCase = fetchHistoricalDataUseCase;
        this.getAllInstrumentsUseCase = getAllInstrumentsUseCase;
        this.getAllMarketsUseCase = getAllMarketsUseCase;
        this.getInstrumentsByMarketUseCase = getInstrumentsByMarketUseCase;
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
     * Retrieves all available market instruments from external API with database fallback.
     * Returns immediately while database is updated asynchronously.
     *
     * @param sourceCode Data source provider code (e.g., "bybit")
     * @return Response with instruments and metadata
     */
    public AvailableInstrumentsResponse getAvailableInstruments(String sourceCode) {
        logger.info("Fetching available instruments from source: {}", sourceCode);
        return getAllInstrumentsUseCase.execute(sourceCode);
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

    /**
     * Retrieves all available markets
     */
    public List<MarketResponse> getAllMarkets() {
        logger.debug("Fetching all available markets");
        return getAllMarketsUseCase.execute();
    }

    /**
     * Retrieves instruments for a specific market.
     * Accepts market details from frontend to avoid database lookup.
     */
    public InstrumentsByMarketResponse getInstrumentsByMarket(GetInstrumentsByMarketRequest request) {
        logger.debug("Fetching instruments for market: {} (ID: {})", request.marketCode(), request.marketId());
        return getInstrumentsByMarketUseCase.execute(request);
    }

    private String generateExecutionId() {
        return "exec-" + UUID.randomUUID().toString().substring(0, 8);
    }
}