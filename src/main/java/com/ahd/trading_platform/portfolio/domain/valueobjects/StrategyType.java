package com.ahd.trading_platform.portfolio.domain.valueobjects;

/**
 * Portfolio strategy types for asset allocation and rebalancing.
 */
public enum StrategyType {
    /**
     * Modern Portfolio Theory - optimizes risk-adjusted returns using Sharpe ratio
     */
    MPT("Modern Portfolio Theory", "Optimizes portfolio using MPT principles (Sharpe ratio, efficient frontier)"),

    /**
     * Equal weight allocation across all instruments
     */
    EQUAL_WEIGHT("Equal Weight", "Allocates capital equally across all instruments"),

    /**
     * Market capitalization weighted allocation
     */
    MARKET_CAP_WEIGHT("Market Cap Weight", "Allocates capital based on market capitalization"),

    /**
     * Momentum-based strategy
     */
    MOMENTUM("Momentum Strategy", "Allocates based on price momentum and trends"),

    /**
     * Custom user-defined strategy
     */
    CUSTOM("Custom Strategy", "User-defined allocation strategy");

    private final String displayName;
    private final String description;

    StrategyType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
