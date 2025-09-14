package com.ahd.trading_platform.forecasting.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity for ETH expected return predictions.
 * Stores ARIMA forecast results with model version tracking.
 */
@Entity
@Table(name = "eth_expected_return_prediction", 
       indexes = {
           @Index(name = "idx_eth_prediction_forecast_date", columnList = "forecast_date"),
           @Index(name = "idx_eth_prediction_model_version", columnList = "model_version"),
           @Index(name = "idx_eth_prediction_execution_id", columnList = "execution_id"),
           @Index(name = "idx_eth_prediction_created", columnList = "created_at")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_eth_prediction_date_model", columnNames = {"forecast_date", "model_version"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EthExpectedReturnPredictionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "execution_id", nullable = false, length = 36)
    private String executionId;
    
    @Column(name = "forecast_date", nullable = false)
    private Instant forecastDate;
    
    @Column(name = "expected_return", nullable = false, precision = 15, scale = 8)
    private BigDecimal expectedReturn;
    
    @Column(name = "confidence_level", nullable = false, precision = 5, scale = 4)
    private BigDecimal confidenceLevel;
    
    @Column(name = "model_version", nullable = false, length = 20)
    private String modelVersion;
    
    @Column(name = "prediction_status", nullable = false, length = 20)
    private String predictionStatus;
    
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;
    
    @Column(name = "data_points_used")
    private Integer dataPointsUsed;
    
    @Column(name = "ar_order")
    private Integer arOrder;
    
    @Column(name = "mean_squared_error", precision = 15, scale = 8)
    private BigDecimal meanSquaredError;
    
    @Column(name = "standard_error", precision = 15, scale = 8)
    private BigDecimal standardError;
    
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;
    
    @Column(name = "data_range_start")
    private Instant dataRangeStart;
    
    @Column(name = "data_range_end")
    private Instant dataRangeEnd;
    
    @Column(name = "has_sufficient_quality")
    private Boolean hasSufficientQuality;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "predict_diff_oc", precision = 15, scale = 8)
    private BigDecimal predictDiffOC;
    
    @Column(name = "predict_oc", precision = 15, scale = 8)
    private BigDecimal predictOC;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    /**
     * Returns the trading instrument code
     */
    public String getInstrumentCode() {
        return "ETH";
    }
}