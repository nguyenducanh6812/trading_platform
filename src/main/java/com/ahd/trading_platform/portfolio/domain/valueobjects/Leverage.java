package com.ahd.trading_platform.portfolio.domain.valueobjects;

import java.math.BigDecimal;

/**
 * Leverage configuration for portfolio trading.
 * Defines leverage ratio and constraints.
 */
public record Leverage(
    boolean enabled,           // Is leverage enabled
    int leverageRatio,         // Leverage multiplier (1x, 2x, 5x, etc.)
    int maxLeverageAllowed     // Maximum leverage allowed
) {
    /**
     * No leverage (1x).
     */
    public static Leverage none() {
        return new Leverage(false, 1, 1);
    }

    /**
     * Factory method with leverage ratio.
     */
    public static Leverage of(int ratio) {
        if (ratio < 1) {
            throw new IllegalArgumentException("Leverage ratio must be at least 1");
        }
        if (ratio > 125) {
            throw new IllegalArgumentException("Leverage ratio cannot exceed 125x");
        }

        return new Leverage(ratio > 1, ratio, ratio);
    }

    /**
     * Factory method with max leverage constraint.
     */
    public static Leverage withMax(int ratio, int maxAllowed) {
        if (ratio > maxAllowed) {
            throw new IllegalArgumentException(
                String.format("Leverage ratio %d exceeds maximum allowed %d", ratio, maxAllowed)
            );
        }

        return new Leverage(ratio > 1, ratio, maxAllowed);
    }

    /**
     * Calculates buying power (capital * leverage).
     */
    public BigDecimal calculateBuyingPower(BigDecimal capital) {
        return capital.multiply(BigDecimal.valueOf(leverageRatio));
    }

    /**
     * Calculates margin requirement.
     */
    public BigDecimal calculateMarginRequirement(BigDecimal positionValue) {
        if (leverageRatio <= 1) {
            return positionValue;
        }
        return positionValue.divide(BigDecimal.valueOf(leverageRatio), BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Updates leverage ratio.
     */
    public Leverage withRatio(int newRatio) {
        return Leverage.withMax(newRatio, maxLeverageAllowed);
    }

    /**
     * Enables leverage.
     */
    public Leverage enable() {
        return new Leverage(true, leverageRatio, maxLeverageAllowed);
    }

    /**
     * Disables leverage (sets to 1x).
     */
    public Leverage disable() {
        return new Leverage(false, 1, maxLeverageAllowed);
    }

    /**
     * Validation.
     */
    public Leverage {
        if (leverageRatio < 1) {
            throw new IllegalArgumentException("Leverage ratio must be at least 1");
        }
        if (leverageRatio > maxLeverageAllowed) {
            throw new IllegalArgumentException(
                String.format("Leverage ratio %d exceeds maximum %d", leverageRatio, maxLeverageAllowed)
            );
        }
    }
}
