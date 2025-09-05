package com.ahd.trading_platform.forecasting.domain.services;

import com.ahd.trading_platform.forecasting.domain.entities.ARIMAModel;
import com.ahd.trading_platform.forecasting.domain.valueobjects.*;
import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.Price;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ARIMACalculationService.
 * Tests the complete 5-step ARIMA forecasting process with real-world scenarios.
 */
@ExtendWith(MockitoExtension.class)
class ARIMACalculationServiceTest {
    
    private ARIMACalculationService arimaCalculationService;
    private ARIMAModel btcModel;
    private List<OHLCV> testPriceData;
    
    @BeforeEach
    void setUp() {
        arimaCalculationService = new ARIMACalculationService();
        
        // Create test BTC ARIMA model with simplified coefficients
        btcModel = createTestBTCModel();
        
        // Create test price data with realistic BTC prices
        testPriceData = createTestPriceData();
    }
    
    @Test
    @DisplayName("Should execute complete ARIMA forecast successfully")
    void shouldExecuteCompleteARIMAForecast() {
        // When
        ForecastResult result = arimaCalculationService.executeForecast(
            TradingInstrument.BTC, testPriceData, btcModel);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.instrument()).isEqualTo(TradingInstrument.BTC);
        assertThat(result.expectedReturn()).isNotNaN().isFinite();
        assertThat(result.confidenceLevel()).isBetween(0.0, 1.0);
        assertThat(result.calculations()).isNotEmpty();
        assertThat(result.metrics()).isNotNull();
        assertThat(result.metrics().dataPointsUsed()).isEqualTo(testPriceData.size());
        assertThat(result.metrics().arOrder()).isEqualTo(btcModel.getPOrder());
        
        // Verify forecast summary
        assertThat(result.getSummary()).contains("BTC", "expected return", "confidence");
    }
    
    @Test
    @DisplayName("Should validate all 5 ARIMA calculation steps")
    void shouldValidateAll5ARIMASteps() {
        // When
        ForecastResult result = arimaCalculationService.executeForecast(
            TradingInstrument.BTC, testPriceData, btcModel);
        
        // Then - verify all calculation steps are present
        List<TimeSeriesCalculation> calculations = result.calculations();
        
        // Check Step 0: Prepare Data
        List<TimeSeriesCalculation> step0Calcs = calculations.stream()
            .filter(calc -> calc.getCurrentStep() == ForecastStep.STEP_0_PREPARE_DATA)
            .toList();
        assertThat(step0Calcs).isNotEmpty();
        
        // Verify OC calculation (Open - Close)
        TimeSeriesCalculation firstCalc = step0Calcs.get(0);
        assertThat(firstCalc.oc()).isEqualTo(firstCalc.openPrice() - firstCalc.closePrice());
        
        // Check Step 1: AR Lag Preparation  
        List<TimeSeriesCalculation> step1Calcs = calculations.stream()
            .filter(calc -> calc.getCurrentStep() == ForecastStep.STEP_1_AR_LAG_PREPARATION)
            .toList();
        assertThat(step1Calcs).isNotEmpty();
        
        // Verify AR lags are calculated after sufficient data points
        step1Calcs.stream()
            .filter(calc -> calc.arLags() != null)
            .forEach(calc -> {
                assertThat(calc.arLags()).hasSize(btcModel.getPOrder());
                assertThat(calc.arLags()).doesNotContainNull();
            });
        
        // Check Step 2: Predicted Difference
        List<TimeSeriesCalculation> step2Calcs = calculations.stream()
            .filter(calc -> calc.getCurrentStep() == ForecastStep.STEP_2_PREDICTED_DIFFERENCE)
            .toList();
        assertThat(step2Calcs).isNotEmpty();
        
        // Verify predictions are calculated
        step2Calcs.stream()
            .filter(calc -> calc.predictedDiffOC() != null)
            .forEach(calc -> {
                assertThat(calc.predictedDiffOC()).isNotNaN().isFinite();
            });
        
        // Check Step 3: Predicted OC
        List<TimeSeriesCalculation> step3Calcs = calculations.stream()
            .filter(calc -> calc.getCurrentStep() == ForecastStep.STEP_3_PREDICTED_OC)
            .toList();
        assertThat(step3Calcs).isNotEmpty();
        
        // Check Step 4: Final Return
        List<TimeSeriesCalculation> step4Calcs = calculations.stream()
            .filter(calc -> calc.getCurrentStep() == ForecastStep.STEP_4_FINAL_RETURN)
            .toList();
        assertThat(step4Calcs).isNotEmpty();
        
        // Verify final return calculation
        step4Calcs.stream()
            .filter(calc -> calc.predictedReturn() != null)
            .forEach(calc -> {
                assertThat(calc.predictedReturn()).isNotNaN().isFinite();
                // Predicted return should be: predictedOC / openPrice
                if (calc.predictedOC() != null) {
                    double expectedReturn = calc.predictedOC() / calc.openPrice();
                    assertThat(calc.predictedReturn()).isCloseTo(expectedReturn, within(0.0001));
                }
            });
    }
    
    @Test
    @DisplayName("Should handle insufficient data gracefully")
    void shouldHandleInsufficientData() {
        // Given - insufficient data (less than required for ARIMA model)
        List<OHLCV> insufficientData = testPriceData.subList(0, 5);
        
        // When & Then
        assertThatThrownBy(() -> 
            arimaCalculationService.executeForecast(TradingInstrument.BTC, insufficientData, btcModel))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Insufficient data for forecasting");
    }
    
    @Test
    @DisplayName("Should validate input parameters")
    void shouldValidateInputParameters() {
        // Test null instrument
        assertThatThrownBy(() -> 
            arimaCalculationService.executeForecast(null, testPriceData, btcModel))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Trading instrument cannot be null");
        
        // Test null price data
        assertThatThrownBy(() -> 
            arimaCalculationService.executeForecast(TradingInstrument.BTC, null, btcModel))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Price data cannot be null or empty");
        
        // Test empty price data
        assertThatThrownBy(() -> 
            arimaCalculationService.executeForecast(TradingInstrument.BTC, List.of(), btcModel))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Price data cannot be null or empty");
        
        // Test null model
        assertThatThrownBy(() -> 
            arimaCalculationService.executeForecast(TradingInstrument.BTC, testPriceData, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ARIMA model cannot be null");
        
        // Test instrument mismatch
        ARIMAModel ethModel = createTestETHModel();
        assertThatThrownBy(() -> 
            arimaCalculationService.executeForecast(TradingInstrument.BTC, testPriceData, ethModel))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Model instrument (ETH) does not match requested instrument (BTC)");
    }
    
    @Test
    @DisplayName("Should calculate forecast metrics accurately")
    void shouldCalculateForecastMetricsAccurately() {
        // When
        ForecastResult result = arimaCalculationService.executeForecast(
            TradingInstrument.BTC, testPriceData, btcModel);
        
        // Then
        ForecastMetrics metrics = result.metrics();
        
        assertThat(metrics.dataPointsUsed()).isEqualTo(testPriceData.size());
        assertThat(metrics.arOrder()).isEqualTo(btcModel.getPOrder());
        assertThat(metrics.executionTime()).isNotNull().isPositive();
        assertThat(metrics.dataRangeStart()).isEqualTo(testPriceData.get(0).timestamp());
        assertThat(metrics.dataRangeEnd()).isEqualTo(testPriceData.get(testPriceData.size() - 1).timestamp());
        assertThat(metrics.modelVersion()).isEqualTo(btcModel.getModelVersion());
        assertThat(metrics.meanSquaredError()).isNotNaN().isFinite();
        assertThat(metrics.standardError()).isNotNaN().isFinite();
        
        // Verify data quality assessment
        assertThat(metrics.hasSufficientQuality()).isTrue();
        assertThat(metrics.getDataRangeDays()).isPositive();
        assertThat(metrics.getDataDensity()).isPositive();
    }
    
    @Test
    @DisplayName("Should sort price data by timestamp before processing")
    void shouldSortPriceDataByTimestamp() {
        // Given - unsorted price data
        List<OHLCV> unsortedData = new ArrayList<>(testPriceData);
        java.util.Collections.shuffle(unsortedData);
        
        // When
        ForecastResult result = arimaCalculationService.executeForecast(
            TradingInstrument.BTC, unsortedData, btcModel);
        
        // Then - should still produce valid results
        assertThat(result).isNotNull();
        assertThat(result.expectedReturn()).isNotNaN().isFinite();
        assertThat(result.calculations()).isNotEmpty();
        
        // Verify calculations are in chronological order
        List<TimeSeriesCalculation> calculations = result.calculations();
        for (int i = 1; i < calculations.size(); i++) {
            Instant current = calculations.get(i).timestamp();
            Instant previous = calculations.get(i - 1).timestamp();
            assertThat(current).isAfterOrEqualTo(previous);
        }
    }
    
    @Test
    @DisplayName("Should handle edge cases in price data")
    void shouldHandleEdgeCasesInPriceData() {
        // Test with very small price changes
        List<OHLCV> stablePrices = createStablePriceData();
        
        ForecastResult result = arimaCalculationService.executeForecast(
            TradingInstrument.BTC, stablePrices, btcModel);
        
        assertThat(result).isNotNull();
        assertThat(result.expectedReturn()).isNotNaN().isFinite();
        
        // Test with high volatility prices
        List<OHLCV> volatilePrices = createVolatilePriceData();
        
        ForecastResult volatileResult = arimaCalculationService.executeForecast(
            TradingInstrument.BTC, volatilePrices, btcModel);
        
        assertThat(volatileResult).isNotNull();
        assertThat(volatileResult.expectedReturn()).isNotNaN().isFinite();
    }
    
    @Test
    @DisplayName("Should record model usage after successful forecast")
    void shouldRecordModelUsageAfterSuccessfulForecast() {
        // Given
        Instant beforeUsage = btcModel.getLastUsed();
        
        // When
        arimaCalculationService.executeForecast(TradingInstrument.BTC, testPriceData, btcModel);
        
        // Then
        assertThat(btcModel.getLastUsed()).isAfter(beforeUsage);
    }
    
    private ARIMAModel createTestBTCModel() {
        Map<String, Object> masterData = new HashMap<>();
        
        // Add simplified AR coefficients (5 lags instead of 30 for testing)
        masterData.put("ar.L1", -0.5);
        masterData.put("ar.L2", -0.3);
        masterData.put("ar.L3", -0.2);
        masterData.put("ar.L4", -0.1);
        masterData.put("ar.L5", -0.05);
        
        masterData.put("mean_diff_oc", 2.5);
        masterData.put("sigma2", 1000.0);
        masterData.put("p", 5);
        
        return ARIMAModel.forBTC(masterData);
    }
    
    private ARIMAModel createTestETHModel() {
        Map<String, Object> masterData = new HashMap<>();
        
        // Add simplified AR coefficients for ETH
        masterData.put("ar.L1", -0.4);
        masterData.put("ar.L2", -0.25);
        masterData.put("ar.L3", -0.15);
        masterData.put("ar.L4", -0.08);
        masterData.put("ar.L5", -0.03);
        
        masterData.put("mean_diff_oc", 0.05);
        masterData.put("sigma2", 100.0);
        masterData.put("p", 5);
        
        return ARIMAModel.forETH(masterData);
    }
    
    private List<OHLCV> createTestPriceData() {
        List<OHLCV> priceData = new ArrayList<>();
        Instant baseTime = Instant.now().minus(50, ChronoUnit.DAYS);
        
        // Create 50 days of realistic BTC price data
        double basePrice = 45000.0;
        for (int i = 0; i < 50; i++) {
            Instant timestamp = baseTime.plus(i, ChronoUnit.DAYS);
            
            // Add some realistic price variation
            double priceVariation = (Math.random() - 0.5) * 2000; // ±$1000 variation
            double open = basePrice + priceVariation;
            double close = open + (Math.random() - 0.5) * 500; // ±$250 daily change
            double high = Math.max(open, close) + Math.random() * 200;
            double low = Math.min(open, close) - Math.random() * 200;
            long volume = (long) (Math.random() * 1000000 + 500000);
            
            OHLCV ohlcv = OHLCV.fromUsdValues(
                open, high, low, close, volume, timestamp
            );
            
            priceData.add(ohlcv);
            basePrice = close; // Use close as next day's base
        }
        
        return priceData;
    }
    
    private List<OHLCV> createStablePriceData() {
        List<OHLCV> priceData = new ArrayList<>();
        Instant baseTime = Instant.now().minus(40, ChronoUnit.DAYS);
        
        // Create stable price data with minimal variation
        double basePrice = 45000.0;
        for (int i = 0; i < 40; i++) {
            Instant timestamp = baseTime.plus(i, ChronoUnit.DAYS);
            
            double open = basePrice + (Math.random() - 0.5) * 10; // ±$5 variation
            double close = open + (Math.random() - 0.5) * 20; // ±$10 daily change
            double high = Math.max(open, close) + Math.random() * 5;
            double low = Math.min(open, close) - Math.random() * 5;
            long volume = 100000;
            
            OHLCV ohlcv = OHLCV.fromUsdValues(
                open, high, low, close, volume, timestamp
            );
            
            priceData.add(ohlcv);
            basePrice = close;
        }
        
        return priceData;
    }
    
    private List<OHLCV> createVolatilePriceData() {
        List<OHLCV> priceData = new ArrayList<>();
        Instant baseTime = Instant.now().minus(40, ChronoUnit.DAYS);
        
        // Create highly volatile price data
        double basePrice = 45000.0;
        for (int i = 0; i < 40; i++) {
            Instant timestamp = baseTime.plus(i, ChronoUnit.DAYS);
            
            double open = basePrice + (Math.random() - 0.5) * 5000; // ±$2500 variation
            double close = open + (Math.random() - 0.5) * 3000; // ±$1500 daily change
            double high = Math.max(open, close) + Math.random() * 1000;
            double low = Math.min(open, close) - Math.random() * 1000;
            long volume = (long) (Math.random() * 2000000 + 1000000);
            
            OHLCV ohlcv = OHLCV.fromUsdValues(
                open, high, low, close, volume, timestamp
            );
            
            priceData.add(ohlcv);
            basePrice = close;
        }
        
        return priceData;
    }
}