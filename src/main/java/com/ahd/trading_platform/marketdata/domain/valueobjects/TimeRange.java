package com.ahd.trading_platform.marketdata.domain.valueobjects;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * TimeRange value object representing a period between two time points.
 * Handles validation and provides utility methods for time-based operations.
 */
public record TimeRange(Instant from, Instant to) {
    
    public TimeRange {
        Objects.requireNonNull(from, "From time cannot be null");
        Objects.requireNonNull(to, "To time cannot be null");
        
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From time must be before or equal to To time");
        }
    }
    
    /**
     * Creates TimeRange from LocalDate values (start of day to end of day)
     */
    public static TimeRange fromDates(LocalDate fromDate, LocalDate toDate) {
        Objects.requireNonNull(fromDate, "From date cannot be null");
        Objects.requireNonNull(toDate, "To date cannot be null");
        
        return new TimeRange(
            fromDate.atStartOfDay().toInstant(ZoneOffset.UTC),
            toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        );
    }
    
    /**
     * Creates TimeRange for the historical data request (March 15, 2021 to now)
     */
    public static TimeRange forHistoricalData() {
        LocalDate startDate = LocalDate.of(2021, 3, 15);
        LocalDate endDate = LocalDate.now();
        return fromDates(startDate, endDate);
    }
    
    /**
     * Creates TimeRange for last N days
     */
    public static TimeRange lastDays(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Days must be positive");
        }
        
        Instant now = Instant.now();
        Instant from = now.minus(days, ChronoUnit.DAYS);
        return new TimeRange(from, now);
    }
    
    /**
     * Creates TimeRange for last N hours
     */
    public static TimeRange lastHours(int hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException("Hours must be positive");
        }
        
        Instant now = Instant.now();
        Instant from = now.minus(hours, ChronoUnit.HOURS);
        return new TimeRange(from, now);
    }
    
    /**
     * Checks if given timestamp falls within this range (inclusive)
     */
    public boolean contains(Instant timestamp) {
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        return !timestamp.isBefore(from) && !timestamp.isAfter(to);
    }
    
    /**
     * Returns the duration in milliseconds
     */
    public long getDurationMillis() {
        return to.toEpochMilli() - from.toEpochMilli();
    }
    
    /**
     * Returns the duration in days
     */
    public long getDurationDays() {
        return ChronoUnit.DAYS.between(from, to);
    }
    
    /**
     * Returns the duration in hours
     */
    public long getDurationHours() {
        return ChronoUnit.HOURS.between(from, to);
    }
    
    /**
     * Checks if this range overlaps with another range
     */
    public boolean overlaps(TimeRange other) {
        Objects.requireNonNull(other, "Other time range cannot be null");
        return from.isBefore(other.to) && to.isAfter(other.from);
    }
    
    /**
     * Splits this range into chunks of specified duration in days
     */
    public TimeRange[] splitIntoDays(int daysPerChunk) {
        if (daysPerChunk <= 0) {
            throw new IllegalArgumentException("Days per chunk must be positive");
        }
        
        long totalDays = getDurationDays();
        int numChunks = (int) Math.ceil((double) totalDays / daysPerChunk);
        TimeRange[] chunks = new TimeRange[numChunks];
        
        Instant currentFrom = from;
        for (int i = 0; i < numChunks; i++) {
            Instant currentTo = currentFrom.plus(daysPerChunk, ChronoUnit.DAYS);
            if (currentTo.isAfter(to)) {
                currentTo = to;
            }
            
            chunks[i] = new TimeRange(currentFrom, currentTo);
            currentFrom = currentTo;
        }
        
        return chunks;
    }
    
    @Override
    public String toString() {
        return String.format("TimeRange[%s to %s]", from, to);
    }
}