package com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories;

import com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.ForecastResultEntity;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ForecastResultEntity.
 * Provides methods to store and retrieve ARIMA forecast results.
 */
@Repository
public interface ForecastResultRepository extends JpaRepository<ForecastResultEntity, Long> {
    
    /**
     * Find forecast result by instrument, date, and model version
     */
    Optional<ForecastResultEntity> findByInstrumentAndForecastDateAndArimaModelVersion(
        TradingInstrument instrument, LocalDate forecastDate, String arimaModelVersion);
    
    /**
     * Find all forecast results for a specific instrument and date range
     */
    List<ForecastResultEntity> findByInstrumentAndForecastDateBetweenOrderByForecastDateAsc(
        TradingInstrument instrument, LocalDate startDate, LocalDate endDate);
    
    /**
     * Find all forecast results for a specific model version
     */
    List<ForecastResultEntity> findByArimaModelVersionOrderByForecastDateAsc(String arimaModelVersion);
    
    /**
     * Find latest forecast result for current date prediction
     */
    @Query("SELECT f FROM ForecastResultEntity f WHERE f.instrument = :instrument " +
           "AND f.isCurrentDatePrediction = true " +
           "ORDER BY f.createdAt DESC")
    Optional<ForecastResultEntity> findLatestCurrentDateForecast(@Param("instrument") TradingInstrument instrument);
    
    /**
     * Find forecast results by execution ID
     */
    List<ForecastResultEntity> findByExecutionId(String executionId);
    
    /**
     * Check if forecast exists for given parameters
     */
    boolean existsByInstrumentAndForecastDateAndArimaModelVersion(
        TradingInstrument instrument, LocalDate forecastDate, String arimaModelVersion);
}