package com.ahd.trading_platform.marketdata.infrastructure.external;

import com.ahd.trading_platform.marketdata.domain.valueobjects.OHLCV;
import com.ahd.trading_platform.marketdata.domain.valueobjects.TimeRange;

import java.util.List;

/**
 * Interface for external market data API clients.
 * Abstracts the specific implementation (Binance, Coinbase, etc.) behind a common interface.
 */
public interface ExternalDataClient {
    
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
}