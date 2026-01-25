package com.ahd.trading_platform.portfolio.infrastructure.persistence.entities;

import com.ahd.trading_platform.portfolio.domain.valueobjects.PortfolioStatus;
import com.ahd.trading_platform.portfolio.domain.valueobjects.RebalancingFrequency;
import com.ahd.trading_platform.portfolio.domain.valueobjects.RiskTolerance;
import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JPA entity for Portfolio persistence.
 */
@Entity
@Table(name = "portfolios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "user_id", nullable = false)
    private String userId;

    // Selected instruments for trading
    // Using OneToMany relationship with PortfolioInstrumentEntity for full audit support
    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<PortfolioInstrumentEntity> portfolioInstruments = new HashSet<>();

    // Capital fields
    @Column(name = "initial_capital", nullable = false, precision = 20, scale = 8)
    private BigDecimal initialCapital;

    @Column(name = "current_capital", nullable = false, precision = 20, scale = 8)
    private BigDecimal currentCapital;

    @Column(name = "available_capital", nullable = false, precision = 20, scale = 8)
    private BigDecimal availableCapital;

    @Column(name = "reserved_capital", nullable = false, precision = 20, scale = 8)
    private BigDecimal reservedCapital;

    @Column(nullable = false, length = 3)
    private String currency;

    // Strategy configuration
    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_type", nullable = false)
    private StrategyType strategyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_tolerance", nullable = false)
    private RiskTolerance riskTolerance;

    @Enumerated(EnumType.STRING)
    @Column(name = "rebalancing_frequency", nullable = false)
    private RebalancingFrequency rebalancingFrequency;

    @Column(name = "auto_rebalance", nullable = false)
    private Boolean autoRebalance;

    @Column(name = "target_risk_free_rate", nullable = false, precision = 10, scale = 6)
    private BigDecimal targetRiskFreeRate;

    // Leverage configuration
    @Column(name = "leverage_enabled", nullable = false)
    private Boolean leverageEnabled;

    @Column(name = "leverage_ratio", nullable = false)
    private Integer leverageRatio;

    @Column(name = "max_leverage_allowed", nullable = false)
    private Integer maxLeverageAllowed;

    // Status and timestamps
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PortfolioStatus status;

    @Column(name = "last_rebalanced_at")
    private Instant lastRebalancedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Relationships
    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PositionEntity> positions = new ArrayList<>();

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TradeEntity> trades = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void addPosition(PositionEntity position) {
        positions.add(position);
        position.setPortfolio(this);
    }

    public void addTrade(TradeEntity trade) {
        trades.add(trade);
        trade.setPortfolio(this);
    }

    /**
     * Add an instrument to this portfolio
     */
    public void addInstrument(String instrumentCode) {
        PortfolioInstrumentEntity portfolioInstrument = new PortfolioInstrumentEntity();
        portfolioInstrument.setInstrumentCode(instrumentCode);
        portfolioInstrument.setPortfolio(this);
        portfolioInstruments.add(portfolioInstrument);
    }

    /**
     * Remove an instrument from this portfolio
     */
    public void removeInstrument(String instrumentCode) {
        portfolioInstruments.removeIf(pi -> pi.getInstrumentCode().equals(instrumentCode));
    }

    /**
     * Get all instrument codes for this portfolio
     */
    public Set<String> getInstrumentCodes() {
        return portfolioInstruments.stream()
                .map(PortfolioInstrumentEntity::getInstrumentCode)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Set instrument codes (replaces existing instruments)
     * Properly handles updates by adding new instruments and removing old ones
     */
    public void setInstrumentCodes(Set<String> instrumentCodes) {
        if (instrumentCodes == null) {
            instrumentCodes = new HashSet<>();
        }

        // Find instruments to remove (existing but not in new set)
        Set<String> existingCodes = getInstrumentCodes();
        Set<String> toRemove = new HashSet<>(existingCodes);
        toRemove.removeAll(instrumentCodes);

        // Find instruments to add (in new set but not existing)
        Set<String> toAdd = new HashSet<>(instrumentCodes);
        toAdd.removeAll(existingCodes);

        // Remove old instruments
        toRemove.forEach(this::removeInstrument);

        // Add new instruments
        toAdd.forEach(this::addInstrument);
    }
}
