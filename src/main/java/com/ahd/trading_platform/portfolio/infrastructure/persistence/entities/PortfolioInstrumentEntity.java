package com.ahd.trading_platform.portfolio.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity for portfolio_instruments junction table.
 * Represents the many-to-many relationship between portfolios and instruments.
 *
 * This entity includes full audit support using Spring Data JPA auditing annotations.
 */
@Entity
@Table(
    name = "portfolio_instruments",
    schema = "trading_platform"
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(PortfolioInstrumentEntity.PortfolioInstrumentId.class)
public class PortfolioInstrumentEntity {

    @Id
    @Column(name = "portfolio_id", nullable = false, insertable = false, updatable = false)
    private Long portfolioId;

    @Id
    @Column(name = "instrument_code", nullable = false, length = 30)
    private String instrumentCode;

    /**
     * Many-to-one relationship to Portfolio
     * @MapsId tells JPA that this relationship manages the portfolioId field
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    @MapsId("portfolioId")
    private PortfolioEntity portfolio;

    /**
     * Audit field: When was this relationship created
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Audit field: When was this relationship last modified
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Audit field: Who created this relationship
     * Will be automatically populated by Spring Security context or custom auditor
     */
    @CreatedBy
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /**
     * Audit field: Who last modified this relationship
     * Will be automatically populated by Spring Security context or custom auditor
     */
    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortfolioInstrumentEntity that = (PortfolioInstrumentEntity) o;
        return Objects.equals(portfolioId, that.portfolioId) &&
               Objects.equals(instrumentCode, that.instrumentCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, instrumentCode);
    }

    @Override
    public String toString() {
        return String.format("PortfolioInstrument[portfolioId=%d, instrumentCode=%s, createdAt=%s]",
                portfolioId, instrumentCode, createdAt);
    }

    /**
     * Composite primary key class for PortfolioInstrumentEntity
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortfolioInstrumentId implements Serializable {
        private Long portfolioId;
        private String instrumentCode;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PortfolioInstrumentId that = (PortfolioInstrumentId) o;
            return Objects.equals(portfolioId, that.portfolioId) &&
                   Objects.equals(instrumentCode, that.instrumentCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(portfolioId, instrumentCode);
        }
    }
}
