package com.ahd.trading_platform.marketdata.application.ports;

import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import org.springframework.modulith.NamedInterface;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Port interface for Market Data module operations.
 * Provides access to historical market data for other modules.
 */
@NamedInterface
public interface MarketDataPort {
    
    /**
     * Retrieves historical OHLCV data for a trading instrument within the specified time range.
     * 
     * @param instrument The trading instrument to get data for
     * @param timeRange The time range for historical data
     * @return List of OHLCV data sorted by timestamp, empty list if no data found
     */
    List<OHLCV> getHistoricalData(TradingInstrument instrument, TimeRange timeRange);
    
    /**
     * Checks if sufficient historical data is available for the given instrument.
     * 
     * @param instrument The trading instrument to check
     * @param minimumDataPoints The minimum number of data points required
     * @return true if sufficient data is available, false otherwise
     */
    boolean hasSufficientHistoricalData(TradingInstrument instrument, int minimumDataPoints);
    
    /**
     * Gets the total number of historical data points available for an instrument.
     * 
     * @param instrument The trading instrument to check
     * @return The total number of data points available
     */
    int getHistoricalDataPointCount(TradingInstrument instrument);
    
    /**
     * Fetches missing historical data from external sources for the specified instrument and time range.
     * This method triggers external API calls to retrieve and store missing price data.
     * 
     * @param instrument The trading instrument to fetch data for
     * @param timeRange The time range for which to fetch missing data
     * @param executionId Execution ID for tracking and logging
     * @return CompletableFuture with the result - true if successful, false if failed
     */
    CompletableFuture<Boolean> fetchMissingHistoricalData(TradingInstrument instrument, TimeRange timeRange, String executionId);
}