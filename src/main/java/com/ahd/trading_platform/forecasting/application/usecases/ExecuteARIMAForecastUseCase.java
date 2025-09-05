package com.ahd.trading_platform.forecasting.application.usecases;

import com.ahd.trading_platform.forecasting.application.dto.ForecastRequest;
import com.ahd.trading_platform.forecasting.application.dto.ForecastResponse;
import com.ahd.trading_platform.forecasting.application.dto.ForecastExecutionMetrics;
import com.ahd.trading_platform.forecasting.application.dto.CalculationStepDto;
import com.ahd.trading_platform.forecasting.application.services.ForecastResultPersistenceService;
import com.ahd.trading_platform.forecasting.domain.entities.ARIMAModel;
import com.ahd.trading_platform.forecasting.domain.valueobjects.DemeanDiffOCMasterData;
import com.ahd.trading_platform.forecasting.domain.valueobjects.*;
import com.ahd.trading_platform.forecasting.infrastructure.repositories.ARIMAModelRepository;
import com.ahd.trading_platform.marketdata.application.ports.MarketDataPort;
import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Use case for executing ARIMA forecasts.
 * Orchestrates the complete forecasting workflow including data retrieval,
 * model loading, calculation execution, and result persistence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExecuteARIMAForecastUseCase {
    
    private final CalculateDemeanDiffOCUseCase calculateDemeanDiffOCUseCase;
    private final ApplyARIMAModelUseCase applyARIMAModelUseCase;
    private final ARIMAModelRepository arimaModelRepository;
    private final MarketDataPort marketDataPort;
    private final ForecastResultPersistenceService persistenceService;
    
    /**
     * Executes ARIMA forecast for the specified request
     */
    public ForecastResponse execute(ForecastRequest request, String arimaModelVersion) {
        String executionId = UUID.randomUUID().toString();
        
        try {
            log.info("Starting ARIMA forecast execution {} for instrument {} (mode: {})", 
                executionId, request.instrumentCode(), request.isCurrentDateMode() ? "current" : "backtest");
            
            // Validate and parse instrument
            TradingInstrument instrument = TradingInstrument.fromCode(request.instrumentCode());
            
            // Use provided ARIMA model version
            
            // Check if we already have predictions for the requested date range
            LocalDate targetDate;
            String cacheDescription;
            
            if (request.isCurrentDateMode()) {
                targetDate = LocalDate.now();
                cacheDescription = "Cached prediction for today";
            } else {
                // For backtesting mode, check if we have prediction for the end date
                targetDate = LocalDate.parse(request.endDate());
                cacheDescription = "Cached prediction from previous backtesting";
            }
            
            if (persistenceService.forecastExists(instrument, targetDate, arimaModelVersion)) {
                log.info("Forecast already exists for {} on {} with model version {} (mode: {})", 
                    instrument, targetDate, arimaModelVersion, request.isCurrentDateMode() ? "current" : "backtest");
                
                var existingResult = persistenceService.getForecastResult(instrument, targetDate, arimaModelVersion);
                if (existingResult.isPresent()) {
                    return ForecastResponse.success(
                        executionId,
                        request.instrumentCode(),
                        existingResult.get().getExpectedReturn().doubleValue(),
                        existingResult.get().getConfidenceLevel().doubleValue(),
                        targetDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                        cacheDescription,
                        null // No metrics for cached results
                    );
                }
            }
            
            // Determine prediction time range (dates for which we want forecasts)
            TimeRange predictionRange = determineTimeRange(request);
            
            // Calculate required historical data range for ARIMA calculation
            // We need at least AR_ORDER (30) days of historical data before the prediction period
            TimeRange historicalDataRange = calculateHistoricalDataRange(predictionRange);
            
            // Retrieve historical price data from Market Data module
            List<OHLCV> priceData = marketDataPort.getHistoricalData(instrument, historicalDataRange);
            if (priceData.isEmpty()) {
                return ForecastResponse.failure(executionId, request.instrumentCode(), 
                    "No historical price data available for the required time range");
            }
            
            // Validate data sufficiency for ARIMA calculation
            if (!hassufficientDataForArima(priceData, predictionRange, 30)) {
                return ForecastResponse.failure(executionId, request.instrumentCode(), 
                    "Insufficient historical data for ARIMA calculation. Need at least 30 days of data before prediction period.");
            }
            
            // Load ARIMA model for the instrument
            ARIMAModel arimaModel = arimaModelRepository.findByInstrument(instrument)
                .orElseThrow(() -> new IllegalStateException(
                    "ARIMA model not found for instrument: " + instrument.getCode()));
            
            // Step 1: Ensure DemeanDiffOC master data exists
            log.debug("Checking for DemeanDiffOC master data for {} in time range {} - {}", 
                instrument.getCode(), historicalDataRange.from(), historicalDataRange.to());
            
            List<DemeanDiffOCMasterData> masterData;
            if (calculateDemeanDiffOCUseCase.masterDataExists(instrument, historicalDataRange)) {
                log.debug("Master data exists, loading from repository");
                masterData = calculateDemeanDiffOCUseCase.getMasterData(instrument, historicalDataRange);
            } else {
                log.info("Master data not found, calculating and storing for {} data points", priceData.size());
                masterData = calculateDemeanDiffOCUseCase.calculateAndStore(instrument, priceData);
            }
            
            // Step 2: Apply ARIMA model using master data
            log.debug("Applying ARIMA model using {} master data points", masterData.size());
            ForecastResult result = applyARIMAModelUseCase.applyModel(instrument, masterData, arimaModel);
            
            // Convert domain result to response DTO
            ForecastResponse response = convertToResponse(executionId, result, request.shouldIncludeCalculationDetails());
            
            // Store the forecast result in the database
            LocalDate forecastDate = request.isCurrentDateMode() ? LocalDate.now() : 
                LocalDate.parse(request.endDate()); // For backtesting, use end date as forecast date
            
            persistenceService.storeForecastResult(
                instrument, 
                forecastDate, 
                response, 
                arimaModelVersion, 
                executionId, 
                request.isCurrentDateMode()
            );

            log.info("ARIMA forecast execution {} completed successfully for {}: expected return = {}%, stored with model version {}",
                    executionId, request.instrumentCode(), String.format("%.4f", response.getExpectedReturnPercent()), arimaModelVersion);
            
            return response;
            
        } catch (Exception e) {
            log.error("ARIMA forecast execution {} failed for {}: {}", executionId, request.instrumentCode(), e.getMessage(), e);
            return ForecastResponse.failure(executionId, request.instrumentCode(), e.getMessage());
        }
    }
    
    /**
     * Executes forecast synchronously and returns result immediately
     */
    public ForecastResponse executeSync(ForecastRequest request, String arimaModelVersion) {
        return execute(request, arimaModelVersion);
    }
    
    /**
     * Executes forecast with current model version (for backward compatibility)
     */
    public ForecastResponse execute(ForecastRequest request) {
        String currentModelVersion = persistenceService.getCurrentArimaModelVersion();
        return execute(request, currentModelVersion);
    }
    
    private TimeRange determineTimeRange(ForecastRequest request) {
        if (request.isCurrentDateMode()) {
            // Current date mode: Only predict for TODAY
            LocalDate today = LocalDate.now();
            
            return new TimeRange(today.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                                   today.atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
        } else {
            // Backtesting mode: Use provided date range (prediction period only)
            if (request.startDate() == null || request.endDate() == null) {
                throw new IllegalArgumentException("Start date and end date are required for backtesting mode");
            }
            
            LocalDate startDate = LocalDate.parse(request.startDate());
            LocalDate endDate = LocalDate.parse(request.endDate());
            
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("Start date must be before end date");
            }
            
            return new TimeRange(startDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                                   endDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
        }
    }
    
    /**
     * Calculate the historical data range needed for ARIMA calculation
     */
    private TimeRange calculateHistoricalDataRange(TimeRange predictionRange) {
        // Get the start and end of prediction period
        LocalDate predictionStart = predictionRange.from().atOffset(java.time.ZoneOffset.UTC).toLocalDate();
        LocalDate predictionEnd = predictionRange.to().atOffset(java.time.ZoneOffset.UTC).toLocalDate();
        
        // For the first prediction (predictionStart), we need arOrder days before it
        LocalDate historicalStart = predictionStart.minusDays(30);
        
        // For the last prediction (predictionEnd), we need data up to the day before it
        // This means historical data should extend to predictionEnd - 1
        LocalDate historicalEnd = predictionEnd.minusDays(1);
        
        return new TimeRange(
            historicalStart.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
            historicalEnd.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)
        );
    }
    
    /**
     * Check if we have sufficient historical data for ARIMA calculation
     */
    private boolean hassufficientDataForArima(List<OHLCV> priceData, TimeRange predictionRange, int arOrder) {
        if (priceData.size() < arOrder) {
            return false;
        }
        
        // Get prediction start date
        LocalDate predictionStart = predictionRange.from().atOffset(java.time.ZoneOffset.UTC).toLocalDate();
        
        // Count how many data points we have before the prediction period
        long historicalDataPoints = priceData.stream()
            .mapToLong(ohlcv -> ohlcv.timestamp().atOffset(java.time.ZoneOffset.UTC).toLocalDate().toEpochDay())
            .filter(epochDay -> epochDay < predictionStart.toEpochDay())
            .distinct()
            .count();
        
        return historicalDataPoints >= arOrder;
    }
    
    private ForecastResponse convertToResponse(String executionId, ForecastResult result, boolean includeCalculationDetails) {
        // Convert metrics
        ForecastExecutionMetrics metricsDto = new ForecastExecutionMetrics(
            result.metrics().dataPointsUsed(),
            result.metrics().arOrder(),
            result.metrics().meanSquaredError(),
            result.metrics().standardError(),
            result.metrics().getExecutionTimeMs(),
            result.metrics().dataRangeStart(),
            result.metrics().dataRangeEnd(),
            result.metrics().modelVersion(),
            result.metrics().hasSufficientQuality()
        );
        
        // Convert calculation steps if requested
        List<CalculationStepDto> calculationSteps = null;
        if (includeCalculationDetails) {
            calculationSteps = result.calculations().stream()
                .map(this::convertCalculationStep)
                .toList();
        }
        
        // Create appropriate response based on whether details are included
        if (includeCalculationDetails && calculationSteps != null) {
            return ForecastResponse.successWithDetails(
                executionId,
                result.instrument().getCode(),
                result.expectedReturn(),
                result.confidenceLevel(),
                result.forecastDate(),
                result.getSummary(),
                metricsDto,
                calculationSteps
            );
        } else {
            return ForecastResponse.success(
                executionId,
                result.instrument().getCode(),
                result.expectedReturn(),
                result.confidenceLevel(),
                result.forecastDate(),
                result.getSummary(),
                metricsDto
            );
        }
    }
    
    private CalculationStepDto convertCalculationStep(TimeSeriesCalculation calculation) {
        return new CalculationStepDto(
            calculation.getCurrentStep(),
            calculation.timestamp(),
            calculation.openPrice(),
            calculation.closePrice(),
            calculation.oc(),
            calculation.diffOC(),
            calculation.demeanDiffOC(),
            calculation.arLags(),
            calculation.predictedDiffOC(),
            calculation.predictedOC(),
            calculation.predictedReturn()
        );
    }
    
}