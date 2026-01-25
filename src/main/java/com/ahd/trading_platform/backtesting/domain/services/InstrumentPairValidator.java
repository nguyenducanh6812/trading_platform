package com.ahd.trading_platform.backtesting.domain.services;

import com.ahd.trading_platform.backtesting.domain.valueobjects.BacktestPeriod;
import com.ahd.trading_platform.backtesting.domain.valueobjects.InstrumentPair;
import com.ahd.trading_platform.marketdata.interfaces.api.InstrumentInfoDto;
import com.ahd.trading_platform.marketdata.interfaces.api.MarketDataPort;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Domain service for validating instrument pairs and determining valid backtest start dates.
 * Handles the business logic of ensuring both instruments in a pair have overlapping trading periods.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InstrumentPairValidator {

    private final MarketDataPort marketDataPort;
    
    /**
     * Validates an instrument pair and determines the earliest valid backtest start date.
     * The start date is constrained by the later of the two instruments' first trading dates.
     * 
     * @param instrumentPair The pair of instruments to validate
     * @param requestedBacktestPeriod The initially requested backtest period
     * @return Validation result with adjusted backtest period if needed
     */
    public InstrumentPairValidationResult validateAndAdjustBacktestPeriod(
            InstrumentPair instrumentPair, 
            BacktestPeriod requestedBacktestPeriod) {
        
        log.info("Validating instrument pair {} for backtest period {} to {}", 
            instrumentPair, 
            requestedBacktestPeriod.getStartLocalDate(), 
            requestedBacktestPeriod.getEndLocalDate());
        
        try {
            // Fetch market instruments for both trading instruments via port
            Optional<InstrumentInfoDto> firstInstrument = marketDataPort.getInstrumentInfo(
                instrumentPair.firstInstrument().getCode());
            Optional<InstrumentInfoDto> secondInstrument = marketDataPort.getInstrumentInfo(
                instrumentPair.secondInstrument().getCode());

            // Validate both instruments exist
            if (firstInstrument.isEmpty() || secondInstrument.isEmpty()) {
                String missingInstruments = "";
                if (firstInstrument.isEmpty()) {
                    missingInstruments += instrumentPair.firstInstrument().getCode();
                }
                if (secondInstrument.isEmpty()) {
                    if (!missingInstruments.isEmpty()) {
                        missingInstruments += ", ";
                    }
                    missingInstruments += instrumentPair.secondInstrument().getCode();
                }

                String errorMessage = String.format("Missing market data for instruments: %s", missingInstruments);
                log.warn("Instrument pair validation failed: {}", errorMessage);
                return InstrumentPairValidationResult.failure(errorMessage, requestedBacktestPeriod);
            }

            // Get first trading dates for both instruments
            Optional<Instant> firstTradingDate1 = firstInstrument.get().firstTradingDate();
            Optional<Instant> firstTradingDate2 = secondInstrument.get().firstTradingDate();
            
            // If either instrument has no first trading date, assume it's available for the requested period
            // This is normal for older instruments like Bitcoin where we don't track the very first trading date
            if (firstTradingDate1.isEmpty() && firstTradingDate2.isEmpty()) {
                // Both instruments have no trading date constraints - proceed normally
                String successMessage = String.format(
                    "Instrument pair validation successful. Both %s and %s have no trading date constraints for backtest period %s to %s",
                    instrumentPair.firstInstrument().getCode(),
                    instrumentPair.secondInstrument().getCode(),
                    requestedBacktestPeriod.getStartLocalDate(),
                    requestedBacktestPeriod.getEndLocalDate()
                );
                log.info("Instrument pair validation successful: {}", successMessage);
                return InstrumentPairValidationResult.success(successMessage, requestedBacktestPeriod);
            }
            
            // Determine the constraining first trading date (only consider instruments that have this data)
            Instant latestFirstTradingDate = null;
            if (firstTradingDate1.isPresent() && firstTradingDate2.isPresent()) {
                latestFirstTradingDate = firstTradingDate1.get().isAfter(firstTradingDate2.get()) 
                    ? firstTradingDate1.get() 
                    : firstTradingDate2.get();
            } else if (firstTradingDate1.isPresent()) {
                latestFirstTradingDate = firstTradingDate1.get();
            } else if (firstTradingDate2.isPresent()) {
                latestFirstTradingDate = firstTradingDate2.get();
            }
            
            // If no constraining date, proceed normally
            if (latestFirstTradingDate == null) {
                String successMessage = String.format(
                    "Instrument pair validation successful. No trading date constraints for %s and %s backtest period %s to %s",
                    instrumentPair.firstInstrument().getCode(),
                    instrumentPair.secondInstrument().getCode(),
                    requestedBacktestPeriod.getStartLocalDate(),
                    requestedBacktestPeriod.getEndLocalDate()
                );
                log.info("Instrument pair validation successful: {}", successMessage);
                return InstrumentPairValidationResult.success(successMessage, requestedBacktestPeriod);
            }
            
            LocalDate constrainingDate = LocalDate.ofInstant(latestFirstTradingDate, ZoneOffset.UTC);
            LocalDate requestedStartDate = requestedBacktestPeriod.getStartLocalDate();
            
            // Check if requested start date is valid
            if (requestedStartDate.isBefore(constrainingDate)) {
                // Adjust start date to the constraining date
                BacktestPeriod adjustedPeriod = BacktestPeriod.fromLocalDates(
                    constrainingDate, requestedBacktestPeriod.getEndLocalDate());
                
                String adjustmentMessage = String.format(
                    "Backtest start date adjusted from %s to %s due to %s first trading date (%s)", 
                    requestedStartDate,
                    constrainingDate,
                    getConstrainingInstrumentName(instrumentPair, firstTradingDate1.get(), firstTradingDate2.get()),
                    constrainingDate
                );
                
                log.info("Instrument pair validation successful with adjustment: {}", adjustmentMessage);
                return InstrumentPairValidationResult.successWithAdjustment(
                    adjustmentMessage, requestedBacktestPeriod, adjustedPeriod);
            } else {
                // No adjustment needed
                String successMessage = String.format(
                    "Instrument pair validation successful. Both %s (first trading: %s) and %s (first trading: %s) " +
                    "support the requested backtest period starting %s",
                    instrumentPair.firstInstrument().getCode(),
                    LocalDate.ofInstant(firstTradingDate1.get(), ZoneOffset.UTC),
                    instrumentPair.secondInstrument().getCode(),
                    LocalDate.ofInstant(firstTradingDate2.get(), ZoneOffset.UTC),
                    requestedStartDate
                );
                
                log.info("Instrument pair validation successful: {}", successMessage);
                return InstrumentPairValidationResult.success(successMessage, requestedBacktestPeriod);
            }
            
        } catch (Exception e) {
            String errorMessage = "Failed to validate instrument pair: " + e.getMessage();
            log.error("Error validating instrument pair {}: {}", instrumentPair, errorMessage, e);
            return InstrumentPairValidationResult.failure(errorMessage, requestedBacktestPeriod);
        }
    }
    
    /**
     * Determines which instrument is the constraining factor (has the later first trading date)
     */
    private String getConstrainingInstrumentName(InstrumentPair pair, Instant firstDate1, Instant firstDate2) {
        return firstDate1.isAfter(firstDate2) 
            ? pair.firstInstrument().getCode() 
            : pair.secondInstrument().getCode();
    }
    
    /**
     * Result of instrument pair validation with optional backtest period adjustment
     */
    public record InstrumentPairValidationResult(
        boolean isValid,
        String message,
        BacktestPeriod originalPeriod,
        BacktestPeriod adjustedPeriod,
        boolean wasAdjusted
    ) {
        
        public static InstrumentPairValidationResult success(String message, BacktestPeriod period) {
            return new InstrumentPairValidationResult(true, message, period, period, false);
        }
        
        public static InstrumentPairValidationResult successWithAdjustment(
                String message, BacktestPeriod originalPeriod, BacktestPeriod adjustedPeriod) {
            return new InstrumentPairValidationResult(true, message, originalPeriod, adjustedPeriod, true);
        }
        
        public static InstrumentPairValidationResult failure(String message, BacktestPeriod period) {
            return new InstrumentPairValidationResult(false, message, period, period, false);
        }
        
        /**
         * Returns the period to use for backtesting (adjusted if needed, original otherwise)
         */
        public BacktestPeriod getEffectivePeriod() {
            return wasAdjusted ? adjustedPeriod : originalPeriod;
        }
    }
}