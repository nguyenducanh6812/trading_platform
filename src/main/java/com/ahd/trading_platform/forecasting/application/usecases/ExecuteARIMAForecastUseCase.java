package com.ahd.trading_platform.forecasting.application.usecases;

import com.ahd.trading_platform.forecasting.application.dto.ForecastRequest;
import com.ahd.trading_platform.forecasting.application.dto.ForecastResponse;
import com.ahd.trading_platform.forecasting.application.dto.ForecastExecutionMetrics;
import com.ahd.trading_platform.forecasting.application.dto.CalculationStepDto;
import com.ahd.trading_platform.forecasting.domain.entities.ARIMAModel;
import com.ahd.trading_platform.forecasting.domain.services.ARIMACalculationService;
import com.ahd.trading_platform.forecasting.domain.valueobjects.*;
import com.ahd.trading_platform.forecasting.infrastructure.repositories.ARIMAModelRepository;
import com.ahd.trading_platform.marketdata.application.ports.MarketDataPort;
import com.ahd.trading_platform.marketdata.domain.valueobjects.OHLCV;
import com.ahd.trading_platform.marketdata.domain.valueobjects.TimeRange;
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
    
    private final ARIMACalculationService arimaCalculationService;
    private final ARIMAModelRepository arimaModelRepository;
    private final MarketDataPort marketDataPort;
    
    /**
     * Executes ARIMA forecast for the specified request
     */
    public ForecastResponse execute(ForecastRequest request) {
        String executionId = UUID.randomUUID().toString();
        
        try {
            log.info("Starting ARIMA forecast execution {} for instrument {}", executionId, request.instrumentCode());
            
            // Validate and parse instrument
            TradingInstrument instrument = TradingInstrument.fromCode(request.instrumentCode());
            
            // Determine date range for historical data
            TimeRange timeRange = determineTimeRange(request);
            
            // Retrieve historical price data from Market Data module
            // Convert shared TradingInstrument to marketdata TradingInstrument for API compatibility
            com.ahd.trading_platform.marketdata.domain.valueobjects.TradingInstrument marketDataInstrument = 
                convertToMarketDataInstrument(instrument);
            List<OHLCV> priceData = marketDataPort.getHistoricalData(marketDataInstrument, timeRange);
            if (priceData.isEmpty()) {
                return ForecastResponse.failure(executionId, request.instrumentCode(), 
                    "No historical price data available for the specified date range");
            }
            
            // Load ARIMA model for the instrument
            ARIMAModel arimaModel = arimaModelRepository.findByInstrument(instrument)
                .orElseThrow(() -> new IllegalStateException(
                    "ARIMA model not found for instrument: " + instrument.getCode()));
            
            // Execute ARIMA calculation
            ForecastResult result = arimaCalculationService.executeForecast(instrument, priceData, arimaModel);
            
            // Convert domain result to response DTO
            ForecastResponse response = convertToResponse(executionId, result, request.shouldIncludeCalculationDetails());
            
            log.info("ARIMA forecast execution {} completed successfully for {}: expected return = {:.4f}%", 
                executionId, request.instrumentCode(), response.getExpectedReturnPercent());
            
            return response;
            
        } catch (Exception e) {
            log.error("ARIMA forecast execution {} failed for {}: {}", executionId, request.instrumentCode(), e.getMessage(), e);
            return ForecastResponse.failure(executionId, request.instrumentCode(), e.getMessage());
        }
    }
    
    /**
     * Executes forecast synchronously and returns result immediately
     */
    public ForecastResponse executeSync(ForecastRequest request) {
        return execute(request);
    }
    
    private TimeRange determineTimeRange(ForecastRequest request) {
        if (request.shouldUseDefaultRange()) {
            // Use default range for historical data (last 2 years)
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusYears(2);
            return new TimeRange(startDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                                   endDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
        } else {
            // Parse provided date range
            if (request.startDate() == null || request.endDate() == null) {
                throw new IllegalArgumentException("Start date and end date are required when not using default range");
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
    
    /**
     * Converts shared TradingInstrument to marketdata module's TradingInstrument for API compatibility.
     * This is a temporary conversion until all modules adopt the shared kernel.
     */
    private com.ahd.trading_platform.marketdata.domain.valueobjects.TradingInstrument convertToMarketDataInstrument(
            TradingInstrument sharedInstrument) {
        return switch (sharedInstrument) {
            case BTC -> com.ahd.trading_platform.marketdata.domain.valueobjects.TradingInstrument.BTC;
            case ETH -> com.ahd.trading_platform.marketdata.domain.valueobjects.TradingInstrument.ETH;
        };
    }
}