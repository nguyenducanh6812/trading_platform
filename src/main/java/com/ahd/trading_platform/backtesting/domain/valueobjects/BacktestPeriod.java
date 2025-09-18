package com.ahd.trading_platform.backtesting.domain.valueobjects;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * Value object representing a backtesting time period with validation rules.
 * Ensures the period is valid for backtesting operations.
 */
public record BacktestPeriod(
    Instant startDate,
    Instant endDate
) {
    
    public BacktestPeriod {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("End date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        if (startDate.equals(endDate)) {
            throw new IllegalArgumentException("Backtest period must span at least one day");
        }
        
        // Validate minimum period length (at least 1 day for backtest)
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween < 1) {
            throw new IllegalArgumentException("Backtest period must be at least 1 day");
        }
        
        // Validate period is not in the future
        Instant now = Instant.now();
        if (endDate.isAfter(now)) {
            throw new IllegalArgumentException("End date cannot be in the future");
        }
    }
    
    /**
     * Creates backtest period from LocalDate values
     */
    public static BacktestPeriod fromLocalDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Dates cannot be null");
        }
        
        Instant startInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endInstant = endDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        
        return new BacktestPeriod(startInstant, endInstant);
    }
    
    /**
     * Creates backtest period from ISO date strings (YYYY-MM-DD format)
     */
    public static BacktestPeriod fromDateStrings(String startDateStr, String endDateStr) {
        if (startDateStr == null || startDateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Start date string cannot be null or empty");
        }
        if (endDateStr == null || endDateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("End date string cannot be null or empty");
        }
        
        try {
            LocalDate startLocal = parseFlexibleDate(startDateStr.trim());
            LocalDate endLocal = parseFlexibleDate(endDateStr.trim());
            return fromLocalDates(startLocal, endLocal);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Expected YYYY-MM-DD format or ISO datetime", e);
        }
    }
    
    /**
     * Helper method to parse date strings flexibly.
     * Handles both simple date format (YYYY-MM-DD) and ISO datetime formats.
     */
    private static LocalDate parseFlexibleDate(String dateStr) {
        try {
            // First try simple date format
            return LocalDate.parse(dateStr);
        } catch (Exception e1) {
            try {
                // Try ISO datetime format and extract the date part
                if (dateStr.contains("T")) {
                    return LocalDate.parse(dateStr.substring(0, dateStr.indexOf("T")));
                }
                // Try other common datetime formats by extracting just the date part
                if (dateStr.length() > 10) {
                    return LocalDate.parse(dateStr.substring(0, 10));
                }
                throw e1;
            } catch (Exception e2) {
                throw new IllegalArgumentException("Unable to parse date: " + dateStr, e2);
            }
        }
    }
    
    /**
     * Gets the duration of the backtest period in days
     */
    public long getDurationInDays() {
        return ChronoUnit.DAYS.between(startDate, endDate);
    }
    
    /**
     * Gets the start date as LocalDate in UTC
     */
    public LocalDate getStartLocalDate() {
        return startDate.atZone(ZoneOffset.UTC).toLocalDate();
    }
    
    /**
     * Gets the end date as LocalDate in UTC
     */
    public LocalDate getEndLocalDate() {
        return endDate.atZone(ZoneOffset.UTC).toLocalDate();
    }
    
    /**
     * Checks if the specified date falls within this backtest period
     */
    public boolean contains(Instant date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}