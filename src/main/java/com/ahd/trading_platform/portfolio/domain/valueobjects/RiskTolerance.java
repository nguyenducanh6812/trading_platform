package com.ahd.trading_platform.portfolio.domain.valueobjects;

/**
 * Risk tolerance levels for portfolio management.
 * Determines the acceptable level of volatility and potential losses.
 */
public enum RiskTolerance {
    /**
     * Conservative - Low risk, stable returns
     * Suitable for risk-averse investors
     */
    CONSERVATIVE("Conservative", "Low risk tolerance, focuses on capital preservation", 0.1),

    /**
     * Moderate - Balanced risk/reward
     * Suitable for most investors
     */
    MODERATE("Moderate", "Balanced risk tolerance, targets steady growth", 0.2),

    /**
     * AGGRESSIVE - High risk, high potential returns
     * Suitable for risk-seeking investors
     */
    AGGRESSIVE("Aggressive", "High risk tolerance, targets maximum returns", 0.3);

    private final String displayName;
    private final String description;
    private final double maxVolatilityThreshold;

    RiskTolerance(String displayName, String description, double maxVolatilityThreshold) {
        this.displayName = displayName;
        this.description = description;
        this.maxVolatilityThreshold = maxVolatilityThreshold;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public double getMaxVolatilityThreshold() {
        return maxVolatilityThreshold;
    }
}
