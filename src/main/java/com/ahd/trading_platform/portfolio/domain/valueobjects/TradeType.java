package com.ahd.trading_platform.portfolio.domain.valueobjects;

/**
 * Type of trade operation.
 */
public enum TradeType {
    /**
     * Buy/Long position - purchasing an asset
     */
    BUY("Buy", "Purchase asset (long position)"),

    /**
     * Sell/Close position - selling an asset
     */
    SELL("Sell", "Sell asset (close/reduce position)");

    private final String displayName;
    private final String description;

    TradeType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isBuy() {
        return this == BUY;
    }

    public boolean isSell() {
        return this == SELL;
    }
}
