package com.ahd.trading_platform.forecasting.application.usecases;

import com.ahd.trading_platform.forecasting.domain.entities.ARIMAModel;
import com.ahd.trading_platform.forecasting.domain.valueobjects.*;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
    
    /**
     * Applies ARIMA model to pre-calculated master data to generate forecast predictions.
     * 
     * @param instrument The trading instrument
     * @param masterData Pre-calculated DemeanDiffOC master data
     * @param arimaModel The ARIMA model with coefficients
     * @return Complete forecast result with predictions and metrics
     */
    public ForecastResult applyModel(
            TradingInstrument instrument,
            List<DemeanDiffOCMasterData> masterData,
            ARIMAModel arimaModel) {
        
        log.info("Applying ARIMA model for {} with {} master data points", 
            instrument.getCode(), masterData.size());
        
        Instant startTime = Instant.now();
        
        try {
            // Validate inputs
            validateInputs(instrument, masterData, arimaModel);
            
            // Apply ARIMA-specific pipeline using master data
            return new ARIMAMasterDataPipeline(masterData, arimaModel)
                .calculateARLags()          // Step 1: AR Lag Data Preparation
                .predictDifferences()       // Step 2: Predicted Difference Calculation  
                .predictOC()                // Step 3: Predicted OC Calculation
                .calculateFinalReturn()     // Step 4: Final Return Prediction
                .buildResult(instrument, startTime);  // Build final result
                
        } catch (Exception e) {
            log.error("ARIMA model application failed for {}: {}", instrument.getCode(), e.getMessage(), e);
            throw new ARIMAModelApplicationException("Failed to apply ARIMA model: " + e.getMessage(), e);
        }
    }
    
    /**
     * Pipeline for applying ARIMA model to master data
     */
    private static class ARIMAMasterDataPipeline {
        
        private final List<DemeanDiffOCMasterData> masterData;
        private final ARIMAModel arimaModel;
        private List<TimeSeriesCalculation> results;
        
        public ARIMAMasterDataPipeline(List<DemeanDiffOCMasterData> masterData, ARIMAModel arimaModel) {
            this.masterData = masterData;
            this.arimaModel = arimaModel;
            this.results = convertMasterDataToCalculations();
        }
        
        /**
         * Converts master data to TimeSeriesCalculation objects
         */
        private List<TimeSeriesCalculation> convertMasterDataToCalculations() {
            List<TimeSeriesCalculation> calculations = new ArrayList<>();
            
            for (DemeanDiffOCMasterData data : masterData) {
                TimeSeriesCalculation calculation;
                
                if (data.hasDifferences()) {
                    calculation = TimeSeriesCalculation.initial(
                        data.timestamp(),
                        data.openPriceAsDouble(),
                        data.closePriceAsDouble()
                    ).withDifferences(
                        data.diffOC().doubleValue(),
                        data.demeanDiffOCAsDouble()
                    );
                } else {
                    // First data point
                    calculation = TimeSeriesCalculation.initial(
                        data.timestamp(),
                        data.openPriceAsDouble(),
                        data.closePriceAsDouble()
                    );
                }
                
                calculations.add(calculation);
            }
            
            return calculations;
        }
        
        /**
         * Step 1: AR Lag Data Preparation
         */
        public ARIMAMasterDataPipeline calculateARLags() {
            log.debug("Executing Step 1: AR Lag Data Preparation");
            
            List<TimeSeriesCalculation> newResults = new ArrayList<>();
            int pOrder = arimaModel.getPOrder();
            
            for (int i = 0; i < results.size(); i++) {
                TimeSeriesCalculation current = results.get(i);
                
                // Can only calculate AR lags if we have enough previous data points
                if (i >= pOrder) {
                    List<Double> arLags = new ArrayList<>();
                    
                    // Calculate AR lags from previous demean_diff_oc values
                    for (int lagNum = 1; lagNum <= pOrder; lagNum++) {
                        int lagIndex = i - lagNum;
                        TimeSeriesCalculation lagData = results.get(lagIndex);
                        
                        if (lagData.demeanDiffOC() != null) {
                            arLags.add(lagData.demeanDiffOC());
                        } else {
                            throw new IllegalStateException(String.format(
                                "Missing demeanDiffOC value at lag %d (index %d) for data point %d. " +
                                "This indicates an error in master data preparation.",
                                lagNum, lagIndex, i
                            ));
                        }
                    }
                    
                    current = current.withARLags(arLags);
                }
                
                newResults.add(current);
            }
            
            this.results = newResults;
            log.debug("Step 1 completed: prepared AR lags for {} points", results.size());
            return this;
        }
        
        /**
         * Step 2: Predicted Difference Calculation
         */
        public ARIMAMasterDataPipeline predictDifferences() {
            log.debug("Executing Step 2: Predicted Difference Calculation");
            
            List<TimeSeriesCalculation> newResults = new ArrayList<>();
            double meanDiffOC = arimaModel.getMeanDiffOC();
            
            for (TimeSeriesCalculation calculation : results) {
                // Can only predict if we have AR lag values
                if (calculation.arLags() != null && !calculation.arLags().isEmpty()) {
                    double predictedDiffOC = meanDiffOC; // Start with mean
                    
                    // Add weighted sum of AR lags
                    List<Double> arLags = calculation.arLags();
                    for (int i = 0; i < arLags.size(); i++) {
                        int lagNumber = i + 1; // Lag numbers are 1-based
                        ARIMACoefficient coefficient = arimaModel.getCoefficient(lagNumber);
                        double arValue = arLags.get(i);
                        
                        predictedDiffOC += arValue * coefficient.doubleValue();
                    }
                    
                    calculation = calculation.withPredictions(predictedDiffOC, 0.0, 0.0);
                }
                
                newResults.add(calculation);
            }
            
            this.results = newResults;
            log.debug("Step 2 completed: calculated predicted differences for {} points", results.size());
            return this;
        }
        
        /**
         * Step 3: Predicted OC Calculation
         */
        public ARIMAMasterDataPipeline predictOC() {
            log.debug("Executing Step 3: Predicted OC Calculation");
            
            List<TimeSeriesCalculation> newResults = new ArrayList<>();
            
            for (int i = 0; i < results.size(); i++) {
                TimeSeriesCalculation current = results.get(i);
                
                // Can only calculate predicted OC if we have predicted diff and previous OC
                if (current.predictedDiffOC() != null && i > 0) {
                    TimeSeriesCalculation previous = results.get(i - 1);
                    double predictedOC = current.predictedDiffOC() + previous.oc();
                    
                    current = current.withPredictions(
                        current.predictedDiffOC(),
                        predictedOC,
                        current.predictedReturn() != null ? current.predictedReturn() : 0.0
                    );
                }
                
                newResults.add(current);
            }
            
            this.results = newResults;
            log.debug("Step 3 completed: calculated predicted OC for {} points", results.size());
            return this;
        }
        
        /**
         * Step 4: Final Return Prediction
         */
        public ARIMAMasterDataPipeline calculateFinalReturn() {
            log.debug("Executing Step 4: Final Return Prediction");
            
            List<TimeSeriesCalculation> newResults = new ArrayList<>();
            
            for (TimeSeriesCalculation calculation : results) {
                // Can only calculate predicted return if we have predicted OC
                if (calculation.predictedOC() != null) {
                    double predictedReturn = calculation.predictedOC() / calculation.openPrice();
                    
                    calculation = calculation.withPredictions(
                        calculation.predictedDiffOC(),
                        calculation.predictedOC(),
                        predictedReturn
                    );
                }
                
                newResults.add(calculation);
            }
            
            this.results = newResults;
            log.debug("Step 4 completed: calculated final returns for {} points", results.size());
            return this;
        }
        
        /**
         * Builds the final ForecastResult
         */
        public ForecastResult buildResult(TradingInstrument instrument, Instant startTime) {
            if (results.isEmpty()) {
                throw new IllegalStateException("Pipeline has no results. Master data may be empty.");
            }
            
            // Calculate metrics and confidence
            Duration executionTime = Duration.between(startTime, Instant.now());
            ForecastMetrics metrics = calculateMetrics(executionTime);
            double confidence = calculateConfidence(metrics);
            
            // Get the final expected return
            double expectedReturn = getFinalExpectedReturn();
            
            // Record model usage
            arimaModel.recordUsage();
            
            log.info("ARIMA model application completed for {} in {}ms: expected return = {}%",
                instrument.getCode(), executionTime.toMillis(),
                String.format("%.4f", expectedReturn * 100));
            
            return ForecastResult.successful(
                instrument,
                Instant.now().plusSeconds(86400), // Next day forecast
                expectedReturn,
                confidence,
                results,
                metrics
            );
        }
        
        private double getFinalExpectedReturn() {
            TimeSeriesCalculation lastCalculation = results.get(results.size() - 1);
            Double predictedReturn = lastCalculation.predictedReturn();
            
            if (predictedReturn == null) {
                throw new IllegalStateException("Final calculation does not have a predicted return");
            }
            
            return predictedReturn;
        }
        
        private ForecastMetrics calculateMetrics(Duration executionTime) {
            Instant dataStart = masterData.get(0).timestamp();
            Instant dataEnd = masterData.get(masterData.size() - 1).timestamp();
            
            // Calculate basic metrics (simplified for now)
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
        
        private double calculateConfidence(ForecastMetrics metrics) {
            // Simplified confidence calculation
            double baseConfidence = 0.8;
            
            // Adjust based on data quality
            if (metrics.dataPointsUsed() < 50) {
                baseConfidence -= 0.1;
            }
            if (metrics.dataPointsUsed() < 30) {
                baseConfidence -= 0.2;
            }
            
            // Check for valid predictions
            long validPredictions = results.stream()
                .filter(calc -> calc.predictedReturn() != null)
                .filter(calc -> !Double.isNaN(calc.predictedReturn()) && !Double.isInfinite(calc.predictedReturn()))
                .count();
            
            double validRatio = (double) validPredictions / results.size();
            baseConfidence *= validRatio;
            
            return Math.max(0.0, Math.min(1.0, baseConfidence));
        }
    }
    
    private void validateInputs(TradingInstrument instrument, List<DemeanDiffOCMasterData> masterData, ARIMAModel model) {
        if (instrument == null) {
            throw new IllegalArgumentException("Trading instrument cannot be null");
        }
        
        if (masterData == null || masterData.isEmpty()) {
            throw new IllegalArgumentException("Master data cannot be null or empty");
        }
        
        if (model == null) {
            throw new IllegalArgumentException("ARIMA model cannot be null");
        }
        
        if (!model.getInstrument().equals(instrument)) {
            throw new IllegalArgumentException(
                String.format("Model instrument (%s) does not match requested instrument (%s)",
                    model.getInstrument().getCode(), instrument.getCode()));
        }
        
        model.validateForForecasting(masterData.size());
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