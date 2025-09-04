package com.ahd.trading_platform.marketdata.infrastructure.persistence.repositories;

import com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.MarketInstrumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}