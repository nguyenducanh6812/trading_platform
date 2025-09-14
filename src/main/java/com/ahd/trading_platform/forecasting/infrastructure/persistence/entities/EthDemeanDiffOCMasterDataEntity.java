package com.ahd.trading_platform.forecasting.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity for storing ETH-specific Demean_Diff_OC master data.
 * Follows the same pattern as EthPriceEntity for instrument-specific table separation.
 */
@Entity
@Table(name = "eth_demean_diff_oc_master_data", indexes = {
    @Index(name = "idx_eth_demean_timestamp", columnList = "timestamp"),
    @Index(name = "idx_eth_demean_date_range", columnList = "timestamp")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EthDemeanDiffOCMasterDataEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Timestamp of the trading data point
     */
    @Column(nullable = false, unique = true)
    private Instant timestamp;
    
    /**
     * Open price at the timestamp
     */
    @Column(name = "open_price", nullable = false, precision = 19, scale = 8)
    private BigDecimal openPrice;
    
    /**
     * Close price at the timestamp
     */
    @Column(name = "close_price", nullable = false, precision = 19, scale = 8)
    private BigDecimal closePrice;
    
    /**
     * Open - Close for this day (OC = Open - Close)
     * Used as previous OC for calculating next day's DiffOC
     */
    @Column(name = "oc", nullable = false, precision = 19, scale = 8)
    private BigDecimal oc;
    
    /**
     * Difference between consecutive days' OC values (DiffOC = OC(today) - OC(yesterday))
     */
    @Column(name = "diff_oc", nullable = true, precision = 19, scale = 8)
    private BigDecimal diffOC;
    
    /**
     * Demeaned difference of OC (removing the mean for stationary analysis)
     */
    @Column(name = "demean_diff_oc", nullable = true, precision = 19, scale = 8)
    private BigDecimal demeanDiffOC;
    
    /**
     * Indicates if this data point has calculated difference values
     */
    @Column(name = "has_differences", nullable = false)
    @Builder.Default
    private Boolean hasDifferences = false;
    
    /**
     * Creation timestamp for audit purposes
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    
    /**
     * Last modification timestamp for audit purposes
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}