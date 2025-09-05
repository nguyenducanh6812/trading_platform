package com.ahd.trading_platform.marketdata.application.dto;

import com.ahd.trading_platform.shared.valueobjects.*;
import com.ahd.trading_platform.marketdata.domain.valueobjects.*;
import com.ahd.trading_platform.marketdata.domain.valueobjects.DataQualityMetrics;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for market data fetch responses.
 * Contains results and metadata about the fetch operation.
 */
public record MarketDataResponse(
    boolean success,
    String message,
    Map<String, InstrumentDataSummary> instrumentData,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    Instant processedAt,
    
    long totalDataPoints,
    String executionId
) {
    
    /**
     * Creates a successful response
     */
    public static MarketDataResponse success(
        Map<String, InstrumentDataSummary> instrumentData, 
        String executionId) {
        
        long totalPoints = instrumentData.values().stream()
            .mapToLong(InstrumentDataSummary::dataPointCount)
            .sum();
        
        return new MarketDataResponse(
            true,
            "Market data fetched successfully",
            instrumentData,
            Instant.now(),
            totalPoints,
            executionId
        );
    }
    
    /**
     * Creates a failure response  
     */
    public static MarketDataResponse failure(String message, String executionId) {
        return new MarketDataResponse(
            false,
            message,
            Map.of(),
            Instant.now(),
            0L,
            executionId
        );
    }
    
    /**
     * Summary information for a single instrument's data
     */
    public record InstrumentDataSummary(
        String symbol,
        String name,
        long dataPointCount,
        DataQualityMetrics qualityMetrics,
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant earliestTimestamp,
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant latestTimestamp,
        
        String status
    ) {
        
        public static InstrumentDataSummary success(
            String symbol, 
            String name,
            long dataPointCount,
            DataQualityMetrics qualityMetrics,
            Instant earliestTimestamp,
            Instant latestTimestamp) {
            
            return new InstrumentDataSummary(
                symbol, name, dataPointCount, qualityMetrics,
                earliestTimestamp, latestTimestamp, "SUCCESS"
            );
        }
        
        public static InstrumentDataSummary failure(String symbol, String name, String status) {
            return new InstrumentDataSummary(
                symbol, name, 0L, null, null, null, status
            );
        }
    }
}