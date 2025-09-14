package com.ahd.trading_platform.forecasting.domain.entities;

import com.ahd.trading_platform.forecasting.domain.valueobjects.ARIMACoefficient;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ARIMA Model aggregate root representing the complete ARIMA model for a trading instrument.
 * Contains AR coefficients, model parameters, and provides domain operations for forecasting.
 */
public class ARIMAModel {
    private final TradingInstrument instrument;
    private final List<ARIMACoefficient> coefficients;
    private final BigDecimal meanDiffOC;
    private final BigDecimal sigma2;
    private final int pOrder;  // AR order (number of lags)
    private final String modelVersion;
    private final Instant createdAt;
    private Instant lastUsed;
    
    public ARIMAModel(TradingInstrument instrument, List<ARIMACoefficient> coefficients, 
                     BigDecimal meanDiffOC, BigDecimal sigma2, int pOrder, String modelVersion) {
        this.instrument = Objects.requireNonNull(instrument, "Trading instrument cannot be null");
        this.coefficients = Objects.requireNonNull(coefficients, "Coefficients cannot be null");
        this.meanDiffOC = Objects.requireNonNull(meanDiffOC, "Mean Diff OC cannot be null");
        this.sigma2 = Objects.requireNonNull(sigma2, "Sigma2 cannot be null");
        this.pOrder = pOrder;
        this.modelVersion = Objects.requireNonNull(modelVersion, "Model version cannot be null");
        this.createdAt = Instant.now();
        this.lastUsed = createdAt;
        
        validateModel();
    }
    
    private void validateModel() {
        if (coefficients.isEmpty()) {
            throw new IllegalArgumentException("Model must have at least one coefficient");
        }
        
        if (pOrder != coefficients.size()) {
            throw new IllegalArgumentException(
                String.format("P order (%d) must match number of coefficients (%d)", pOrder, coefficients.size()));
        }
        
        if (pOrder <= 0 || pOrder > 50) {
            throw new IllegalArgumentException("P order must be between 1 and 50");
        }
        
        // Validate that all coefficients are AR lags
        for (ARIMACoefficient coeff : coefficients) {
            if (!coeff.isARLag()) {
                throw new IllegalArgumentException("All coefficients must be AR lag coefficients");
            }
        }
        
        // Validate lag sequence (ar.L1, ar.L2, ..., ar.LN)
        for (int i = 0; i < coefficients.size(); i++) {
            ARIMACoefficient coeff = coefficients.get(i);
            int expectedLag = i + 1;
            if (coeff.getLagNumber() != expectedLag) {
                throw new IllegalArgumentException(
                    String.format("Expected lag L%d but found %s", expectedLag, coeff.lagName()));
            }
        }
    }
    
    /**
     * Factory method for creating BTC ARIMA model from master data
     */
    public static ARIMAModel forBTC(Map<String, Object> masterData) {
        return createFromMasterData(TradingInstrument.BTC, masterData, "BTC_ARIMA_v1.0");
    }
    
    /**
     * Factory method for creating ETH ARIMA model from master data
     */
    public static ARIMAModel forETH(Map<String, Object> masterData) {
        return createFromMasterData(TradingInstrument.ETH, masterData, "ETH_ARIMA_v1.0");
    }
    
    /**
     * Factory method for creating BTC ARIMA model with custom version
     */
    public static ARIMAModel forBTC(Map<String, Object> masterData, String version) {
        return createFromMasterData(TradingInstrument.BTC, masterData, version);
    }
    
    /**
     * Factory method for creating ETH ARIMA model with custom version
     */
    public static ARIMAModel forETH(Map<String, Object> masterData, String version) {
        return createFromMasterData(TradingInstrument.ETH, masterData, version);
    }
    
    private static ARIMAModel createFromMasterData(TradingInstrument instrument, Map<String, Object> masterData, String version) {
        // Extract model parameters
        BigDecimal meanDiffOC = new BigDecimal(masterData.get("mean_diff_oc").toString());
        BigDecimal sigma2 = new BigDecimal(masterData.get("sigma2").toString());
        int pOrder = ((Number) masterData.get("p")).intValue();
        
        // Extract AR coefficients
        List<ARIMACoefficient> coefficients = masterData.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("ar.L"))
            .sorted((e1, e2) -> {
                int lag1 = Integer.parseInt(e1.getKey().substring(4));
                int lag2 = Integer.parseInt(e2.getKey().substring(4));
                return Integer.compare(lag1, lag2);
            })
            .map(entry -> ARIMACoefficient.of(entry.getKey(), ((Number) entry.getValue()).doubleValue()))
            .toList();
        
        return new ARIMAModel(instrument, coefficients, meanDiffOC, sigma2, pOrder, version);
    }
    
    /**
     * Gets coefficient for specific AR lag (1-based index)
     */
    public ARIMACoefficient getCoefficient(int lagNumber) {
        if (lagNumber < 1 || lagNumber > coefficients.size()) {
            throw new IllegalArgumentException(
                String.format("Lag number %d is out of range [1, %d]", lagNumber, coefficients.size()));
        }
        return coefficients.get(lagNumber - 1);  // Convert to 0-based index
    }
    
    /**
     * Gets all AR coefficients ordered by lag
     */
    public List<ARIMACoefficient> getCoefficients() {
        return List.copyOf(coefficients);
    }
    
    /**
     * Gets the mean difference OC value
     */
    public double getMeanDiffOC() {
        return meanDiffOC.doubleValue();
    }
    
    /**
     * Gets the sigma2 (variance) parameter
     */
    public double getSigma2() {
        return sigma2.doubleValue();
    }
    
    /**
     * Gets the AR order (number of lags)
     */
    public int getPOrder() {
        return pOrder;
    }
    
    /**
     * Records that this model was used for forecasting
     */
    public void recordUsage() {
        this.lastUsed = Instant.now();
    }
    
    /**
     * Checks if model has sufficient historical data for forecasting
     */
    public boolean requiresSufficientData(int availableDataPoints) {
        return availableDataPoints >= pOrder; // Need exactly P points to make predictions
    }
    
    /**
     * Validates that the model can be used for forecasting
     */
    public void validateForForecasting(int availableDataPoints) {
        if (!requiresSufficientData(availableDataPoints)) {
            throw new IllegalArgumentException(
                String.format("Insufficient data for forecasting. Need at least %d points (P order), but have %d", 
                    pOrder, availableDataPoints));
        }
    }
    
    // Getters
    public TradingInstrument getInstrument() {
        return instrument;
    }
    
    public String getModelVersion() {
        return modelVersion;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getLastUsed() {
        return lastUsed;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ARIMAModel that = (ARIMAModel) obj;
        return Objects.equals(instrument, that.instrument) && 
               Objects.equals(modelVersion, that.modelVersion);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(instrument, modelVersion);
    }
    
    @Override
    public String toString() {
        return String.format("ARIMAModel[instrument=%s, pOrder=%d, version=%s, coefficients=%d]",
            instrument.getCode(), pOrder, modelVersion, coefficients.size());
    }
}