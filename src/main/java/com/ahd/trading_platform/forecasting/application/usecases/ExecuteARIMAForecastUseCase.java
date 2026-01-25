package com.ahd.trading_platform.forecasting.application.usecases;

import com.ahd.trading_platform.forecasting.application.dto.ForecastRequest;
import com.ahd.trading_platform.forecasting.application.dto.ForecastResponse;
import com.ahd.trading_platform.forecasting.application.dto.ForecastExecutionMetrics;
import com.ahd.trading_platform.forecasting.application.dto.CalculationStepDto;
import com.ahd.trading_platform.forecasting.application.dto.BatchForecastOrchestrationResponse;
import com.ahd.trading_platform.forecasting.domain.entities.ARIMAModel;
import com.ahd.trading_platform.forecasting.domain.valueobjects.DemeanDiffOCMasterData;
import com.ahd.trading_platform.forecasting.domain.valueobjects.*;
import com.ahd.trading_platform.forecasting.domain.repositories.ARIMAModelRepository;
import com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories.ExpectedReturnPredictionRepositoryFactory;
import com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.*;
import com.ahd.trading_platform.marketdata.domain.services.MarketResolver;
import com.ahd.trading_platform.marketdata.domain.valueobjects.BybitMarketType;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

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

    private final PrepareMasterDataUseCase prepareMasterDataUseCase;
    private final ApplyARIMAModelUseCase applyARIMAModelUseCase;
    private final ARIMAModelRepository arimaModelRepository;
    private final ExpectedReturnPredictionRepositoryFactory predictionRepositoryFactory;
    private final MarketResolver marketResolver;

    /**
     * Executes ARIMA forecast for the specified request
     */
    public ForecastResponse execute(ForecastRequest request, String arimaModelVersion) {
        String executionId = UUID.randomUUID().toString();

        try {
            log.info("Starting ARIMA forecast execution {} for instrument, from-{} to-{} {} (mode: {})",
                executionId, request.instrumentCode(), request.startDate(), request.endDate(), request.isCurrentDateMode() ? "current" : "backtest");

            // Use symbol directly as String (no longer converting to TradingInstrument)
            String symbol = request.instrumentCode();

            // Load specific ARIMA model by symbol and version
            ARIMAModel arimaModel = arimaModelRepository.findBySymbolAndVersion(symbol, arimaModelVersion)
                .orElseThrow(() -> new IllegalStateException(
                    "ARIMA model not found for symbol: " + symbol +
                    " with version: " + arimaModelVersion));

            // Determine prediction time range (dates for which we want forecasts)
            TimeRange predictionRange = determineTimeRange(request);

            // Calculate required historical data range using dynamic AR order
            int arOrder = arimaModel.getPOrder();
            log.debug("Using dynamic AR order {} for ARIMA calculation", arOrder);

            TimeRange historicalDataRange = calculateHistoricalDataRange(predictionRange, arOrder);

            // Step 1: Prepare master data using reusable use case (efficient data flow)
            log.debug("Preparing DemeanDiffOC master data for {} in time range {} - {}",
                symbol, historicalDataRange.from(), historicalDataRange.to());

            List<DemeanDiffOCMasterData> masterData = prepareMasterDataUseCase.prepareMasterData(
                symbol, historicalDataRange, arOrder, arimaModel, executionId);

            // Final verification that master data meets requirements
            if (masterData.size() < arOrder) {
                return ForecastResponse.failure(executionId, symbol,
                    String.format("Insufficient master data for ARIMA calculation. Have %d points, need exactly %d (AR order). " +
                        "Unable to obtain sufficient data for time range: %s to %s",
                        masterData.size(), arOrder, historicalDataRange.from(), historicalDataRange.to()));
            }

            // Step 2: Apply ARIMA model using historical master data
            ForecastResult result;

            if (request.isCurrentDateMode()) {
                // Single date prediction for current date mode
                Instant targetPredictionDate = LocalDate.now().atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
                log.debug("Applying ARIMA model using {} historical master data points to predict for current date {}",
                    masterData.size(), targetPredictionDate);
                result = applyARIMAModelUseCase.applyModel(symbol, masterData, arimaModel, targetPredictionDate);
            } else {
                // Date range prediction for backtesting mode
                Instant startPredictionDate = LocalDate.parse(request.startDate()).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
                Instant endPredictionDate = LocalDate.parse(request.endDate()).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
                log.debug("Applying ARIMA model using {} historical master data points to predict for date range {} to {}",
                    masterData.size(), startPredictionDate, endPredictionDate);
                result = applyARIMAModelUseCase.applyModelForDateRange(symbol, masterData, arimaModel, startPredictionDate, endPredictionDate);
            }

            // Convert domain result to response DTO
            ForecastResponse response = convertToResponse(executionId, result, request.shouldIncludeCalculationDetails());

            // Store successful prediction results to database
            if (response.isSuccessful()) {
                if (request.isCurrentDateMode()) {
                    // Single prediction storage
                    storePredictionResult(response, arimaModelVersion, symbol);
                } else {
                    // Range prediction storage - store each individual prediction
                    storeRangePredictionResults(result, response, arimaModelVersion, symbol);
                }
            }

            log.info("ARIMA forecast execution {} completed successfully for {}: expected return = {}%",
                    executionId, symbol, String.format("%.4f", response.getExpectedReturnPercent()));

            return response;

        } catch (Exception e) {
            log.error("ARIMA forecast execution {} failed for {}: {}", executionId, request.instrumentCode(), e.getMessage(), e);
            ForecastResponse errorResponse = ForecastResponse.failure(executionId, request.instrumentCode(), e.getMessage());

            // Store failed prediction for tracking
            storePredictionResult(errorResponse, arimaModelVersion, request.instrumentCode());

            return errorResponse;
        }
    }

    /**
     * Executes forecast with default model version (for backward compatibility)
     */
    public ForecastResponse execute(ForecastRequest request) {
        String defaultModelVersion = "20250904"; // Default model version
        return execute(request, defaultModelVersion);
    }

    /**
     * Executes batch forecasts for multiple instruments.
     * Returns orchestration response without business data.
     * Used by Camunda workers for process orchestration.
     */
    public BatchForecastOrchestrationResponse executeBatch(
            List<String> instrumentCodes,
            String startDate,
            String endDate,
            Boolean isCurrentDate,
            Boolean includeCalculationDetails,
            String arimaModelVersion) {

        String executionId = UUID.randomUUID().toString();
        log.info("Starting batch ARIMA forecast execution {} for {} instruments, startDate-{}, endDate-{}",
                executionId, instrumentCodes.size(), startDate, endDate);

        Map<String, String> failedInstruments = new HashMap<>();
        int successfulForecasts = 0;
        boolean hasCriticalErrors = false;

        for (String instrumentCode : instrumentCodes) {
            try {
                // Create forecast request for this instrument
                ForecastRequest request = new ForecastRequest(
                    instrumentCode,
                    startDate,
                    endDate,
                    isCurrentDate != null ? isCurrentDate : false,
                    includeCalculationDetails != null ? includeCalculationDetails : false
                );

                // Execute forecast using existing single forecast logic
                ForecastResponse response = execute(request, arimaModelVersion);

                if (response.isSuccessful()) {
                    successfulForecasts++;
                    log.debug("Forecast successful for instrument: {}", instrumentCode);
                } else {
                    failedInstruments.put(instrumentCode, response.errorMessage());

                    // Check for critical errors that should fail the entire batch
                    if (isCriticalError(response.errorMessage())) {
                        hasCriticalErrors = true;
                        log.error("Critical ARIMA forecast error for {}: {}", instrumentCode, response.errorMessage());
                    }
                }

                // Note: Individual predictions are already stored by the execute() method
                // No need to duplicate storage logic here

            } catch (Exception e) {
                String errorMessage = "Unexpected error: " + e.getMessage();
                failedInstruments.put(instrumentCode, errorMessage);
                log.error("Unexpected error in batch forecast for {}: {}", instrumentCode, e.getMessage(), e);
            }
        }

        log.info("Batch ARIMA forecast execution {} completed: {}/{} successful",
            executionId, successfulForecasts, instrumentCodes.size());

        return BatchForecastOrchestrationResponse.withFailures(
            instrumentCodes.size(),
            successfulForecasts,
            failedInstruments,
            hasCriticalErrors,
            arimaModelVersion,
            executionId
        );
    }

    /**
     * Determines if an error message represents a critical error that should fail the batch
     */
    private boolean isCriticalError(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }

        // Critical errors that should fail the entire Camunda task
        return errorMessage.contains("Insufficient historical data for ARIMA calculation") ||
               errorMessage.contains("ARIMA model not found for instrument") ||
               errorMessage.contains("No historical price data available") ||
               errorMessage.contains("Failed to load ARIMA model");
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
     * @param predictionRange The period for which we want to make predictions
     * @param arOrder The dynamic AR order from the ARIMA model (p parameter)
     */
    private TimeRange calculateHistoricalDataRange(TimeRange predictionRange, int arOrder) {
        // Get the start and end of prediction period
        LocalDate predictionStart = predictionRange.from().atOffset(java.time.ZoneOffset.UTC).toLocalDate();
        LocalDate predictionEnd = predictionRange.to().atOffset(java.time.ZoneOffset.UTC).toLocalDate();

        // For ARIMA calculation, we need exactly AR order days of data before prediction period
        // (dynamic AR order - no buffer needed)
        LocalDate historicalStart = predictionStart.minusDays(arOrder);

        // For ARIMA prediction, we need the open price of the prediction end day
        // This means historical data should extend to predictionEnd (inclusive)
        LocalDate historicalEnd = predictionEnd;

        log.debug("Historical data range calculated: {} to {} (AR order = {})",
            historicalStart, historicalEnd, arOrder);

        return new TimeRange(
            historicalStart.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
            historicalEnd.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)
        );
    }


    private ForecastResponse convertToResponse(String executionId, ForecastResult result, boolean includeCalculationDetails) {
        // Convert metrics
        ForecastExecutionMetrics metricsDto = new ForecastExecutionMetrics(
            result.metrics().dataPointsUsed(),
            result.metrics().arOrder(),
            result.metrics().meanSquaredError(),
            result.metrics().standardError(),
            result.metrics().getExecutionTimeMs(),
            result.metrics().modelVersion(),
            result.metrics().hasSufficientQuality(),
            result.forecastDate()
        );

        // Convert calculation steps if requested
        List<CalculationStepDto> calculationSteps = null;
        if (includeCalculationDetails) {
            calculationSteps = result.calculations().stream()
                .map(this::convertCalculationStep)
                .toList();
        }

        // Create appropriate response based on whether details are included
        // Use symbol directly from ForecastResult
        if (includeCalculationDetails && calculationSteps != null) {
            return ForecastResponse.successWithDetails(
                executionId,
                result.symbol(),
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
                result.symbol(),
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
     * Stores forecast prediction result to database for tracking and future reference.
     * Uses market-based repository factory with symbol routing.
     */
    private void storePredictionResult(ForecastResponse response, String modelVersion, String symbol) {
        try {
            // Determine market type for this symbol
            BybitMarketType marketType = marketResolver.resolveMarket(symbol);

            // Create market-specific entity based on symbol's market type
            Object entity = createPredictionEntity(response, modelVersion, symbol, marketType);

            // Save using factory's market routing
            predictionRepositoryFactory.save(symbol, entity);

            log.info("Successfully upserted {} prediction result for {} with model version {} (executionId: {})",
                response.status(), symbol, modelVersion, response.executionId());

        } catch (Exception e) {
            log.warn("Failed to store prediction result for {} with model version {}: {}. " +
                "Forecast execution continues normally.",
                symbol, modelVersion, e.getMessage());
            // Don't fail the forecast if storage fails - just log the warning
        }
    }

    /**
     * Creates a market-specific prediction entity based on the market type
     */
    private Object createPredictionEntity(ForecastResponse response, String modelVersion, String symbol, BybitMarketType marketType) {
        // Extract predicted values from calculation steps for verification
        CalculationStepDto lastCalculationStep = response.calculationSteps() != null && !response.calculationSteps().isEmpty()
            ? response.calculationSteps().get(response.calculationSteps().size() - 1) : null;

        BigDecimal predictDiffOC = lastCalculationStep != null && lastCalculationStep.predictedDiffOC() != null
            ? BigDecimal.valueOf(lastCalculationStep.predictedDiffOC()) : null;
        BigDecimal predictOC = lastCalculationStep != null && lastCalculationStep.predictedOC() != null
            ? BigDecimal.valueOf(lastCalculationStep.predictedOC()) : null;

        // Common entity data
        String executionId = response.executionId();
        Instant forecastDate = response.forecastDate() != null ? response.forecastDate() : Instant.now();
        BigDecimal expectedReturn = response.isSuccessful() ? BigDecimal.valueOf(response.expectedReturn()) : BigDecimal.ZERO;
        BigDecimal confidenceLevel = response.isSuccessful() ? BigDecimal.valueOf(response.confidenceLevel()) : BigDecimal.ZERO;
        String predictionStatus = response.status();
        String summary = response.summary();
        String errorMessage = response.errorMessage();

        // Metrics (only for successful predictions)
        ForecastExecutionMetrics metrics = response.metrics();
        Integer dataPointsUsed = metrics != null ? metrics.dataPointsUsed() : null;
        Integer arOrder = metrics != null ? metrics.arOrder() : null;
        BigDecimal meanSquaredError = metrics != null ? BigDecimal.valueOf(metrics.meanSquaredError()) : null;
        BigDecimal standardError = metrics != null ? BigDecimal.valueOf(metrics.standardError()) : null;
        Long executionTimeMs = metrics != null ? metrics.executionTimeMs() : null;
        Boolean hasSufficientQuality = metrics != null ? metrics.hasSufficientQuality() : null;

        // Create market-specific entity
        return switch (marketType) {
            case SPOT -> new SpotExpectedReturnPredictionEntity(
                null, symbol, executionId, forecastDate, expectedReturn, confidenceLevel,
                modelVersion, predictionStatus, summary, dataPointsUsed, arOrder,
                meanSquaredError, standardError, executionTimeMs, hasSufficientQuality,
                errorMessage, predictDiffOC, predictOC, null, null
            );
            case LINEAR -> new LinearExpectedReturnPredictionEntity(
                null, symbol, executionId, forecastDate, expectedReturn, confidenceLevel,
                modelVersion, predictionStatus, summary, dataPointsUsed, arOrder,
                meanSquaredError, standardError, executionTimeMs, hasSufficientQuality,
                errorMessage, predictDiffOC, predictOC, null, null
            );
            case INVERSE -> new InverseExpectedReturnPredictionEntity(
                null, symbol, executionId, forecastDate, expectedReturn, confidenceLevel,
                modelVersion, predictionStatus, summary, dataPointsUsed, arOrder,
                meanSquaredError, standardError, executionTimeMs, hasSufficientQuality,
                errorMessage, predictDiffOC, predictOC, null, null
            );
            case OPTION -> new OptionExpectedReturnPredictionEntity(
                null, symbol, executionId, forecastDate, expectedReturn, confidenceLevel,
                modelVersion, predictionStatus, summary, dataPointsUsed, arOrder,
                meanSquaredError, standardError, executionTimeMs, hasSufficientQuality,
                errorMessage, predictDiffOC, predictOC, null, null
            );
        };
    }

    /**
     * Stores multiple prediction results for date range forecasts.
     * Extracts individual predictions from calculations and stores each one separately.
     */
    private void storeRangePredictionResults(ForecastResult result, ForecastResponse summaryResponse, String modelVersion, String symbol) {
        try {
            // Determine market type for this symbol
            BybitMarketType marketType = marketResolver.resolveMarket(symbol);

            // Extract individual predictions from calculations
            List<Object> entities = new ArrayList<>();

            for (TimeSeriesCalculation calculation : result.calculations()) {
                // Only store calculations that have prediction results (not historical data points)
                if (calculation.predictedReturn() != null && calculation.timestamp() != null) {

                    // Generate shorter execution ID for range predictions to fit 36 char limit
                    String dateStr = calculation.timestamp().toString().substring(0, 10);
                    String shortExecutionId = summaryResponse.executionId().substring(0, 24) + "-" + dateStr; // 24 + 1 + 10 = 35 chars

                    // Create market-specific entity for this prediction
                    Object entity = createRangePredictionEntity(
                        shortExecutionId,
                        symbol,
                        calculation,
                        result,
                        modelVersion,
                        marketType
                    );

                    entities.add(entity);
                }
            }

            // Store all individual predictions using factory's saveAll method
            if (!entities.isEmpty()) {
                predictionRepositoryFactory.saveAll(symbol, entities);

                log.info("Successfully upserted {} individual prediction results for {} with model version {} from range forecast (executionId: {})",
                    entities.size(), symbol, modelVersion, summaryResponse.executionId());
            }

        } catch (Exception e) {
            log.warn("Failed to store range prediction results for {} with model version {}: {}. " +
                "Forecast execution continues normally.",
                symbol, modelVersion, e.getMessage());
            // Don't fail the forecast if storage fails - just log the warning
        }
    }

    /**
     * Creates a market-specific entity for a single prediction within a date range
     */
    private Object createRangePredictionEntity(
            String executionId,
            String symbol,
            TimeSeriesCalculation calculation,
            ForecastResult result,
            String modelVersion,
            BybitMarketType marketType) {

        // Extract data from calculation
        Instant forecastDate = calculation.timestamp();
        BigDecimal expectedReturn = BigDecimal.valueOf(calculation.predictedReturn());
        BigDecimal confidenceLevel = BigDecimal.valueOf(result.confidenceLevel());
        BigDecimal predictDiffOC = calculation.predictedDiffOC() != null ? BigDecimal.valueOf(calculation.predictedDiffOC()) : null;
        BigDecimal predictOC = calculation.predictedOC() != null ? BigDecimal.valueOf(calculation.predictedOC()) : null;

        // Generate summary
        String summary = String.format("ARIMA forecast for %s on %s: %.4f%% expected return (%.1f%% confidence)",
            symbol,
            calculation.timestamp().toString().substring(0, 10),
            calculation.predictedReturn() * 100.0,
            result.confidenceLevel() * 100.0);

        // Extract metrics
        Integer dataPointsUsed = result.metrics().dataPointsUsed();
        Integer arOrder = result.metrics().arOrder();
        BigDecimal meanSquaredError = BigDecimal.valueOf(result.metrics().meanSquaredError());
        BigDecimal standardError = BigDecimal.valueOf(result.metrics().standardError());
        Long executionTimeMs = result.metrics().getExecutionTimeMs();
        Boolean hasSufficientQuality = result.metrics().hasSufficientQuality();

        // Create market-specific entity
        return switch (marketType) {
            case SPOT -> new SpotExpectedReturnPredictionEntity(
                null, symbol, executionId, forecastDate, expectedReturn, confidenceLevel,
                modelVersion, "SUCCESS", summary, dataPointsUsed, arOrder,
                meanSquaredError, standardError, executionTimeMs, hasSufficientQuality,
                null, predictDiffOC, predictOC, null, null
            );
            case LINEAR -> new LinearExpectedReturnPredictionEntity(
                null, symbol, executionId, forecastDate, expectedReturn, confidenceLevel,
                modelVersion, "SUCCESS", summary, dataPointsUsed, arOrder,
                meanSquaredError, standardError, executionTimeMs, hasSufficientQuality,
                null, predictDiffOC, predictOC, null, null
            );
            case INVERSE -> new InverseExpectedReturnPredictionEntity(
                null, symbol, executionId, forecastDate, expectedReturn, confidenceLevel,
                modelVersion, "SUCCESS", summary, dataPointsUsed, arOrder,
                meanSquaredError, standardError, executionTimeMs, hasSufficientQuality,
                null, predictDiffOC, predictOC, null, null
            );
            case OPTION -> new OptionExpectedReturnPredictionEntity(
                null, symbol, executionId, forecastDate, expectedReturn, confidenceLevel,
                modelVersion, "SUCCESS", summary, dataPointsUsed, arOrder,
                meanSquaredError, standardError, executionTimeMs, hasSufficientQuality,
                null, predictDiffOC, predictOC, null, null
            );
        };
    }
}
