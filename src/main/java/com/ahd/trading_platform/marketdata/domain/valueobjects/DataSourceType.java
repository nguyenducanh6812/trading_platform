package com.ahd.trading_platform.marketdata.domain.valueobjects;

/**
 * Enum representing supported external data sources.
 * Provides type safety and centralized configuration for data providers.
 */
public enum DataSourceType {
    BYBIT("bybit", "Bybit", "Professional trading platform with high-quality OHLCV data"),
    BINANCE("binance", "Binance", "World's largest crypto exchange (future implementation)"),
    COINBASE("coinbase", "Coinbase", "US-regulated exchange (future implementation)");
    
    private final String code;
    private final String displayName;
    private final String description;
    
    DataSourceType(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets default data source
     */
    public static DataSourceType getDefault() {
        return BYBIT;
    }
    
    /**
     * Converts string code to enum with validation
     */
    public static DataSourceType fromCode(String code) {
        for (DataSourceType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported data source: " + code);
    }
    
    /**
     * Validates if a code is supported
     */
    public static boolean isSupported(String code) {
        for (DataSourceType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }
}