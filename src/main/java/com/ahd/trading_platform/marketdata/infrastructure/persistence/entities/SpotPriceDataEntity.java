package com.ahd.trading_platform.marketdata.infrastructure.persistence.entities;

import jakarta.persistence.*;
import com.ahd.trading_platform.shared.valueobjects.*;
import com.ahd.trading_platform.marketdata.domain.valueobjects.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA Entity for SPOT market price data storage.
 */
@Entity
@Table(
    name = "spot_price_data",
    schema = "trading_platform",
    uniqueConstraints = @UniqueConstraint(name = "uk_spot_price_symbol_timestamp", columnNames = {"symbol", "timestamp"}),
    indexes = {
        @Index(name = "idx_spot_price_symbol", columnList = "symbol"),
        @Index(name = "idx_spot_price_timestamp", columnList = "timestamp"),
        @Index(name = "idx_spot_price_symbol_timestamp", columnList = "symbol, timestamp")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotPriceDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 30)
    private String symbol;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "open_price", nullable = false, precision = 26, scale = 16)
    private BigDecimal openPrice;

    @Column(name = "high_price", nullable = false, precision = 26, scale = 16)
    private BigDecimal highPrice;

    @Column(name = "low_price", nullable = false, precision = 26, scale = 16)
    private BigDecimal lowPrice;

    @Column(name = "close_price", nullable = false, precision = 26, scale = 16)
    private BigDecimal closePrice;

    @Column(name = "volume", nullable = false, precision = 32, scale = 16)
    private BigDecimal volume;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "USD";

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SpotPriceDataEntity that = (SpotPriceDataEntity) obj;
        return Objects.equals(symbol, that.symbol) && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, timestamp);
    }
}
