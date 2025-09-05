package com.ahd.trading_platform.marketdata.application.ports;

import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import org.springframework.modulith.NamedInterface;

import java.util.List;

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
}