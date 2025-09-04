package com.ahd.trading_platform.forecasting.domain.services;

import com.ahd.trading_platform.forecasting.domain.entities.ARIMAModel;
import com.ahd.trading_platform.forecasting.domain.valueobjects.*;
import com.ahd.trading_platform.marketdata.domain.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Domain service implementing the complete 5-step ARIMA forecasting process.
 * Follows the mathematical process described in the requirements specification.
 */
@Service
@Slf4j
public class ARIMACalculationService {
    
    /**
     * Executes the complete ARIMA forecasting process for a trading instrument.
     * 
     * @param instrument The trading instrument (BTC/ETH)
     * @param priceData Historical OHLCV data sorted by timestamp
     * @param arimaModel The ARIMA model with coefficients
     * @return Complete forecast result with all intermediate calculations
     */
    public ForecastResult executeForecast(TradingInstrument instrument, List<OHLCV> priceData, ARIMAModel arimaModel) {
        log.info("Starting ARIMA forecast for {} with {} data points", instrument.getCode(), priceData.size());
        Instant startTime = Instant.now();
        
        try {
            // Validate inputs
            validateInputs(instrument, priceData, arimaModel);
            
            // Sort data by timestamp to ensure correct ordering
            List<OHLCV> sortedData = priceData.stream()
                .sorted(Comparator.comparing(OHLCV::timestamp))
                .toList();
            
            // Execute the 5-step ARIMA process
            List<TimeSeriesCalculation> calculations = new ArrayList<>();
            
            // Step 0: Prepare Data - Calculate OC and Diff_OC
            List<TimeSeriesCalculation> step0Results = executeStep0PrepareData(sortedData, arimaModel);
            calculations.addAll(step0Results);
            
            // Step 1: AR Lag Data Preparation
            List<TimeSeriesCalculation> step1Results = executeStep1ARLagPreparation(step0Results, arimaModel);
            calculations.addAll(step1Results);
            
            // Step 2: Predicted Difference Calculation
            List<TimeSeriesCalculation> step2Results = executeStep2PredictedDifference(step1Results, arimaModel);
            calculations.addAll(step2Results);
            
            // Step 3: Predicted OC Calculation
            List<TimeSeriesCalculation> step3Results = executeStep3PredictedOC(step2Results);
            calculations.addAll(step3Results);
            
            // Step 4: Final Return Prediction
            List<TimeSeriesCalculation> step4Results = executeStep4FinalReturn(step3Results);
            calculations.addAll(step4Results);
            
            // Calculate metrics and confidence
            Duration executionTime = Duration.between(startTime, Instant.now());
            ForecastMetrics metrics = calculateMetrics(sortedData, arimaModel, executionTime);
            double confidence = calculateConfidence(step4Results, metrics);
            
            // Get the final expected return (last calculation)
            TimeSeriesCalculation finalCalculation = step4Results.get(step4Results.size() - 1);
            double expectedReturn = finalCalculation.predictedReturn();
            
            // Record model usage
            arimaModel.recordUsage();
            
            log.info("ARIMA forecast completed for {} in {}ms: expected return = {:.4f}%", 
                instrument.getCode(), executionTime.toMillis(), expectedReturn * 100);
            
            return ForecastResult.successful(
                instrument, 
                Instant.now().plusSeconds(86400), // Next day forecast
                expectedReturn,
                confidence,
                calculations,
                metrics
            );
            
        } catch (Exception e) {
            log.error("ARIMA forecast failed for {}: {}", instrument.getCode(), e.getMessage(), e);
            throw new ARIMACalculationException("Failed to execute ARIMA forecast: " + e.getMessage(), e);
        }
    }
    
    /**
     * Step 0: Prepare Data to apply ARIMA
     * OC: Open - Close
     * Diff_OC: OC(T) - OC(T-1)
     * Demean_Diff_OC: Diff_OC - Mean_Diff_OC
     */
    private List<TimeSeriesCalculation> executeStep0PrepareData(List<OHLCV> priceData, ARIMAModel model) {
        log.debug("Executing Step 0: Prepare Data");
        
        List<TimeSeriesCalculation> results = new ArrayList<>();
        double meanDiffOC = model.getMeanDiffOC();
        
        for (int i = 0; i < priceData.size(); i++) {
            OHLCV current = priceData.get(i);
            double openPrice = current.open().amount().doubleValue();
            double closePrice = current.close().amount().doubleValue();
            
            // Calculate OC (Open - Close)
            TimeSeriesCalculation calculation = TimeSeriesCalculation.initial(
                current.timestamp(), openPrice, closePrice);
            
            // Calculate Diff_OC and Demean_Diff_OC for points after the first
            if (i > 0) {
                TimeSeriesCalculation previous = results.get(i - 1);
                double diffOC = calculation.oc() - previous.oc();
                double demeanDiffOC = diffOC - meanDiffOC;
                
                calculation = calculation.withDifferences(diffOC, demeanDiffOC);
            }
            
            results.add(calculation);
        }
        
        log.debug("Step 0 completed: processed {} price points", results.size());
        return results;
    }
    
    /**
     * Step 1: AR Lag Data Preparation
     * Ar.L1 (T) = Demean_Diff_OC (T-1)
     * Ar.L2 (T) = Demean_Diff_OC (T-2)
     * ...
     * Ar.LN (T) = Demean_Diff_OC (T-N)
     */
    private List<TimeSeriesCalculation> executeStep1ARLagPreparation(List<TimeSeriesCalculation> step0Results, ARIMAModel model) {
        log.debug("Executing Step 1: AR Lag Data Preparation");
        
        List<TimeSeriesCalculation> results = new ArrayList<>();
        int pOrder = model.getPOrder();
        
        for (int i = 0; i < step0Results.size(); i++) {
            TimeSeriesCalculation current = step0Results.get(i);
            
            // Can only calculate AR lags if we have enough previous data points
            if (i >= pOrder) {
                List<Double> arLags = new ArrayList<>();
                
                // Calculate AR lags from previous demean_diff_oc values
                for (int lagNum = 1; lagNum <= pOrder; lagNum++) {
                    int lagIndex = i - lagNum;
                    TimeSeriesCalculation lagData = step0Results.get(lagIndex);
                    
                    if (lagData.demeanDiffOC() != null) {
                        arLags.add(lagData.demeanDiffOC());
                    } else {
                        arLags.add(0.0); // Use 0 for missing demean values
                    }
                }
                
                current = current.withARLags(arLags);
            }
            
            results.add(current);
        }
        
        log.debug("Step 1 completed: prepared AR lags for {} points", results.size());
        return results;
    }
    
    /**
     * Step 2: Predicted Difference Calculation
     * Prd_Diff_OC(T) = Σ[Ar.L(i) × ARIMA.Coefficient(i)] + Mean_Diff_OC
     */
    private List<TimeSeriesCalculation> executeStep2PredictedDifference(List<TimeSeriesCalculation> step1Results, ARIMAModel model) {
        log.debug("Executing Step 2: Predicted Difference Calculation");
        
        List<TimeSeriesCalculation> results = new ArrayList<>();
        double meanDiffOC = model.getMeanDiffOC();
        
        for (TimeSeriesCalculation calculation : step1Results) {
            // Can only predict if we have AR lag values
            if (calculation.arLags() != null && !calculation.arLags().isEmpty()) {
                double predictedDiffOC = meanDiffOC; // Start with mean
                
                // Add weighted sum of AR lags
                List<Double> arLags = calculation.arLags();
                for (int i = 0; i < arLags.size(); i++) {
                    int lagNumber = i + 1; // Lag numbers are 1-based
                    ARIMACoefficient coefficient = model.getCoefficient(lagNumber);
                    double arValue = arLags.get(i);
                    
                    predictedDiffOC += arValue * coefficient.doubleValue();
                }
                
                calculation = calculation.withPredictions(predictedDiffOC, 0.0, 0.0);
            }
            
            results.add(calculation);
        }
        
        log.debug("Step 2 completed: calculated predicted differences for {} points", results.size());
        return results;
    }
    
    /**
     * Step 3: Predicted OC Calculation
     * Prd_OC(T) = Prd_Diff_OC(T) + OC(T-1)
     */
    private List<TimeSeriesCalculation> executeStep3PredictedOC(List<TimeSeriesCalculation> step2Results) {
        log.debug("Executing Step 3: Predicted OC Calculation");
        
        List<TimeSeriesCalculation> results = new ArrayList<>();
        
        for (int i = 0; i < step2Results.size(); i++) {
            TimeSeriesCalculation current = step2Results.get(i);
            
            // Can only calculate predicted OC if we have predicted diff and previous OC
            if (current.predictedDiffOC() != null && i > 0) {
                TimeSeriesCalculation previous = step2Results.get(i - 1);
                double predictedOC = current.predictedDiffOC() + previous.oc();
                
                current = current.withPredictions(
                    current.predictedDiffOC(), 
                    predictedOC, 
                    current.predictedReturn() != null ? current.predictedReturn() : 0.0
                );
            }
            
            results.add(current);
        }
        
        log.debug("Step 3 completed: calculated predicted OC for {} points", results.size());
        return results;
    }
    
    /**
     * Step 4: Final Return Prediction
     * Prd_Return_Arima(T) = Prd_OC(T) / Open_Price(T)
     */
    private List<TimeSeriesCalculation> executeStep4FinalReturn(List<TimeSeriesCalculation> step3Results) {
        log.debug("Executing Step 4: Final Return Prediction");
        
        List<TimeSeriesCalculation> results = new ArrayList<>();
        
        for (TimeSeriesCalculation calculation : step3Results) {
            // Can only calculate predicted return if we have predicted OC
            if (calculation.predictedOC() != null) {
                double predictedReturn = calculation.predictedOC() / calculation.openPrice();
                
                calculation = calculation.withPredictions(
                    calculation.predictedDiffOC(),
                    calculation.predictedOC(),
                    predictedReturn
                );
            }
            
            results.add(calculation);
        }
        
        log.debug("Step 4 completed: calculated final returns for {} points", results.size());
        return results;
    }
    
    private void validateInputs(TradingInstrument instrument, List<OHLCV> priceData, ARIMAModel model) {
        if (instrument == null) {
            throw new IllegalArgumentException("Trading instrument cannot be null");
        }
        
        if (priceData == null || priceData.isEmpty()) {
            throw new IllegalArgumentException("Price data cannot be null or empty");
        }
        
        if (model == null) {
            throw new IllegalArgumentException("ARIMA model cannot be null");
        }
        
        if (!model.getInstrument().equals(instrument)) {
            throw new IllegalArgumentException(
                String.format("Model instrument (%s) does not match requested instrument (%s)",
                    model.getInstrument().getCode(), instrument.getCode()));
        }
        
        model.validateForForecasting(priceData.size());
    }
    
    private ForecastMetrics calculateMetrics(List<OHLCV> priceData, ARIMAModel model, Duration executionTime) {
        Instant dataStart = priceData.get(0).timestamp();
        Instant dataEnd = priceData.get(priceData.size() - 1).timestamp();
        
        // Calculate basic metrics (simplified for now)
        double mse = 0.001; // Placeholder - would calculate from residuals
        double standardError = Math.sqrt(model.getSigma2());
        
        return ForecastMetrics.successful(
            priceData.size(),
            model.getPOrder(),
            mse,
            standardError,
            executionTime,
            dataStart,
            dataEnd,
            model.getModelVersion()
        );
    }
    
    private double calculateConfidence(List<TimeSeriesCalculation> calculations, ForecastMetrics metrics) {
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
        long validPredictions = calculations.stream()
            .filter(calc -> calc.predictedReturn() != null)
            .filter(calc -> !Double.isNaN(calc.predictedReturn()) && !Double.isInfinite(calc.predictedReturn()))
            .count();
        
        double validRatio = (double) validPredictions / calculations.size();
        baseConfidence *= validRatio;
        
        return Math.max(0.0, Math.min(1.0, baseConfidence));
    }
    
    public static class ARIMACalculationException extends RuntimeException {
        public ARIMACalculationException(String message) {
            super(message);
        }
        
        public ARIMACalculationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}