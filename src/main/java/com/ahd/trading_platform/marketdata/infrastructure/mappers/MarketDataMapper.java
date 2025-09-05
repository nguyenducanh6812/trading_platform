package com.ahd.trading_platform.marketdata.infrastructure.mappers;

import com.ahd.trading_platform.marketdata.application.dto.MarketDataResponse;
import com.ahd.trading_platform.marketdata.domain.entities.MarketInstrument;
import com.ahd.trading_platform.shared.valueobjects.*;
import com.ahd.trading_platform.marketdata.domain.valueobjects.*;
import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.Price;
import com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.MarketInstrumentEntity;
import com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.BtcPriceEntity;
import com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.EthPriceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * MapStruct mapper for converting between domain objects, entities, and DTOs.
 * 
 * This mapper handles the critical conversions in the trading platform:
 * - Domain objects ↔ JPA entities (persistence layer)
 * - Domain objects ↔ DTOs (API layer)
 * - Asset-specific entity mappings (BTC, ETH)
 * 
 * Benefits for trading platform:
 * - Type safety prevents data corruption in financial calculations
 * - Compile-time validation catches mapping errors early
 * - Performance-critical for high-frequency data processing
 * - Centralized mapping logic for audit requirements
 */
@Mapper(componentModel = "spring", uses = {BybitMapper.class})
public interface MarketDataMapper {
    
    // ========== Domain ↔ JPA Entity Mappings ==========
    
    /**
     * Maps domain MarketInstrument to JPA entity.
     */
    default MarketInstrumentEntity toEntity(MarketInstrument instrument) {
        if (instrument == null) return null;
        
        return MarketInstrumentEntity.builder()
            .symbol(instrument.getSymbol())
            .name(instrument.getName())
            .baseCurrency(instrument.getBaseCurrency())
            .quoteCurrency(instrument.getQuoteCurrency())
            .dataPointCount(instrument.getQualityMetrics() != null ? 
                instrument.getQualityMetrics().totalDataPoints() : 0)
            .qualityScore(instrument.getQualityMetrics() != null ? 
                instrument.getQualityMetrics().getQualityScore() : null)
            .qualityLevel(instrument.getQualityMetrics() != null ? 
                instrument.getQualityMetrics().getQualityLevel() : null)
            .dataSource(instrument.getQualityMetrics() != null ? 
                instrument.getQualityMetrics().dataSource() : null)
            .build();
    }
    
    /**
     * Maps JPA entity to domain MarketInstrument.
     * Note: Since MarketInstrument is immutable, we need to use factory methods
     */
    default MarketInstrument toDomain(MarketInstrumentEntity entity) {
        MarketInstrument instrument = new MarketInstrument(
            entity.getSymbol(),
            entity.getName(),
            entity.getBaseCurrency(),
            entity.getQuoteCurrency()
        );
        // Quality metrics will be set via domain operations
        return instrument;
    }
    
    /**
     * Maps domain OHLCV to BTC-specific JPA entity.
     */
    default BtcPriceEntity toBtcEntity(OHLCV ohlcv) {
        if (ohlcv == null) return null;
        
        return BtcPriceEntity.builder()
            .timestamp(ohlcv.timestamp())
            .openPrice(priceToAmount(ohlcv.open()))
            .highPrice(priceToAmount(ohlcv.high()))
            .lowPrice(priceToAmount(ohlcv.low()))
            .closePrice(priceToAmount(ohlcv.close()))
            .volume(ohlcv.volume())
            .currency("USD")
            .build();
    }
    
    /**
     * Maps BTC entity back to domain OHLCV.
     */
    default OHLCV fromBtcEntity(BtcPriceEntity entity) {
        if (entity == null) return null;
        
        return new OHLCV(
            amountToPrice(entity.getOpenPrice()),
            amountToPrice(entity.getHighPrice()),
            amountToPrice(entity.getLowPrice()),
            amountToPrice(entity.getClosePrice()),
            entity.getVolume(),
            entity.getTimestamp()
        );
    }
    
    /**
     * Maps domain OHLCV to ETH-specific JPA entity.
     */
    default EthPriceEntity toEthEntity(OHLCV ohlcv) {
        if (ohlcv == null) return null;
        
        return EthPriceEntity.builder()
            .timestamp(ohlcv.timestamp())
            .openPrice(priceToAmount(ohlcv.open()))
            .highPrice(priceToAmount(ohlcv.high()))
            .lowPrice(priceToAmount(ohlcv.low()))
            .closePrice(priceToAmount(ohlcv.close()))
            .volume(ohlcv.volume())
            .currency("USD")
            .build();
    }
    
    /**
     * Maps ETH entity back to domain OHLCV.
     */
    default OHLCV fromEthEntity(EthPriceEntity entity) {
        if (entity == null) return null;
        
        return new OHLCV(
            amountToPrice(entity.getOpenPrice()),
            amountToPrice(entity.getHighPrice()),
            amountToPrice(entity.getLowPrice()),
            amountToPrice(entity.getClosePrice()),
            entity.getVolume(),
            entity.getTimestamp()
        );
    }
    
    // ========== Domain ↔ DTO Mappings ==========
    
    /**
     * Creates InstrumentDataSummary from domain MarketInstrument.
     */
    default MarketDataResponse.InstrumentDataSummary toInstrumentDataSummary(MarketInstrument instrument) {
        if (instrument == null) return null;
        
        return MarketDataResponse.InstrumentDataSummary.success(
            instrument.getSymbol(),
            instrument.getName(),
            instrument.getQualityMetrics() != null ? 
                instrument.getQualityMetrics().totalDataPoints() : 0,
            instrument.getQualityMetrics(),
            getEarliestTimestamp(instrument.getPriceHistory()),
            getLatestTimestamp(instrument.getPriceHistory())
        );
    }
    
    // ========== List Mappings ==========
    
    default List<OHLCV> fromBtcEntities(List<BtcPriceEntity> entities) {
        if (entities == null) return null;
        return entities.stream()
            .map(this::fromBtcEntity)
            .collect(java.util.stream.Collectors.toList());
    }
    
    default List<OHLCV> fromEthEntities(List<EthPriceEntity> entities) {
        if (entities == null) return null;
        return entities.stream()
            .map(this::fromEthEntity)
            .collect(java.util.stream.Collectors.toList());
    }
    
    default List<BtcPriceEntity> toBtcEntities(List<OHLCV> ohlcvList) {
        if (ohlcvList == null) return null;
        return ohlcvList.stream()
            .map(this::toBtcEntity)
            .collect(java.util.stream.Collectors.toList());
    }
    
    default List<EthPriceEntity> toEthEntities(List<OHLCV> ohlcvList) {
        if (ohlcvList == null) return null;
        return ohlcvList.stream()
            .map(this::toEthEntity)
            .collect(java.util.stream.Collectors.toList());
    }
    
    // ========== Custom Mapping Methods ==========
    
    /**
     * Creates DataQualityMetrics from entity data.
     */
    default DataQualityMetrics createQualityMetrics(MarketInstrumentEntity entity) {
        if (entity.getDataPointCount() == null || entity.getDataPointCount() <= 0) {
            return new DataQualityMetrics(
                0,                              // totalDataPoints
                0,                              // missingDataPoints  
                0,                              // duplicateDataPoints
                0.0,                            // completenessPercentage
                Instant.now(),                  // lastUpdated
                entity.getDataSource() != null ? entity.getDataSource() : "UNKNOWN" // dataSource
            );
        }
        
        int totalDataPoints = entity.getDataPointCount();
        double completeness = calculateCompleteness(totalDataPoints);
        int expectedDataPoints = (int) java.time.temporal.ChronoUnit.DAYS.between(
            java.time.LocalDate.of(2021, 3, 15),
            java.time.LocalDate.now()
        );
        int missingDataPoints = Math.max(0, expectedDataPoints - totalDataPoints);
        
        return new DataQualityMetrics(
            totalDataPoints,                    // totalDataPoints
            missingDataPoints,                  // missingDataPoints
            0,                                  // duplicateDataPoints (assume 0 for now)
            completeness * 100.0,               // completenessPercentage (convert to percentage)
            entity.getAuditInfo() != null ? entity.getAuditInfo().getUpdatedAt() : Instant.now(), // lastUpdated
            entity.getDataSource() != null ? entity.getDataSource() : "BYBIT" // dataSource
        );
    }
    
    /**
     * Calculates data completeness based on expected vs actual data points.
     * For daily data, we expect ~365 points per year.
     */
    default Double calculateCompleteness(Integer dataPointCount) {
        if (dataPointCount == null || dataPointCount <= 0) {
            return 0.0;
        }
        
        // Assuming we expect data from March 15, 2021 to now
        long daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(
            java.time.LocalDate.of(2021, 3, 15),
            java.time.LocalDate.now()
        );
        
        if (daysSinceStart <= 0) {
            return 1.0; // Perfect completeness for edge case
        }
        
        double completeness = (double) dataPointCount / daysSinceStart;
        return Math.min(1.0, completeness); // Cap at 100%
    }
    
    /**
     * Validates OHLCV data before mapping to entity.
     * Ensures data integrity for financial calculations.
     */
    default boolean isValidForPersistence(OHLCV ohlcv) {
        return ohlcv != null &&
               ohlcv.timestamp() != null &&
               ohlcv.open() != null && !ohlcv.open().isZero() &&
               ohlcv.high() != null && !ohlcv.high().isZero() &&
               ohlcv.low() != null && !ohlcv.low().isZero() &&
               ohlcv.close() != null && !ohlcv.close().isZero() &&
               ohlcv.volume() != null && ohlcv.volume().compareTo(BigDecimal.ZERO) >= 0;
    }
    
    /**
     * Extracts earliest timestamp from price history.
     */
    default Instant getEarliestTimestamp(List<OHLCV> priceHistory) {
        if (priceHistory == null || priceHistory.isEmpty()) {
            return null;
        }
        return priceHistory.stream()
            .map(OHLCV::timestamp)
            .min(Instant::compareTo)
            .orElse(null);
    }
    
    /**
     * Extracts latest timestamp from price history.
     */
    default Instant getLatestTimestamp(List<OHLCV> priceHistory) {
        if (priceHistory == null || priceHistory.isEmpty()) {
            return null;
        }
        return priceHistory.stream()
            .map(OHLCV::timestamp)
            .max(Instant::compareTo)
            .orElse(null);
    }
    
    /**
     * Converts Price to BigDecimal amount for entity storage.
     */
    default BigDecimal priceToAmount(Price price) {
        return price != null ? price.amount() : null;
    }
    
    /**
     * Converts BigDecimal amount to Price object for domain use.
     */
    default Price amountToPrice(BigDecimal amount) {
        return amount != null ? Price.usd(amount.toString()) : null;
    }
}