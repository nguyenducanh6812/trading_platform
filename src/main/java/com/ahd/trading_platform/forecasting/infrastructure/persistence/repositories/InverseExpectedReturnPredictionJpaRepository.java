package com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories;

import com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.InverseExpectedReturnPredictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for INVERSE market expected return predictions.
 * Supports multiple trading symbols in the INVERSE perpetual market.
 */
@Repository
public interface InverseExpectedReturnPredictionJpaRepository extends JpaRepository<InverseExpectedReturnPredictionEntity, Long> {

    /**
     * Find predictions for a specific symbol within a date range
     */
    @Query("SELECT p FROM InverseExpectedReturnPredictionEntity p " +
           "WHERE p.symbol = :symbol AND p.forecastDate >= :startDate AND p.forecastDate <= :endDate " +
           "ORDER BY p.forecastDate DESC")
    List<InverseExpectedReturnPredictionEntity> findBySymbolAndForecastDateBetween(
        @Param("symbol") String symbol,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate);

    /**
     * Find predictions by symbol and model version
     */
    @Query("SELECT p FROM InverseExpectedReturnPredictionEntity p " +
           "WHERE p.symbol = :symbol AND p.modelVersion = :modelVersion " +
           "ORDER BY p.forecastDate DESC")
    List<InverseExpectedReturnPredictionEntity> findBySymbolAndModelVersion(
        @Param("symbol") String symbol,
        @Param("modelVersion") String modelVersion);

    /**
     * Find prediction for specific symbol, date and model version
     */
    Optional<InverseExpectedReturnPredictionEntity> findBySymbolAndForecastDateAndModelVersion(
        String symbol, Instant forecastDate, String modelVersion);

    /**
     * Find latest prediction for a symbol and model version
     */
    @Query("SELECT p FROM InverseExpectedReturnPredictionEntity p " +
           "WHERE p.symbol = :symbol AND p.modelVersion = :modelVersion " +
           "ORDER BY p.forecastDate DESC " +
           "LIMIT 1")
    Optional<InverseExpectedReturnPredictionEntity> findLatestBySymbolAndModelVersion(
        @Param("symbol") String symbol,
        @Param("modelVersion") String modelVersion);

    /**
     * Find successful predictions only for a specific symbol
     */
    @Query("SELECT p FROM InverseExpectedReturnPredictionEntity p " +
           "WHERE p.symbol = :symbol AND p.predictionStatus = 'SUCCESS' " +
           "ORDER BY p.forecastDate DESC")
    List<InverseExpectedReturnPredictionEntity> findSuccessfulPredictionsBySymbol(@Param("symbol") String symbol);

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
    List<InverseExpectedReturnPredictionEntity> findBySymbolAndModelVersionAndPredictionStatusAndForecastDateBetweenOrderByForecastDate(
        String symbol, String modelVersion, String predictionStatus, Instant startDate, Instant endDate);

    /**
     * Find predictions by execution ID
     */
    List<InverseExpectedReturnPredictionEntity> findByExecutionId(String executionId);

    /**
     * Delete predictions for a specific symbol older than specified date
     */
    void deleteBySymbolAndCreatedAtBefore(String symbol, Instant cutoffDate);

    /**
     * Check if prediction exists for symbol, date and model version
     */
    boolean existsBySymbolAndForecastDateAndModelVersion(String symbol, Instant forecastDate, String modelVersion);

    /**
     * Finds all distinct symbols in INVERSE predictions
     */
    @Query("SELECT DISTINCT p.symbol FROM InverseExpectedReturnPredictionEntity p ORDER BY p.symbol")
    List<String> findAllDistinctSymbols();
}
