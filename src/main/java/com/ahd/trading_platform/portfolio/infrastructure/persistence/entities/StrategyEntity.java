package com.ahd.trading_platform.portfolio.infrastructure.persistence.entities;

import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for Strategy persistence.
 * Stores the master list of available trading strategies.
 */
@Entity
@Table(name = "strategies", indexes = {
    @Index(name = "idx_strategy_code", columnList = "code", unique = true),
    @Index(name = "idx_strategy_category", columnList = "category"),
    @Index(name = "idx_strategy_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StrategyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StrategyCategory category;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "strategy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<StrategyParameterEntity> parameters = new ArrayList<>();

    @OneToMany(mappedBy = "strategy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<StrategyDependencyEntity> dependencies = new ArrayList<>();

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

    /**
     * Helper method to add parameter
     */
    public void addParameter(StrategyParameterEntity parameter) {
        parameters.add(parameter);
        parameter.setStrategy(this);
    }

    /**
     * Helper method to add dependency
     */
    public void addDependency(StrategyDependencyEntity dependency) {
        dependencies.add(dependency);
        dependency.setStrategy(this);
    }
}
