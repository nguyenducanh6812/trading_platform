package com.ahd.trading_platform.portfolio.infrastructure.persistence.mappers;

import com.ahd.trading_platform.portfolio.domain.entities.Portfolio;
import com.ahd.trading_platform.portfolio.domain.entities.Trade;
import com.ahd.trading_platform.portfolio.domain.valueobjects.*;
import com.ahd.trading_platform.portfolio.infrastructure.persistence.entities.PortfolioEntity;
import com.ahd.trading_platform.portfolio.infrastructure.persistence.entities.PositionEntity;
import com.ahd.trading_platform.portfolio.infrastructure.persistence.entities.TradeEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Portfolio domain entities and JPA entities.
 */
@Component
public class PortfolioEntityMapper {

    public Portfolio toDomain(PortfolioEntity entity) {
        Capital capital = new Capital(
            entity.getInitialCapital(),
            entity.getCurrentCapital(),
            entity.getAvailableCapital(),
            entity.getReservedCapital(),
            entity.getCurrency()
        );

        StrategyConfig strategyConfig = new StrategyConfig(
            entity.getStrategyType(),
            entity.getRiskTolerance(),
            entity.getRebalancingFrequency(),
            entity.getAutoRebalance(),
            entity.getTargetRiskFreeRate().doubleValue()
        );

        Leverage leverage = entity.getLeverageEnabled()
            ? Leverage.of(entity.getLeverageRatio())
            : Leverage.none();

        List<Position> positions = entity.getPositions().stream()
            .map(this::positionToDomain)
            .collect(Collectors.toList());

        List<Trade> trades = entity.getTrades().stream()
            .map(this::tradeToDomain)
            .collect(Collectors.toList());

        // Use symbols directly
        Set<String> symbols = new HashSet<>(entity.getInstrumentCodes());

        return Portfolio.reconstitute(
            entity.getId(),
            entity.getName(),
            entity.getDescription(),
            entity.getUserId(),
            symbols,
            capital,
            positions,
            strategyConfig,
            leverage,
            entity.getStatus(),
            trades,
            entity.getLastRebalancedAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public PortfolioEntity toEntity(Portfolio portfolio) {
        PortfolioEntity entity = new PortfolioEntity();
        entity.setId(portfolio.getId());
        updateEntityFields(entity, portfolio);
        return entity;
    }

    /**
     * Updates an existing entity with portfolio data.
     * Used for update operations to avoid Hibernate session conflicts.
     */
    public void updateEntity(PortfolioEntity entity, Portfolio portfolio) {
        updateEntityFields(entity, portfolio);
    }

    /**
     * Common method to update entity fields from portfolio domain object.
     */
    private void updateEntityFields(PortfolioEntity entity, Portfolio portfolio) {
        entity.setName(portfolio.getName());
        entity.setDescription(portfolio.getDescription());
        entity.setUserId(portfolio.getUserId());

        // Use symbols directly
        Set<String> symbols = portfolio.getSelectedSymbols();
        entity.setInstrumentCodes(symbols);

        // Capital
        Capital capital = portfolio.getCapital();
        entity.setInitialCapital(capital.initialAmount());
        entity.setCurrentCapital(capital.currentAmount());
        entity.setAvailableCapital(capital.availableAmount());
        entity.setReservedCapital(capital.reservedAmount());
        entity.setCurrency(capital.currency());

        // Strategy
        StrategyConfig strategy = portfolio.getStrategyConfig();
        entity.setStrategyType(strategy.strategyType());
        entity.setRiskTolerance(strategy.riskTolerance());
        entity.setRebalancingFrequency(strategy.rebalancingFrequency());
        entity.setAutoRebalance(strategy.autoRebalance());
        entity.setTargetRiskFreeRate(java.math.BigDecimal.valueOf(strategy.targetRiskFreeRate()));

        // Leverage
        Leverage leverage = portfolio.getLeverage();
        entity.setLeverageEnabled(leverage.enabled());
        entity.setLeverageRatio(leverage.leverageRatio());
        entity.setMaxLeverageAllowed(leverage.maxLeverageAllowed());

        // Status and timestamps
        entity.setStatus(portfolio.getStatus());
        entity.setLastRebalancedAt(portfolio.getLastRebalancedAt());
        entity.setCreatedAt(portfolio.getCreatedAt());
        entity.setUpdatedAt(portfolio.getUpdatedAt());

        // Update positions (clear and add to avoid orphan removal errors)
        entity.getPositions().clear();
        portfolio.getAllPositions().stream()
            .map(position -> positionToEntity(position, entity))
            .forEach(entity.getPositions()::add);

        // Update trades (clear and add to avoid orphan removal errors)
        entity.getTrades().clear();
        portfolio.getTradeHistory().stream()
            .map(trade -> tradeToEntity(trade, entity))
            .forEach(entity.getTrades()::add);
    }

    private Position positionToDomain(PositionEntity entity) {
        return new Position(
            entity.getInstrumentCode(),
            entity.getQuantity(),
            entity.getAverageEntryPrice(),
            entity.getCurrentMarketPrice(),
            entity.getLastUpdated()
        );
    }

    private PositionEntity positionToEntity(Position position, PortfolioEntity portfolio) {
        PositionEntity entity = new PositionEntity();
        entity.setPortfolio(portfolio);
        entity.setInstrumentCode(position.symbol());
        entity.setQuantity(position.quantity());
        entity.setAverageEntryPrice(position.averageEntryPrice());
        entity.setCurrentMarketPrice(position.currentMarketPrice());
        entity.setLastUpdated(position.lastUpdated());
        return entity;
    }

    private Trade tradeToDomain(TradeEntity entity) {
        return Trade.reconstitute(
            entity.getId(),
            entity.getTradeType(),
            entity.getInstrumentCode(),
            entity.getQuantity(),
            entity.getPrice(),
            entity.getFeeAmount(),
            entity.getFeeCurrency(),
            entity.getStatus(),
            entity.getExecutionReference(),
            entity.getFailureReason(),
            entity.getExecutedAt()
        );
    }

    private TradeEntity tradeToEntity(Trade trade, PortfolioEntity portfolio) {
        TradeEntity entity = new TradeEntity();
        entity.setId(trade.getId());
        entity.setPortfolio(portfolio);
        entity.setTradeType(trade.getTradeType());
        entity.setInstrumentCode(trade.getSymbol());
        entity.setQuantity(trade.getQuantity());
        entity.setPrice(trade.getPrice());
        entity.setFeeAmount(trade.getFeeAmount());
        entity.setFeeCurrency(trade.getFeeCurrency());
        entity.setStatus(trade.getStatus());
        entity.setExecutionReference(trade.getExecutionReference());
        entity.setFailureReason(trade.getFailureReason());
        entity.setExecutedAt(trade.getExecutedAt());
        return entity;
    }
}
