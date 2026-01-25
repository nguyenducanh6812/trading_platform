package com.ahd.trading_platform.portfolio.domain.entities;

import com.ahd.trading_platform.portfolio.domain.valueobjects.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Portfolio aggregate root.
 * Manages trading symbols, positions, capital, and strategy configuration.
 * Portfolio can contain symbols from different markets (SPOT, LINEAR, INVERSE, OPTION).
 */
@Getter
public class Portfolio {
    private final Long id;
    private String name;
    private String description;
    private final String userId;  // Owner of the portfolio
    private final Set<String> symbols;  // Selected trading symbols (e.g., BTCUSDT, ETHUSDT)
    private Capital capital;
    private final Map<String, Position> positions;  // Symbol -> Position mapping
    private final List<Trade> tradeHistory;
    private StrategyConfig strategyConfig;
    private Leverage leverage;
    private PortfolioStatus status;
    private Instant lastRebalancedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    // Constructor for creating new portfolio
    public Portfolio(
        String name,
        String description,
        String userId,
        Set<String> symbols,
        BigDecimal initialCapital,
        String currency,
        StrategyConfig strategyConfig,
        Leverage leverage,
        PortfolioStatus status
    ) {
        this(null, name, description, userId, symbols,
            Capital.initial(initialCapital, currency),
            new HashMap<>(), new ArrayList<>(),
            strategyConfig, leverage,
            status != null ? status : PortfolioStatus.ACTIVE, null,
            Instant.now(), Instant.now());
    }

    // Full constructor (for reconstitution from database)
    public Portfolio(
        Long id,
        String name,
        String description,
        String userId,
        Set<String> symbols,
        Capital capital,
        Map<String, Position> positions,
        List<Trade> tradeHistory,
        StrategyConfig strategyConfig,
        Leverage leverage,
        PortfolioStatus status,
        Instant lastRebalancedAt,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Portfolio name cannot be null");
        this.description = description;
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.symbols = Objects.requireNonNull(symbols, "Symbols cannot be null");
        if (symbols.isEmpty()) {
            throw new IllegalArgumentException("Portfolio must have at least one symbol");
        }
        this.capital = Objects.requireNonNull(capital, "Capital cannot be null");
        this.positions = positions != null ? positions : new HashMap<>();
        this.tradeHistory = tradeHistory != null ? tradeHistory : new ArrayList<>();
        this.strategyConfig = Objects.requireNonNull(strategyConfig, "Strategy config cannot be null");
        this.leverage = Objects.requireNonNull(leverage, "Leverage cannot be null");
        this.status = status != null ? status : PortfolioStatus.ACTIVE;
        this.lastRebalancedAt = lastRebalancedAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    /**
     * Factory method for creating portfolio with MPT strategy.
     */
    public static Portfolio createWithMPT(
        String name,
        String userId,
        Set<String> symbols,
        BigDecimal initialCapital,
        RiskTolerance riskTolerance
    ) {
        StrategyConfig config = StrategyConfig.mptDefault().withRiskTolerance(riskTolerance);
        return new Portfolio(name, null, userId, symbols, initialCapital, "USD", config, Leverage.none(), PortfolioStatus.ACTIVE);
    }

    /**
     * Factory method for reconstituting portfolio from persistence (with positions as List).
     */
    public static Portfolio reconstitute(
        Long id,
        String name,
        String description,
        String userId,
        Set<String> symbols,
        Capital capital,
        List<Position> positionsList,
        StrategyConfig strategyConfig,
        Leverage leverage,
        PortfolioStatus status,
        List<Trade> tradeHistory,
        Instant lastRebalancedAt,
        Instant createdAt,
        Instant updatedAt
    ) {
        Map<String, Position> positionsMap = positionsList.stream()
            .collect(Collectors.toMap(Position::symbol, p -> p));

        return new Portfolio(
            id, name, description, userId, symbols, capital, positionsMap, tradeHistory,
            strategyConfig, leverage, status, lastRebalancedAt, createdAt, updatedAt
        );
    }

    /**
     * Validates that a symbol is in the portfolio's selected symbols.
     *
     * @param symbol The trading symbol to validate
     * @throws IllegalArgumentException if symbol not in portfolio's selected symbols
     */
    private void validateSymbolInPortfolio(String symbol) {
        if (!symbols.contains(symbol)) {
            throw new IllegalArgumentException(
                String.format("Symbol %s is not in portfolio's selected symbols", symbol)
            );
        }
    }

    /**
     * Adds symbol to portfolio (opens new position).
     * The symbol must be in the portfolio's selected symbols.
     */
    public void addInstrument(String symbol, BigDecimal quantity, BigDecimal entryPrice) {
        ensureActive();
        validateSymbolInPortfolio(symbol);

        if (positions.containsKey(symbol)) {
            throw new IllegalArgumentException(
                String.format("Symbol %s already has a position in portfolio", symbol)
            );
        }

        // Calculate cost
        BigDecimal cost = quantity.multiply(entryPrice);

        // Reserve capital
        this.capital = capital.reserve(cost);

        // Create position
        Position newPosition = Position.open(symbol, quantity, entryPrice);
        positions.put(symbol, newPosition);

        // Record trade
        Trade trade = Trade.buy(symbol, quantity, entryPrice);
        trade.markExecuted("INITIAL_POSITION");
        tradeHistory.add(trade);

        this.updatedAt = Instant.now();
    }

    /**
     * Increases existing position (buy more).
     */
    public void increasePosition(String symbol, BigDecimal quantity, BigDecimal purchasePrice) {
        ensureActive();
        validateSymbolInPortfolio(symbol);

        Position existingPosition = positions.get(symbol);
        if (existingPosition == null) {
            throw new IllegalArgumentException(
                String.format("Symbol %s not found in portfolio", symbol)
            );
        }

        // Calculate cost
        BigDecimal cost = quantity.multiply(purchasePrice);

        // Reserve capital
        this.capital = capital.reserve(cost);

        // Update position
        Position updatedPosition = existingPosition.increase(quantity, purchasePrice);
        positions.put(symbol, updatedPosition);

        // Record trade
        Trade trade = Trade.buy(symbol, quantity, purchasePrice);
        trade.markExecuted("INCREASE_POSITION");
        tradeHistory.add(trade);

        this.updatedAt = Instant.now();
    }

    /**
     * Decreases position (sell).
     */
    public void decreasePosition(String symbol, BigDecimal quantity, BigDecimal sellPrice) {
        ensureActive();

        Position existingPosition = positions.get(symbol);
        if (existingPosition == null) {
            throw new IllegalArgumentException(
                String.format("Symbol %s not found in portfolio", symbol)
            );
        }

        // Calculate proceeds
        BigDecimal proceeds = quantity.multiply(sellPrice);

        // Update position
        Position updatedPosition = existingPosition.decrease(quantity);

        // Remove position if empty
        if (updatedPosition.isEmpty()) {
            positions.remove(symbol);
        } else {
            positions.put(symbol, updatedPosition);
        }

        // Release capital (cost basis of sold portion)
        BigDecimal costBasisReleased = quantity.multiply(existingPosition.averageEntryPrice());
        this.capital = capital.release(costBasisReleased);

        // Update capital with realized P&L
        BigDecimal realizedPnL = proceeds.subtract(costBasisReleased);
        BigDecimal newCurrentValue = capital.currentAmount().add(realizedPnL);
        this.capital = capital.updateCurrentValue(newCurrentValue);

        // Record trade
        Trade trade = Trade.sell(symbol, quantity, sellPrice);
        trade.markExecuted("DECREASE_POSITION");
        tradeHistory.add(trade);

        this.updatedAt = Instant.now();
    }

    /**
     * Removes symbol completely from portfolio.
     */
    public void removeInstrument(String symbol, BigDecimal currentPrice) {
        Position position = positions.get(symbol);
        if (position == null) {
            throw new IllegalArgumentException(
                String.format("Symbol %s not found in portfolio", symbol)
            );
        }

        // Sell entire position
        decreasePosition(symbol, position.quantity(), currentPrice);
    }

    /**
     * Updates market prices for all positions (for P&L calculation).
     */
    public void updateMarketPrices(Map<String, BigDecimal> marketPrices) {
        for (Map.Entry<String, BigDecimal> entry : marketPrices.entrySet()) {
            Position position = positions.get(entry.getKey());
            if (position != null) {
                Position updatedPosition = position.updateMarketPrice(entry.getValue());
                positions.put(entry.getKey(), updatedPosition);
            }
        }

        this.updatedAt = Instant.now();
    }

    /**
     * Calculates total portfolio value.
     */
    public BigDecimal getTotalValue() {
        BigDecimal positionsValue = positions.values().stream()
            .map(Position::getCurrentValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return capital.availableAmount().add(positionsValue);
    }

    /**
     * Calculates total unrealized P&L from all positions.
     */
    public BigDecimal getTotalUnrealizedPnL() {
        return positions.values().stream()
            .map(Position::getUnrealizedPnL)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Gets position for specific symbol.
     */
    public Optional<Position> getPosition(String symbol) {
        return Optional.ofNullable(positions.get(symbol));
    }

    /**
     * Gets all positions as list.
     */
    public List<Position> getAllPositions() {
        return new ArrayList<>(positions.values());
    }

    /**
     * Gets selected symbols in portfolio.
     */
    public Set<String> getSelectedSymbols() {
        return new HashSet<>(symbols);
    }

    /**
     * Gets symbols that currently have positions.
     */
    public List<String> getSymbolsWithPositions() {
        return new ArrayList<>(positions.keySet());
    }

    /**
     * Updates portfolio name and description.
     */
    public void updateDetails(String newName, String newDescription) {
        if (newName != null && !newName.isBlank()) {
            this.name = newName;
        }
        this.description = newDescription;
        this.updatedAt = Instant.now();
    }

    /**
     * Updates strategy configuration.
     */
    public void updateStrategy(StrategyConfig newConfig) {
        this.strategyConfig = Objects.requireNonNull(newConfig, "Strategy config cannot be null");
        this.updatedAt = Instant.now();
    }

    /**
     * Updates leverage settings.
     */
    public void updateLeverage(Leverage newLeverage) {
        this.leverage = Objects.requireNonNull(newLeverage, "Leverage cannot be null");
        this.updatedAt = Instant.now();
    }

    /**
     * Activates the portfolio from DRAFT status.
     */
    public void activate() {
        if (this.status != PortfolioStatus.DRAFT) {
            throw new IllegalStateException("Only draft portfolios can be activated");
        }
        this.status = PortfolioStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    /**
     * Pauses the portfolio (stops trading).
     */
    public void pause() {
        if (this.status == PortfolioStatus.CLOSED) {
            throw new IllegalStateException("Cannot pause a closed portfolio");
        }
        this.status = PortfolioStatus.PAUSED;
        this.updatedAt = Instant.now();
    }

    /**
     * Resumes the portfolio.
     */
    public void resume() {
        if (this.status == PortfolioStatus.CLOSED) {
            throw new IllegalStateException("Cannot resume a closed portfolio");
        }
        this.status = PortfolioStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    /**
     * Closes the portfolio.
     */
    public void close() {
        if (!positions.isEmpty()) {
            throw new IllegalStateException("Cannot close portfolio with open positions. Liquidate all positions first.");
        }
        this.status = PortfolioStatus.CLOSED;
        this.updatedAt = Instant.now();
    }

    /**
     * Marks portfolio as being rebalanced.
     */
    public void startRebalancing() {
        ensureActive();
        this.status = PortfolioStatus.REBALANCING;
        this.updatedAt = Instant.now();
    }

    /**
     * Marks rebalancing as complete.
     */
    public void completeRebalancing() {
        if (this.status != PortfolioStatus.REBALANCING) {
            throw new IllegalStateException("Portfolio is not being rebalanced");
        }
        this.status = PortfolioStatus.ACTIVE;
        this.lastRebalancedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Gets recent trades (last N trades).
     */
    public List<Trade> getRecentTrades(int limit) {
        return tradeHistory.stream()
            .sorted(Comparator.comparing(Trade::getExecutedAt).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Ensures portfolio is active before operations.
     */
    private void ensureActive() {
        if (!status.canTrade()) {
            throw new IllegalStateException(
                String.format("Portfolio is %s and cannot perform trading operations", status)
            );
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Portfolio portfolio = (Portfolio) obj;
        return Objects.equals(id, portfolio.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Portfolio[id=%d, name=%s, instruments=%d, value=%s, status=%s]",
            id, name, positions.size(), getTotalValue(), status);
    }
}
