package com.ahd.trading_platform.portfolio.infrastructure.persistence.entities;

import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA entity for portfolio strategy configurations.
 * Stores the strategies applied to portfolios with their parameter values.
 */
@Entity
@Table(name = "portfolio_strategy_configurations", indexes = {
    @Index(name = "idx_config_portfolio_id", columnList = "portfolio_id"),
    @Index(name = "idx_config_strategy_code", columnList = "strategy_code"),
    @Index(name = "idx_config_parent_id", columnList = "parent_configuration_id"),
    @Index(name = "idx_config_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioStrategyConfigurationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;

    @Column(name = "strategy_code", nullable = false, length = 50)
    private String strategyCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StrategyCategory category;

    @Column(name = "parent_configuration_id")
    private Long parentConfigurationId;

    @Column(nullable = false)
    private Boolean active = true;

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
