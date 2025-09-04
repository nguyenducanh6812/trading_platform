package com.ahd.trading_platform.marketdata.domain.services;

import com.ahd.trading_platform.marketdata.domain.valueobjects.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Domain service for validating market data quality and consistency.
 * Contains pure business logic for data validation rules.
 */
public class DataValidationService {
    
    /**
     * Validates OHLCV data for business rule compliance
     */
    public ValidationResult validateOHLCVData(List<OHLCV> ohlcvData) {
        Objects.requireNonNull(ohlcvData, "OHLCV data cannot be null");
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (ohlcvData.isEmpty()) {
            errors.add("OHLCV data list is empty");
            return ValidationResult.failed(errors, warnings);
        }
        
        // Check for price consistency within each OHLCV
        for (int i = 0; i < ohlcvData.size(); i++) {
            OHLCV ohlcv = ohlcvData.get(i);
            validateSingleOHLCV(ohlcv, i, errors, warnings);
        }
        
        // Check temporal consistency
        validateTemporalConsistency(ohlcvData, errors, warnings);
        
        // Check for gaps in data
        validateDataContinuity(ohlcvData, warnings);
        
        return errors.isEmpty() ? 
            ValidationResult.success(warnings) : 
            ValidationResult.failed(errors, warnings);
    }
    
    /**
     * Validates that market data covers the expected historical range
     */
    public ValidationResult validateHistoricalDataRange(List<OHLCV> ohlcvData, TimeRange expectedRange) {
        Objects.requireNonNull(ohlcvData, "OHLCV data cannot be null");
        Objects.requireNonNull(expectedRange, "Expected range cannot be null");
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (ohlcvData.isEmpty()) {
            errors.add("No historical data provided");
            return ValidationResult.failed(errors, warnings);
        }
        
        // Find actual data range
        Instant earliestTimestamp = ohlcvData.stream()
            .map(OHLCV::timestamp)
            .min(Instant::compareTo)
            .orElse(Instant.now());
            
        Instant latestTimestamp = ohlcvData.stream()
            .map(OHLCV::timestamp)
            .max(Instant::compareTo)
            .orElse(Instant.now());
        
        TimeRange actualRange = new TimeRange(earliestTimestamp, latestTimestamp);
        
        // Check if actual range covers expected range
        if (actualRange.from().isAfter(expectedRange.from())) {
            long daysMissing = ChronoUnit.DAYS.between(expectedRange.from(), actualRange.from());
            warnings.add(String.format("Missing %d days of data at the beginning", daysMissing));
        }
        
        if (actualRange.to().isBefore(expectedRange.to())) {
            long daysMissing = ChronoUnit.DAYS.between(actualRange.to(), expectedRange.to());
            warnings.add(String.format("Missing %d days of data at the end", daysMissing));
        }
        
        // Check data density (should have data for at least 80% of expected days)
        long expectedDays = expectedRange.getDurationDays();
        long actualDays = actualRange.getDurationDays();
        double coverage = (double) actualDays / expectedDays;
        
        if (coverage < 0.8) {
            warnings.add(String.format("Low data coverage: %.1f%% (expected >= 80%%)", coverage * 100));
        }
        
        return ValidationResult.success(warnings);
    }
    
    /**
     * Validates price volatility to detect anomalies
     */
    public ValidationResult validatePriceVolatility(List<OHLCV> ohlcvData, double maxDailyChange) {
        Objects.requireNonNull(ohlcvData, "OHLCV data cannot be null");
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (maxDailyChange <= 0) {
            errors.add("Max daily change must be positive");
            return ValidationResult.failed(errors, warnings);
        }
        
        for (int i = 1; i < ohlcvData.size(); i++) {
            OHLCV previous = ohlcvData.get(i - 1);
            OHLCV current = ohlcvData.get(i);
            
            double previousClose = previous.close().amount().doubleValue();
            double currentClose = current.close().amount().doubleValue();
            
            if (previousClose > 0) {
                double dailyChange = Math.abs((currentClose - previousClose) / previousClose);
                
                if (dailyChange > maxDailyChange) {
                    warnings.add(String.format("Unusual price movement detected at %s: %.2f%% change", 
                        current.timestamp(), dailyChange * 100));
                }
            }
        }
        
        return ValidationResult.success(warnings);
    }
    
    private void validateSingleOHLCV(OHLCV ohlcv, int index, List<String> errors, List<String> warnings) {
        String position = "at index " + index + " (" + ohlcv.timestamp() + ")";
        
        // Check for zero prices (might indicate missing data)
        if (ohlcv.open().isZero() || ohlcv.high().isZero() || 
            ohlcv.low().isZero() || ohlcv.close().isZero()) {
            warnings.add("Zero price detected " + position);
        }
        
        // Check for extremely low volume (might indicate low liquidity)
        if (ohlcv.volume().doubleValue() < 1.0) {
            warnings.add("Very low volume detected " + position);
        }
        
        // Check for suspicious price patterns (e.g., all prices identical)
        if (ohlcv.open().equals(ohlcv.high()) && 
            ohlcv.high().equals(ohlcv.low()) && 
            ohlcv.low().equals(ohlcv.close())) {
            warnings.add("Flat price line detected " + position);
        }
    }
    
    private void validateTemporalConsistency(List<OHLCV> ohlcvData, List<String> errors, List<String> warnings) {
        for (int i = 1; i < ohlcvData.size(); i++) {
            OHLCV previous = ohlcvData.get(i - 1);
            OHLCV current = ohlcvData.get(i);
            
            // Check for duplicate timestamps
            if (previous.timestamp().equals(current.timestamp())) {
                errors.add(String.format("Duplicate timestamp detected: %s", current.timestamp()));
            }
            
            // Check for reasonable time intervals (should be consistent)
            long hoursBetween = ChronoUnit.HOURS.between(previous.timestamp(), current.timestamp());
            if (hoursBetween > 48) { // More than 2 days gap
                warnings.add(String.format("Large time gap detected: %d hours between %s and %s", 
                    hoursBetween, previous.timestamp(), current.timestamp()));
            }
        }
    }
    
    private void validateDataContinuity(List<OHLCV> ohlcvData, List<String> warnings) {
        if (ohlcvData.size() < 2) return;
        
        // Calculate expected interval based on first few data points
        long totalIntervals = 0;
        for (int i = 1; i < Math.min(10, ohlcvData.size()); i++) {
            totalIntervals += ChronoUnit.HOURS.between(
                ohlcvData.get(i - 1).timestamp(), 
                ohlcvData.get(i).timestamp()
            );
        }
        long avgInterval = totalIntervals / (Math.min(9, ohlcvData.size() - 1));
        
        // Check for gaps larger than expected
        for (int i = 1; i < ohlcvData.size(); i++) {
            long actualInterval = ChronoUnit.HOURS.between(
                ohlcvData.get(i - 1).timestamp(), 
                ohlcvData.get(i).timestamp()
            );
            
            if (actualInterval > avgInterval * 2) {
                warnings.add(String.format("Data gap detected: %d hours (expected ~%d hours) before %s", 
                    actualInterval, avgInterval, ohlcvData.get(i).timestamp()));
            }
        }
    }
    
    /**
     * Result of data validation containing success status and any issues found
     */
    public record ValidationResult(
        boolean isValid,
        List<String> errors,
        List<String> warnings
    ) {
        
        public ValidationResult {
            Objects.requireNonNull(errors, "Errors list cannot be null");
            Objects.requireNonNull(warnings, "Warnings list cannot be null");
        }
        
        public static ValidationResult success(List<String> warnings) {
            return new ValidationResult(true, List.of(), List.copyOf(warnings));
        }
        
        public static ValidationResult failed(List<String> errors, List<String> warnings) {
            return new ValidationResult(false, List.copyOf(errors), List.copyOf(warnings));
        }
        
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}