package com.ahd.trading_platform.portfolio.domain.entities;

import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyCategory;
import lombok.Getter;

import java.time.Instant;
import java.util.*;

/**
 * Represents a strategy configuration applied to a portfolio.
 * Links a portfolio to a strategy with user-configured parameter values.
 *
 * Example:
 * Portfolio #123 uses:
 * - MPT strategy (Portfolio Optimization)
 *   - riskFreeRate: 0.04
 *   - optimizationType: MAX_SHARPE
 *   - Depends on ARIMA strategy (Forecasting)
 *     - p: 1, d: 1, q: 1
 *     - forecastHorizon: 30
 */
@Getter
public class PortfolioStrategyConfiguration {

    private final Long id;
    private final Long portfolioId;
    private final String strategyCode;
    private final StrategyCategory category;
    private final Map<String, String> parameterValues;
    private final Long parentConfigurationId; // For nested strategies
    private final boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    public PortfolioStrategyConfiguration(
        Long id,
        Long portfolioId,
        String strategyCode,
        StrategyCategory category,
        Map<String, String> parameterValues,
        Long parentConfigurationId,
        boolean active,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.portfolioId = Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null");
        this.strategyCode = Objects.requireNonNull(strategyCode, "Strategy code cannot be null");
        this.category = Objects.requireNonNull(category, "Strategy category cannot be null");
        this.parameterValues = parameterValues != null ? new HashMap<>(parameterValues) : new HashMap<>();
        this.parentConfigurationId = parentConfigurationId;
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    /**
     * Factory: New strategy configuration
     */
    public static PortfolioStrategyConfiguration create(
        Long portfolioId,
        String strategyCode,
        StrategyCategory category,
        Map<String, String> parameterValues
    ) {
        return new PortfolioStrategyConfiguration(
            null,
            portfolioId,
            strategyCode,
            category,
            parameterValues,
            null, // No parent (top-level strategy)
            true,
            Instant.now(),
            Instant.now()
        );
    }

    /**
     * Factory: Nested strategy configuration (child of another strategy)
     */
    public static PortfolioStrategyConfiguration createNested(
        Long portfolioId,
        String strategyCode,
        StrategyCategory category,
        Map<String, String> parameterValues,
        Long parentConfigurationId
    ) {
        return new PortfolioStrategyConfiguration(
            null,
            portfolioId,
            strategyCode,
            category,
            parameterValues,
            parentConfigurationId,
            true,
            Instant.now(),
            Instant.now()
        );
    }

    /**
     * Gets parameter value by code
     */
    public String getParameter(String code) {
        return parameterValues.get(code);
    }

    /**
     * Gets parameter value as double
     */
    public Optional<Double> getParameterAsDouble(String code) {
        String value = parameterValues.get(code);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Gets parameter value as integer
     */
    public Optional<Integer> getParameterAsInteger(String code) {
        String value = parameterValues.get(code);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Gets parameter value as boolean
     */
    public Optional<Boolean> getParameterAsBoolean(String code) {
        String value = parameterValues.get(code);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Boolean.parseBoolean(value));
    }

    /**
     * Updates parameter value
     */
    public PortfolioStrategyConfiguration updateParameter(String code, String value) {
        Map<String, String> newParams = new HashMap<>(this.parameterValues);
        newParams.put(code, value);

        return new PortfolioStrategyConfiguration(
            this.id,
            this.portfolioId,
            this.strategyCode,
            this.category,
            newParams,
            this.parentConfigurationId,
            this.active,
            this.createdAt,
            Instant.now()
        );
    }

    /**
     * Activates configuration
     */
    public PortfolioStrategyConfiguration activate() {
        return new PortfolioStrategyConfiguration(
            this.id,
            this.portfolioId,
            this.strategyCode,
            this.category,
            this.parameterValues,
            this.parentConfigurationId,
            true,
            this.createdAt,
            Instant.now()
        );
    }

    /**
     * Deactivates configuration
     */
    public PortfolioStrategyConfiguration deactivate() {
        return new PortfolioStrategyConfiguration(
            this.id,
            this.portfolioId,
            this.strategyCode,
            this.category,
            this.parameterValues,
            this.parentConfigurationId,
            false,
            this.createdAt,
            Instant.now()
        );
    }

    /**
     * Checks if this is a nested strategy (child of another strategy)
     */
    public boolean isNested() {
        return parentConfigurationId != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortfolioStrategyConfiguration that = (PortfolioStrategyConfiguration) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PortfolioStrategyConfiguration{" +
            "id=" + id +
            ", portfolioId=" + portfolioId +
            ", strategyCode='" + strategyCode + '\'' +
            ", category=" + category +
            ", active=" + active +
            ", isNested=" + isNested() +
            '}';
    }
}
