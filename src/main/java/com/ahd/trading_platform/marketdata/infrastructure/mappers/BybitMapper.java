package com.ahd.trading_platform.marketdata.infrastructure.mappers;

import com.ahd.trading_platform.marketdata.domain.valueobjects.OHLCV;
import com.ahd.trading_platform.marketdata.domain.valueobjects.Price;
import com.ahd.trading_platform.marketdata.infrastructure.external.dto.bybit.BybitKlineResponse;
import com.ahd.trading_platform.marketdata.infrastructure.external.dto.bybit.BybitTickerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/**
 * MapStruct mapper for converting Bybit API responses to domain objects.
 * Provides type-safe, compile-time validated mapping with zero reflection overhead.
 * 
 * Key benefits for trading platform:
 * - Compile-time validation prevents runtime mapping errors
 * - Zero reflection = highest performance for high-frequency data
 * - Centralized mapping logic with proper financial precision handling
 * - Automatic null handling and validation
 */
@Mapper(componentModel = "spring")
public interface BybitMapper {
    
    /**
     * Maps Bybit kline data to domain OHLCV object.
     * 
     * Bybit kline format:
     * [0] Start time (timestamp in ms)
     * [1] Open price
     * [2] High price  
     * [3] Low price
     * [4] Close price
     * [5] Volume
     * [6] Turnover (optional)
     */
    @Mapping(target = "timestamp", expression = "java(parseTimestamp(klineData))")
    @Mapping(target = "open", expression = "java(parsePriceObject(klineData, 1))")
    @Mapping(target = "high", expression = "java(parsePriceObject(klineData, 2))")
    @Mapping(target = "low", expression = "java(parsePriceObject(klineData, 3))")
    @Mapping(target = "close", expression = "java(parsePriceObject(klineData, 4))")
    @Mapping(target = "volume", expression = "java(parseVolume(klineData, 5))")
    OHLCV mapKlineToOHLCV(List<String> klineData, String symbol);
    
    /**
     * Maps list of Bybit klines to OHLCV list for batch processing.
     */
    default List<OHLCV> mapKlineListToOHLCV(List<List<String>> klineDataList, String symbol) {
        if (klineDataList == null || klineDataList.isEmpty()) {
            return List.of();
        }
        
        return klineDataList.stream()
            .filter(klineData -> klineData != null && klineData.size() >= 6)
            .map(klineData -> mapKlineToOHLCV(klineData, symbol))
            .toList();
    }
    
    /**
     * Maps Bybit kline response to OHLCV list with validation.
     */
    default List<OHLCV> mapKlineResponseToOHLCV(BybitKlineResponse response, String symbol) {
        if (!response.hasValidData()) {
            return List.of();
        }
        
        return mapKlineListToOHLCV(response.getResult().getKlineData(), symbol);
    }
    
    /**
     * Extracts current price from Bybit ticker response.
     */
    @Mapping(target = ".", source = "tickerInfo")
    default BigDecimal mapTickerToPrice(BybitTickerResponse.TickerInfo tickerInfo) {
        return tickerInfo != null ? tickerInfo.getLastPriceAsBigDecimal() : null;
    }
    
    /**
     * Maps Bybit ticker response to OHLCV for real-time data.
     * Uses current price as all OHLC values since ticker only provides current price.
     */
    default OHLCV mapTickerToOHLCV(BybitTickerResponse.TickerInfo tickerInfo, String symbol) {
        if (tickerInfo == null) {
            return null;
        }
        
        Price currentPrice = Price.usd(tickerInfo.getLastPrice());
        BigDecimal volume = tickerInfo.getVolume24hAsBigDecimal() != null 
            ? tickerInfo.getVolume24hAsBigDecimal() 
            : BigDecimal.ZERO;
        
        return new OHLCV(
            currentPrice,  // Open = current price
            currentPrice,  // High = current price  
            currentPrice,  // Low = current price
            currentPrice,  // Close = current price
            volume,        // Volume
            Instant.now()  // Current timestamp
        );
    }
    
    // ========== Custom Mapping Methods ==========
    
    /**
     * Parses timestamp from kline data with validation.
     */
    default Instant parseTimestamp(List<String> klineData) {
        if (klineData == null || klineData.isEmpty()) {
            throw new IllegalArgumentException("Kline data cannot be null or empty");
        }
        
        try {
            long timestampMs = Long.parseLong(klineData.get(0));
            return Instant.ofEpochMilli(timestampMs);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid timestamp in kline data: " + klineData.get(0), e);
        }
    }
    
    /**
     * Parses price values as Price objects with proper financial precision.
     * Critical for cryptocurrency trading where precision matters.
     */
    default Price parsePriceObject(List<String> klineData, int index) {
        if (klineData == null || klineData.size() <= index) {
            throw new IllegalArgumentException("Kline data missing price at index " + index);
        }
        
        try {
            String priceStr = klineData.get(index);
            if (priceStr == null || priceStr.trim().isEmpty()) {
                throw new IllegalArgumentException("Price value is null or empty at index " + index);
            }
            
            return Price.usd(priceStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid price format at index " + index + ": " + klineData.get(index), e);
        }
    }
    
    /**
     * Parses volume with proper precision handling.
     * Volume can have different precision requirements than price.
     */
    default BigDecimal parseVolume(List<String> klineData, int index) {
        if (klineData == null || klineData.size() <= index) {
            throw new IllegalArgumentException("Kline data missing volume at index " + index);
        }
        
        try {
            String volumeStr = klineData.get(index);
            if (volumeStr == null || volumeStr.trim().isEmpty()) {
                return BigDecimal.ZERO; // Volume can be zero, but not null
            }
            
            // Volume precision: 6 decimal places (sufficient for most cryptocurrencies)
            return new BigDecimal(volumeStr).setScale(6, RoundingMode.HALF_EVEN);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid volume format at index " + index + ": " + klineData.get(index), e);
        }
    }
    
    /**
     * Validates that OHLC prices follow logical constraints.
     * High >= max(Open, Close) and Low <= min(Open, Close)
     */
    default boolean isValidOHLC(Price open, Price high, Price low, Price close) {
        if (open == null || high == null || low == null || close == null) {
            return false;
        }
        
        if (open.isZero() || high.isZero() || low.isZero() || close.isZero()) {
            return false;
        }
        
        // High should be >= Open and Close
        if (high.isLessThan(open) || high.isLessThan(close)) {
            return false;
        }
        
        // Low should be <= Open and Close  
        if (low.isGreaterThan(open) || low.isGreaterThan(close)) {
            return false;
        }
        
        return true;
    }
}