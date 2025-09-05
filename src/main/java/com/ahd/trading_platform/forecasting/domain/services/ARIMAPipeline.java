package com.ahd.trading_platform.forecasting.domain.services;

import com.ahd.trading_platform.forecasting.domain.entities.ARIMAModel;
import com.ahd.trading_platform.forecasting.domain.valueobjects.*;
import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Fluent pipeline for executing the 5-step ARIMA forecasting process.
 * Provides a clean, chainable interface that makes the process flow explicit and maintainable.
 */
@Getter
@Slf4j
public class ARIMAPipeline {

    /**
     * -- GETTER --
     *  Gets the current processing context (for advanced usage)
     */
    private ARIMAProcessingContext context;
    
    private ARIMAPipeline(ARIMAProcessingContext context) {
        this.context = context;
    }
    
    /**
     * Creates a new ARIMA processing pipeline
     */
    public static ARIMAPipeline create(TradingInstrument instrument, List<OHLCV> priceData, ARIMAModel arimaModel) {
        // Sort data by timestamp to ensure correct ordering
        List<OHLCV> sortedData = priceData.stream()
            .sorted(Comparator.comparing(OHLCV::timestamp))
            .toList();
            
        ARIMAProcessingContext context = ARIMAProcessingContext.create(instrument, sortedData, arimaModel);
        return new ARIMAPipeline(context);
    }
    
    /**
     * Step 0: Prepare Data to apply ARIMA
     * Calculates OC, Diff_OC, and Demean_Diff_OC
     */
    public ARIMAPipeline prepareData() {
        log.debug("Executing Step 0: Prepare Data");
        
        List<TimeSeriesCalculation> results = new ArrayList<>();
        double meanDiffOC = context.getArimaModel().getMeanDiffOC();
        List<OHLCV> priceData = context.getPriceData();
        
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
        
        context = context.withResults(results);
        log.debug("Step 0 completed: processed {} price points", results.size());
        return this;
    }
    
    /**
     * Step 1: AR Lag Data Preparation
     * Calculates Ar.L1, Ar.L2, ..., Ar.LN from previous demean_diff_oc values
     */
    public ARIMAPipeline calculateARLags() {
        log.debug("Executing Step 1: AR Lag Data Preparation");
        
        List<TimeSeriesCalculation> previousResults = context.getCurrentResults();
        List<TimeSeriesCalculation> results = new ArrayList<>();
        int pOrder = context.getArimaModel().getPOrder();
        
        for (int i = 0; i < previousResults.size(); i++) {
            TimeSeriesCalculation current = previousResults.get(i);
            
            // Can only calculate AR lags if we have enough previous data points
            if (i >= pOrder) {
                List<Double> arLags = new ArrayList<>();
                
                // Calculate AR lags from previous demean_diff_oc values
                for (int lagNum = 1; lagNum <= pOrder; lagNum++) {
                    int lagIndex = i - lagNum;
                    TimeSeriesCalculation lagData = previousResults.get(lagIndex);
                    
                    if (lagData.demeanDiffOC() != null) {
                        arLags.add(lagData.demeanDiffOC());
                    } else {
                        // Missing demean values indicate a serious business logic error
                        throw new IllegalStateException(String.format(
                            "Missing demeanDiffOC value at lag %d (index %d) for data point %d. " +
                            "This indicates an error in Step 0 (Data Preparation). " +
                            "Expected demeanDiffOC to be calculated for all points after the first.",
                            lagNum, lagIndex, i
                        ));
                    }
                }
                
                current = current.withARLags(arLags);
            }
            
            results.add(current);
        }
        
        context = context.withResults(results);
        log.debug("Step 1 completed: prepared AR lags for {} points", results.size());
        return this;
    }
    
    /**
     * Step 2: Predicted Difference Calculation
     * Calculates Prd_Diff_OC(T) = Σ[Ar.L(i) × ARIMA.Coefficient(i)] + Mean_Diff_OC
     */
    public ARIMAPipeline predictDifferences() {
        log.debug("Executing Step 2: Predicted Difference Calculation");
        
        List<TimeSeriesCalculation> previousResults = context.getCurrentResults();
        List<TimeSeriesCalculation> results = new ArrayList<>();
        double meanDiffOC = context.getArimaModel().getMeanDiffOC();
        
        for (TimeSeriesCalculation calculation : previousResults) {
            // Can only predict if we have AR lag values
            if (calculation.arLags() != null && !calculation.arLags().isEmpty()) {
                double predictedDiffOC = meanDiffOC; // Start with mean
                
                // Add weighted sum of AR lags
                List<Double> arLags = calculation.arLags();
                for (int i = 0; i < arLags.size(); i++) {
                    int lagNumber = i + 1; // Lag numbers are 1-based
                    ARIMACoefficient coefficient = context.getArimaModel().getCoefficient(lagNumber);
                    double arValue = arLags.get(i);
                    
                    predictedDiffOC += arValue * coefficient.doubleValue();
                }
                
                calculation = calculation.withPredictions(predictedDiffOC, 0.0, 0.0);
            }
            
            results.add(calculation);
        }
        
        context = context.withResults(results);
        log.debug("Step 2 completed: calculated predicted differences for {} points", results.size());
        return this;
    }
    
    /**
     * Step 3: Predicted OC Calculation
     * Calculates Prd_OC(T) = Prd_Diff_OC(T) + OC(T-1)
     */
    public ARIMAPipeline predictOC() {
        log.debug("Executing Step 3: Predicted OC Calculation");
        
        List<TimeSeriesCalculation> previousResults = context.getCurrentResults();
        List<TimeSeriesCalculation> results = new ArrayList<>();
        
        for (int i = 0; i < previousResults.size(); i++) {
            TimeSeriesCalculation current = previousResults.get(i);
            
            // Can only calculate predicted OC if we have predicted diff and previous OC
            if (current.predictedDiffOC() != null && i > 0) {
                TimeSeriesCalculation previous = previousResults.get(i - 1);
                double predictedOC = current.predictedDiffOC() + previous.oc();
                
                current = current.withPredictions(
                    current.predictedDiffOC(), 
                    predictedOC, 
                    current.predictedReturn() != null ? current.predictedReturn() : 0.0
                );
            }
            
            results.add(current);
        }
        
        context = context.withResults(results);
        log.debug("Step 3 completed: calculated predicted OC for {} points", results.size());
        return this;
    }
    
    /**
     * Step 4: Final Return Prediction
     * Calculates Prd_Return_Arima(T) = Prd_OC(T) / Open_Price(T)
     */
    public ARIMAPipeline calculateFinalReturn() {
        log.debug("Executing Step 4: Final Return Prediction");
        
        List<TimeSeriesCalculation> previousResults = context.getCurrentResults();
        List<TimeSeriesCalculation> results = new ArrayList<>();
        
        for (TimeSeriesCalculation calculation : previousResults) {
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
        
        context = context.withResults(results);
        log.debug("Step 4 completed: calculated final returns for {} points", results.size());
        return this;
    }
    
    /**
     * Builds the final ForecastResult from the pipeline execution
     */
    public ForecastResult buildResult() {
        if (!context.hasResults()) {
            throw new IllegalStateException("Pipeline has no results. Did you forget to execute the steps?");
        }
        
        // Calculate metrics and confidence
        Duration executionTime = Duration.between(context.getStartTime(), Instant.now());
        ForecastMetrics metrics = calculateMetrics(executionTime);
        double confidence = calculateConfidence(metrics);
        
        // Get the final expected return
        double expectedReturn = context.getFinalExpectedReturn();
        
        // Record model usage
        context.getArimaModel().recordUsage();
        
        log.info("ARIMA forecast completed for {} in {}ms: expected return = {}%", 
            context.getInstrument().getCode(), executionTime.toMillis(), 
            String.format("%.4f", expectedReturn * 100));
        
        return ForecastResult.successful(
            context.getInstrument(),
            Instant.now().plusSeconds(86400), // Next day forecast
            expectedReturn,
            confidence,
            context.getAllCalculations(),
            metrics
        );
    }

    private ForecastMetrics calculateMetrics(Duration executionTime) {
        List<OHLCV> priceData = context.getPriceData();
        ARIMAModel model = context.getArimaModel();
        
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
        List<TimeSeriesCalculation> calculations = context.getCurrentResults();
        long validPredictions = calculations.stream()
            .filter(calc -> calc.predictedReturn() != null)
            .filter(calc -> !Double.isNaN(calc.predictedReturn()) && !Double.isInfinite(calc.predictedReturn()))
            .count();
        
        double validRatio = (double) validPredictions / calculations.size();
        baseConfidence *= validRatio;
        
        return Math.max(0.0, Math.min(1.0, baseConfidence));
    }
}