package com.ahd.trading_platform.portfolio.infrastructure.persistence.entities;

import com.ahd.trading_platform.portfolio.domain.valueobjects.TradeStatus;
import com.ahd.trading_platform.portfolio.domain.valueobjects.TradeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity for Trade persistence.
 */
@Entity
@Table(name = "trades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TradeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private PortfolioEntity portfolio;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false)
    private TradeType tradeType;

    @Column(name = "instrument_code", nullable = false, length = 30)
    private String instrumentCode;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(name = "fee_amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal feeAmount;

    @Column(name = "fee_currency", nullable = false, length = 10)
    private String feeCurrency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeStatus status;

    @Column(name = "execution_reference", length = 100)
    private String executionReference;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
