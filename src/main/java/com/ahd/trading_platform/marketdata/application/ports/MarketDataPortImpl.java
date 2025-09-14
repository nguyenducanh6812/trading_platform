package com.ahd.trading_platform.marketdata.application.ports;

import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import com.ahd.trading_platform.marketdata.application.dto.MarketDataRequest;
import com.ahd.trading_platform.marketdata.application.dto.MarketDataResponse;
import com.ahd.trading_platform.marketdata.application.usecases.FetchHistoricalDataUseCase;
import com.ahd.trading_platform.marketdata.domain.repositories.MarketDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of MarketDataPort that integrates with the existing Market Data module.
 * Provides access to historical market data stored in the database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataPortImpl implements MarketDataPort {
    
    private final MarketDataRepository marketDataRepository;
    private final FetchHistoricalDataUseCase fetchHistoricalDataUseCase;
    
    @Override
    public List<OHLCV> getHistoricalData(TradingInstrument instrument, TimeRange timeRange) {
        log.debug("Retrieving historical data for {} within time range {}", instrument.getCode(), timeRange);
        
        try {
            // Use the market data repository to get historical data
            return marketDataRepository.findHistoricalData(instrument.getCode(), timeRange);
                
        } catch (Exception e) {
            log.error("Failed to retrieve historical data for {}: {}", instrument.getCode(), e.getMessage(), e);
            return List.of();
        }
    }
    
    @Override
    public boolean hasSufficientHistoricalData(TradingInstrument instrument, int minimumDataPoints) {
        try {
            long dataPointCount = marketDataRepository.getDataPointCount(instrument.getCode());
            return dataPointCount >= minimumDataPoints;
                
        } catch (Exception e) {
            log.error("Failed to check data sufficiency for {}: {}", instrument.getCode(), e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public int getHistoricalDataPointCount(TradingInstrument instrument) {
        try {
            return (int) marketDataRepository.getDataPointCount(instrument.getCode());
                
        } catch (Exception e) {
            log.error("Failed to get data point count for {}: {}", instrument.getCode(), e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public CompletableFuture<Boolean> fetchMissingHistoricalData(TradingInstrument instrument, TimeRange timeRange, String executionId) {
        log.info("Fetching missing historical data for {} in time range {} (execution: {})", 
            instrument.getCode(), timeRange, executionId);
        
        try {
            // Create market data request for the specified instrument and time range
            MarketDataRequest request = new MarketDataRequest(
                List.of(instrument.getCode()),
                timeRange.from().atOffset(java.time.ZoneOffset.UTC).toLocalDate(),
                timeRange.to().atOffset(java.time.ZoneOffset.UTC).toLocalDate(),
                "bybit" // Default to bybit data source
            );
            
            // Execute the fetch request and transform result to boolean
            return fetchHistoricalDataUseCase.execute(request, executionId)
                .thenApply(response -> {
                    if (!response.success()) {
                        log.error("External data fetch failed for {} - {}: {} (execution: {})",
                            instrument.getCode(), timeRange, response.message(), executionId);
                        return false;
                    }
                    
                    // Check if the specific instrument was successfully fetched
                    MarketDataResponse.InstrumentDataSummary instrumentSummary = 
                        response.instrumentData().get(instrument.getCode());
                        
                    if (instrumentSummary == null || !"SUCCESS".equals(instrumentSummary.status())) {
                        log.error("No data or failed fetch for instrument {} (status: {}) (execution: {})",
                            instrument.getCode(), instrumentSummary != null ? instrumentSummary.status() : "NULL", executionId);
                        return false;
                    }
                    
                    log.info("Successfully fetched {} price data points for {} from external source (execution: {})",
                        instrumentSummary.dataPointCount(), instrument.getCode(), executionId);
                    return true;
                })
                .exceptionally(throwable -> {
                    log.error("Exception during external price data fetch for {} - range {} (execution: {}): {}",
                        instrument.getCode(), timeRange, executionId, throwable.getMessage(), throwable);
                    return false;
                });
                
        } catch (Exception e) {
            log.error("Failed to initiate external data fetch for {} - range {} (execution: {}): {}",
                instrument.getCode(), timeRange, executionId, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
}