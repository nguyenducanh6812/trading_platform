package com.ahd.trading_platform.portfolio.infrastructure.persistence.entities;

import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity for strategy dependencies.
 * Defines what other strategy categories this strategy depends on.
 */
@Entity
@Table(name = "strategy_dependencies", indexes = {
    @Index(name = "idx_dep_strategy_id", columnList = "strategy_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StrategyDependencyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id", nullable = false)
    private StrategyEntity strategy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StrategyCategory category;

    @Column(nullable = false)
    private Boolean required = false;

    @Column(length = 500)
    private String description;
}
