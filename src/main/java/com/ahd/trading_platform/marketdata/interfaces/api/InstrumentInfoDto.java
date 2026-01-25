package com.ahd.trading_platform.marketdata.interfaces.api;

import java.time.Instant;
import java.util.Optional;

/**
 * Data Transfer Object for instrument information exposed to other modules.
 * Provides essential instrument metadata without exposing domain entities.
 */
public record InstrumentInfoDto(
    String symbol,
    String name,
    String baseCurrency,
    String quoteCurrency,
    long dataPointCount,
    Optional<Instant> firstTradingDate,
    String dataSource,
    double completenessPercentage
) {

    /**
     * Check if the instrument has a recorded first trading date.
     */
    public boolean hasFirstTradingDate() {
        return firstTradingDate.isPresent();
    }

    /**
     * Check if the instrument has sufficient data points.
     */
    public boolean hasSufficientData(int minimumDataPoints) {
        return dataPointCount >= minimumDataPoints;
    }
}
