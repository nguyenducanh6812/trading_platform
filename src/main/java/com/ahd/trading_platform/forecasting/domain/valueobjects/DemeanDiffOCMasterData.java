package com.ahd.trading_platform.forecasting.domain.valueobjects;

import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain value object representing pre-calculated Demean_Diff_OC master data.
 * This data is reusable across different forecasting models (ARIMA, LSTM, Prophet, etc.).
 * 
 * Contains the fundamental time series transformations:
 * - OC: Open - Close price difference
 * - Diff_OC: First difference of OC series  
 * - Demean_Diff_OC: Demeaned first difference (stationary series)
 */
public record DemeanDiffOCMasterData(
    TradingInstrument instrument,
    Instant timestamp,
    BigDecimal openPrice,
    BigDecimal closePrice,
    BigDecimal oc,
    BigDecimal diffOC,           // Null for first data point
    BigDecimal demeanDiffOC,     // Null for first data point  
    BigDecimal meanDiffOC,       // Mean used for demeaning
    String calculationVersion,
    Instant calculatedAt
) {
    
    /**
     * Creates initial master data point (first in series)
     */
    public static DemeanDiffOCMasterData initial(
            TradingInstrument instrument,
            Instant timestamp, 
            BigDecimal openPrice,
            BigDecimal closePrice,
            BigDecimal meanDiffOC,
            String calculationVersion) {
        
        BigDecimal oc = openPrice.subtract(closePrice);
        
        return new DemeanDiffOCMasterData(
            instrument, timestamp, openPrice, closePrice, oc,
            null, null, // No diff/demean for first point
            meanDiffOC, calculationVersion, Instant.now()
        );
    }
    
    /**
     * Creates subsequent master data point with differences calculated
     */
    public static DemeanDiffOCMasterData withDifferences(
            TradingInstrument instrument,
            Instant timestamp,
            BigDecimal openPrice, 
            BigDecimal closePrice,
            BigDecimal previousOC,
            BigDecimal meanDiffOC,
            String calculationVersion) {
        
        BigDecimal oc = openPrice.subtract(closePrice);
        BigDecimal diffOC = oc.subtract(previousOC);
        BigDecimal demeanDiffOC = diffOC.subtract(meanDiffOC);
        
        return new DemeanDiffOCMasterData(
            instrument, timestamp, openPrice, closePrice, oc,
            diffOC, demeanDiffOC, meanDiffOC, calculationVersion, Instant.now()
        );
    }
    
    /**
     * Checks if this data point has difference calculations
     */
    public boolean hasDifferences() {
        return diffOC != null && demeanDiffOC != null;
    }
    
    /**
     * Validates that demean difference is available for model calculations
     */
    public void requireDemeanDiffOC() {
        if (demeanDiffOC == null) {
            throw new IllegalStateException(
                String.format("DemeanDiffOC is required for model calculations but is null for %s at %s", 
                    instrument, timestamp));
        }
    }
    
    /**
     * Gets the unique identifier for this master data point
     */
    public String getUniqueKey() {
        return String.format("%s_%s_%s", 
            instrument.name(), 
            timestamp.toEpochMilli(),
            calculationVersion);
    }
    
    /**
     * Converts to double value for numerical calculations
     */
    public double demeanDiffOCAsDouble() {
        requireDemeanDiffOC();
        return demeanDiffOC.doubleValue();
    }
    
    /**
     * Converts OC to double value for calculations
     */
    public double ocAsDouble() {
        return oc.doubleValue();
    }
    
    /**
     * Converts open price to double value for calculations  
     */
    public double openPriceAsDouble() {
        return openPrice.doubleValue();
    }
    
    /**
     * Converts close price to double value for calculations
     */
    public double closePriceAsDouble() {
        return closePrice.doubleValue();
    }
}