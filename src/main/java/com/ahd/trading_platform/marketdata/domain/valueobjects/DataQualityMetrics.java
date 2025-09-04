package com.ahd.trading_platform.marketdata.domain.valueobjects;

import java.time.Instant;
import java.util.Objects;

/**
 * DataQualityMetrics value object representing quality indicators for market data.
 * Used for data validation and quality assurance in trading strategies.
 */
public record DataQualityMetrics(
    int totalDataPoints,
    int missingDataPoints,
    int duplicateDataPoints,
    double completenessPercentage,
    Instant lastUpdated,
    String dataSource
) {
    
    public DataQualityMetrics {
        Objects.requireNonNull(lastUpdated, "Last updated timestamp cannot be null");
        Objects.requireNonNull(dataSource, "Data source cannot be null");
        
        if (totalDataPoints < 0) {
            throw new IllegalArgumentException("Total data points cannot be negative");
        }
        if (missingDataPoints < 0) {
            throw new IllegalArgumentException("Missing data points cannot be negative");
        }
        if (duplicateDataPoints < 0) {
            throw new IllegalArgumentException("Duplicate data points cannot be negative");
        }
        if (completenessPercentage < 0.0 || completenessPercentage > 100.0) {
            throw new IllegalArgumentException("Completeness percentage must be between 0 and 100");
        }
        if (missingDataPoints > totalDataPoints) {
            throw new IllegalArgumentException("Missing data points cannot exceed total data points");
        }
    }
    
    /**
     * Creates DataQualityMetrics with calculated completeness
     */
    public static DataQualityMetrics create(
        int totalDataPoints, 
        int missingDataPoints, 
        int duplicateDataPoints,
        String dataSource) {
        
        double completeness = totalDataPoints == 0 ? 0.0 : 
            ((double) (totalDataPoints - missingDataPoints) / totalDataPoints) * 100.0;
            
        return new DataQualityMetrics(
            totalDataPoints,
            missingDataPoints,
            duplicateDataPoints,
            completeness,
            Instant.now(),
            dataSource
        );
    }
    
    /**
     * Creates perfect quality metrics (no missing or duplicate data)
     */
    public static DataQualityMetrics perfect(int totalDataPoints, String dataSource) {
        return create(totalDataPoints, 0, 0, dataSource);
    }
    
    /**
     * Checks if data quality is acceptable (>= 95% completeness, <5% duplicates)
     */
    public boolean isAcceptable() {
        double duplicatePercentage = totalDataPoints == 0 ? 0.0 : 
            ((double) duplicateDataPoints / totalDataPoints) * 100.0;
            
        return completenessPercentage >= 95.0 && duplicatePercentage < 5.0;
    }
    
    /**
     * Checks if data quality is excellent (>= 99% completeness, <1% duplicates)
     */
    public boolean isExcellent() {
        double duplicatePercentage = totalDataPoints == 0 ? 0.0 : 
            ((double) duplicateDataPoints / totalDataPoints) * 100.0;
            
        return completenessPercentage >= 99.0 && duplicatePercentage < 1.0;
    }
    
    /**
     * Returns the number of valid data points (total - missing - duplicates)
     */
    public int getValidDataPoints() {
        return Math.max(0, totalDataPoints - missingDataPoints - duplicateDataPoints);
    }
    
    /**
     * Returns quality score from 0-100 based on completeness and duplicate ratio
     */
    public double getQualityScore() {
        if (totalDataPoints == 0) return 0.0;
        
        double duplicatePercentage = ((double) duplicateDataPoints / totalDataPoints) * 100.0;
        double duplicatePenalty = Math.min(duplicatePercentage * 2, 50.0); // Max 50% penalty
        
        return Math.max(0.0, completenessPercentage - duplicatePenalty);
    }
    
    /**
     * Returns quality level as string (EXCELLENT, GOOD, ACCEPTABLE, POOR)
     */
    public String getQualityLevel() {
        double score = getQualityScore();
        return switch ((int) score / 10) {
            case 10, 9 -> "EXCELLENT";
            case 8, 7 -> "GOOD";
            case 6, 5 -> "ACCEPTABLE";
            default -> "POOR";
        };
    }
    
    /**
     * Combines this metrics with another (useful for aggregating multiple sources)
     */
    public DataQualityMetrics combine(DataQualityMetrics other) {
        Objects.requireNonNull(other, "Other metrics cannot be null");
        
        int combinedTotal = this.totalDataPoints + other.totalDataPoints;
        int combinedMissing = this.missingDataPoints + other.missingDataPoints;
        int combinedDuplicates = this.duplicateDataPoints + other.duplicateDataPoints;
        
        String combinedSource = this.dataSource.equals(other.dataSource) ? 
            this.dataSource : this.dataSource + "+" + other.dataSource;
            
        Instant latestUpdate = this.lastUpdated.isAfter(other.lastUpdated) ? 
            this.lastUpdated : other.lastUpdated;
        
        double combinedCompleteness = combinedTotal == 0 ? 0.0 : 
            ((double) (combinedTotal - combinedMissing) / combinedTotal) * 100.0;
        
        return new DataQualityMetrics(
            combinedTotal,
            combinedMissing,
            combinedDuplicates,
            combinedCompleteness,
            latestUpdate,
            combinedSource
        );
    }
    
    @Override
    public String toString() {
        return String.format(
            "DataQuality[total=%d, missing=%d, duplicates=%d, completeness=%.1f%%, quality=%s, source=%s]",
            totalDataPoints, missingDataPoints, duplicateDataPoints, 
            completenessPercentage, getQualityLevel(), dataSource
        );
    }
}