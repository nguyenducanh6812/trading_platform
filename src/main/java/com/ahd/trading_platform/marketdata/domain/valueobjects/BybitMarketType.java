package com.ahd.trading_platform.marketdata.domain.valueobjects;

import lombok.Getter;

/**
 * Enum representing Bybit market types for different trading contracts.
 * Centralizes configuration for Bybit API categories.
 */
@Getter
public enum BybitMarketType {
    SPOT("spot", "Spot Trading", "Regular spot trading pairs"),
    LINEAR("linear", "Linear Futures", "USDT-margined perpetual contracts"),
    INVERSE("inverse", "Inverse Futures", "Coin-margined perpetual contracts"),
    OPTION("option", "Options", "Options contracts");
    
    private final String category;
    private final String displayName;
    private final String description;
    
    BybitMarketType(String category, String displayName, String description) {
        this.category = category;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Gets default market type for cryptocurrency trading
     */
    public static BybitMarketType getDefault() {
        return LINEAR; // Changed from SPOT to LINEAR for futures contracts as per feedback
    }
    
    /**
     * Gets market type for crypto futures trading (as mentioned in feedback)
     */
    public static BybitMarketType getForCryptoFutures() {
        return LINEAR;
    }
}