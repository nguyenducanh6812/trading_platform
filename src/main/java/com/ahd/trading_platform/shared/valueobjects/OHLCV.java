package com.ahd.trading_platform.shared.valueobjects;

import org.springframework.modulith.NamedInterface;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * OHLCV (Open, High, Low, Close, Volume) data point value object.
 * Represents a single candlestick/bar of market data with volume.
 */
@NamedInterface
public record OHLCV(
    Price open,
    Price high, 
    Price low,
    Price close,
    BigDecimal volume,
    Instant timestamp
) {
    
    public OHLCV {
        Objects.requireNonNull(open, "Open price cannot be null");
        Objects.requireNonNull(high, "High price cannot be null");
        Objects.requireNonNull(low, "Low price cannot be null");
        Objects.requireNonNull(close, "Close price cannot be null");
        Objects.requireNonNull(volume, "Volume cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        
        // Validate price relationships
        if (high.isLessThan(open) || high.isLessThan(close) || 
            high.isLessThan(low)) {
            throw new IllegalArgumentException("High price must be >= open, close, and low prices");
        }
        
        if (low.isGreaterThan(open) || low.isGreaterThan(close) || 
            low.isGreaterThan(high)) {
            throw new IllegalArgumentException("Low price must be <= open, close, and high prices");
        }
        
        if (volume.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Volume cannot be negative");
        }
    }
    
    /**
     * Creates OHLCV from raw double values in USD
     */
    public static OHLCV fromUsdValues(
        double open, double high, double low, double close, 
        double volume, Instant timestamp) {
        return new OHLCV(
            Price.usd(open),
            Price.usd(high), 
            Price.usd(low),
            Price.usd(close),
            BigDecimal.valueOf(volume),
            timestamp
        );
    }
    
    /**
     * Calculates the price range (high - low)
     */
    public Price getRange() {
        return high.subtract(low);
    }
    
    /**
     * Calculates the body size (|close - open|)
     */
    public Price getBodySize() {
        return close.isGreaterThan(open) ? 
            close.subtract(open) : open.subtract(close);
    }
    
    /**
     * Determines if this is a bullish candle (close > open)
     */
    public boolean isBullish() {
        return close.isGreaterThan(open);
    }
    
    /**
     * Determines if this is a bearish candle (close < open)
     */
    public boolean isBearish() {
        return open.isGreaterThan(close);
    }
    
    /**
     * Determines if this is a doji (close == open)
     */
    public boolean isDoji() {
        return close.amount().compareTo(open.amount()) == 0;
    }
    
    /**
     * Calculates typical price (H+L+C)/3
     */
    public Price getTypicalPrice() {
        return high.add(low).add(close)
            .divide(BigDecimal.valueOf(3));
    }
    
    /**
     * Calculates weighted price (H+L+C+C)/4
     */
    public Price getWeightedPrice() {
        return high.add(low).add(close).add(close)
            .divide(BigDecimal.valueOf(4));
    }
}