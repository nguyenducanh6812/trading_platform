package com.ahd.trading_platform.backtesting.domain.valueobjects;

import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;

/**
 * Value object representing a pair of trading instruments for backtesting.
 * Ensures both instruments are different and valid for backtesting operations.
 */
public record InstrumentPair(
    TradingInstrument firstInstrument,
    TradingInstrument secondInstrument
) {
    
    public InstrumentPair {
        if (firstInstrument == null) {
            throw new IllegalArgumentException("First instrument cannot be null");
        }
        if (secondInstrument == null) {
            throw new IllegalArgumentException("Second instrument cannot be null");
        }
        if (firstInstrument == secondInstrument) {
            throw new IllegalArgumentException("Instruments in pair must be different");
        }
    }
    
    /**
     * Creates instrument pair from string representation (e.g., "BTC-ETH")
     */
    public static InstrumentPair fromString(String pairString) {
        if (pairString == null || pairString.trim().isEmpty()) {
            throw new IllegalArgumentException("Pair string cannot be null or empty");
        }
        
        String[] parts = pairString.trim().split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid pair format. Expected format: 'BTC-ETH'");
        }
        
        try {
            TradingInstrument first = TradingInstrument.fromCode(parts[0].trim());
            TradingInstrument second = TradingInstrument.fromCode(parts[1].trim());
            return new InstrumentPair(first, second);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid instrument codes in pair: " + pairString, e);
        }
    }
    
    /**
     * Returns string representation of the pair (e.g., "BTC-ETH")
     */
    @Override
    public String toString() {
        return firstInstrument.getCode() + "-" + secondInstrument.getCode();
    }
    
    /**
     * Checks if the pair contains the specified instrument
     */
    public boolean contains(TradingInstrument instrument) {
        return firstInstrument == instrument || secondInstrument == instrument;
    }
}