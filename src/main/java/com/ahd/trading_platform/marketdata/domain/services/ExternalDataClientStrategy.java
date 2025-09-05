package com.ahd.trading_platform.marketdata.domain.services;

import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;

import java.util.List;

/**
 * Strategy interface for external market data providers.
 * Follows Strategy pattern to allow easy switching between different data sources.
 */
public interface ExternalDataClientStrategy {
    
    /**
     * Fetches historical OHLCV data for a symbol within a time range
     */
    List<OHLCV> fetchHistoricalData(String symbol, TimeRange timeRange);
    
    /**
     * Fetches the latest price data for a symbol
     */
    OHLCV fetchLatestData(String symbol);
    
    /**
     * Checks if the client supports the given symbol
     */
    boolean supportsSymbol(String symbol);
    
    /**
     * Returns the name/identifier of this data source
     */
    String getDataSource();
    
    /**
     * Returns the list of supported symbols
     */
    List<String> getSupportedSymbols();
    
    /**
     * Checks if the external API is healthy and responsive
     */
    boolean isHealthy();
}