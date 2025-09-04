package com.ahd.trading_platform.marketdata.domain.events;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event raised when market data is updated for an instrument.
 * Other modules can subscribe to this event to react to new market data.
 */
public record MarketDataUpdatedEvent(
    String symbol,
    int dataPointsAdded,
    Instant timestamp
) {
    
    public MarketDataUpdatedEvent {
        Objects.requireNonNull(symbol, "Symbol cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        
        if (dataPointsAdded < 0) {
            throw new IllegalArgumentException("Data points added cannot be negative");
        }
    }
    
    /**
     * Creates an event for current timestamp
     */
    public static MarketDataUpdatedEvent now(String symbol, int dataPointsAdded) {
        return new MarketDataUpdatedEvent(symbol, dataPointsAdded, Instant.now());
    }
}