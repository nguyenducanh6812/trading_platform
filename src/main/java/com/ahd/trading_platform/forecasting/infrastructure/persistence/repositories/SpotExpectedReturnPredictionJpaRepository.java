package com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories;

import com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.SpotExpectedReturnPredictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for SPOT market expected return predictions.
 * Supports multiple trading symbols in the SPOT market.
 */
@Repository
public interface SpotExpectedReturnPredictionJpaRepository extends JpaRepository<SpotExpectedReturnPredictionEntity, Long> {

    /**
     * Find predictions for a specific symbol within a date range
     */
    @Query("SELECT p FROM SpotExpectedReturnPredictionEntity p " +
           "WHERE p.symbol = :symbol AND p.forecastDate >= :startDate AND p.forecastDate <= :endDate " +
           "ORDER BY p.forecastDate DESC")
    List<SpotExpectedReturnPredictionEntity> findBySymbolAndForecastDateBetween(
        @Param("symbol") String symbol,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate);

    /**
     * Find predictions by symbol and model version
     */
    @Query("SELECT p FROM SpotExpectedReturnPredictionEntity p " +
           "WHERE p.symbol = :symbol AND p.modelVersion = :modelVersion " +
           "ORDER BY p.forecastDate DESC")
    List<SpotExpectedReturnPredictionEntity> findBySymbolAndModelVersion(
        @Param("symbol") String symbol,
        @Param("modelVersion") String modelVersion);

    /**
     * Find prediction for specific symbol, date and model version
     */
    Optional<SpotExpectedReturnPredictionEntity> findBySymbolAndForecastDateAndModelVersion(
        String symbol, Instant forecastDate, String modelVersion);

    /**
     * Find latest prediction for a symbol and model version
     */
    @Query("SELECT p FROM SpotExpectedReturnPredictionEntity p " +
           "WHERE p.symbol = :symbol AND p.modelVersion = :modelVersion " +
           "ORDER BY p.forecastDate DESC " +
           "LIMIT 1")
    Optional<SpotExpectedReturnPredictionEntity> findLatestBySymbolAndModelVersion(
        @Param("symbol") String symbol,
        @Param("modelVersion") String modelVersion);

    /**
     * Find successful predictions only for a specific symbol
     */
    @Query("SELECT p FROM SpotExpectedReturnPredictionEntity p " +
           "WHERE p.symbol = :symbol AND p.predictionStatus = 'SUCCESS' " +
           "ORDER BY p.forecastDate DESC")
    List<SpotExpectedReturnPredictionEntity> findSuccessfulPredictionsBySymbol(@Param("symbol") String symbol);

    /**
     * Count predictions by symbol and model version
     */
    long countBySymbolAndModelVersion(String symbol, String modelVersion);

    /**
     * Count successful predictions by symbol and model version within date range
     */
    long countBySymbolAndModelVersionAndPredictionStatusAndForecastDateBetween(
        String symbol, String modelVersion, String predictionStatus, Instant startDate, Instant endDate);

    /**
     * Find successful predictions by symbol and model version within date range
     */
    List<SpotExpectedReturnPredictionEntity> findBySymbolAndModelVersionAndPredictionStatusAndForecastDateBetweenOrderByForecastDate(
        String symbol, String modelVersion, String predictionStatus, Instant startDate, Instant endDate);

    /**
     * Find predictions by execution ID
     */
    List<SpotExpectedReturnPredictionEntity> findByExecutionId(String executionId);

    /**
     * Delete predictions for a specific symbol older than specified date
     */
    void deleteBySymbolAndCreatedAtBefore(String symbol, Instant cutoffDate);

    /**
     * Check if prediction exists for symbol, date and model version
     */
    boolean existsBySymbolAndForecastDateAndModelVersion(String symbol, Instant forecastDate, String modelVersion);

    /**
     * Finds all distinct symbols in SPOT predictions
     */
    @Query("SELECT DISTINCT p.symbol FROM SpotExpectedReturnPredictionEntity p ORDER BY p.symbol")
    List<String> findAllDistinctSymbols();
}
