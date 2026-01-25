package com.ahd.trading_platform.portfolio.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity for strategy parameters.
 * Defines the parameter schema for a strategy.
 */
@Entity
@Table(name = "strategy_parameters", indexes = {
    @Index(name = "idx_param_strategy_id", columnList = "strategy_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StrategyParameterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id", nullable = false)
    private StrategyEntity strategy;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType;

    @Column(name = "default_value", length = 100)
    private String defaultValue;

    @Column(nullable = false)
    private Boolean required = false;

    @Column(length = 500)
    private String description;

    @Column(name = "validation_rule", length = 200)
    private String validationRule;
}
