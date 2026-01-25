package com.ahd.trading_platform.portfolio.domain.valueobjects;

/**
 * Categories of trading strategies.
 * Each category represents a different aspect of the trading workflow.
 */
public enum StrategyCategory {
    /**
     * Portfolio optimization strategies
     * Examples: MPT, Maximum Sharpe Ratio, Risk Parity
     */
    PORTFOLIO_OPTIMIZATION("Portfolio Optimization", "Strategies for optimizing portfolio allocation"),

    /**
     * Price forecasting strategies
     * Examples: ARIMA, GARCH, Machine Learning models
     */
    FORECASTING("Forecasting", "Strategies for predicting future prices"),

    /**
     * Risk management strategies
     * Examples: Stop Loss, Position Sizing, VaR
     */
    RISK_MANAGEMENT("Risk Management", "Strategies for managing portfolio risk"),

    /**
     * Execution strategies
     * Examples: TWAP, VWAP, Market Making
     */
    EXECUTION("Execution", "Strategies for order execution");

    private final String displayName;
    private final String description;

    StrategyCategory(String displayName, String description) {
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
