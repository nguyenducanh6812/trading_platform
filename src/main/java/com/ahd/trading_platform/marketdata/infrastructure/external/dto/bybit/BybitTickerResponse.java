package com.ahd.trading_platform.marketdata.infrastructure.external.dto.bybit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for Bybit ticker API.
 * Contains real-time price and volume information.
 */
@Data
@NoArgsConstructor
public class BybitTickerResponse {
    
    @JsonProperty("retCode")
    private int returnCode;
    
    @JsonProperty("retMsg")
    private String returnMessage;
    
    @JsonProperty("result")
    private TickerResult result;
    
    @JsonProperty("retExtInfo")
    private Object extendedInfo;
    
    @JsonProperty("time")
    private long responseTime;
    
    @Data
    @NoArgsConstructor
    public static class TickerResult {
        
        @JsonProperty("category")
        private String category;
        
        @JsonProperty("list")
        private List<TickerInfo> tickerList;
    }
    
    @Data
    @NoArgsConstructor
    public static class TickerInfo {
        
        @JsonProperty("symbol")
        private String symbol;
        
        @JsonProperty("lastPrice")
        private String lastPrice;
        
        @JsonProperty("indexPrice")
        private String indexPrice;
        
        @JsonProperty("markPrice")
        private String markPrice;
        
        @JsonProperty("prevPrice24h")
        private String prevPrice24h;
        
        @JsonProperty("price24hPcnt")
        private String price24hPercent;
        
        @JsonProperty("highPrice24h")
        private String highPrice24h;
        
        @JsonProperty("lowPrice24h")
        private String lowPrice24h;
        
        @JsonProperty("volume24h")
        private String volume24h;
        
        @JsonProperty("turnover24h")
        private String turnover24h;
        
        @JsonProperty("bid1Price")
        private String bid1Price;
        
        @JsonProperty("bid1Size")
        private String bid1Size;
        
        @JsonProperty("ask1Price") 
        private String ask1Price;
        
        @JsonProperty("ask1Size")
        private String ask1Size;
        
        /**
         * Gets the last traded price as BigDecimal.
         */
        public BigDecimal getLastPriceAsBigDecimal() {
            return lastPrice != null ? new BigDecimal(lastPrice) : null;
        }
        
        /**
         * Gets the 24h volume as BigDecimal.
         */
        public BigDecimal getVolume24hAsBigDecimal() {
            return volume24h != null ? new BigDecimal(volume24h) : null;
        }
        
        /**
         * Gets the 24h price change percentage as BigDecimal.
         */
        public BigDecimal getPrice24hPercentAsBigDecimal() {
            return price24hPercent != null ? new BigDecimal(price24hPercent) : null;
        }
        
        /**
         * Gets the 24h high price as BigDecimal.
         */
        public BigDecimal getHighPrice24hAsBigDecimal() {
            return highPrice24h != null ? new BigDecimal(highPrice24h) : null;
        }
        
        /**
         * Gets the 24h low price as BigDecimal.
         */
        public BigDecimal getLowPrice24hAsBigDecimal() {
            return lowPrice24h != null ? new BigDecimal(lowPrice24h) : null;
        }
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
     * Checks if the response contains valid ticker data.
     */
    public boolean hasValidData() {
        return isSuccess() && 
               result != null && 
               result.tickerList != null && 
               !result.tickerList.isEmpty();
    }
}