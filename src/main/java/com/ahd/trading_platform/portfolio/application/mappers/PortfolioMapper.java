package com.ahd.trading_platform.portfolio.application.mappers;

import com.ahd.trading_platform.portfolio.application.dto.PortfolioResponse;
import com.ahd.trading_platform.portfolio.domain.entities.Portfolio;
import com.ahd.trading_platform.portfolio.domain.valueobjects.Position;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting Portfolio domain entities to DTOs.
 */
@Component
public class PortfolioMapper {

    public PortfolioResponse toResponse(Portfolio portfolio) {
        // Get selected symbols
        List<String> symbols = List.copyOf(portfolio.getSelectedSymbols());

        return new PortfolioResponse(
            portfolio.getId(),
            portfolio.getName(),
            portfolio.getDescription(),
            portfolio.getUserId(),
            symbols,
            mapCapital(portfolio),
            mapPositions(portfolio.getAllPositions()),
            mapStrategy(portfolio),
            mapLeverage(portfolio),
            portfolio.getStatus(),
            mapPerformance(portfolio),
            portfolio.getLastRebalancedAt(),
            portfolio.getCreatedAt(),
            portfolio.getUpdatedAt()
        );
    }

    private PortfolioResponse.CapitalInfo mapCapital(Portfolio portfolio) {
        var capital = portfolio.getCapital();
        return new PortfolioResponse.CapitalInfo(
            capital.initialAmount(),
            capital.currentAmount(),
            capital.availableAmount(),
            capital.reservedAmount(),
            capital.currency(),
            capital.getTotalProfitLoss(),
            capital.getProfitLossPercentage()
        );
    }

    private List<PortfolioResponse.PositionInfo> mapPositions(List<Position> positions) {
        return positions.stream()
            .map(p -> new PortfolioResponse.PositionInfo(
                p.symbol(),
                p.quantity(),
                p.averageEntryPrice(),
                p.currentMarketPrice(),
                p.getCurrentValue(),
                p.getCostBasis(),
                p.getUnrealizedPnL(),
                p.getUnrealizedPnLPercentage(),
                p.lastUpdated()
            ))
            .collect(Collectors.toList());
    }

    private PortfolioResponse.StrategyInfo mapStrategy(Portfolio portfolio) {
        var strategy = portfolio.getStrategyConfig();
        return new PortfolioResponse.StrategyInfo(
            strategy.strategyType(),
            strategy.riskTolerance(),
            strategy.rebalancingFrequency(),
            strategy.autoRebalance(),
            strategy.targetRiskFreeRate()
        );
    }

    private PortfolioResponse.LeverageInfo mapLeverage(Portfolio portfolio) {
        var leverage = portfolio.getLeverage();
        return new PortfolioResponse.LeverageInfo(
            leverage.enabled(),
            leverage.leverageRatio(),
            leverage.maxLeverageAllowed()
        );
    }

    private PortfolioResponse.PerformanceMetrics mapPerformance(Portfolio portfolio) {
        return new PortfolioResponse.PerformanceMetrics(
            portfolio.getTotalValue(),
            portfolio.getTotalUnrealizedPnL(),
            BigDecimal.ZERO, // TODO: Calculate realized P&L from trade history
            portfolio.getAllPositions().size(),
            portfolio.getTradeHistory().size()
        );
    }
}
