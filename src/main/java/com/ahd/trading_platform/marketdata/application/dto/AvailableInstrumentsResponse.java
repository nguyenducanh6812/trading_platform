package com.ahd.trading_platform.marketdata.application.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response containing available trading instruments for portfolio creation.
 * Includes metadata about data source and origin for transparency.
 */
public record AvailableInstrumentsResponse(
    List<InstrumentInfo> instruments,
    String source,          // Data source provider (e.g., "bybit")
    String dataOrigin,      // "DATABASE" (primary cache) or "EXTERNAL_API" (fallback when DB empty)
    LocalDateTime timestamp
) {
    /**
     * Factory method for primary database cache responses.
     * Used when database is the primary data source (database-first strategy).
     */
    public static AvailableInstrumentsResponse fromDatabase(
        List<InstrumentInfo> instruments,
        String source
    ) {
        return new AvailableInstrumentsResponse(
            instruments,
            source,
            "DATABASE",
            LocalDateTime.now()
        );
    }

    /**
     * Factory method for successful responses from external API.
     * Used when database is empty and API is used as fallback.
     */
    public static AvailableInstrumentsResponse fromExternalApi(
        List<InstrumentInfo> instruments,
        String source
    ) {
        return new AvailableInstrumentsResponse(
            instruments,
            source,
            "EXTERNAL_API",
            LocalDateTime.now()
        );
    }

    /**
     * Factory method for fallback responses from database.
     * @deprecated Use fromDatabase() for database-first strategy
     */
    @Deprecated
    public static AvailableInstrumentsResponse fromDatabaseFallback(
        List<InstrumentInfo> instruments,
        String source
    ) {
        return new AvailableInstrumentsResponse(
            instruments,
            source,
            "DATABASE_FALLBACK",
            LocalDateTime.now()
        );
    }
}
