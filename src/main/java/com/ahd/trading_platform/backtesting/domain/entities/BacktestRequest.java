package com.ahd.trading_platform.backtesting.domain.entities;

import com.ahd.trading_platform.backtesting.domain.valueobjects.BacktestPeriod;
import com.ahd.trading_platform.backtesting.domain.valueobjects.InstrumentPair;
import com.ahd.trading_platform.backtesting.domain.valueobjects.PredictionModelVersion;

import java.time.Instant;

/**
 * Domain entity representing a backtesting request with validation status.
 * Tracks the validation progress through the backtesting workflow.
 */
public record BacktestRequest(
    String requestId,
    InstrumentPair instrumentPair,
    BacktestPeriod backtestPeriod,
    PredictionModelVersion modelVersion,
    ValidationStatus validationStatus,
    String validationMessage,
    Instant createdAt,
    Instant updatedAt
) {
    
    public BacktestRequest {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }
        if (instrumentPair == null) {
            throw new IllegalArgumentException("Instrument pair cannot be null");
        }
        if (backtestPeriod == null) {
            throw new IllegalArgumentException("Backtest period cannot be null");
        }
        if (modelVersion == null) {
            throw new IllegalArgumentException("Model version cannot be null");
        }
        if (validationStatus == null) {
            throw new IllegalArgumentException("Validation status cannot be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Created timestamp cannot be null");
        }
    }
    
    /**
     * Validation status enum for tracking backtest request progress
     */
    public enum ValidationStatus {
        PENDING,                    // Initial state
        VALIDATING_INSTRUMENTS,     // Step 1: Validating instrument pair data
        INSTRUMENTS_VALIDATED,      // Step 1: Complete
        INSTRUMENTS_INVALID,        // Step 1: Failed
        VALIDATING_PREDICTIONS,     // Step 2: Validating prediction data
        PREDICTIONS_VALIDATED,      // Step 2: Complete
        PREDICTIONS_INVALID,        // Step 2: Failed
        READY_FOR_BACKTEST,         // All validations passed
        VALIDATION_FAILED           // Overall validation failed
    }
    
    /**
     * Creates a new backtest request with pending status
     */
    public static BacktestRequest create(
            String requestId,
            InstrumentPair instrumentPair,
            BacktestPeriod backtestPeriod,
            PredictionModelVersion modelVersion) {
        
        Instant now = Instant.now();
        return new BacktestRequest(
            requestId,
            instrumentPair,
            backtestPeriod,
            modelVersion,
            ValidationStatus.PENDING,
            "Request created, validation pending",
            now,
            now
        );
    }
    
    /**
     * Updates validation status with message
     */
    public BacktestRequest withValidationStatus(ValidationStatus status, String message) {
        return new BacktestRequest(
            requestId,
            instrumentPair,
            backtestPeriod,
            modelVersion,
            status,
            message,
            createdAt,
            Instant.now()
        );
    }
    
    /**
     * Marks instruments as validated
     */
    public BacktestRequest instrumentsValidated() {
        return withValidationStatus(
            ValidationStatus.INSTRUMENTS_VALIDATED,
            "Instrument pair validation successful"
        );
    }
    
    /**
     * Marks instruments as invalid
     */
    public BacktestRequest instrumentsInvalid(String reason) {
        return withValidationStatus(
            ValidationStatus.INSTRUMENTS_INVALID,
            "Instrument pair validation failed: " + reason
        );
    }
    
    /**
     * Marks predictions as validated
     */
    public BacktestRequest predictionsValidated() {
        return withValidationStatus(
            ValidationStatus.PREDICTIONS_VALIDATED,
            "Prediction model data validation successful"
        );
    }
    
    /**
     * Marks predictions as invalid
     */
    public BacktestRequest predictionsInvalid(String reason) {
        return withValidationStatus(
            ValidationStatus.PREDICTIONS_INVALID,
            "Prediction model data validation failed: " + reason
        );
    }
    
    /**
     * Marks request as ready for backtesting
     */
    public BacktestRequest readyForBacktest() {
        return withValidationStatus(
            ValidationStatus.READY_FOR_BACKTEST,
            "All validations passed, ready for backtesting"
        );
    }
    
    /**
     * Checks if all validations have passed
     */
    public boolean isValid() {
        return validationStatus == ValidationStatus.READY_FOR_BACKTEST;
    }
    
    /**
     * Checks if validation has failed
     */
    public boolean isInvalid() {
        return validationStatus == ValidationStatus.INSTRUMENTS_INVALID ||
               validationStatus == ValidationStatus.PREDICTIONS_INVALID ||
               validationStatus == ValidationStatus.VALIDATION_FAILED;
    }
}