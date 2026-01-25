package com.ahd.trading_platform.portfolio.domain.valueobjects;

/**
 * Strategy configuration for portfolio management.
 * Defines how the portfolio should be managed and rebalanced.
 */
public record StrategyConfig(
    StrategyType strategyType,                  // Strategy type (MPT, EQUAL_WEIGHT, etc.)
    RiskTolerance riskTolerance,                // Risk tolerance level
    RebalancingFrequency rebalancingFrequency,  // How often to rebalance
    boolean autoRebalance,                      // Enable automatic rebalancing
    double targetRiskFreeRate                   // Target risk-free rate (for MPT, default 0.04 = 4%)
) {
    /**
     * Factory method for MPT strategy with default settings.
     */
    public static StrategyConfig mptDefault() {
        return new StrategyConfig(
            StrategyType.MPT,
            RiskTolerance.MODERATE,
            RebalancingFrequency.MONTHLY,
            true,
            0.04  // 4% risk-free rate
        );
    }

    /**
     * Factory method for equal weight strategy.
     */
    public static StrategyConfig equalWeight() {
        return new StrategyConfig(
            StrategyType.EQUAL_WEIGHT,
            RiskTolerance.MODERATE,
            RebalancingFrequency.WEEKLY,
            true,
            0.04
        );
    }

    /**
     * Factory method for custom strategy.
     */
    public static StrategyConfig custom(
        StrategyType type,
        RiskTolerance risk,
        RebalancingFrequency frequency,
        boolean autoRebalance
    ) {
        return new StrategyConfig(type, risk, frequency, autoRebalance, 0.04);
    }

    /**
     * Updates strategy type.
     */
    public StrategyConfig withStrategyType(StrategyType newType) {
        return new StrategyConfig(
            newType,
            riskTolerance,
            rebalancingFrequency,
            autoRebalance,
            targetRiskFreeRate
        );
    }

    /**
     * Updates risk tolerance.
     */
    public StrategyConfig withRiskTolerance(RiskTolerance newTolerance) {
        return new StrategyConfig(
            strategyType,
            newTolerance,
            rebalancingFrequency,
            autoRebalance,
            targetRiskFreeRate
        );
    }

    /**
     * Updates rebalancing frequency.
     */
    public StrategyConfig withRebalancingFrequency(RebalancingFrequency newFrequency) {
        return new StrategyConfig(
            strategyType,
            riskTolerance,
            newFrequency,
            autoRebalance,
            targetRiskFreeRate
        );
    }

    /**
     * Enables or disables auto-rebalancing.
     */
    public StrategyConfig withAutoRebalance(boolean enabled) {
        return new StrategyConfig(
            strategyType,
            riskTolerance,
            rebalancingFrequency,
            enabled,
            targetRiskFreeRate
        );
    }
}
