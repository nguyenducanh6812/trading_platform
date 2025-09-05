package com.ahd.trading_platform.forecasting.domain.services;

import com.ahd.trading_platform.forecasting.domain.entities.ARIMAModel;
import com.ahd.trading_platform.forecasting.domain.valueobjects.*;
import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
     * Uses a fluent pipeline pattern for clean, maintainable processing.
     * 
     * @param instrument The trading instrument (BTC/ETH)
     * @param priceData Historical OHLCV data sorted by timestamp
     * @param arimaModel The ARIMA model with coefficients
     * @return Complete forecast result with all intermediate calculations
     */
    public ForecastResult executeForecast(TradingInstrument instrument, List<OHLCV> priceData, ARIMAModel arimaModel) {
        log.info("Starting ARIMA forecast for {} with {} data points", instrument.getCode(), priceData.size());
        
        try {
            // Validate inputs
            validateInputs(instrument, priceData, arimaModel);
            
            // Execute the complete 5-step ARIMA process using pipeline pattern
            return ARIMAPipeline.create(instrument, priceData, arimaModel)
                .prepareData()              // Step 0: Calculate OC, Diff_OC, Demean_Diff_OC
                .calculateARLags()          // Step 1: AR Lag Data Preparation
                .predictDifferences()       // Step 2: Predicted Difference Calculation
                .predictOC()                // Step 3: Predicted OC Calculation
                .calculateFinalReturn()     // Step 4: Final Return Prediction
                .buildResult();             // Build final ForecastResult
            
        } catch (Exception e) {
            log.error("ARIMA forecast failed for {}: {}", instrument.getCode(), e.getMessage(), e);
            throw new ARIMACalculationException("Failed to execute ARIMA forecast: " + e.getMessage(), e);
        }
    }
    
    // Note: Individual step methods have been moved to ARIMAPipeline class
    // This keeps the service focused on validation and orchestration
    // while the pipeline handles the detailed step-by-step processing
    
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
    
    // Note: calculateMetrics and calculateConfidence methods have been moved to ARIMAPipeline class
    // This centralizes all calculation logic within the pipeline for better cohesion
    
    public static class ARIMACalculationException extends RuntimeException {
        public ARIMACalculationException(String message) {
            super(message);
        }
        
        public ARIMACalculationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}