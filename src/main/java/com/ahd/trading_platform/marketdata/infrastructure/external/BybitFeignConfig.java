package com.ahd.trading_platform.marketdata.infrastructure.external;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Feign configuration for Bybit API client.
 * Optimized for Bybit's API characteristics and rate limits.
 */
@Configuration
@Slf4j
public class BybitFeignConfig {
    
    /**
     * Configure request timeouts for Bybit API.
     * Bybit generally has good response times, but historical data can take longer.
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
            10, TimeUnit.SECONDS,  // Connect timeout
            30, TimeUnit.SECONDS,  // Read timeout
            true                   // Follow redirects
        );
    }
    
    /**
     * Configure retry strategy for Bybit API.
     * Bybit has good uptime but we implement conservative retries.
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
            500,     // Initial retry interval (500ms)
            2000,    // Max retry interval (2 seconds)
            3        // Max attempts
        );
    }
    
    /**
     * Configure Feign logging for debugging.
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
    
    /**
     * Custom error decoder for Bybit API responses.
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new BybitErrorDecoder();
    }
    
    /**
     * Custom error decoder implementation for Bybit API.
     */
    private static class BybitErrorDecoder implements ErrorDecoder {
        
        private static final org.slf4j.Logger log = LoggerFactory.getLogger(BybitErrorDecoder.class);
        private final ErrorDecoder defaultDecoder = new Default();
        
        @Override
        public Exception decode(String methodKey, feign.Response response) {
            switch (response.status()) {
                case 429: // Rate limit exceeded
                    log.warn("Bybit API rate limit exceeded for method: {}", methodKey);
                    return new BybitRateLimitException("Rate limit exceeded. Bybit allows 120 requests/minute.");
                
                case 400:
                    log.warn("Bybit API bad request for method: {}", methodKey);
                    return new BybitBadRequestException("Invalid request parameters.");
                
                case 404:
                    log.warn("Bybit API endpoint not found for method: {}", methodKey);
                    return new BybitNotFoundException("Requested endpoint or symbol not found.");
                
                case 500:
                case 502:
                case 503:
                    log.error("Bybit API server error (status: {}) for method: {}", response.status(), methodKey);
                    return new BybitServerException("Bybit API server error. Please retry later.");
                
                default:
                    log.error("Bybit API error (status: {}) for method: {}", response.status(), methodKey);
                    return defaultDecoder.decode(methodKey, response);
            }
        }
    }
    
    /**
     * Exception for Bybit API rate limiting.
     */
    public static class BybitRateLimitException extends RuntimeException {
        public BybitRateLimitException(String message) {
            super(message);
        }
    }
    
    /**
     * Exception for Bybit API bad requests.
     */
    public static class BybitBadRequestException extends RuntimeException {
        public BybitBadRequestException(String message) {
            super(message);
        }
    }
    
    /**
     * Exception for Bybit API not found errors.
     */
    public static class BybitNotFoundException extends RuntimeException {
        public BybitNotFoundException(String message) {
            super(message);
        }
    }
    
    /**
     * Exception for Bybit API server errors.
     */
    public static class BybitServerException extends RuntimeException {
        public BybitServerException(String message) {
            super(message);
        }
    }
}