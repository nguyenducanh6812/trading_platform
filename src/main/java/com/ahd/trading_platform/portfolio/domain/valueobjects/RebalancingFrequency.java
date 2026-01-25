package com.ahd.trading_platform.portfolio.domain.valueobjects;

/**
 * Portfolio rebalancing frequency options.
 * Determines how often the portfolio should be rebalanced to maintain target allocations.
 */
public enum RebalancingFrequency {
    /**
     * Daily rebalancing - highest frequency, best for active strategies
     */
    DAILY("Daily", "Rebalance every day", 1),

    /**
     * Weekly rebalancing - balanced frequency
     */
    WEEKLY("Weekly", "Rebalance every week", 7),

    /**
     * Monthly rebalancing - standard for most strategies
     */
    MONTHLY("Monthly", "Rebalance every month", 30),

    /**
     * Quarterly rebalancing - lower frequency
     */
    QUARTERLY("Quarterly", "Rebalance every quarter", 90),

    /**
     * Manual rebalancing - only when explicitly triggered
     */
    MANUAL("Manual", "Rebalance only when manually triggered", 0);

    private final String displayName;
    private final String description;
    private final int daysInterval;

    RebalancingFrequency(String displayName, String description, int daysInterval) {
        this.displayName = displayName;
        this.description = description;
        this.daysInterval = daysInterval;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getDaysInterval() {
        return daysInterval;
    }
}
