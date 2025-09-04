package com.ahd.trading_platform.marketdata.infrastructure.external.dto.bybit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for Bybit instruments info API.
 * Contains information about available trading pairs and their specifications.
 */
@Data
@NoArgsConstructor
public class BybitInstrumentsResponse {
    
    @JsonProperty("retCode")
    private int returnCode;
    
    @JsonProperty("retMsg")
    private String returnMessage;
    
    @JsonProperty("result")
    private InstrumentsResult result;
    
    @JsonProperty("retExtInfo")
    private Object extendedInfo;
    
    @JsonProperty("time")
    private long responseTime;
    
    @Data
    @NoArgsConstructor
    public static class InstrumentsResult {
        
        @JsonProperty("category")
        private String category;
        
        @JsonProperty("list")
        private List<InstrumentInfo> instrumentList;
        
        @JsonProperty("nextPageCursor")
        private String nextPageCursor;
    }
    
    @Data
    @NoArgsConstructor
    public static class InstrumentInfo {
        
        @JsonProperty("symbol")
        private String symbol;
        
        @JsonProperty("baseCoin")
        private String baseCoin;
        
        @JsonProperty("quoteCoin")
        private String quoteCoin;
        
        @JsonProperty("status")
        private String status; // Trading, Closed, etc.
        
        @JsonProperty("minOrderQty")
        private String minOrderQty;
        
        @JsonProperty("maxOrderQty")
        private String maxOrderQty;
        
        @JsonProperty("minOrderAmt")
        private String minOrderAmount;
        
        @JsonProperty("maxOrderAmt") 
        private String maxOrderAmount;
        
        @JsonProperty("qtyStep")
        private String qtyStep;
        
        @JsonProperty("priceScale")
        private Integer priceScale;
        
        @JsonProperty("lotSizeFilter")
        private LotSizeFilter lotSizeFilter;
        
        @JsonProperty("priceFilter")
        private PriceFilter priceFilter;
        
        /**
         * Checks if the instrument is currently tradable.
         */
        public boolean isTradable() {
            return "Trading".equals(status);
        }
        
        /**
         * Checks if this is a BTC trading pair.
         */
        public boolean isBitcoinPair() {
            return "BTC".equals(baseCoin) || symbol.startsWith("BTC");
        }
        
        /**
         * Checks if this is an ETH trading pair.
         */
        public boolean isEthereumPair() {
            return "ETH".equals(baseCoin) || symbol.startsWith("ETH");
        }
        
        /**
         * Checks if this is a USDT quoted pair.
         */
        public boolean isUSDTQuoted() {
            return "USDT".equals(quoteCoin);
        }
    }
    
    @Data
    @NoArgsConstructor
    public static class LotSizeFilter {
        
        @JsonProperty("basePrecision")
        private String basePrecision;
        
        @JsonProperty("quotePrecision")
        private String quotePrecision;
        
        @JsonProperty("minOrderQty")
        private String minOrderQty;
        
        @JsonProperty("maxOrderQty")
        private String maxOrderQty;
        
        @JsonProperty("minOrderAmt")
        private String minOrderAmount;
        
        @JsonProperty("maxOrderAmt")
        private String maxOrderAmount;
        
        @JsonProperty("qtyStep")
        private String qtyStep;
    }
    
    @Data
    @NoArgsConstructor
    public static class PriceFilter {
        
        @JsonProperty("minPrice")
        private String minPrice;
        
        @JsonProperty("maxPrice")
        private String maxPrice;
        
        @JsonProperty("tickSize")
        private String tickSize;
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
     * Checks if the response contains valid instruments data.
     */
    public boolean hasValidData() {
        return isSuccess() && 
               result != null && 
               result.instrumentList != null && 
               !result.instrumentList.isEmpty();
    }
}