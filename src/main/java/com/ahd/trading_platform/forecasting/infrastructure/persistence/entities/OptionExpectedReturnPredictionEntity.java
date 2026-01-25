package com.ahd.trading_platform.forecasting.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "option_expected_return_prediction",
       schema = "trading_platform",
       indexes = {
           @Index(name = "idx_option_prediction_symbol", columnList = "symbol"),
           @Index(name = "idx_option_prediction_forecast_date", columnList = "forecast_date"),
           @Index(name = "idx_option_prediction_model_version", columnList = "model_version"),
           @Index(name = "idx_option_prediction_execution_id", columnList = "execution_id"),
           @Index(name = "idx_option_prediction_created", columnList = "created_at")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_option_prediction_symbol_date_model",
                           columnNames = {"symbol", "forecast_date", "model_version"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionExpectedReturnPredictionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 30)
    private String symbol;

    @Column(name = "execution_id", nullable = false, length = 36)
    private String executionId;

    @Column(name = "forecast_date", nullable = false)
    private Instant forecastDate;

    @Column(name = "expected_return", nullable = false, precision = 23, scale = 16)
    private BigDecimal expectedReturn;

    @Column(name = "confidence_level", nullable = false, precision = 20, scale = 16)
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

    @Column(name = "mean_squared_error", precision = 23, scale = 16)
    private BigDecimal meanSquaredError;

    @Column(name = "standard_error", precision = 23, scale = 16)
    private BigDecimal standardError;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "has_sufficient_quality")
    private Boolean hasSufficientQuality;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "predict_diff_oc", precision = 23, scale = 16)
    private BigDecimal predictDiffOC;

    @Column(name = "predict_oc", precision = 23, scale = 16)
    private BigDecimal predictOC;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
