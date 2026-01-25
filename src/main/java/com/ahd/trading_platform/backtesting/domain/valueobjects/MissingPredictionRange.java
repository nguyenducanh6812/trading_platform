package com.ahd.trading_platform.backtesting.domain.valueobjects;

import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Domain value object representing a missing prediction range for a specific instrument.
 * Used to track exactly which date ranges need prediction data to be fetched and calculated.
 */
public record MissingPredictionRange(
    TradingInstrument instrument,
    Instant startDate,
    Instant endDate,
    String modelVersion
) {
    
    /**
     * Creates a missing range from LocalDate boundaries
     */
    public static MissingPredictionRange fromLocalDates(
            TradingInstrument instrument,
            LocalDate startDate, 
            LocalDate endDate,
            String modelVersion) {
        return new MissingPredictionRange(
            instrument,
            startDate.atStartOfDay().toInstant(ZoneOffset.UTC),
            endDate.atStartOfDay().toInstant(ZoneOffset.UTC),
            modelVersion
        );
    }
    
    /**
     * Creates a missing range for the entire backtest period
     */
    public static MissingPredictionRange fromBacktestPeriod(
            TradingInstrument instrument,
            BacktestPeriod backtestPeriod,
            String modelVersion) {
        return new MissingPredictionRange(
            instrument,
            backtestPeriod.startDate(),
            backtestPeriod.endDate(),
            modelVersion
        );
    }
    
    /**
     * Gets the start date as LocalDate
     */
    public LocalDate getStartLocalDate() {
        return LocalDate.ofInstant(startDate, ZoneOffset.UTC);
    }
    
    /**
     * Gets the end date as LocalDate
     */
    public LocalDate getEndLocalDate() {
        return LocalDate.ofInstant(endDate, ZoneOffset.UTC);
    }
    
    /**
     * Gets the instrument code
     */
    public String getInstrumentCode() {
        return instrument.getCode();
    }
    
    /**
     * Gets the number of days in this range
     */
    public long getDaysCount() {
        return java.time.Duration.between(startDate, endDate).toDays() + 1;
    }
    
    /**
     * Checks if this range covers the entire period
     */
    public boolean isFullPeriod(BacktestPeriod backtestPeriod) {
        return startDate.equals(backtestPeriod.startDate()) && 
               endDate.equals(backtestPeriod.endDate());
    }
    
    /**
     * Creates a formatted description of this missing range
     */
    public String getDescription() {
        return String.format("%s missing predictions from %s to %s (model: %s)", 
            instrument.getCode(),
            getStartLocalDate(),
            getEndLocalDate(),
            modelVersion);
    }
}