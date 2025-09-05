package com.ahd.trading_platform.marketdata.infrastructure.external;

import com.ahd.trading_platform.marketdata.domain.services.ExternalDataClientStrategy;
import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.Price;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
import com.ahd.trading_platform.shared.valueobjects.*;
import com.ahd.trading_platform.marketdata.domain.valueobjects.*;
import com.ahd.trading_platform.marketdata.domain.constants.TradingConstants;
import com.ahd.trading_platform.marketdata.infrastructure.external.dto.bybit.BybitInstrumentsResponse;
import com.ahd.trading_platform.marketdata.infrastructure.external.dto.bybit.BybitKlineResponse;
import com.ahd.trading_platform.marketdata.infrastructure.external.dto.bybit.BybitTickerResponse;
import com.ahd.trading_platform.marketdata.infrastructure.mappers.BybitMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bybit implementation of the ExternalDataClientStrategy.
 * Handles data fetching from Bybit API following the Strategy pattern.
 */
@Component
@Slf4j
@ConditionalOnProperty(
    name = "market-data.external.provider", 
    havingValue = "bybit", 
    matchIfMissing = true
)
public class BybitDataClientStrategy implements ExternalDataClientStrategy {
    
    private final BybitFeignClient bybitClient;
    private final BybitMapper bybitMapper;
    private final RateLimiter rateLimiter;
    
    private final BybitMarketType marketType = BybitMarketType.getForCryptoFutures();
    private static final int MAX_LIMIT_PER_REQUEST = TradingConstants.MAX_BYBIT_RECORDS_PER_REQUEST;
    private static final String DAILY_INTERVAL = "D";
    
    public BybitDataClientStrategy(
        BybitFeignClient bybitClient,
        BybitMapper bybitMapper,
        @Qualifier("bybitRateLimiter") RateLimiter rateLimiter) {
        this.bybitClient = bybitClient;
        this.bybitMapper = bybitMapper;
        this.rateLimiter = rateLimiter;
    }
    
    @Override
    public List<OHLCV> fetchHistoricalData(String symbol, TimeRange timeRange) {
        try {
            log.info("Fetching historical data from Bybit for {} from {} to {}", 
                symbol, timeRange.from(), timeRange.to());
            
            String bybitSymbol = convertToBybitSymbol(symbol);
            
            long startTimestamp = timeRange.from().toEpochMilli();
            long endTimestamp = timeRange.to().toEpochMilli();
            
            long totalDays = timeRange.getDurationDays();
            List<OHLCV> allData = new ArrayList<>();
            
            if (totalDays <= MAX_LIMIT_PER_REQUEST) {
                allData.addAll(fetchSingleBatch(bybitSymbol, startTimestamp, endTimestamp));
            } else {
                allData.addAll(fetchMultipleBatches(bybitSymbol, timeRange));
            }
            
            allData.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));
            
            log.info("Successfully fetched {} OHLCV data points for {} from Bybit", allData.size(), symbol);
            return allData;
            
        } catch (Exception e) {
            log.error("Error fetching historical data from Bybit for symbol {}: {}", symbol, e.getMessage(), e);
            throw new ExternalDataClientException(
                String.format("Failed to fetch historical data from Bybit for %s: %s", symbol, e.getMessage()), e
            );
        }
    }
    
    @Override
    public OHLCV fetchLatestData(String symbol) {
        try {
            log.debug("Fetching latest data from Bybit for {}", symbol);
            
            String bybitSymbol = convertToBybitSymbol(symbol);
            BybitTickerResponse response = bybitClient.getTickerInfo(marketType.getCategory(), bybitSymbol);
            
            if (!response.hasValidData()) {
                throw new ExternalDataClientException("No ticker data available for " + symbol + " on Bybit");
            }
            
            BybitTickerResponse.TickerInfo ticker = response.getResult().getTickerList().get(0);
            
            // Create OHLCV from ticker data (using last price for all OHLC values)
            Price lastPrice = Price.of(ticker.getLastPrice(), "USD");
            Instant timestamp = Instant.now();
            
            return new OHLCV(
                lastPrice, // open = last price
                lastPrice, // high = last price  
                lastPrice, // low = last price
                lastPrice, // close = last price
                new BigDecimal(ticker.getVolume24h()), // 24h volume
                timestamp
            );
            
        } catch (Exception e) {
            log.error("Error fetching latest data from Bybit for symbol {}: {}", symbol, e.getMessage(), e);
            throw new ExternalDataClientException(
                String.format("Failed to fetch latest data from Bybit for %s: %s", symbol, e.getMessage()), e
            );
        }
    }
    
    @Override
    public boolean supportsSymbol(String symbol) {
        return getSupportedSymbols().contains(symbol.toUpperCase());
    }
    
    @Override
    public String getDataSource() {
        return "bybit";
    }
    
    @Override
    public List<String> getSupportedSymbols() {
        try {
            log.debug("Fetching supported cryptocurrency symbols from Bybit");
            
            BybitInstrumentsResponse response = bybitClient.getInstrumentsInfo(
                marketType.getCategory(), null, "Trading", null, 1000, null);
            
            if (!response.hasValidData()) {
                log.warn("No instruments data available from Bybit");
                return List.of("BTC", "ETH");
            }
            
            List<String> symbols = response.getResult().getInstrumentList().stream()
                .filter(BybitInstrumentsResponse.InstrumentInfo::isTradable)
                .filter(BybitInstrumentsResponse.InstrumentInfo::isUSDTQuoted)
                .filter(instrument -> instrument.isBitcoinPair() || instrument.isEthereumPair())
                .map(instrument -> instrument.getBaseCoin())
                .distinct()
                .collect(Collectors.toList());
            
            log.debug("Found {} supported symbols from Bybit: {}", symbols.size(), symbols);
            return symbols;
            
        } catch (Exception e) {
            log.error("Error fetching supported symbols from Bybit: {}", e.getMessage(), e);
            return List.of("BTC", "ETH");
        }
    }
    
    @Override
    public boolean isHealthy() {
        try {
            BybitTickerResponse response = bybitClient.getTickerInfo(marketType.getCategory(), "BTCUSDT");
            boolean isHealthy = response.isSuccess();
            
            log.debug("Bybit API health check: {}", isHealthy ? "HEALTHY" : "UNHEALTHY");
            return isHealthy;
            
        } catch (Exception e) {
            log.warn("Bybit API health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    private List<OHLCV> fetchSingleBatch(String symbol, long startTimestamp, long endTimestamp) {
        BybitKlineResponse response = bybitClient.getKlineData(
            marketType.getCategory(), symbol, DAILY_INTERVAL, startTimestamp, endTimestamp, MAX_LIMIT_PER_REQUEST
        );
        
        if (!response.hasValidData()) {
            log.warn("No valid kline data received from Bybit for symbol: {}", symbol);
            return List.of();
        }
        
        return bybitMapper.mapKlineResponseToOHLCV(response, extractBaseSymbol(symbol));
    }
    
    private List<OHLCV> fetchMultipleBatches(String symbol, TimeRange timeRange) {
        List<OHLCV> allData = new ArrayList<>();
        
        Instant currentTime = timeRange.from();
        int batchCount = 0;
        
        while (currentTime.isBefore(timeRange.to())) {
            Instant batchEnd = currentTime.plus(MAX_LIMIT_PER_REQUEST, ChronoUnit.DAYS);
            if (batchEnd.isAfter(timeRange.to())) {
                batchEnd = timeRange.to();
            }
            
            try {
                List<OHLCV> batchData = fetchSingleBatch(
                    symbol, currentTime.toEpochMilli(), batchEnd.toEpochMilli());
                allData.addAll(batchData);
                batchCount++;
                
                log.debug("Fetched batch {}: {} to {} ({} data points, total: {})", 
                    batchCount, currentTime, batchEnd, batchData.size(), allData.size());
                
                // Memory management: if we have too much data, log a warning
                if (allData.size() > 1000) {
                    log.warn("Large dataset fetched for {}: {} data points. Consider using smaller time ranges.", 
                        symbol, allData.size());
                }
                
                // Use resilience4j rate limiter instead of Thread.sleep
                rateLimiter.executeSupplier(() -> {
                    // Rate limiter ensures we don't exceed API limits
                    return null;
                });
                
            } catch (Exception e) {
                log.warn("Failed to fetch batch {} to {} for symbol {}: {}", 
                    currentTime, batchEnd, symbol, e.getMessage());
            }
            
            currentTime = batchEnd.plus(1, ChronoUnit.DAYS);
        }
        
        log.info("Completed fetching {} batches for {} with total {} data points", 
            batchCount, symbol, allData.size());
        return allData;
    }
    
    private String convertToBybitSymbol(String symbol) {
        // For linear futures, symbols typically end with USDT for perpetual contracts
        return switch (symbol.toUpperCase()) {
            case "BTC", "BITCOIN" -> "BTCUSDT";
            case "ETH", "ETHEREUM" -> "ETHUSDT";
            case "ADA", "CARDANO" -> "ADAUSDT";
            default -> symbol.toUpperCase() + "USDT";
        };
    }
    
    private String extractBaseSymbol(String bybitSymbol) {
        if (bybitSymbol.endsWith("USDT")) {
            return bybitSymbol.substring(0, bybitSymbol.length() - 4);
        }
        return bybitSymbol;
    }
}