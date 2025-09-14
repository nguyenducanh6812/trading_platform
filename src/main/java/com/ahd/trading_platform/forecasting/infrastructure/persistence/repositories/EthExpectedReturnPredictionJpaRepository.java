package com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories;

import com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.EthExpectedReturnPredictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for ETH expected return predictions
 */
@Repository
public interface EthExpectedReturnPredictionJpaRepository extends JpaRepository<EthExpectedReturnPredictionEntity, Long> {
    
    /**
     * Find predictions within a date range
     */
    @Query("SELECT p FROM EthExpectedReturnPredictionEntity p " +
           "WHERE p.forecastDate >= :startDate AND p.forecastDate <= :endDate " +
           "ORDER BY p.forecastDate DESC")
    List<EthExpectedReturnPredictionEntity> findByForecastDateBetween(
        @Param("startDate") Instant startDate, 
        @Param("endDate") Instant endDate);
    
    /**
     * Find predictions by model version
     */
    @Query("SELECT p FROM EthExpectedReturnPredictionEntity p " +
           "WHERE p.modelVersion = :modelVersion " +
           "ORDER BY p.forecastDate DESC")
    List<EthExpectedReturnPredictionEntity> findByModelVersion(@Param("modelVersion") String modelVersion);
    
    /**
     * Find prediction for specific date and model version
     */
    Optional<EthExpectedReturnPredictionEntity> findByForecastDateAndModelVersion(
        Instant forecastDate, String modelVersion);
    
    /**
     * Find latest prediction for a model version
     */
    @Query("SELECT p FROM EthExpectedReturnPredictionEntity p " +
           "WHERE p.modelVersion = :modelVersion " +
           "ORDER BY p.forecastDate DESC " +
           "LIMIT 1")
    Optional<EthExpectedReturnPredictionEntity> findLatestByModelVersion(@Param("modelVersion") String modelVersion);
    
    /**
     * Find successful predictions only
     */
    @Query("SELECT p FROM EthExpectedReturnPredictionEntity p " +
           "WHERE p.predictionStatus = 'SUCCESS' " +
           "ORDER BY p.forecastDate DESC")
    List<EthExpectedReturnPredictionEntity> findSuccessfulPredictions();
    
    /**
     * Count predictions by model version
     */
    long countByModelVersion(String modelVersion);
    
    /**
     * Find predictions by execution ID
     */
    List<EthExpectedReturnPredictionEntity> findByExecutionId(String executionId);
    
    /**
     * Delete predictions older than specified date
     */
    void deleteByCreatedAtBefore(Instant cutoffDate);
    
    /**
     * Check if prediction exists for date and model version
     */
    boolean existsByForecastDateAndModelVersion(Instant forecastDate, String modelVersion);
}