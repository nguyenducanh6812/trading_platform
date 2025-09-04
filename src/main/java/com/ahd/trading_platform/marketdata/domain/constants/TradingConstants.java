package com.ahd.trading_platform.marketdata.domain.constants;

import java.time.LocalDate;

/**
 * Constants used across the trading platform.
 * Centralizes configuration values to improve maintainability.
 */
public final class TradingConstants {
    
    private TradingConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    // Historical data constants
    public static final LocalDate HISTORICAL_START_DATE = LocalDate.of(2021, 3, 15);
    public static final String DEFAULT_CURRENCY = "USD";
    public static final String DEFAULT_DATA_SOURCE = "bybit";
    
    // Process constants
    public static final String EXTERNAL_API_SOURCE = "EXTERNAL_API";
    public static final String CAMUNDA_PROCESS_SOURCE = "CAMUNDA_PROCESS";
    
    // Validation constants
    public static final int MIN_DATA_POINTS_FOR_ANALYSIS = 30;
    public static final int BATCH_SIZE = 100;
    public static final int CHUNK_SIZE_DAYS = 90;
    
    // Rate limiting constants
    public static final long RATE_LIMIT_DELAY_MS = 100;
    public static final long CHUNK_DELAY_MS = 200;
    
    // API limits
    public static final int MAX_BYBIT_RECORDS_PER_REQUEST = 200;
    public static final int INTERMEDIATE_SAVE_FREQUENCY = 500; // Save every 500 records
}