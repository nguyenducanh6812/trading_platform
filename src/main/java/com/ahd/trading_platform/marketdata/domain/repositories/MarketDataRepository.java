package com.ahd.trading_platform.marketdata.domain.repositories;

import com.ahd.trading_platform.marketdata.domain.entities.MarketInstrument;
import com.ahd.trading_platform.marketdata.domain.valueobjects.OHLCV;
import com.ahd.trading_platform.marketdata.domain.valueobjects.TimeRange;

import java.util.List;
import java.util.Optional;

/**
 * Repository contract for market data persistence operations.
 * This is a domain interface that should be implemented in the infrastructure layer.
 */
public interface MarketDataRepository {
    
    /**
     * Saves or updates a market instrument with its data
     */
    void save(MarketInstrument instrument);
    
    /**
     * Finds a market instrument by symbol
     */
    Optional<MarketInstrument> findBySymbol(String symbol);
    
    /**
     * Finds all market instruments
     */
    List<MarketInstrument> findAll();
    
    /**
     * Saves historical OHLCV data for an instrument
     */
    void saveHistoricalData(String symbol, List<OHLCV> ohlcvData);
    
    /**
     * Retrieves historical data for an instrument within a time range
     */
    List<OHLCV> findHistoricalData(String symbol, TimeRange timeRange);
    
    /**
     * Retrieves all historical data for an instrument
     */
    List<OHLCV> findAllHistoricalData(String symbol);
    
    /**
     * Checks if historical data exists for an instrument in the given time range
     */
    boolean hasHistoricalData(String symbol, TimeRange timeRange);
    
    /**
     * Deletes all data for an instrument
     */
    void deleteBySymbol(String symbol);
    
    /**
     * Returns the count of data points for an instrument
     */
    long getDataPointCount(String symbol);
}