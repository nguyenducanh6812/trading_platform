package com.ahd.trading_platform.marketdata.application.ports;

import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import com.ahd.trading_platform.marketdata.domain.repositories.MarketDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of MarketDataPort that integrates with the existing Market Data module.
 * Provides access to historical market data stored in the database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataPortImpl implements MarketDataPort {
    
    private final MarketDataRepository marketDataRepository;
    
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
}