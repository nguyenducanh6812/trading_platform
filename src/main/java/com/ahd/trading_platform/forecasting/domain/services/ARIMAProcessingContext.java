package com.ahd.trading_platform.forecasting.domain.services;

import com.ahd.trading_platform.forecasting.domain.entities.ARIMAModel;
import com.ahd.trading_platform.forecasting.domain.valueobjects.TimeSeriesCalculation;
import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Context object that holds the state throughout the ARIMA processing pipeline.
 * Contains all intermediate results and configuration needed for the 5-step ARIMA process.
 */
public class ARIMAProcessingContext {
    
    private final TradingInstrument instrument;
    private final List<OHLCV> priceData;
    private final ARIMAModel arimaModel;
    private final Instant startTime;
    
    private List<TimeSeriesCalculation> currentResults;
    private final List<TimeSeriesCalculation> allCalculations;
    
    private ARIMAProcessingContext(TradingInstrument instrument, List<OHLCV> priceData, ARIMAModel arimaModel, Instant startTime) {
        this.instrument = instrument;
        this.priceData = priceData;
        this.arimaModel = arimaModel;
        this.startTime = startTime;
        this.currentResults = new ArrayList<>();
        this.allCalculations = new ArrayList<>();
    }
    
    /**
     * Creates a new processing context for ARIMA pipeline
     */
    public static ARIMAProcessingContext create(TradingInstrument instrument, List<OHLCV> priceData, ARIMAModel arimaModel) {
        return new ARIMAProcessingContext(instrument, priceData, arimaModel, Instant.now());
    }
    
    /**
     * Updates the current results and adds them to all calculations
     */
    public ARIMAProcessingContext withResults(List<TimeSeriesCalculation> results) {
        this.currentResults = new ArrayList<>(results);
        this.allCalculations.addAll(results);
        return this;
    }
    
    /**
     * Gets the current step results
     */
    public List<TimeSeriesCalculation> getCurrentResults() {
        return new ArrayList<>(currentResults);
    }
    
    /**
     * Gets all calculations from all steps
     */
    public List<TimeSeriesCalculation> getAllCalculations() {
        return new ArrayList<>(allCalculations);
    }
    
    /**
     * Gets the trading instrument
     */
    public TradingInstrument getInstrument() {
        return instrument;
    }
    
    /**
     * Gets the price data
     */
    public List<OHLCV> getPriceData() {
        return priceData;
    }
    
    /**
     * Gets the ARIMA model
     */
    public ARIMAModel getArimaModel() {
        return arimaModel;
    }
    
    /**
     * Gets the processing start time
     */
    public Instant getStartTime() {
        return startTime;
    }
    
    /**
     * Gets the final expected return from the last calculation
     */
    public double getFinalExpectedReturn() {
        if (currentResults.isEmpty()) {
            throw new IllegalStateException("No calculations available to get final expected return");
        }
        
        TimeSeriesCalculation lastCalculation = currentResults.get(currentResults.size() - 1);
        Double predictedReturn = lastCalculation.predictedReturn();
        
        if (predictedReturn == null) {
            throw new IllegalStateException("Final calculation does not have a predicted return");
        }
        
        return predictedReturn;
    }
    
    /**
     * Checks if the context has any results
     */
    public boolean hasResults() {
        return !currentResults.isEmpty();
    }
    
    /**
     * Gets the count of current results
     */
    public int getResultCount() {
        return currentResults.size();
    }
    
    /**
     * Gets the count of all calculations across all steps
     */
    public int getTotalCalculationCount() {
        return allCalculations.size();
    }
}