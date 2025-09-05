package com.ahd.trading_platform.forecasting.application.services;

import com.ahd.trading_platform.forecasting.application.dto.ForecastResponse;
import com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.ForecastResultEntity;
import com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories.ForecastResultRepository;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for persisting ARIMA forecast results to the database.
 * Handles storage, retrieval, and caching of forecast predictions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ForecastResultPersistenceService {
    
    private final ForecastResultRepository forecastResultRepository;
    
    /**
     * Store forecast result in the database
     */
    public ForecastResultEntity storeForecastResult(
            TradingInstrument instrument,
            LocalDate forecastDate,
            ForecastResponse response,
            String arimaModelVersion,
            String executionId,
            boolean isCurrentDatePrediction) {
        
        log.debug("Storing forecast result for {} on {} with model version {}", 
            instrument, forecastDate, arimaModelVersion);
        
        ForecastResultEntity entity = ForecastResultEntity.builder()
            .instrument(instrument)
            .forecastDate(forecastDate)
            .expectedReturn(BigDecimal.valueOf(response.expectedReturn()))
            .confidenceLevel(BigDecimal.valueOf(response.confidenceLevel()))
            .arimaModelVersion(arimaModelVersion)
            .executionId(executionId)
            .isCurrentDatePrediction(isCurrentDatePrediction)
            .build();
        
        return forecastResultRepository.save(entity);
    }
    
    /**
     * Store multiple forecast results (for backtesting)
     */
    public List<ForecastResultEntity> storeForecastResults(
            TradingInstrument instrument,
            List<LocalDate> forecastDates,
            List<ForecastResponse> responses,
            String arimaModelVersion,
            String executionId) {
        
        log.debug("Storing {} forecast results for {} with model version {}", 
            forecastDates.size(), instrument, arimaModelVersion);
        
        List<ForecastResultEntity> entities = forecastDates.stream()
            .map(date -> {
                ForecastResponse response = responses.get(forecastDates.indexOf(date));
                return ForecastResultEntity.builder()
                    .instrument(instrument)
                    .forecastDate(date)
                    .expectedReturn(BigDecimal.valueOf(response.expectedReturn()))
                    .confidenceLevel(BigDecimal.valueOf(response.confidenceLevel()))
                    .arimaModelVersion(arimaModelVersion)
                    .executionId(executionId)
                    .isCurrentDatePrediction(false)
                    .build();
            })
            .toList();
        
        return forecastResultRepository.saveAll(entities);
    }
    
    /**
     * Check if forecast result already exists (to avoid duplicate calculations)
     */
    public boolean forecastExists(TradingInstrument instrument, LocalDate forecastDate, String arimaModelVersion) {
        return forecastResultRepository.existsByInstrumentAndForecastDateAndArimaModelVersion(
            instrument, forecastDate, arimaModelVersion);
    }
    
    /**
     * Retrieve existing forecast result
     */
    public Optional<ForecastResultEntity> getForecastResult(
            TradingInstrument instrument, LocalDate forecastDate, String arimaModelVersion) {
        return forecastResultRepository.findByInstrumentAndForecastDateAndArimaModelVersion(
            instrument, forecastDate, arimaModelVersion);
    }
    
    /**
     * Retrieve all forecast results for a model version
     */
    public List<ForecastResultEntity> getForecastResultsByModelVersion(String arimaModelVersion) {
        return forecastResultRepository.findByArimaModelVersionOrderByForecastDateAsc(arimaModelVersion);
    }
    
    /**
     * Generate current ARIMA model version (YYYYMMDD format)
     */
    public String getCurrentArimaModelVersion() {
        return LocalDate.now().toString().replace("-", ""); // e.g., "20250904"
    }
}