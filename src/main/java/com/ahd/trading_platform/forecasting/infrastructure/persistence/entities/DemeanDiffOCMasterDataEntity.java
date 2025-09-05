package com.ahd.trading_platform.forecasting.infrastructure.persistence.entities;

import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity for storing Demean_Diff_OC master data that can be reused across different forecasting models.
 * This separates data preparation from model application, enabling:
 * - Reusability across ARIMA, LSTM, Prophet, etc.
 * - Performance optimization through caching
 * - Independent testing of data preparation vs model logic
 */
@Entity
@Table(name = "demean_diff_oc_master_data", indexes = {
    @Index(name = "idx_demean_instrument_timestamp", columnList = "instrument, timestamp"),
    @Index(name = "idx_demean_instrument_date_range", columnList = "instrument, timestamp")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemeanDiffOCMasterDataEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Trading instrument (BTC, ETH)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradingInstrument instrument;
    
    /**
     * Timestamp for this data point
     */
    @Column(nullable = false)
    private Instant timestamp;
    
    /**
     * Open price for this timestamp
     */
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal openPrice;
    
    /**
     * Close price for this timestamp  
     */
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal closePrice;
    
    /**
     * OC = Open - Close
     */
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal oc;
    
    /**
     * Diff_OC = OC(t) - OC(t-1)
     * Null for the first data point in a series
     */
    @Column(precision = 19, scale = 8)
    private BigDecimal diffOC;
    
    /**
     * Demean_Diff_OC = Diff_OC - Mean(Diff_OC)
     * Null for the first data point in a series
     */
    @Column(precision = 19, scale = 8)
    private BigDecimal demeanDiffOC;
    
    /**
     * Mean of Diff_OC for the dataset this point belongs to.
     * This is needed for demeaning calculations.
     */
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal meanDiffOC;
    
    /**
     * Version identifier for the calculation algorithm.
     * Allows for algorithm updates while maintaining data lineage.
     */
    @Column(nullable = false)
    private String calculationVersion;
    
    /**
     * When this master data was calculated
     */
    @CreatedDate
    @Column(nullable = false)
    private Instant calculatedAt;
    
    /**
     * When this master data was last updated
     */
    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
    
    /**
     * Unique constraint to prevent duplicate data points
     */
    @Column(nullable = false, unique = true)
    private String uniqueKey;
    
    /**
     * Generates unique key for this data point
     */
    @PrePersist
    @PreUpdate
    private void generateUniqueKey() {
        this.uniqueKey = String.format("%s_%s_%s", 
            instrument.name(), 
            timestamp.toEpochMilli(),
            calculationVersion);
    }
}