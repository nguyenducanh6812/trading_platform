package com.ahd.trading_platform.shared.valueobjects;

import org.springframework.modulith.NamedInterface;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Price value object representing monetary price with proper precision handling.
 * Immutable and handles financial calculations with proper rounding.
 */
@NamedInterface
public record Price(BigDecimal amount, String currency) {
    
    private static final int SCALE = 8; // Crypto precision
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    
    public Price {
        Objects.requireNonNull(amount, "Price amount cannot be null");
        Objects.requireNonNull(currency, "Price currency cannot be null");
        
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price amount cannot be negative");
        }
        
        // Ensure proper scale for financial calculations
        amount = amount.setScale(SCALE, ROUNDING_MODE);
    }
    
    public static Price of(double amount, String currency) {
        return new Price(BigDecimal.valueOf(amount), currency);
    }
    
    public static Price of(String amount, String currency) {
        return new Price(new BigDecimal(amount), currency);
    }
    
    public static Price usd(double amount) {
        return of(amount, "USD");
    }
    
    public static Price usd(String amount) {
        return of(amount, "USD");
    }
    
    public Price add(Price other) {
        validateSameCurrency(other);
        return new Price(this.amount.add(other.amount), this.currency);
    }
    
    public Price subtract(Price other) {
        validateSameCurrency(other);
        return new Price(this.amount.subtract(other.amount), this.currency);
    }
    
    public Price multiply(BigDecimal multiplier) {
        Objects.requireNonNull(multiplier, "Multiplier cannot be null");
        return new Price(this.amount.multiply(multiplier), this.currency);
    }
    
    public Price divide(BigDecimal divisor) {
        Objects.requireNonNull(divisor, "Divisor cannot be null");
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        return new Price(this.amount.divide(divisor, SCALE, ROUNDING_MODE), this.currency);
    }
    
    public boolean isGreaterThan(Price other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }
    
    public boolean isLessThan(Price other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }
    
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    private void validateSameCurrency(Price other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Cannot operate on different currencies: %s vs %s", 
                    this.currency, other.currency));
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s %s", amount.toPlainString(), currency);
    }
}