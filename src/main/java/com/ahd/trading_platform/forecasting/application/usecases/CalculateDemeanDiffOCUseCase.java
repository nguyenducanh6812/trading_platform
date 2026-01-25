package com.ahd.trading_platform.forecasting.application.usecases;

import com.ahd.trading_platform.forecasting.domain.repositories.MasterDataRepository;
import com.ahd.trading_platform.forecasting.domain.valueobjects.DemeanDiffOCMasterData;
import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
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
 *
 * Supports any trading symbol across all markets (SPOT, LINEAR, INVERSE, OPTION).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CalculateDemeanDiffOCUseCase {

    private final MasterDataRepository masterDataRepository;
    
    /**
     * Calculates and stores DemeanDiffOC master data for the specified symbol and price data.
     * Note: This method focuses solely on calculation and storage.
     * Existence checking should be handled by the calling use case.
     *
     * @param symbol The trading symbol (e.g., "BTC", "ETH", "BTCUSDT")
     * @param priceData Historical OHLCV data sorted by timestamp (first day used for DiffOC calculation only)
     * @return List of calculated and stored master data points
     */
    public List<DemeanDiffOCMasterData> calculateAndStore(
            String symbol,
            List<OHLCV> priceData) {

        log.info("Starting DemeanDiffOC calculation for {} with {} data points",
            symbol, priceData.size());

        if (priceData.isEmpty()) {
            throw new IllegalArgumentException("Price data cannot be null or empty");
        }

        // Sort data by timestamp to ensure correct ordering
        List<OHLCV> sortedData = priceData.stream()
            .sorted(Comparator.comparing(OHLCV::timestamp))
            .toList();

        // Step 1: Calculate mean of Diff_OC for the entire dataset
        BigDecimal meanDiffOC = calculateMeanDiffOC(sortedData);
        log.debug("Calculated meanDiffOC for {}: {}", symbol, meanDiffOC);

        // Step 2: Calculate master data for each data point
        List<DemeanDiffOCMasterData> masterDataList = new ArrayList<>();

        for (int i = 0; i < sortedData.size(); i++) {
            OHLCV current = sortedData.get(i);

            DemeanDiffOCMasterData masterData;

            if (i == 0) {
                // First data point - no differences calculated
                masterData = DemeanDiffOCMasterData.initial(
                    symbol,
                    current.timestamp(),
                    current.open().amount(),
                    current.close().amount()
                );
            } else {
                // Subsequent data points - calculate differences
                DemeanDiffOCMasterData previous = masterDataList.get(i - 1);

                masterData = DemeanDiffOCMasterData.withDifferences(
                    symbol,
                    current.timestamp(),
                    current.open().amount(),
                    current.close().amount(),
                    previous.oc(),
                    meanDiffOC
                );
            }

            masterDataList.add(masterData);
        }

        // Step 3: Save all master data to repository
        List<DemeanDiffOCMasterData> savedMasterData = masterDataRepository.saveAll(symbol, masterDataList);

        log.info("Successfully calculated and stored {} DemeanDiffOC master data points for {}",
            savedMasterData.size(), symbol);

        return savedMasterData;
    }

    /**
     * Checks if master data exists for the specified parameters
     */
    public boolean masterDataExists(String symbol, TimeRange timeRange) {
        return masterDataRepository.existsForTimeRange(symbol, timeRange);
    }

    /**
     * Gets count of existing master data for the specified parameters
     * (more efficient than loading all data when only count is needed)
     */
    public long getMasterDataCount(String symbol, TimeRange timeRange) {
        return masterDataRepository.countByTimeRange(symbol, timeRange);
    }

    /**
     * Retrieves existing master data for the specified parameters
     */
    public List<DemeanDiffOCMasterData> getMasterData(String symbol, TimeRange timeRange) {
        return masterDataRepository.findByTimeRange(symbol, timeRange);
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
            BigDecimal currentOC = ohlcv.close().amount().subtract(ohlcv.open().amount());
            
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
}