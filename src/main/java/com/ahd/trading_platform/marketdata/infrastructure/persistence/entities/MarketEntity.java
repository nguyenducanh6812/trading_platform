package com.ahd.trading_platform.marketdata.infrastructure.persistence.entities;

import com.ahd.trading_platform.marketdata.domain.valueobjects.AuditInfo;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Objects;

/**
 * JPA Entity for Market persistence.
 * Maps to the markets table.
 * Represents reference data for trading market categories.
 */
@Entity
@Table(
    name = "markets",
    schema = "trading_platform",
    uniqueConstraints = @UniqueConstraint(columnNames = "code"),
    indexes = {
        @Index(name = "idx_markets_code", columnList = "code")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Embedded
    private AuditInfo auditInfo = new AuditInfo();

    /**
     * Custom equals based on business key (code).
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MarketEntity that = (MarketEntity) obj;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
