package com.ahd.trading_platform.forecasting.application.usecases;

import com.ahd.trading_platform.forecasting.domain.entities.ARIMAModel;
import com.ahd.trading_platform.forecasting.domain.valueobjects.*;
import com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories.AssetSpecificMasterDataRepository;
import com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories.AssetSpecificMasterDataRepositoryFactory;
import com.ahd.trading_platform.marketdata.domain.repositories.MarketDataRepository;
import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Use case for applying ARIMA model predictions using pre-calculated DemeanDiffOC master data.
 * This separates model application from data preparation, enabling:
 * - Clean separation of concerns
 * - Easy addition of new models (LSTM, Prophet, etc.)
 * - Reuse of expensive data preparation calculations
 * - Independent testing of model logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplyARIMAModelUseCase {
    
    private final MarketDataRepository marketDataRepository;
    private final AssetSpecificMasterDataRepositoryFactory repositoryFactory;
    
    /**
     * Applies ARIMA model to predict expected return for a specific target date.
     * Uses historical master data as input to predict the target date.
     * 
     * @param instrument The trading instrument
     * @param masterData Historical DemeanDiffOC master data (used as prediction input)
     * @param arimaModel The ARIMA model with coefficients
     * @param targetDate The date for which we want to predict the expected return
     * @return Complete forecast result with prediction for target date
     */
    public ForecastResult applyModel(
            TradingInstrument instrument,
            List<DemeanDiffOCMasterData> masterData,
            ARIMAModel arimaModel,
            Instant targetDate) {
        return applyModelForDateRange(instrument, masterData, arimaModel, targetDate, targetDate);
    }
    
    /**
     * Applies ARIMA model to predict expected returns for a range of target dates (backtesting).
     * Uses historical master data as input to predict each date in the range sequentially.
     * 
     * @param instrument The trading instrument
     * @param masterData Historical DemeanDiffOC master data (used as prediction input)
     * @param arimaModel The ARIMA model with coefficients
     * @param startDate The start date of the prediction range (inclusive)
     * @param endDate The end date of the prediction range (inclusive)
     * @return Complete forecast result with predictions for date range
     */
    public ForecastResult applyModelForDateRange(
            TradingInstrument instrument,
            List<DemeanDiffOCMasterData> masterData,
            ARIMAModel arimaModel,
            Instant startDate,
            Instant endDate) {
        
        boolean isSingleDate = startDate.equals(endDate);
        log.info("Applying ARIMA model for {} with {} master data points for {} prediction", 
            instrument.getCode(), masterData.size(), isSingleDate ? "single date" : "date range");
        
        Instant executionStartTime = Instant.now();
        
        try {
            // Validate inputs
            validateInputsForDateRange(instrument, masterData, arimaModel, startDate, endDate);
            
            if (isSingleDate) {
                // Single date prediction (current date mode)
                return new ARIMAMasterDataPipeline(masterData, arimaModel, startDate, marketDataRepository, repositoryFactory)
                    .extractARLagsFromHistory()     
                    .predictTargetDifference()      
                    .calculateTargetPrediction()    
                    .buildSingleResult(instrument, executionStartTime);
            } else {
                // Date range prediction (backtesting mode)
                return new ARIMADateRangePipeline(masterData, arimaModel, startDate, endDate, marketDataRepository, repositoryFactory)
                    .generateDateSequence()         // Step 1: Generate prediction date sequence
                    .predictForEachDate()           // Step 2: Predict for each date in sequence  
                    .buildRangeResult(instrument, executionStartTime);  // Step 3: Build combined result
            }
                
        } catch (Exception e) {
            log.error("ARIMA model application failed for {}: {}", instrument.getCode(), e.getMessage(), e);
            throw new ARIMAModelApplicationException("Failed to apply ARIMA model: " + e.getMessage(), e);
        }
    }
    
    /**
     * Pipeline for applying ARIMA model to predict for target date
     */
    private static class ARIMAMasterDataPipeline {
        
        private final List<DemeanDiffOCMasterData> masterData;
        private final ARIMAModel arimaModel;
        private final Instant targetDate;
        private final MarketDataRepository marketDataRepository;
        private final AssetSpecificMasterDataRepositoryFactory repositoryFactory;
        private List<Double> arLags;
        private double predictedDiffOC;
        private double predictedReturn;
        private double confidence;
        
        public ARIMAMasterDataPipeline(List<DemeanDiffOCMasterData> masterData, ARIMAModel arimaModel, 
                                     Instant targetDate, MarketDataRepository marketDataRepository,
                                     AssetSpecificMasterDataRepositoryFactory repositoryFactory) {
            this.masterData = masterData;
            this.arimaModel = arimaModel;
            this.targetDate = targetDate;
            this.marketDataRepository = marketDataRepository;
            this.repositoryFactory = repositoryFactory;
        }
        
        /**
         * Step 1: Extract AR lags from historical master data for prediction
         */
        public ARIMAMasterDataPipeline extractARLagsFromHistory() {
            log.debug("Step 1: Extracting AR lags from {} historical data points for target date prediction", masterData.size());
            
            int pOrder = arimaModel.getPOrder();
            
            // Extract the last P demean_diff_oc values as AR lags (L1 = most recent, L30 = earliest)
            this.arLags = new ArrayList<>();
            
            for (int i = 0; i < pOrder; i++) {
                int dataIndex = masterData.size() - 1 - i; // Reverse order: most recent to earliest
                int lagNumber = i + 1; // L1, L2, ..., L30
                DemeanDiffOCMasterData lagData = masterData.get(dataIndex);
                
                log.debug("Processing AR lag L{}: data index {}, timestamp {}, hasDifferences: {}", 
                    lagNumber, dataIndex, lagData.timestamp(), lagData.hasDifferences());
                
                double lagValue;
                
                // Enhanced validation: check for missing differences, null, or zero values
                if (!lagData.hasDifferences() || lagData.demeanDiffOC() == null || 
                    lagData.demeanDiffOC().compareTo(BigDecimal.ZERO) == 0) {
                    
                    log.warn("AR lag L{} at index {} has insufficient data (hasDifferences: {}, demeanDiffOC: {}). Attempting recalculation from price data.",
                        lagNumber, dataIndex, lagData.hasDifferences(), lagData.demeanDiffOC());
                    
                    // Try to recalculate demean diff OC value from historical price data
                    try {
                        lagValue = recalculateDemeanDiffOCFromPriceData(lagData, dataIndex, lagNumber);
                        log.info("Successfully recalculated AR lag L{} = {} from price data", lagNumber, lagValue);
                    } catch (Exception recalcException) {
                        log.error("Failed to recalculate AR lag L{} from price data: {}", lagNumber, recalcException.getMessage());
                        throw new IllegalStateException(String.format(
                            "Historical data point at index %d lacks sufficient data for AR lag L%d. " +
                            "Original data: hasDifferences=%s, demeanDiffOC=%s. Recalculation failed: %s",
                            dataIndex, lagNumber, lagData.hasDifferences(), lagData.demeanDiffOC(), 
                            recalcException.getMessage()), recalcException);
                    }
                } else {
                    // Use existing calculated value
                    try {
                        lagValue = lagData.demeanDiffOCAsDouble();
                    } catch (Exception e) {
                        log.error("Failed to get demeanDiffOC for AR lag L{} at data index {}: {}", lagNumber, dataIndex, e.getMessage());
                        throw new IllegalStateException(String.format(
                            "Cannot extract AR lag L%d from data index %d: %s", lagNumber, dataIndex, e.getMessage()), e);
                    }
                }
                
                // Final validation of the lag value
                if (Double.isNaN(lagValue) || Double.isInfinite(lagValue)) {
                    throw new IllegalStateException(String.format(
                        "AR lag L%d has invalid value (%f) at data index %d after validation/recalculation. Cannot proceed with ARIMA prediction.",
                        lagNumber, lagValue, dataIndex));
                }
                
                log.debug("Adding AR lag L{} = {} (most recent to earliest order)", lagNumber, lagValue);
                arLags.add(lagValue);
            }
            
            // Validate final arLags list for any null values
            if (arLags.contains(null)) {
                throw new IllegalStateException("AR lags list contains null values: " + arLags);
            }
            
            log.debug("Step 1 completed: extracted {} AR lag values: {}", arLags.size(), arLags);
            return this;
        }
        
        /**
         * Recalculates demean diff OC value from fresh database price data when master data is insufficient
         * and stores the updated value back to the database
         */
        private double recalculateDemeanDiffOCFromPriceData(DemeanDiffOCMasterData lagData, int dataIndex, int lagNumber) {
            log.debug("Recalculating demean diff OC for AR lag L{} from fresh database price data", lagNumber);
            
            // Get current and previous day timestamps
            Instant currentTimestamp = lagData.timestamp();
            Instant previousTimestamp = currentTimestamp.minus(1, java.time.temporal.ChronoUnit.DAYS);
            
            // Fetch fresh price data from database for both days
            TimeRange currentDayRange = new TimeRange(currentTimestamp, currentTimestamp);
            TimeRange previousDayRange = new TimeRange(previousTimestamp, previousTimestamp);
            
            String instrumentCode = arimaModel.getInstrument().getCode();
            List<OHLCV> currentDayPrices = marketDataRepository.findHistoricalData(instrumentCode, currentDayRange);
            List<OHLCV> previousDayPrices = marketDataRepository.findHistoricalData(instrumentCode, previousDayRange);
            
            // Validate we have the required price data
            if (currentDayPrices.isEmpty()) {
                throw new IllegalStateException(String.format(
                    "No fresh price data found in database for current day %s (AR lag L%d)", currentTimestamp, lagNumber));
            }
            if (previousDayPrices.isEmpty()) {
                throw new IllegalStateException(String.format(
                    "No fresh price data found in database for previous day %s (AR lag L%d)", previousTimestamp, lagNumber));
            }
            
            // Get the price data (first entry should be the day's data)
            OHLCV currentPrice = currentDayPrices.get(0);
            OHLCV previousPrice = previousDayPrices.get(0);
            
            // Calculate current day's OC from fresh price data
            BigDecimal currentOC = currentPrice.close().amount().subtract(currentPrice.open().amount());
            
            // Calculate previous day's OC from fresh price data
            BigDecimal previousOC = previousPrice.close().amount().subtract(previousPrice.open().amount());
            
            // Calculate DiffOC = Current OC - Previous OC  
            BigDecimal diffOC = currentOC.subtract(previousOC);
            
            // Calculate DemeanDiffOC = DiffOC - Mean DiffOC (from ARIMA model)
            BigDecimal meanDiffOC = BigDecimal.valueOf(arimaModel.getMeanDiffOC());
            BigDecimal demeanDiffOC = diffOC.subtract(meanDiffOC);
            
            double recalculatedValue = demeanDiffOC.doubleValue();
            
            log.debug("Recalculated AR lag L{} from fresh DB data: currentOC={}, previousOC={}, diffOC={}, meanDiffOC={}, demeanDiffOC={}", 
                lagNumber, currentOC, previousOC, diffOC, meanDiffOC, recalculatedValue);
            
            // Validate recalculated value
            if (Double.isNaN(recalculatedValue) || Double.isInfinite(recalculatedValue)) {
                throw new IllegalStateException(String.format(
                    "Recalculated demean diff OC for AR lag L%d is invalid: %f", lagNumber, recalculatedValue));
            }
            
            // Create updated master data with the recalculated values
            DemeanDiffOCMasterData updatedMasterData = lagData.withRecalculatedDifferences(
                currentOC, diffOC, demeanDiffOC);
            
            // Store the updated master data back to the database using upsert to avoid constraint violations
            try {
                TradingInstrument instrument = arimaModel.getInstrument();
                AssetSpecificMasterDataRepository repository = repositoryFactory.getRepository(instrument);
                repository.upsert(updatedMasterData);
                
                log.info("Successfully upserted recalculated master data for AR lag L{} (timestamp: {}) to database", 
                    lagNumber, currentTimestamp);
                    
            } catch (Exception saveException) {
                log.warn("Failed to save recalculated master data for AR lag L{} to database: {}. " +
                    "Continuing with calculation using recalculated value.", 
                    lagNumber, saveException.getMessage());
                // Don't fail the entire ARIMA calculation if saving fails - just log the warning
            }
            
            return recalculatedValue;
        }
        
        /**
         * Step 2: Predict difference for target date using ARIMA model
         */
        public ARIMAMasterDataPipeline predictTargetDifference() {
            log.debug("Step 2: Predicting difference for target date {} using ARIMA model", targetDate);
            
            if (arLags == null || arLags.size() != arimaModel.getPOrder()) {
                throw new IllegalStateException("AR lags must be extracted before predicting differences");
            }
            
            // Start with mean difference
            double meanDiffOC = arimaModel.getMeanDiffOC();
            this.predictedDiffOC = meanDiffOC;
            
            // Add weighted sum of AR lags: predicted_diff = mean + Σ(coefficient * lag_value)
            for (int i = 0; i < arLags.size(); i++) {
                int lagNumber = i + 1; // Lag numbers are 1-based (L1, L2, ...)
                ARIMACoefficient coefficient = arimaModel.getCoefficient(lagNumber);
                double lagValue = arLags.get(i);
                
                predictedDiffOC += coefficient.doubleValue() * lagValue;
                
                log.debug("Step 2: AR lag L{} = {}, coefficient = {}, contribution = {}", 
                    lagNumber, lagValue, coefficient.doubleValue(), coefficient.doubleValue() * lagValue);
            }
            
            log.debug("Step 2 completed: predicted difference for target date = {} (mean: {}, AR contribution: {})", 
                predictedDiffOC, meanDiffOC, predictedDiffOC - meanDiffOC);
            return this;
        }
        
        /**
         * Step 3: Calculate final expected return prediction for target date
         */
        public ARIMAMasterDataPipeline calculateTargetPrediction() {
            log.debug("Step 3: Calculating final expected return for target date {}", targetDate);
            
            if (Double.isNaN(predictedDiffOC)) {
                throw new IllegalStateException("Predicted difference must be calculated before final prediction");
            }
            
            // Get the previous day's OC value relative to target date to calculate predicted OC
            // Since we predict diffOC for targetDate, we need previous day's OC value
            Instant previousDay = targetDate.minus(1, ChronoUnit.DAYS);
            DemeanDiffOCMasterData previousDayData = findMasterDataByDate(masterData, previousDay);
            
            if (previousDayData == null) {
                // Fallback to most recent available data if previous day not found
                log.warn("Previous day data not found for {}, using most recent data as fallback", previousDay);
                previousDayData = masterData.get(masterData.size() - 1);
            }
            
            double previousDayOC = previousDayData.oc().doubleValue();
            
            // Calculate predicted OC: predicted_oc = predicted_diff + previous_day_oc  
            double predictedOC = predictedDiffOC + previousDayOC;
            
            // Need to get open price for target date - for now, use previous day's open price as approximation
            // In a real scenario, this would come from market data for the target date
            double targetOpenPrice = previousDayData.openPriceAsDouble();
            
            // Calculate final expected return: return = predicted_oc / open_price
            this.predictedReturn = predictedOC / targetOpenPrice;
            
            // Calculate confidence based on model quality and data sufficiency
            this.confidence = calculatePredictionConfidence();
            
            log.debug("Step 3 completed: target expected return = {}% (predicted_oc: {}, open_price: {}, confidence: {})", 
                String.format("%.4f", predictedReturn * 100), predictedOC, targetOpenPrice, confidence);
            
            return this;
        }
        
        /**
         * Calculate prediction confidence based on model and data quality
         */
        private double calculatePredictionConfidence() {
            double baseConfidence = 0.8;
            
            // Adjust based on data sufficiency
            if (masterData.size() < 50) {
                baseConfidence -= 0.1;
            }
            if (masterData.size() < 30) {
                baseConfidence -= 0.2;
            }
            
            // Validate prediction quality
            if (Double.isNaN(predictedReturn) || Double.isInfinite(predictedReturn)) {
                baseConfidence -= 0.3;
            }
            
            return Math.min(1.0, baseConfidence);
        }
        
        /**
         * Builds the final ForecastResult for single target date
         */
        public ForecastResult buildSingleResult(TradingInstrument instrument, Instant startTime) {
            if (Double.isNaN(predictedReturn)) {
                throw new IllegalStateException("No valid prediction calculated for target date");
            }
            
            // Calculate execution metrics
            Duration executionTime = Duration.between(startTime, Instant.now());
            ForecastMetrics metrics = calculateMetrics(executionTime);
            
            // Get previous day's OC value relative to target date for correct predictOC calculation
            Instant previousDay = targetDate.minus(1, ChronoUnit.DAYS);
            DemeanDiffOCMasterData previousDayData = findMasterDataByDate(masterData, previousDay);
            
            if (previousDayData == null) {
                // Fallback to most recent available data if previous day not found
                log.warn("Previous day data not found for {}, using most recent data as fallback", previousDay);
                previousDayData = masterData.getLast();
            }
            
            double baseOpenPrice = previousDayData.openPriceAsDouble();
            double baseClosePrice = previousDayData.closePriceAsDouble();
            double previousDayOC = previousDayData.oc().doubleValue();
            
            // Calculate predicted prices based on ARIMA prediction
            double predictedOC = predictedDiffOC + previousDayOC;  // From ARIMA calculation
            double predictedClosePrice = baseOpenPrice - predictedOC; // Since OC = Open - Close
            
            // Final safety check before creating TimeSeriesCalculation
            if (arLags == null) {
                throw new IllegalStateException("AR lags is null when building result");
            }
            if (arLags.contains(null)) {
                throw new IllegalStateException("AR lags contains null values: " + arLags);
            }
            
            log.debug("Creating TimeSeriesCalculation with {} AR lags: {}", arLags.size(), arLags);
            
            // Create realistic calculation for the target prediction
            List<TimeSeriesCalculation> singlePrediction = List.of(
                TimeSeriesCalculation.initial(targetDate, baseOpenPrice, predictedClosePrice)
                    .withARLags(arLags)
                    .withPredictions(predictedDiffOC, predictedOC, predictedReturn)
            );
            
            // Record model usage
            arimaModel.recordUsage();
            
            log.info("ARIMA model prediction completed for {} target date {} in {}ms: expected return = {}% (predicted_oc: {}, base_open: {})",
                instrument.getCode(), targetDate, executionTime.toMillis(),
                String.format("%.4f", predictedReturn * 100), predictedOC, baseOpenPrice);
            
            return ForecastResult.successful(
                instrument,
                targetDate, // ✅ Return prediction for requested target date
                predictedReturn,
                confidence,
                singlePrediction,
                metrics
            );
        }
        
        private ForecastMetrics calculateMetrics(Duration executionTime) {
            Instant dataStart = masterData.get(0).timestamp();
            Instant dataEnd = masterData.getLast().timestamp();
            
            // Calculate basic metrics
            double mse = 0.001; // Placeholder - would calculate from residuals
            double standardError = Math.sqrt(arimaModel.getSigma2());
            
            return ForecastMetrics.successful(
                masterData.size(),
                arimaModel.getPOrder(),
                mse,
                standardError,
                executionTime,
                dataStart,
                dataEnd,
                arimaModel.getModelVersion()
            );
        }
    }
    
    /**
     * Simplified Pipeline for applying ARIMA model to predict for multiple dates (backtesting).
     * Uses efficient Map-based master data lookup and sequential prediction calculation.
     * Implements the formula: Prd_Diff_OC(T) = Σ[Ar.L(i) × ARIMA.Coefficient(i)] + Mean_Diff_OC
     */
    private static class ARIMADateRangePipeline {
        
        private final List<DemeanDiffOCMasterData> masterData;
        private final ARIMAModel arimaModel;
        private final Instant startDate;
        private final Instant endDate;
        private final MarketDataRepository marketDataRepository;
        private final AssetSpecificMasterDataRepositoryFactory repositoryFactory;
        private List<Instant> predictionDates;
        private final List<TimeSeriesCalculation> predictions;
        private Map<Instant, Double> masterDataMap; // Efficient timestamp -> demean_diff_oc lookup
        
        public ARIMADateRangePipeline(List<DemeanDiffOCMasterData> masterData, ARIMAModel arimaModel, 
                Instant startDate, Instant endDate, MarketDataRepository marketDataRepository,
                AssetSpecificMasterDataRepositoryFactory repositoryFactory) {
            this.masterData = masterData;
            this.arimaModel = arimaModel;
            this.startDate = startDate;
            this.endDate = endDate;
            this.marketDataRepository = marketDataRepository;
            this.repositoryFactory = repositoryFactory;
            this.predictions = new ArrayList<>();
        }
        
        /**
         * Step 1: Generate sequence of dates to predict and convert master data to Map
         */
        public ARIMADateRangePipeline generateDateSequence() {
            log.debug("Step 1: Generating date sequence from {} to {} and converting master data to Map", startDate, endDate);
            
            // Generate prediction dates
            this.predictionDates = new ArrayList<>();
            LocalDate current = startDate.atOffset(java.time.ZoneOffset.UTC).toLocalDate();
            LocalDate end = endDate.atOffset(java.time.ZoneOffset.UTC).toLocalDate();
            
            while (!current.isAfter(end)) {
                predictionDates.add(current.atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
                current = current.plusDays(1);
            }
            
            // Convert master data to Map for efficient lookup: timestamp -> demean_diff_oc
            this.masterDataMap = new HashMap<>();
            for (DemeanDiffOCMasterData data : masterData) {
                masterDataMap.put(data.timestamp(), data.demeanDiffOC().doubleValue());
            }
            
            log.debug("Step 1 completed: generated {} prediction dates and {} master data entries", 
                predictionDates.size(), masterDataMap.size());
            return this;
        }
        
        /**
         * Step 2: Predict for each date using efficient sequential calculation.
         * Implements: Prd_Diff_OC(T) = Σ[Ar.L(i) × ARIMA.Coefficient(i)] + Mean_Diff_OC
         */
        public ARIMADateRangePipeline predictForEachDate() {
            log.debug("Step 2: Predicting for {} dates using simplified pipeline", predictionDates.size());
            
            int arOrder = arimaModel.getPOrder();
            List<BigDecimal> coefficients = arimaModel.getCoefficients().stream()
                .map(coeff -> coeff.value())
                .toList();
            double meanDiffOC = arimaModel.getMeanDiffOC();
            
            for (Instant predictionDate : predictionDates) {
                try {
                    // Calculate prediction for this date using efficient lookup
                    double predictedDiffOC = calculatePredictedDiffOC(predictionDate, arOrder, coefficients, meanDiffOC);
                    
                    // Get previous day's OC value relative to current prediction date 
                    Instant previousDay = predictionDate.minus(1, ChronoUnit.DAYS);
                    DemeanDiffOCMasterData previousDayData = findMasterDataByDate(masterData, previousDay);
                    
                    if (previousDayData == null) {
                        // Fallback to most recent available data if previous day not found
                        log.warn("Previous day data not found for {}, using most recent data as fallback", previousDay);
                        previousDayData = masterData.get(masterData.size() - 1);
                    }
                    
                    // Calculate predicted OC and expected return using correct previous day OC
                    double previousDayOC = previousDayData.oc().doubleValue();
                    double predictedOC = predictedDiffOC + previousDayOC;
                    double baseOpenPrice = previousDayData.openPriceAsDouble();
                    double predictedReturn = predictedOC / baseOpenPrice;
                    double predictedClosePrice = baseOpenPrice - predictedOC;
                    
                    // Extract AR lags for this prediction (for tracking/debugging)
                    List<Double> arLags = extractARLagsForDate(predictionDate, arOrder);
                    
                    // Create calculation result
                    TimeSeriesCalculation calculation = TimeSeriesCalculation
                            .initial(predictionDate, baseOpenPrice, predictedClosePrice)
                            .withARLags(arLags)
                            .withPredictions(predictedDiffOC, predictedOC, predictedReturn);

                    predictions.add(calculation);
                    
                    log.debug("Predicted for {}: diff_oc={}, oc={}, return={}%", 
                        predictionDate, predictedDiffOC, predictedOC, predictedReturn * 100);

                } catch (Exception e) {
                    log.warn("Failed to predict for date {}: {}", predictionDate, e.getMessage());
                    // Create failed prediction entry using most recent available data as fallback
                    DemeanDiffOCMasterData fallbackData = masterData.get(masterData.size() - 1);
                    TimeSeriesCalculation failedCalculation = TimeSeriesCalculation
                            .initial(predictionDate, fallbackData.openPriceAsDouble(), fallbackData.closePriceAsDouble())
                            .withPredictions(0.0, 0.0, 0.0);
                    predictions.add(failedCalculation);
                }
            }
            
            log.debug("Step 2 completed: generated {} predictions using simplified pipeline", predictions.size());
            return this;
        }
        
        /**
         * Calculate predicted Diff OC using the ARIMA formula:
         * Prd_Diff_OC(T) = Σ[Ar.L(i) × ARIMA.Coefficient(i)] + Mean_Diff_OC
         */
        private double calculatePredictedDiffOC(Instant predictionDate, int arOrder, 
                List<BigDecimal> coefficients, double meanDiffOC) {
            
            double arContribution = 0.0;
            
            // Look back arOrder days from prediction date
            LocalDate predictionLocalDate = predictionDate.atOffset(java.time.ZoneOffset.UTC).toLocalDate();
            
            for (int i = 1; i <= arOrder; i++) {
                // Calculate lag date: predictionDate - i days
                LocalDate lagDate = predictionLocalDate.minusDays(i);
                Instant lagTimestamp = lagDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
                
                // Get demean diff OC value for this lag
                Double demeanDiffOC = masterDataMap.get(lagTimestamp);
                if (demeanDiffOC == null) {
                    log.warn("Missing master data for lag {} (date: {}), using 0.0", i, lagDate);
                    demeanDiffOC = 0.0;
                }
                
                // Get corresponding ARIMA coefficient (i-1 because list is 0-indexed)
                BigDecimal coefficient = coefficients.get(i - 1);
                
                // Calculate contribution: AR.L(i) × ARIMA.Coefficient(i)
                double contribution = demeanDiffOC * coefficient.doubleValue();
                arContribution += contribution;
                
                log.debug("AR lag L{} ({}): demean_diff_oc={}, coefficient={}, contribution={}", 
                    i, lagDate, demeanDiffOC, coefficient.doubleValue(), contribution);
            }
            
            // Final prediction: AR contribution + Mean Diff OC
            double predictedDiffOC = arContribution + meanDiffOC;
            
            log.debug("ARIMA prediction: AR_contribution={}, mean_diff_oc={}, predicted_diff_oc={}", 
                arContribution, meanDiffOC, predictedDiffOC);
            
            return predictedDiffOC;
        }
        
        /**
         * Extract AR lags for a specific prediction date for tracking purposes
         */
        private List<Double> extractARLagsForDate(Instant predictionDate, int arOrder) {
            List<Double> arLags = new ArrayList<>();
            LocalDate predictionLocalDate = predictionDate.atOffset(java.time.ZoneOffset.UTC).toLocalDate();
            
            for (int i = 1; i <= arOrder; i++) {
                LocalDate lagDate = predictionLocalDate.minusDays(i);
                Instant lagTimestamp = lagDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
                Double demeanDiffOC = masterDataMap.getOrDefault(lagTimestamp, 0.0);
                arLags.add(demeanDiffOC);
            }
            
            return arLags;
        }
        
        /**
         * Step 3: Build combined result for date range
         */
        public ForecastResult buildRangeResult(TradingInstrument instrument, Instant startTime) {
            if (predictions.isEmpty()) {
                throw new IllegalStateException("No predictions generated for date range");
            }
            
            Duration executionTime = Duration.between(startTime, Instant.now());
            
            // Calculate average return for the range (simplified approach)
            double averageReturn = predictions.stream()
                .filter(calc -> calc.predictedReturn() != null)
                .mapToDouble(TimeSeriesCalculation::predictedReturn)
                .average()
                .orElse(0.0);
            
            double confidence = calculateRangeConfidence();
            
            // Calculate metrics
            ForecastMetrics metrics = ForecastMetrics.successful(
                masterData.size(),
                arimaModel.getPOrder(),
                0.001, // Placeholder MSE
                Math.sqrt(arimaModel.getSigma2()),
                executionTime,
                masterData.get(0).timestamp(),
                masterData.get(masterData.size() - 1).timestamp(),
                arimaModel.getModelVersion()
            );
            
            arimaModel.recordUsage();
            
            log.info("ARIMA date range prediction completed for {} from {} to {} in {}ms: {} predictions, avg return = {}%",
                instrument.getCode(), startDate, endDate, executionTime.toMillis(),
                predictions.size(), String.format("%.4f", averageReturn * 100));
            
            return ForecastResult.successful(
                instrument,
                endDate, // Use end date as the forecast date for the range
                averageReturn,
                confidence,
                predictions,
                metrics
            );
        }
        
        private double calculateRangeConfidence() {
            long validPredictions = predictions.stream()
                .filter(calc -> calc.predictedReturn() != null)
                .filter(calc -> !Double.isNaN(calc.predictedReturn()) && !Double.isInfinite(calc.predictedReturn()))
                .count();
            
            double validRatio = (double) validPredictions / predictions.size();
            double baseConfidence = 0.7; // Lower base confidence for range predictions
            
            return Math.max(0.1, Math.min(1.0, baseConfidence * validRatio));
        }
    }
    
    private void validateInputs(TradingInstrument instrument, List<DemeanDiffOCMasterData> masterData, ARIMAModel model, Instant targetDate) {
        if (instrument == null) {
            throw new IllegalArgumentException("Trading instrument cannot be null");
        }
        
        if (masterData == null || masterData.isEmpty()) {
            throw new IllegalArgumentException("Master data cannot be null or empty");
        }
        
        if (model == null) {
            throw new IllegalArgumentException("ARIMA model cannot be null");
        }
        
        if (targetDate == null) {
            throw new IllegalArgumentException("Target prediction date cannot be null");
        }
        
        if (!model.getInstrument().equals(instrument)) {
            throw new IllegalArgumentException(
                String.format("Model instrument (%s) does not match requested instrument (%s)",
                    model.getInstrument().getCode(), instrument.getCode()));
        }
        
        model.validateForForecasting(masterData.size());
    }
    
    private void validateInputsForDateRange(TradingInstrument instrument, List<DemeanDiffOCMasterData> masterData, 
            ARIMAModel model, Instant startDate, Instant endDate) {
        
        validateInputs(instrument, masterData, model, startDate); // Reuse basic validation
        
        if (endDate == null) {
            throw new IllegalArgumentException("End date cannot be null");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
    }
    
    /**
     * Finds master data by date using efficient O(n) linear search
     * Master data is typically ordered by date, so this should be fast
     */
    private static DemeanDiffOCMasterData findMasterDataByDate(List<DemeanDiffOCMasterData> masterDataList, Instant targetDate) {
        LocalDate targetLocalDate = targetDate.atZone(java.time.ZoneOffset.UTC).toLocalDate();
        
        for (DemeanDiffOCMasterData data : masterDataList) {
            LocalDate dataDate = data.timestamp().atZone(java.time.ZoneOffset.UTC).toLocalDate();
            if (dataDate.equals(targetLocalDate)) {
                return data;
            }
        }
        return null; // Not found
    }
    
    public static class ARIMAModelApplicationException extends RuntimeException {
        public ARIMAModelApplicationException(String message) {
            super(message);
        }
        
        public ARIMAModelApplicationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}