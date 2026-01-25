package com.ahd.trading_platform.portfolio.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity for Position persistence.
 */
@Entity
@Table(name = "positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PositionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private PortfolioEntity portfolio;

    @Column(name = "instrument_code", nullable = false, length = 30)
    private String instrumentCode;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(name = "average_entry_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal averageEntryPrice;

    @Column(name = "current_market_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal currentMarketPrice;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        lastUpdated = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        lastUpdated = Instant.now();
    }
}
