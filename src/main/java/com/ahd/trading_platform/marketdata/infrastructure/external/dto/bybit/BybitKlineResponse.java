package com.ahd.trading_platform.marketdata.infrastructure.external.dto.bybit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for Bybit kline/candlestick API.
 * Contains OHLCV data with actual open, high, low, close prices.
 */
@Data
@NoArgsConstructor
public class BybitKlineResponse {
    
    @JsonProperty("retCode")
    private int returnCode;
    
    @JsonProperty("retMsg") 
    private String returnMessage;
    
    @JsonProperty("result")
    private KlineResult result;
    
    @JsonProperty("retExtInfo")
    private Object extendedInfo;
    
    @JsonProperty("time")
    private long responseTime;
    
    @Data
    @NoArgsConstructor
    public static class KlineResult {
        
        @JsonProperty("symbol")
        private String symbol;
        
        @JsonProperty("category") 
        private String category;
        
        /**
         * Kline data array. Each item contains:
         * [0] Start time (timestamp in ms)
         * [1] Open price 
         * [2] High price
         * [3] Low price  
         * [4] Close price
         * [5] Volume
         * [6] Turnover (volume in quote currency)
         */
        @JsonProperty("list")
        private List<List<String>> klineData;
    }
    
    /**
     * Checks if the API response was successful.
     */
    public boolean isSuccess() {
        return returnCode == 0;
    }
    
    /**
     * Gets the error message if the request failed.
     */
    public String getErrorMessage() {
        return isSuccess() ? null : returnMessage;
    }
    
    /**
     * Checks if the response contains valid kline data.
     */
    public boolean hasValidData() {
        return isSuccess() && 
               result != null && 
               result.klineData != null && 
               !result.klineData.isEmpty() &&
               result.klineData.stream().allMatch(kline -> kline.size() >= 6);
    }
    
    /**
     * Gets the number of kline data points in the response.
     */
    public int getDataPointCount() {
        return hasValidData() ? result.klineData.size() : 0;
    }
    
    /**
     * Converts a kline data point to OHLCV values.
     * 
     * @param klineDataPoint Single kline data point from the API
     * @return OHLCV values as BigDecimal array [open, high, low, close, volume]
     */
    public static BigDecimal[] parseKlineData(List<String> klineDataPoint) {
        if (klineDataPoint == null || klineDataPoint.size() < 6) {
            throw new IllegalArgumentException("Invalid kline data point");
        }
        
        return new BigDecimal[] {
            new BigDecimal(klineDataPoint.get(1)), // Open
            new BigDecimal(klineDataPoint.get(2)), // High  
            new BigDecimal(klineDataPoint.get(3)), // Low
            new BigDecimal(klineDataPoint.get(4)), // Close
            new BigDecimal(klineDataPoint.get(5))  // Volume
        };
    }
    
    /**
     * Gets timestamp from kline data point.
     */
    public static long parseTimestamp(List<String> klineDataPoint) {
        if (klineDataPoint == null || klineDataPoint.isEmpty()) {
            throw new IllegalArgumentException("Invalid kline data point");
        }
        return Long.parseLong(klineDataPoint.get(0));
    }
}