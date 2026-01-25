package com.ahd.trading_platform.forecasting.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "inverse_demean_diff_oc_master_data",
       schema = "trading_platform",
       uniqueConstraints = @UniqueConstraint(name = "uk_inverse_demean_symbol_timestamp", columnNames = {"symbol", "timestamp"}),
       indexes = {
           @Index(name = "idx_inverse_demean_symbol", columnList = "symbol"),
           @Index(name = "idx_inverse_demean_timestamp", columnList = "timestamp"),
           @Index(name = "idx_inverse_demean_symbol_timestamp", columnList = "symbol, timestamp")
       })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InverseDemeanDiffOCMasterDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 30)
    private String symbol;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "open_price", nullable = false, precision = 27, scale = 16)
    private BigDecimal openPrice;

    @Column(name = "close_price", nullable = false, precision = 27, scale = 16)
    private BigDecimal closePrice;

    @Column(name = "oc", nullable = false, precision = 27, scale = 16)
    private BigDecimal oc;

    @Column(name = "diff_oc", nullable = true, precision = 27, scale = 16)
    private BigDecimal diffOC;

    @Column(name = "demean_diff_oc", nullable = true, precision = 27, scale = 16)
    private BigDecimal demeanDiffOC;

    @Column(name = "has_differences", nullable = false)
    @Builder.Default
    private Boolean hasDifferences = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
