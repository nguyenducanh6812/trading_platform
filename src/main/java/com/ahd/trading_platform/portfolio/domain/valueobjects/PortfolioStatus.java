package com.ahd.trading_platform.portfolio.domain.valueobjects;

/**
 * Portfolio lifecycle status.
 */
public enum PortfolioStatus {
    /**
     * Draft portfolio - being configured, not yet activated
     */
    DRAFT("Draft", "Portfolio is being configured as draft"),

    /**
     * Active portfolio - can execute trades and rebalance
     */
    ACTIVE("Active", "Portfolio is active and can execute trades"),

    /**
     * Paused portfolio - temporarily suspended, no new trades
     */
    PAUSED("Paused", "Portfolio is paused, no new trades allowed"),

    /**
     * Closed portfolio - permanently closed, all positions liquidated
     */
    CLOSED("Closed", "Portfolio is closed, all positions liquidated"),

    /**
     * Under rebalancing - currently being rebalanced
     */
    REBALANCING("Rebalancing", "Portfolio is currently being rebalanced");

    private final String displayName;
    private final String description;

    PortfolioStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTrade() {
        return this == ACTIVE;
    }

    public boolean canRebalance() {
        return this == ACTIVE;
    }

    public boolean isDraft() {
        return this == DRAFT;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }
}
