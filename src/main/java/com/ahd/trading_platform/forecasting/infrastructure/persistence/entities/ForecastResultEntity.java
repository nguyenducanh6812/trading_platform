package com.ahd.trading_platform.forecasting.infrastructure.persistence.entities;

import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity for storing ARIMA forecast results in the database.
 * Stores predicted expected returns with model version tracking for reuse.
 */
@Entity
@Table(name = "forecast_results", indexes = {
    @Index(name = "idx_forecast_instrument_date_model", columnList = "instrument, forecast_date, arima_model_version"),
    @Index(name = "idx_forecast_date_model", columnList = "forecast_date, arima_model_version"),
    @Index(name = "idx_forecast_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ForecastResultEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Trading instrument (BTC, ETH)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "instrument", nullable = false, length = 10)
    private TradingInstrument instrument;
    
    /**
     * Date for which the forecast was made (prediction date)
     */
    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;
    
    /**
     * Predicted expected return value
     */
    @Column(name = "expected_return", nullable = false, precision = 18, scale = 8)
    private BigDecimal expectedReturn;
    
    /**
     * Confidence level of the prediction (0.0 to 1.0)
     */
    @Column(name = "confidence_level", nullable = false, precision = 5, scale = 4)
    private BigDecimal confidenceLevel;
    
    /**
     * ARIMA model version used (e.g., "20250904")
     * Identifies which master data file was used
     */
    @Column(name = "arima_model_version", nullable = false, length = 20)
    private String arimaModelVersion;
    
    /**
     * Execution ID from the Camunda process
     */
    @Column(name = "execution_id", length = 100)
    private String executionId;
    
    /**
     * Whether this was current date prediction or backtesting
     */
    @Column(name = "is_current_date_prediction")
    private Boolean isCurrentDatePrediction;
    
    /**
     * Additional metadata (JSON format)
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}