package com.ahd.trading_platform.backtesting.domain.valueobjects;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Value object representing a prediction model version identifier.
 * Ensures the version follows the expected format and validation rules.
 */
public record PredictionModelVersion(String version) {
    
    private static final DateTimeFormatter VERSION_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    public PredictionModelVersion {
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("Model version cannot be null or empty");
        }
        
        String trimmedVersion = version.trim();
        
        // Validate format: should be 8-digit date format (YYYYMMDD)
        if (trimmedVersion.length() != 8) {
            throw new IllegalArgumentException("Model version must be in YYYYMMDD format (8 digits)");
        }
        
        if (!trimmedVersion.matches("\\d{8}")) {
            throw new IllegalArgumentException("Model version must contain only digits");
        }
        
        // Validate it's a valid date
        try {
            LocalDate.parse(trimmedVersion, VERSION_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Model version must be a valid date in YYYYMMDD format", e);
        }
        
        // Replace with trimmed version
        version = trimmedVersion;
    }
    
    /**
     * Gets the model version as a LocalDate
     */
    public LocalDate getVersionAsDate() {
        return LocalDate.parse(version, VERSION_FORMAT);
    }
    
    /**
     * Creates current version based on today's date
     */
    public static PredictionModelVersion current() {
        String currentVersion = LocalDate.now().format(VERSION_FORMAT);
        return new PredictionModelVersion(currentVersion);
    }
    
    /**
     * Creates version from LocalDate
     */
    public static PredictionModelVersion fromDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        String versionStr = date.format(VERSION_FORMAT);
        return new PredictionModelVersion(versionStr);
    }
    
    /**
     * Checks if this version is older than the specified version
     */
    public boolean isOlderThan(PredictionModelVersion other) {
        return getVersionAsDate().isBefore(other.getVersionAsDate());
    }
    
    /**
     * Checks if this version is newer than the specified version
     */
    public boolean isNewerThan(PredictionModelVersion other) {
        return getVersionAsDate().isAfter(other.getVersionAsDate());
    }
}