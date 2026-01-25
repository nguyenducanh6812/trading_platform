package com.ahd.trading_platform.backtesting.application.usecases;

import com.ahd.trading_platform.backtesting.domain.valueobjects.BacktestPeriod;
import com.ahd.trading_platform.backtesting.domain.valueobjects.InstrumentPair;
import com.ahd.trading_platform.backtesting.domain.valueobjects.MissingPredictionRange;
import com.ahd.trading_platform.backtesting.domain.valueobjects.PredictionModelVersion;
import com.ahd.trading_platform.forecasting.interfaces.api.ForecastingPort;
import com.ahd.trading_platform.forecasting.interfaces.api.PredictionInfoDto;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Use case for validating that prediction model has sufficient expected return data
 * for both instruments in the pair across the specified backtest period.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValidatePredictionModelDataUseCase {

    private final ForecastingPort forecastingPort;
    
    /**
     * Validates prediction model data availability and identifies specific missing date ranges.
     * Analyzes each instrument's prediction coverage across the backtest period to determine
     * exactly which date ranges need data fetching and prediction calculation.
     */
    public PredictionModelValidationResult execute(
            InstrumentPair instrumentPair,
            BacktestPeriod backtestPeriod,
            PredictionModelVersion modelVersion) {
        
        log.info("Validating prediction model {} for instrument pair {} over period {} to {}", 
            modelVersion.version(), instrumentPair, 
            backtestPeriod.getStartLocalDate(), backtestPeriod.getEndLocalDate());
        
        try {
            // Analyze missing date ranges for each instrument
            List<MissingPredictionRange> missingRanges = new ArrayList<>();
            
            // Analyze first instrument
            MissingPredictionRange firstInstrumentMissingRange = analyzeMissingDateRanges(
                instrumentPair.firstInstrument(), backtestPeriod, modelVersion);
            if (firstInstrumentMissingRange != null) {
                missingRanges.add(firstInstrumentMissingRange);
            }
            
            // Analyze second instrument
            MissingPredictionRange secondInstrumentMissingRange = analyzeMissingDateRanges(
                instrumentPair.secondInstrument(), backtestPeriod, modelVersion);
            if (secondInstrumentMissingRange != null) {
                missingRanges.add(secondInstrumentMissingRange);
            }
            
            // Extract missing instrument codes for backward compatibility
            List<String> missingInstruments = missingRanges.stream()
                .map(MissingPredictionRange::getInstrumentCode)
                .distinct()
                .collect(Collectors.toList());
            
            if (missingRanges.isEmpty()) {
                String message = String.format(
                    "Prediction data validation successful. Complete coverage found for %s and %s with model version %s over period %s to %s", 
                    instrumentPair.firstInstrument().getCode(),
                    instrumentPair.secondInstrument().getCode(),
                    modelVersion.version(),
                    backtestPeriod.getStartLocalDate(),
                    backtestPeriod.getEndLocalDate()
                );
                log.info("Prediction data validation successful for pair {} with model {}", instrumentPair, modelVersion.version());
                return PredictionModelValidationResult.success(message, List.of(), List.of());
            } else {
                String failureReason = buildFailureReasonForMissingRanges(missingRanges, modelVersion);
                log.warn("Prediction data validation failed for pair {} with model {}: {}", 
                    instrumentPair, modelVersion.version(), failureReason);
                return PredictionModelValidationResult.failure(failureReason, missingInstruments, missingRanges);
            }
            
        } catch (Exception e) {
            String errorMessage = "Failed to validate prediction model data: " + e.getMessage();
            log.error("Error validating prediction model {} for pair {}: {}", 
                modelVersion.version(), instrumentPair, errorMessage, e);
            
            // In case of error, assume both instruments need full period coverage
            List<MissingPredictionRange> fallbackRanges = List.of(
                MissingPredictionRange.fromBacktestPeriod(instrumentPair.firstInstrument(), backtestPeriod, modelVersion.version()),
                MissingPredictionRange.fromBacktestPeriod(instrumentPair.secondInstrument(), backtestPeriod, modelVersion.version())
            );
            List<String> allInstruments = List.of(instrumentPair.firstInstrument().getCode(), instrumentPair.secondInstrument().getCode());
            return PredictionModelValidationResult.failure(errorMessage, allInstruments, fallbackRanges);
        }
    }
    
    /**
     * Analyzes missing date ranges for a specific instrument using optimized two-phase approach.
     * Phase 1: Count successful predictions to quickly check complete coverage
     * Phase 2: If incomplete, fetch data to find specific gaps
     * Returns the specific date range that needs prediction data, or null if complete coverage exists.
     */
    private MissingPredictionRange analyzeMissingDateRanges(
            TradingInstrument instrument,
            BacktestPeriod backtestPeriod,
            PredictionModelVersion modelVersion) {

        try {
            // PHASE 1: Fast count-based validation to check if we have complete coverage
            long expectedDays = ChronoUnit.DAYS.between(backtestPeriod.startDate(), backtestPeriod.endDate()) + 1;
            long actualSuccessfulPredictions = forecastingPort.countSuccessfulPredictions(
                instrument, modelVersion.version(), backtestPeriod.startDate(), backtestPeriod.endDate());

            log.debug("Phase 1 - Count validation for {} with model {}: expected {} days, found {} successful predictions",
                instrument.getCode(), modelVersion.version(), expectedDays, actualSuccessfulPredictions);

            if (actualSuccessfulPredictions == expectedDays) {
                // Complete coverage exists - no need to fetch all data
                log.debug("Complete prediction coverage confirmed for {} (count match: {} predictions for {} days)",
                    instrument.getCode(), actualSuccessfulPredictions, expectedDays);
                return null;
            }

            if (actualSuccessfulPredictions == 0) {
                // No predictions exist, entire period is missing - no need to fetch data
                log.debug("No successful predictions found for {}, entire period is missing", instrument.getCode());
                return MissingPredictionRange.fromBacktestPeriod(instrument, backtestPeriod, modelVersion.version());
            }

            // PHASE 2: Partial coverage detected - fetch pre-filtered data to find specific gaps
            log.debug("Phase 2 - Gap detection for {} (partial coverage: {} of {} days)",
                instrument.getCode(), actualSuccessfulPredictions, expectedDays);

            List<PredictionInfoDto> existingPredictions = forecastingPort.findSuccessfulPredictions(
                instrument, modelVersion.version(), backtestPeriod.startDate(), backtestPeriod.endDate()
            );

            // Find the first gap (most efficient - return first missing range found)
            return findFirstMissingRange(instrument, backtestPeriod, modelVersion, existingPredictions);

        } catch (Exception e) {
            log.warn("Error analyzing missing date ranges for {}: {}", instrument.getCode(), e.getMessage());
            // Return full period as missing when in doubt
            return MissingPredictionRange.fromBacktestPeriod(instrument, backtestPeriod, modelVersion.version());
        }
    }
    
    /**
     * Finds the first missing date range from existing predictions.
     * This is more efficient than finding all gaps - we only need the first one to start data preparation.
     */
    private MissingPredictionRange findFirstMissingRange(
            TradingInstrument instrument,
            BacktestPeriod backtestPeriod,
            PredictionModelVersion modelVersion,
            List<PredictionInfoDto> existingPredictions) {

        Instant backtestStart = backtestPeriod.startDate();
        Instant backtestEnd = backtestPeriod.endDate();

        if (existingPredictions.isEmpty()) {
            return MissingPredictionRange.fromBacktestPeriod(instrument, backtestPeriod, modelVersion.version());
        }

        // Check gap at the end first (most common scenario - new data missing at end)
        Instant lastPredictionDate = existingPredictions.get(existingPredictions.size() - 1).forecastDate();
        if (lastPredictionDate.isBefore(backtestEnd)) {
            Instant missingStartDate = lastPredictionDate.plus(1, ChronoUnit.DAYS);
            log.debug("Found missing range for {} from {} to {} (gap after last prediction)",
                instrument.getCode(),
                LocalDate.ofInstant(missingStartDate, ZoneOffset.UTC),
                LocalDate.ofInstant(backtestEnd, ZoneOffset.UTC));
            return new MissingPredictionRange(instrument, missingStartDate, backtestEnd, modelVersion.version());
        }

        // Check gap at the beginning
        Instant firstPredictionDate = existingPredictions.getFirst().forecastDate();
        if (backtestStart.isBefore(firstPredictionDate)) {
            Instant missingEndDate = firstPredictionDate.minus(1, ChronoUnit.DAYS);
            log.debug("Found missing range for {} from {} to {} (gap before first prediction)",
                instrument.getCode(),
                LocalDate.ofInstant(backtestStart, ZoneOffset.UTC),
                LocalDate.ofInstant(missingEndDate, ZoneOffset.UTC));
            return new MissingPredictionRange(instrument, backtestStart, missingEndDate, modelVersion.version());
        }

        // Check gaps between predictions (least common scenario)
        for (int i = 0; i < existingPredictions.size() - 1; i++) {
            Instant currentDate = existingPredictions.get(i).forecastDate();
            Instant nextDate = existingPredictions.get(i + 1).forecastDate();

            long daysBetween = ChronoUnit.DAYS.between(currentDate, nextDate);
            if (daysBetween > 1) {
                Instant gapStart = currentDate.plus(1, ChronoUnit.DAYS);
                Instant gapEnd = nextDate.minus(1, ChronoUnit.DAYS);
                log.debug("Found missing range for {} from {} to {} (gap between predictions)",
                    instrument.getCode(),
                    LocalDate.ofInstant(gapStart, ZoneOffset.UTC),
                    LocalDate.ofInstant(gapEnd, ZoneOffset.UTC));
                return new MissingPredictionRange(instrument, gapStart, gapEnd, modelVersion.version());
            }
        }

        // This shouldn't happen if our count was correct, but handle gracefully
        log.warn("Count mismatch detected for {}: expected gaps but none found. This may indicate data inconsistency.",
            instrument.getCode());
        return null;
    }
    
    /**
     * Builds failure reason message from missing prediction ranges
     */
    private String buildFailureReasonForMissingRanges(List<MissingPredictionRange> missingRanges, 
                                                     PredictionModelVersion modelVersion) {
        return missingRanges.stream()
            .map(MissingPredictionRange::getDescription)
            .collect(Collectors.joining("; "));
    }
    
    /**
     * Result of prediction model validation with detailed missing range information
     */
    public record PredictionModelValidationResult(
        boolean isValid,
        String message,
        List<String> missingInstruments,
        List<MissingPredictionRange> missingRanges
    ) {
        
        public static PredictionModelValidationResult success(String message, 
                                                             List<String> missingInstruments,
                                                             List<MissingPredictionRange> missingRanges) {
            return new PredictionModelValidationResult(true, message, missingInstruments, missingRanges);
        }
        
        public static PredictionModelValidationResult failure(String message, 
                                                             List<String> missingInstruments,
                                                             List<MissingPredictionRange> missingRanges) {
            return new PredictionModelValidationResult(false, message, missingInstruments, missingRanges);
        }
    }
}