package com.ahd.trading_platform.portfolio.domain.valueobjects;

/**
 * Status of a trade execution.
 */
public enum TradeStatus {
    /**
     * Trade is pending execution
     */
    PENDING("Pending", "Trade is pending execution"),

    /**
     * Trade successfully executed
     */
    EXECUTED("Executed", "Trade successfully executed"),

    /**
     * Trade execution failed
     */
    FAILED("Failed", "Trade execution failed"),

    /**
     * Trade was cancelled before execution
     */
    CANCELLED("Cancelled", "Trade was cancelled");

    private final String displayName;
    private final String description;

    TradeStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompleted() {
        return this == EXECUTED || this == FAILED || this == CANCELLED;
    }

    public boolean isSuccessful() {
        return this == EXECUTED;
    }
}
