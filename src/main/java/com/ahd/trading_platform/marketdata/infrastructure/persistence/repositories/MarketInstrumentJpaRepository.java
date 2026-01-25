package com.ahd.trading_platform.marketdata.infrastructure.persistence.repositories;

import com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.MarketInstrumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * JPA Repository for MarketInstrumentEntity.
 * Provides standard CRUD operations and custom queries.
 */
@Repository
public interface MarketInstrumentJpaRepository extends JpaRepository<MarketInstrumentEntity, Long> {
    
    /**
     * Finds a market instrument by symbol (case-insensitive)
     */
    Optional<MarketInstrumentEntity> findBySymbolIgnoreCase(String symbol);
    
    /**
     * Checks if a market instrument exists by symbol
     */
    boolean existsBySymbolIgnoreCase(String symbol);
    
    /**
     * Finds instruments by base currency
     */
    @Query("SELECT mi FROM MarketInstrumentEntity mi WHERE mi.baseCurrency = :baseCurrency ORDER BY mi.symbol")
    java.util.List<MarketInstrumentEntity> findByBaseCurrency(@Param("baseCurrency") String baseCurrency);
    
    /**
     * Finds instruments by quote currency
     */
    @Query("SELECT mi FROM MarketInstrumentEntity mi WHERE mi.quoteCurrency = :quoteCurrency ORDER BY mi.symbol")
    java.util.List<MarketInstrumentEntity> findByQuoteCurrency(@Param("quoteCurrency") String quoteCurrency);

    /**
     * Finds instruments by market code
     */
    @Query("SELECT mi FROM MarketInstrumentEntity mi WHERE mi.market.code = :marketCode ORDER BY mi.symbol")
    java.util.List<MarketInstrumentEntity> findByMarketCode(@Param("marketCode") String marketCode);

    /**
     * Finds instruments by market ID (optimized using FK index)
     */
    java.util.List<MarketInstrumentEntity> findByMarket_Id(Long marketId);

    /**
     * Finds instruments with quality score above threshold
     */
    @Query("SELECT mi FROM MarketInstrumentEntity mi WHERE mi.qualityScore >= :minQuality ORDER BY mi.qualityScore DESC")
    java.util.List<MarketInstrumentEntity> findByQualityScoreGreaterThanEqual(@Param("minQuality") Double minQuality);
    
    /**
     * Finds instruments with data point count above threshold
     */
    @Query("SELECT mi FROM MarketInstrumentEntity mi WHERE mi.dataPointCount >= :minDataPoints ORDER BY mi.dataPointCount DESC")
    java.util.List<MarketInstrumentEntity> findByDataPointCountGreaterThanEqual(@Param("minDataPoints") Integer minDataPoints);
    
    /**
     * Gets the total number of data points across all instruments
     */
    @Query("SELECT COALESCE(SUM(mi.dataPointCount), 0) FROM MarketInstrumentEntity mi")
    Long getTotalDataPointCount();

    /**
     * Finds instruments that have first trading date set
     */
    @Query("SELECT mi FROM MarketInstrumentEntity mi WHERE mi.firstTradingDate IS NOT NULL ORDER BY mi.firstTradingDate")
    java.util.List<MarketInstrumentEntity> findInstrumentsWithFirstTradingDate();

    /**
     * Finds instruments that started trading after a specific date
     */
    @Query("SELECT mi FROM MarketInstrumentEntity mi WHERE mi.firstTradingDate >= :startDate ORDER BY mi.firstTradingDate")
    java.util.List<MarketInstrumentEntity> findInstrumentsStartingAfter(@Param("startDate") Instant startDate);

    /**
     * Finds instruments that started trading before a specific date
     */
    @Query("SELECT mi FROM MarketInstrumentEntity mi WHERE mi.firstTradingDate <= :endDate ORDER BY mi.firstTradingDate")
    java.util.List<MarketInstrumentEntity> findInstrumentsStartingBefore(@Param("endDate") Instant endDate);

    /**
     * Finds instruments that started trading within a date range
     */
    @Query("SELECT mi FROM MarketInstrumentEntity mi WHERE mi.firstTradingDate BETWEEN :startDate AND :endDate ORDER BY mi.firstTradingDate")
    java.util.List<MarketInstrumentEntity> findInstrumentsStartingBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    /**
     * Gets the earliest first trading date across all instruments
     */
    @Query("SELECT MIN(mi.firstTradingDate) FROM MarketInstrumentEntity mi WHERE mi.firstTradingDate IS NOT NULL")
    Optional<Instant> getEarliestFirstTradingDate();

    /**
     * Gets the latest first trading date across all instruments
     */
    @Query("SELECT MAX(mi.firstTradingDate) FROM MarketInstrumentEntity mi WHERE mi.firstTradingDate IS NOT NULL")
    Optional<Instant> getLatestFirstTradingDate();
}