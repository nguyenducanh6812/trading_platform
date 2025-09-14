package com.ahd.trading_platform.forecasting.domain.valueobjects;

import java.time.Instant;
import java.util.List;

/**
 * Value object representing intermediate calculations in the ARIMA forecasting process.
 * Stores all calculated values for monitoring and troubleshooting as specified in requirements.
 */
public record TimeSeriesCalculation(
    Instant timestamp,
    double openPrice,
    double closePrice,
    double oc,                    // Open - Close
    Double diffOC,               // OC(T) - OC(T-1), nullable for first data point
    Double demeanDiffOC,         // Diff_OC - Mean_Diff_OC, nullable for first data point  
    List<Double> arLags,         // AR lag values [Ar.L1, Ar.L2, ..., Ar.LN]
    Double predictedDiffOC,      // Predicted difference from ARIMA model
    Double predictedOC,          // Predicted OC value
    Double predictedReturn       // Final predicted return ratio
) {
    
    public TimeSeriesCalculation {
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        if (openPrice <= 0) {
            throw new IllegalArgumentException("Open price must be positive");
        }
        if (closePrice <= 0) {
            throw new IllegalArgumentException("Close price must be positive");
        }
        
        // Validate AR lags if present - avoid using contains(null) due to potential issues
        if (arLags != null) {
            for (int i = 0; i < arLags.size(); i++) {
                if (arLags.get(i) == null) {
                    throw new IllegalArgumentException(String.format("AR lag at index %d cannot be null", i));
                }
            }
        }
    }
    
    /**
     * Creates initial calculation with basic price data
     */
    public static TimeSeriesCalculation initial(Instant timestamp, double openPrice, double closePrice) {
        double oc = openPrice - closePrice;
        return new TimeSeriesCalculation(
            timestamp, openPrice, closePrice, oc, null, null, null, null, null, null
        );
    }
    
    /**
     * Creates calculation with difference values
     */
    public TimeSeriesCalculation withDifferences(double diffOC, double demeanDiffOC) {
        return new TimeSeriesCalculation(
            timestamp, openPrice, closePrice, oc, diffOC, demeanDiffOC, 
            arLags, predictedDiffOC, predictedOC, predictedReturn
        );
    }
    
    /**
     * Creates calculation with AR lag values
     */
    public TimeSeriesCalculation withARLags(List<Double> arLags) {
        // Explicit null validation before creating immutable list to avoid List.copyOf() issues
        if (arLags != null) {
            for (int i = 0; i < arLags.size(); i++) {
                if (arLags.get(i) == null) {
                    throw new IllegalArgumentException(String.format("AR lag at index %d is null", i));
                }
            }
        }
        
        return new TimeSeriesCalculation(
            timestamp, openPrice, closePrice, oc, diffOC, demeanDiffOC,
            arLags != null ? List.copyOf(arLags) : null, predictedDiffOC, predictedOC, predictedReturn
        );
    }
    
    /**
     * Creates final calculation with predictions
     */
    public TimeSeriesCalculation withPredictions(double predictedDiffOC, double predictedOC, double predictedReturn) {
        return new TimeSeriesCalculation(
            timestamp, openPrice, closePrice, oc, diffOC, demeanDiffOC, arLags,
            predictedDiffOC, predictedOC, predictedReturn
        );
    }
    
    /**
     * Checks if this calculation has all intermediate values
     */
    public boolean isComplete() {
        return diffOC != null && demeanDiffOC != null && 
               arLags != null && !arLags.isEmpty() &&
               predictedDiffOC != null && predictedOC != null && predictedReturn != null;
    }
    
    /**
     * Gets the calculation step this data represents
     */
    public ForecastStep getCurrentStep() {
        if (predictedReturn != null) return ForecastStep.STEP_4_FINAL_RETURN;
        if (predictedOC != null) return ForecastStep.STEP_3_PREDICTED_OC;
        if (predictedDiffOC != null) return ForecastStep.STEP_2_PREDICTED_DIFFERENCE;
        if (arLags != null && !arLags.isEmpty()) return ForecastStep.STEP_1_AR_LAG_PREPARATION;
        return ForecastStep.STEP_0_PREPARE_DATA;
    }
}