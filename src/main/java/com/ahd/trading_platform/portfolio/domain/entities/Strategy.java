package com.ahd.trading_platform.portfolio.domain.entities;

import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyCategory;
import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyParameter;
import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Strategy entity representing a trading/portfolio strategy.
 * Defines what strategies are available, their configuration parameters,
 * and dependencies on other strategies.
 *
 * Domain Model for Strategy Composition:
 * - Portfolio strategies (e.g., MPT) can depend on forecasting strategies (e.g., ARIMA)
 * - Each strategy defines its required and optional parameters
 * - Strategies can be composed hierarchically
 */
@Getter
public class Strategy {

    private final Long id;
    private final StrategyType type;
    private final StrategyCategory category;
    private final String code;
    private final String name;
    private final String description;
    private final boolean active;
    private final List<StrategyParameter> parameters;
    private final List<StrategyDependency> dependencies;

    public Strategy(
        Long id,
        StrategyType type,
        StrategyCategory category,
        String code,
        String name,
        String description,
        boolean active,
        List<StrategyParameter> parameters,
        List<StrategyDependency> dependencies
    ) {
        this.id = id;
        this.type = Objects.requireNonNull(type, "Strategy type cannot be null");
        this.category = Objects.requireNonNull(category, "Strategy category cannot be null");
        this.code = Objects.requireNonNull(code, "Strategy code cannot be null");
        this.name = Objects.requireNonNull(name, "Strategy name cannot be null");
        this.description = description;
        this.active = active;
        this.parameters = parameters != null ? new ArrayList<>(parameters) : new ArrayList<>();
        this.dependencies = dependencies != null ? new ArrayList<>(dependencies) : new ArrayList<>();
    }

    /**
     * Factory: Modern Portfolio Theory strategy
     */
    public static Strategy mpt() {
        List<StrategyParameter> params = List.of(
            StrategyParameter.required("riskFreeRate", "Risk-Free Rate", "double", "0.04",
                "Annual risk-free rate for Sharpe ratio calculation"),
            StrategyParameter.optional("optimizationType", "Optimization Type", "string", "MAX_SHARPE",
                "Type of portfolio optimization (MAX_SHARPE, MIN_VARIANCE, EFFICIENT_FRONTIER)"),
            StrategyParameter.optional("constraintsEnabled", "Enable Constraints", "boolean", "true",
                "Enable position size and leverage constraints"),
            StrategyParameter.optional("rebalancingThreshold", "Rebalancing Threshold", "double", "0.05",
                "Trigger rebalancing when drift exceeds this percentage")
        );

        List<StrategyDependency> deps = List.of(
            new StrategyDependency(
                StrategyCategory.FORECASTING,
                true, // required
                "Price forecasting strategy for expected returns"
            )
        );

        return new Strategy(
            null,
            StrategyType.MPT,
            StrategyCategory.PORTFOLIO_OPTIMIZATION,
            "MPT",
            "Modern Portfolio Theory",
            "Optimizes portfolio allocation using mean-variance optimization. " +
            "Requires a forecasting strategy (e.g., ARIMA) to predict expected returns.",
            true,
            params,
            deps
        );
    }

    /**
     * Factory: ARIMA forecasting strategy
     */
    public static Strategy arima() {
        List<StrategyParameter> params = List.of(
            StrategyParameter.required("p", "AR Order", "integer", "1",
                "AutoRegressive order"),
            StrategyParameter.required("d", "Differencing Order", "integer", "1",
                "Degree of differencing"),
            StrategyParameter.required("q", "MA Order", "integer", "1",
                "Moving Average order"),
            StrategyParameter.optional("forecastHorizon", "Forecast Horizon", "integer", "30",
                "Number of days to forecast"),
            StrategyParameter.optional("confidenceLevel", "Confidence Level", "double", "0.95",
                "Confidence level for prediction intervals")
        );

        return new Strategy(
            null,
            StrategyType.MPT, // Using existing enum, should add ARIMA to StrategyType
            StrategyCategory.FORECASTING,
            "ARIMA",
            "ARIMA Forecasting",
            "Time series forecasting using AutoRegressive Integrated Moving Average model. " +
            "Used by portfolio optimization strategies to predict expected returns.",
            true,
            params,
            Collections.emptyList() // No dependencies
        );
    }

    /**
     * Gets parameter by code
     */
    public StrategyParameter getParameter(String code) {
        return parameters.stream()
            .filter(p -> p.getCode().equals(code))
            .findFirst()
            .orElse(null);
    }

    /**
     * Checks if strategy has dependencies
     */
    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }

    /**
     * Gets required dependencies
     */
    public List<StrategyDependency> getRequiredDependencies() {
        return dependencies.stream()
            .filter(StrategyDependency::isRequired)
            .toList();
    }

    /**
     * Checks if strategy depends on a specific category
     */
    public boolean dependsOn(StrategyCategory category) {
        return dependencies.stream()
            .anyMatch(d -> d.getCategory() == category);
    }

    /**
     * Value object representing a strategy dependency
     */
    @Getter
    public static class StrategyDependency {
        private final StrategyCategory category;
        private final boolean required;
        private final String description;

        public StrategyDependency(StrategyCategory category, boolean required, String description) {
            this.category = Objects.requireNonNull(category, "Dependency category cannot be null");
            this.required = required;
            this.description = description;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Strategy strategy = (Strategy) o;
        return Objects.equals(code, strategy.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return "Strategy{" +
            "code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", category=" + category +
            ", active=" + active +
            '}';
    }
}
