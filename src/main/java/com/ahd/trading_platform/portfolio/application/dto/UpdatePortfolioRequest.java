package com.ahd.trading_platform.portfolio.application.dto;

import com.ahd.trading_platform.portfolio.domain.valueobjects.RebalancingFrequency;
import com.ahd.trading_platform.portfolio.domain.valueobjects.RiskTolerance;
import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyType;
import jakarta.validation.constraints.*;

/**
 * Request DTO for updating portfolio settings.
 */
public record UpdatePortfolioRequest(
    @Size(min = 3, max = 100, message = "Portfolio name must be between 3 and 100 characters")
    String name,

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    String description,

    StrategyType strategyType,
    RiskTolerance riskTolerance,
    RebalancingFrequency rebalancingFrequency,
    Boolean autoRebalance,

    @Min(value = 1, message = "Leverage ratio must be at least 1")
    @Max(value = 125, message = "Leverage ratio cannot exceed 125")
    Integer leverageRatio,

    Boolean leverageEnabled
) {}
