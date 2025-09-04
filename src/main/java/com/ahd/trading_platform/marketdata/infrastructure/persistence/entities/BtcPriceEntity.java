package com.ahd.trading_platform.marketdata.infrastructure.persistence.entities;

import jakarta.persistence.*;
import com.ahd.trading_platform.marketdata.domain.valueobjects.AuditInfo;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA Entity for BTC price data storage.
 * Implements asset-specific storage strategy with dedicated table for BTC data.
 */
@Entity
@Table(
    name = "btc_price_data",
    uniqueConstraints = @UniqueConstraint(columnNames = {"timestamp"}),
    indexes = {
        @Index(name = "idx_btc_timestamp", columnList = "timestamp"),
        @Index(name = "idx_btc_timestamp_desc", columnList = "timestamp DESC"),
        @Index(name = "idx_btc_close_price", columnList = "close_price")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BtcPriceEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "timestamp", nullable = false, unique = true)
    private Instant timestamp;
    
    @Column(name = "open_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal openPrice;
    
    @Column(name = "high_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal highPrice;
    
    @Column(name = "low_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal lowPrice;
    
    @Column(name = "close_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal closePrice;
    
    @Column(name = "volume", nullable = false, precision = 24, scale = 8)
    private BigDecimal volume;
    
    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "USD";
    
    @Embedded
    private AuditInfo auditInfo = new AuditInfo();
    
    // Custom constructor for domain logic (Lombok handles the rest)
    public BtcPriceEntity(
        Instant timestamp, 
        BigDecimal openPrice, 
        BigDecimal highPrice, 
        BigDecimal lowPrice,
        BigDecimal closePrice, 
        BigDecimal volume) {
        
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.openPrice = Objects.requireNonNull(openPrice, "Open price cannot be null");
        this.highPrice = Objects.requireNonNull(highPrice, "High price cannot be null");
        this.lowPrice = Objects.requireNonNull(lowPrice, "Low price cannot be null");
        this.closePrice = Objects.requireNonNull(closePrice, "Close price cannot be null");
        this.volume = Objects.requireNonNull(volume, "Volume cannot be null");
        this.auditInfo = new AuditInfo();
    }
    
    /**
     * Custom equals based on business key (timestamp).
     * Lombok @Data provides default equals, but we override for business logic.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BtcPriceEntity that = (BtcPriceEntity) obj;
        return Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(timestamp);
    }
}