package com.ahd.trading_platform.portfolio.domain.entities;

import com.ahd.trading_platform.portfolio.domain.valueobjects.TradeStatus;
import com.ahd.trading_platform.portfolio.domain.valueobjects.TradeType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Trade entity representing a single trade execution.
 * Part of Portfolio aggregate but has its own identity.
 */
@Getter
public class Trade {
    private final Long id;
    private final String symbol;  // Trading symbol (e.g., BTCUSDT, ETHUSDT)
    private final TradeType tradeType;
    private final BigDecimal quantity;
    private final BigDecimal price;
    private final BigDecimal feeAmount;
    private final String feeCurrency;
    private final Instant executedAt;
    private TradeStatus status;
    private String executionReference;  // External execution reference (e.g., from exchange)
    private String failureReason;

    // Constructor for creating new trades
    public Trade(
        String symbol,
        TradeType tradeType,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal feeAmount,
        String feeCurrency
    ) {
        this(null, symbol, tradeType, quantity, price, feeAmount, feeCurrency,
            Instant.now(), TradeStatus.PENDING, null, null);
    }

    // Full constructor (for reconstitution from database)
    public Trade(
        Long id,
        String symbol,
        TradeType tradeType,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal feeAmount,
        String feeCurrency,
        Instant executedAt,
        TradeStatus status,
        String executionReference,
        String failureReason
    ) {
        this.id = id;
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");
        this.tradeType = Objects.requireNonNull(tradeType, "Trade type cannot be null");
        this.quantity = Objects.requireNonNull(quantity, "Quantity cannot be null");
        this.price = Objects.requireNonNull(price, "Price cannot be null");
        this.feeAmount = feeAmount != null ? feeAmount : BigDecimal.ZERO;
        this.feeCurrency = feeCurrency != null ? feeCurrency : "USD";
        this.executedAt = executedAt != null ? executedAt : Instant.now();
        this.status = status != null ? status : TradeStatus.PENDING;
        this.executionReference = executionReference;
        this.failureReason = failureReason;

        validateTrade();
    }

    /**
     * Factory method for buy trade.
     */
    public static Trade buy(String symbol, BigDecimal quantity, BigDecimal price) {
        return new Trade(symbol, TradeType.BUY, quantity, price, BigDecimal.ZERO, "USD");
    }

    /**
     * Factory method for sell trade.
     */
    public static Trade sell(String symbol, BigDecimal quantity, BigDecimal price) {
        return new Trade(symbol, TradeType.SELL, quantity, price, BigDecimal.ZERO, "USD");
    }

    /**
     * Factory method for reconstituting trade from persistence.
     */
    public static Trade reconstitute(
        Long id,
        TradeType tradeType,
        String symbol,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal feeAmount,
        String feeCurrency,
        TradeStatus status,
        String executionReference,
        String failureReason,
        Instant executedAt
    ) {
        return new Trade(
            id, symbol, tradeType, quantity, price, feeAmount, feeCurrency,
            executedAt, status, executionReference, failureReason
        );
    }

    /**
     * Marks trade as executed successfully.
     */
    public void markExecuted(String executionReference) {
        if (this.status == TradeStatus.EXECUTED) {
            throw new IllegalStateException("Trade is already executed");
        }
        if (this.status == TradeStatus.CANCELLED) {
            throw new IllegalStateException("Cannot execute a cancelled trade");
        }

        this.status = TradeStatus.EXECUTED;
        this.executionReference = executionReference;
        this.failureReason = null;
    }

    /**
     * Marks trade as failed.
     */
    public void markFailed(String reason) {
        if (this.status == TradeStatus.EXECUTED) {
            throw new IllegalStateException("Cannot fail an already executed trade");
        }
        if (this.status == TradeStatus.CANCELLED) {
            throw new IllegalStateException("Cannot fail a cancelled trade");
        }

        this.status = TradeStatus.FAILED;
        this.failureReason = reason;
    }

    /**
     * Cancels the trade.
     */
    public void cancel() {
        if (this.status == TradeStatus.EXECUTED) {
            throw new IllegalStateException("Cannot cancel an executed trade");
        }

        this.status = TradeStatus.CANCELLED;
    }

    /**
     * Calculates total trade value (quantity * price).
     */
    public BigDecimal getTotalValue() {
        return quantity.multiply(price);
    }

    /**
     * Calculates total cost including fees.
     */
    public BigDecimal getTotalCost() {
        return getTotalValue().add(feeAmount);
    }

    /**
     * Calculates net proceeds (for sell trades, after fees).
     */
    public BigDecimal getNetProceeds() {
        return getTotalValue().subtract(feeAmount);
    }

    /**
     * Checks if trade is a buy.
     */
    public boolean isBuy() {
        return tradeType == TradeType.BUY;
    }

    /**
     * Checks if trade is a sell.
     */
    public boolean isSell() {
        return tradeType == TradeType.SELL;
    }

    /**
     * Checks if trade is completed (executed, failed, or cancelled).
     */
    public boolean isCompleted() {
        return status.isCompleted();
    }

    /**
     * Validates trade data.
     */
    private void validateTrade() {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        if (feeAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fee amount cannot be negative");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Trade trade = (Trade) obj;
        return Objects.equals(id, trade.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Trade[id=%d, %s %s %s @ %s, status=%s]",
            id, tradeType, quantity, symbol, price, status);
    }
}
