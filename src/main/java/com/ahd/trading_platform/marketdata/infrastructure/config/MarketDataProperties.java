package com.ahd.trading_platform.marketdata.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for the Market Data module.
 * Maps to the 'market-data' section in application.yaml.
 */
@ConfigurationProperties(prefix = "market-data")
public record MarketDataProperties(
    External external,
    Storage storage,
    Validation validation
) {
    
    public MarketDataProperties() {
        this(
            new External(Duration.ofSeconds(30), 3, new RateLimit(60)),
            new Storage(1000, true),
            new Validation(0.5, 30, 0.8)
        );
    }
    
    public record External(
        Duration timeout,
        int maxRetries,
        RateLimit rateLimit
    ) {}
    
    public record RateLimit(
        int requestsPerMinute
    ) {}
    
    public record Storage(
        int batchSize,
        boolean enableCompression
    ) {}
    
    public record Validation(
        double maxDailyChange,
        int minDataPoints,
        double qualityThreshold
    ) {}
}