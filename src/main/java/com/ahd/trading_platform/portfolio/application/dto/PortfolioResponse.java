package com.ahd.trading_platform.portfolio.application.dto;

import com.ahd.trading_platform.portfolio.domain.valueobjects.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response DTO for portfolio information.
 */
public record PortfolioResponse(
    Long id,
    String name,
    String description,
    String userId,
    List<String> symbols,  // Selected trading symbols (e.g., BTCUSDT, ETHUSDT)
    CapitalInfo capital,
    List<PositionInfo> positions,
    StrategyInfo strategy,
    LeverageInfo leverage,
    PortfolioStatus status,
    PerformanceMetrics performance,
    Instant lastRebalancedAt,
    Instant createdAt,
    Instant updatedAt
) {
    public record CapitalInfo(
        BigDecimal initial,
        BigDecimal current,
        BigDecimal available,
        BigDecimal reserved,
        String currency,
        BigDecimal profitLoss,
        BigDecimal profitLossPercentage
    ) {}

    public record PositionInfo(
        String symbol,
        BigDecimal quantity,
        BigDecimal averageEntryPrice,
        BigDecimal currentMarketPrice,
        BigDecimal currentValue,
        BigDecimal costBasis,
        BigDecimal unrealizedPnL,
        BigDecimal unrealizedPnLPercentage,
        Instant lastUpdated
    ) {}

    public record StrategyInfo(
        StrategyType type,
        RiskTolerance riskTolerance,
        RebalancingFrequency rebalancingFrequency,
        boolean autoRebalance,
        double targetRiskFreeRate
    ) {}

    public record LeverageInfo(
        boolean enabled,
        int ratio,
        int maxAllowed
    ) {}

    public record PerformanceMetrics(
        BigDecimal totalValue,
        BigDecimal totalUnrealizedPnL,
        BigDecimal totalRealizedPnL,
        int numberOfPositions,
        int numberOfTrades
    ) {}
}
