package com.ahd.trading_platform.forecasting.application.usecases;

import com.ahd.trading_platform.forecasting.domain.repositories.DemeanDiffOCMasterDataRepository;
import com.ahd.trading_platform.forecasting.domain.valueobjects.DemeanDiffOCMasterData;
import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Use case for calculating and storing DemeanDiffOC master data.
 * This separates data preparation from model application, enabling:
 * - Reusability across different forecasting models (ARIMA, LSTM, Prophet, etc.)
 * - Caching of expensive calculations
 * - Independent testing of data preparation logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CalculateDemeanDiffOCUseCase {
    
    private final DemeanDiffOCMasterDataRepository masterDataRepository;
    
    private static final String CURRENT_CALCULATION_VERSION = "v1.0.0";
    
    /**
     * Calculates DemeanDiffOC master data for the specified instrument and price data.
     * If master data already exists for the time range, it will be skipped.
     * 
     * @param instrument The trading instrument
     * @param priceData Historical OHLCV data sorted by timestamp
     * @return List of calculated master data points
     */
    public List<DemeanDiffOCMasterData> calculateAndStore(
            TradingInstrument instrument, 
            List<OHLCV> priceData) {
        
        log.info("Starting DemeanDiffOC calculation for {} with {} data points", 
            instrument.getCode(), priceData.size());
        
        if (priceData == null || priceData.isEmpty()) {
            throw new IllegalArgumentException("Price data cannot be null or empty");
        }
        
        // Sort data by timestamp to ensure correct ordering
        List<OHLCV> sortedData = priceData.stream()
            .sorted(Comparator.comparing(OHLCV::timestamp))
            .toList();
            
        // Check if we already have master data for this time range
        TimeRange timeRange = new TimeRange(
            sortedData.get(0).timestamp(),
            sortedData.get(sortedData.size() - 1).timestamp()
        );
        
        if (masterDataRepository.existsForInstrumentAndTimeRange(
                instrument, timeRange, CURRENT_CALCULATION_VERSION)) {
            log.info("Master data already exists for {} in time range {} - {}. Loading existing data.", 
                instrument.getCode(), timeRange.from(), timeRange.to());
            return masterDataRepository.findByInstrumentAndTimeRange(
                instrument, timeRange, CURRENT_CALCULATION_VERSION);
        }
        
        // Step 1: Calculate mean of Diff_OC for the entire dataset
        BigDecimal meanDiffOC = calculateMeanDiffOC(sortedData);
        log.debug("Calculated meanDiffOC for {}: {}", instrument.getCode(), meanDiffOC);
        
        // Step 2: Calculate master data for each data point
        List<DemeanDiffOCMasterData> masterDataList = new ArrayList<>();
        
        for (int i = 0; i < sortedData.size(); i++) {
            OHLCV current = sortedData.get(i);
            
            DemeanDiffOCMasterData masterData;
            
            if (i == 0) {
                // First data point - no differences calculated
                masterData = DemeanDiffOCMasterData.initial(
                    instrument,
                    current.timestamp(),
                    current.open().amount(),
                    current.close().amount(),
                    meanDiffOC,
                    CURRENT_CALCULATION_VERSION
                );
            } else {
                // Subsequent data points - calculate differences
                DemeanDiffOCMasterData previous = masterDataList.get(i - 1);
                
                masterData = DemeanDiffOCMasterData.withDifferences(
                    instrument,
                    current.timestamp(),
                    current.open().amount(),
                    current.close().amount(),
                    previous.oc(),
                    meanDiffOC,
                    CURRENT_CALCULATION_VERSION
                );
            }
            
            masterDataList.add(masterData);
        }
        
        // Step 3: Save all master data to repository
        List<DemeanDiffOCMasterData> savedMasterData = masterDataRepository.saveAll(masterDataList);
        
        log.info("Successfully calculated and stored {} DemeanDiffOC master data points for {}", 
            savedMasterData.size(), instrument.getCode());
        
        return savedMasterData;
    }
    
    /**
     * Checks if master data exists for the specified parameters
     */
    public boolean masterDataExists(TradingInstrument instrument, TimeRange timeRange) {
        return masterDataRepository.existsForInstrumentAndTimeRange(
            instrument, timeRange, CURRENT_CALCULATION_VERSION);
    }
    
    /**
     * Retrieves existing master data for the specified parameters
     */
    public List<DemeanDiffOCMasterData> getMasterData(
            TradingInstrument instrument, 
            TimeRange timeRange) {
        
        return masterDataRepository.findByInstrumentAndTimeRange(
            instrument, timeRange, CURRENT_CALCULATION_VERSION);
    }
    
    /**
     * Calculates mean of Diff_OC for the entire dataset.
     * This is needed for demeaning calculations.
     */
    private BigDecimal calculateMeanDiffOC(List<OHLCV> priceData) {
        if (priceData.size() < 2) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sumDiffOC = BigDecimal.ZERO;
        int count = 0;
        
        BigDecimal previousOC = null;
        
        for (OHLCV ohlcv : priceData) {
            BigDecimal currentOC = ohlcv.open().amount().subtract(ohlcv.close().amount());
            
            if (previousOC != null) {
                BigDecimal diffOC = currentOC.subtract(previousOC);
                sumDiffOC = sumDiffOC.add(diffOC);
                count++;
            }
            
            previousOC = currentOC;
        }
        
        if (count == 0) {
            return BigDecimal.ZERO;
        }
        
        return sumDiffOC.divide(BigDecimal.valueOf(count), 8, RoundingMode.HALF_UP);
    }
    
    /**
     * Clears master data for the specified instrument (for testing/maintenance)
     */
    public void clearMasterData(TradingInstrument instrument) {
        log.warn("Clearing all master data for instrument: {}", instrument.getCode());
        masterDataRepository.deleteByInstrumentAndCalculationVersion(
            instrument, CURRENT_CALCULATION_VERSION);
    }
}