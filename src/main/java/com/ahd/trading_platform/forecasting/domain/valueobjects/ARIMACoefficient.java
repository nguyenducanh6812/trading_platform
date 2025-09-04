package com.ahd.trading_platform.forecasting.domain.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value object representing an ARIMA model coefficient.
 * Provides type-safe coefficient handling with proper financial precision.
 */
public record ARIMACoefficient(
    String lagName,
    BigDecimal value
) {
    
    public ARIMACoefficient {
        if (lagName == null || lagName.trim().isEmpty()) {
            throw new IllegalArgumentException("Lag name cannot be null or empty");
        }
        if (value == null) {
            throw new IllegalArgumentException("Coefficient value cannot be null");
        }
        
        // Ensure proper precision for financial calculations
        value = value.setScale(16, RoundingMode.HALF_UP);
    }
    
    /**
     * Creates a coefficient from double value with proper precision
     */
    public static ARIMACoefficient of(String lagName, double value) {
        return new ARIMACoefficient(lagName, BigDecimal.valueOf(value));
    }
    
    /**
     * Gets the lag number from lag name (e.g., "ar.L1" -> 1)
     */
    public int getLagNumber() {
        if (!lagName.startsWith("ar.L")) {
            throw new IllegalArgumentException("Invalid lag name format: " + lagName);
        }
        
        try {
            return Integer.parseInt(lagName.substring(4));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot parse lag number from: " + lagName, e);
        }
    }
    
    /**
     * Returns the coefficient value as double for calculations
     */
    public double doubleValue() {
        return value.doubleValue();
    }
    
    /**
     * Checks if this is a valid AR lag coefficient
     */
    public boolean isARLag() {
        return lagName.startsWith("ar.L");
    }
}