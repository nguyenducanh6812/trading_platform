package com.ahd.trading_platform.marketdata.infrastructure.persistence.entities;

import jakarta.persistence.*;
import com.ahd.trading_platform.shared.valueobjects.*;
import com.ahd.trading_platform.marketdata.domain.valueobjects.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Objects;

/**
 * JPA Entity for MarketInstrument persistence.
 * Maps to the market_instruments table.
 */
@Entity
@Table(
    name = "market_instruments",
    uniqueConstraints = @UniqueConstraint(columnNames = "symbol"),
    indexes = {
        @Index(name = "idx_market_instrument_symbol", columnList = "symbol"),
        @Index(name = "idx_market_instrument_base_currency", columnList = "base_currency"),
        @Index(name = "idx_market_instrument_quote_currency", columnList = "quote_currency")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketInstrumentEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "symbol", nullable = false, unique = true, length = 10)
    private String symbol;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "base_currency", nullable = false, length = 10)
    private String baseCurrency;
    
    @Column(name = "quote_currency", nullable = false, length = 10) 
    private String quoteCurrency;
    
    @Column(name = "data_point_count", nullable = false)
    private Integer dataPointCount = 0;
    
    @Column(name = "quality_score")
    private Double qualityScore;
    
    @Column(name = "quality_level", length = 20)
    private String qualityLevel;
    
    @Column(name = "data_source", length = 50)
    private String dataSource;
    
    @Embedded
    private AuditInfo auditInfo = new AuditInfo();
    
    
    public MarketInstrumentEntity(String symbol, String name, String baseCurrency, String quoteCurrency) {
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.baseCurrency = Objects.requireNonNull(baseCurrency, "Base currency cannot be null");
        this.quoteCurrency = Objects.requireNonNull(quoteCurrency, "Quote currency cannot be null");
    }
    
    /**
     * Custom equals based on business key (symbol).
     * Lombok @Data provides default equals, but we override for business logic.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MarketInstrumentEntity that = (MarketInstrumentEntity) obj;
        return Objects.equals(symbol, that.symbol);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }
}