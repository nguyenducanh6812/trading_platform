package com.ahd.trading_platform.portfolio.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for configuring portfolio strategies.
 * Supports hierarchical strategy configuration (strategy with nested dependencies).
 */
public record ConfigurePortfolioStrategyRequest(
    @NotNull(message = "Portfolio ID is required")
    Long portfolioId,

    @NotBlank(message = "Strategy code is required")
    String strategyCode,

    Map<String, String> parameters,

    List<NestedStrategyConfig> nestedStrategies
) {
    /**
     * Configuration for nested/dependent strategies
     */
    public record NestedStrategyConfig(
        @NotBlank(message = "Strategy code is required")
        String strategyCode,

        Map<String, String> parameters
    ) {}
}
