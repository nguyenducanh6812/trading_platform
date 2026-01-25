package com.ahd.trading_platform.portfolio.domain.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Capital management value object for portfolio.
 * Tracks initial investment, current value, and allocation.
 */
public record Capital(
    BigDecimal initialAmount,     // Starting capital
    BigDecimal currentAmount,      // Current total value (available + reserved)
    BigDecimal availableAmount,    // Available for new trades
    BigDecimal reservedAmount,     // Locked in open positions
    String currency                // Currency code (USD, USDT, etc.)
) {
    /**
     * Factory method for initial capital creation.
     */
    public static Capital initial(BigDecimal amount, String currency) {
        return new Capital(
            amount,
            amount,
            amount,
            BigDecimal.ZERO,
            currency
        );
    }

    /**
     * Reserves capital for a trade (reduces available).
     */
    public Capital reserve(BigDecimal amount) {
        if (amount.compareTo(availableAmount) > 0) {
            throw new IllegalArgumentException(
                String.format("Insufficient available capital. Requested: %s, Available: %s",
                    amount, availableAmount)
            );
        }

        return new Capital(
            initialAmount,
            currentAmount,
            availableAmount.subtract(amount),
            reservedAmount.add(amount),
            currency
        );
    }

    /**
     * Releases reserved capital (increases available).
     */
    public Capital release(BigDecimal amount) {
        if (amount.compareTo(reservedAmount) > 0) {
            throw new IllegalArgumentException(
                String.format("Cannot release more than reserved. Requested: %s, Reserved: %s",
                    amount, reservedAmount)
            );
        }

        return new Capital(
            initialAmount,
            currentAmount,
            availableAmount.add(amount),
            reservedAmount.subtract(amount),
            currency
        );
    }

    /**
     * Updates current capital value (from position changes).
     */
    public Capital updateCurrentValue(BigDecimal newValue) {
        return new Capital(
            initialAmount,
            newValue,
            newValue.subtract(reservedAmount),
            reservedAmount,
            currency
        );
    }

    /**
     * Calculates total profit/loss.
     */
    public BigDecimal getTotalProfitLoss() {
        return currentAmount.subtract(initialAmount);
    }

    /**
     * Calculates profit/loss percentage.
     */
    public BigDecimal getProfitLossPercentage() {
        if (initialAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getTotalProfitLoss()
            .divide(initialAmount, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }

    /**
     * Calculates utilization rate (reserved/current).
     */
    public BigDecimal getUtilizationRate() {
        if (currentAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return reservedAmount
            .divide(currentAmount, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }
}
