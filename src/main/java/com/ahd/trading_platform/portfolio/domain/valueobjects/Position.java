package com.ahd.trading_platform.portfolio.domain.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Position value object representing holdings in a specific trading symbol.
 * Immutable - any changes create a new Position instance.
 */
public record Position(
    String symbol,                      // Trading symbol (e.g., BTCUSDT, ETHUSDT)
    BigDecimal quantity,                // Quantity held
    BigDecimal averageEntryPrice,       // Average price paid
    BigDecimal currentMarketPrice,      // Current market price (optional, can be null)
    Instant lastUpdated                 // Last update timestamp
) {
    /**
     * Factory method for opening a new position.
     */
    public static Position open(String symbol, BigDecimal quantity, BigDecimal entryPrice) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (entryPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Entry price must be positive");
        }

        return new Position(
            symbol,
            quantity,
            entryPrice,
            entryPrice,  // Initial market price = entry price
            Instant.now()
        );
    }

    /**
     * Increases position by buying more (updates average entry price).
     */
    public Position increase(BigDecimal additionalQuantity, BigDecimal purchasePrice) {
        if (additionalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Additional quantity must be positive");
        }

        BigDecimal totalCost = quantity.multiply(averageEntryPrice)
            .add(additionalQuantity.multiply(purchasePrice));

        BigDecimal newQuantity = quantity.add(additionalQuantity);

        BigDecimal newAveragePrice = totalCost.divide(newQuantity, 8, RoundingMode.HALF_UP);

        return new Position(
            symbol,
            newQuantity,
            newAveragePrice,
            purchasePrice,  // Update current price
            Instant.now()
        );
    }

    /**
     * Decreases position by selling (average entry price stays same).
     */
    public Position decrease(BigDecimal soldQuantity) {
        if (soldQuantity.compareTo(quantity) > 0) {
            throw new IllegalArgumentException(
                String.format("Cannot sell more than held. Held: %s, Selling: %s",
                    quantity, soldQuantity)
            );
        }

        BigDecimal newQuantity = quantity.subtract(soldQuantity);

        return new Position(
            symbol,
            newQuantity,
            averageEntryPrice,  // Average entry price remains same
            currentMarketPrice,
            Instant.now()
        );
    }

    /**
     * Updates current market price (for P&L calculation).
     */
    public Position updateMarketPrice(BigDecimal newMarketPrice) {
        return new Position(
            symbol,
            quantity,
            averageEntryPrice,
            newMarketPrice,
            Instant.now()
        );
    }

    /**
     * Calculates current market value.
     */
    public BigDecimal getCurrentValue() {
        if (currentMarketPrice == null) {
            return quantity.multiply(averageEntryPrice);
        }
        return quantity.multiply(currentMarketPrice);
    }

    /**
     * Calculates cost basis (total amount invested).
     */
    public BigDecimal getCostBasis() {
        return quantity.multiply(averageEntryPrice);
    }

    /**
     * Calculates unrealized profit/loss.
     */
    public BigDecimal getUnrealizedPnL() {
        return getCurrentValue().subtract(getCostBasis());
    }

    /**
     * Calculates unrealized P&L percentage.
     */
    public BigDecimal getUnrealizedPnLPercentage() {
        BigDecimal costBasis = getCostBasis();
        if (costBasis.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getUnrealizedPnL()
            .divide(costBasis, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }

    /**
     * Checks if position is empty (quantity is zero).
     */
    public boolean isEmpty() {
        return quantity.compareTo(BigDecimal.ZERO) == 0;
    }
}
