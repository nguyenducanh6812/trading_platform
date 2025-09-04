package com.ahd.trading_platform.marketdata.infrastructure.external;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for rate limiting external API calls.
 * Uses Resilience4j for professional rate limiting instead of Thread.sleep.
 */
@Configuration
public class RateLimiterConfig {
    
    /**
     * Rate limiter for Bybit API calls.
     * Bybit allows 120 requests per minute for public data.
     */
    @Bean("bybitRateLimiter")
    public RateLimiter bybitRateLimiter() {
        io.github.resilience4j.ratelimiter.RateLimiterConfig config = 
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitForPeriod(100) // Allow 100 requests 
                .limitRefreshPeriod(Duration.ofMinutes(1)) // per 1 minute
                .timeoutDuration(Duration.ofSeconds(10)) // Wait up to 10 seconds
                .build();
                
        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        return registry.rateLimiter("bybit-api");
    }
    
    /**
     * Rate limiter for chunk processing to avoid overwhelming the system
     */
    @Bean("chunkRateLimiter")
    public RateLimiter chunkRateLimiter() {
        io.github.resilience4j.ratelimiter.RateLimiterConfig config = 
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitForPeriod(5) // Allow 5 chunks
                .limitRefreshPeriod(Duration.ofMinutes(1)) // per 1 minute
                .timeoutDuration(Duration.ofSeconds(30)) // Wait up to 30 seconds
                .build();
                
        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        return registry.rateLimiter("chunk-processing");
    }
}