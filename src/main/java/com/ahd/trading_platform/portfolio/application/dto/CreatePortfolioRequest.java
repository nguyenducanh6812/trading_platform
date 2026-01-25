package com.ahd.trading_platform.portfolio.application.dto;

import com.ahd.trading_platform.portfolio.domain.valueobjects.PortfolioStatus;
import com.ahd.trading_platform.portfolio.domain.valueobjects.RebalancingFrequency;
import com.ahd.trading_platform.portfolio.domain.valueobjects.RiskTolerance;
import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating a new portfolio.
 */
public record CreatePortfolioRequest(
    @NotBlank(message = "Portfolio name is required")
    @Size(min = 3, max = 100, message = "Portfolio name must be between 3 and 100 characters")
    String name,

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    String description,

    @NotNull(message = "Symbols are required")
    @Size(min = 1, message = "At least one symbol is required")
    List<String> symbols,

    @NotNull(message = "Initial capital is required")
    @DecimalMin(value = "0.01", message = "Initial capital must be at least 0.01")
    BigDecimal initialCapital,

    @NotBlank(message = "Currency is required")
    String currency,

    @NotNull(message = "Strategy type is required")
    StrategyType strategyType,

    @NotNull(message = "Risk tolerance is required")
    RiskTolerance riskTolerance,

    @NotNull(message = "Rebalancing frequency is required")
    RebalancingFrequency rebalancingFrequency,

    boolean autoRebalance,

    @Min(value = 1, message = "Leverage ratio must be at least 1")
    @Max(value = 125, message = "Leverage ratio cannot exceed 125")
    int leverageRatio,

    boolean leverageEnabled,

    // Optional status - defaults to ACTIVE if not provided
    PortfolioStatus status
) {
    /**
     * Factory method for MPT strategy with default settings.
     */
    public static CreatePortfolioRequest mptDefault(String name, List<String> symbols, BigDecimal initialCapital) {
        return new CreatePortfolioRequest(
            name,
            "Portfolio using Modern Portfolio Theory",
            symbols,
            initialCapital,
            "USD",
            StrategyType.MPT,
            RiskTolerance.MODERATE,
            RebalancingFrequency.MONTHLY,
            true,
            1,
            false,
            null  // defaults to ACTIVE
        );
    }
}
