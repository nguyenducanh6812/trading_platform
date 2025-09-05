package com.ahd.trading_platform.marketdata.domain.entities;

import com.ahd.trading_platform.shared.valueobjects.*;
import com.ahd.trading_platform.marketdata.domain.valueobjects.*;
import com.ahd.trading_platform.marketdata.domain.valueobjects.DataQualityMetrics;
import com.ahd.trading_platform.marketdata.domain.events.MarketDataUpdatedEvent;
import lombok.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MarketInstrument aggregate root representing a tradable financial instrument (BTC, ETH, etc.).
 * Manages historical price data and provides domain operations for data analysis.
 */
public class MarketInstrument {
    private final String symbol;
    private final String name;
    private final String baseCurrency;
    private final String quoteCurrency;
    private final List<OHLCV> priceHistory;
    private DataQualityMetrics qualityMetrics;
    private Instant lastUpdated;
    private final List<MarketDataUpdatedEvent> domainEvents;

    public MarketInstrument(String symbol, String name, String baseCurrency, String quoteCurrency) {
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.baseCurrency = Objects.requireNonNull(baseCurrency, "Base currency cannot be null");
        this.quoteCurrency = Objects.requireNonNull(quoteCurrency, "Quote currency cannot be null");
        this.priceHistory = new ArrayList<>();
        this.domainEvents = new ArrayList<>();
        this.lastUpdated = Instant.now();
    }

    // Getter methods
    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    public DataQualityMetrics getQualityMetrics() {
        return qualityMetrics;
    }

    public void setQualityMetrics(DataQualityMetrics qualityMetrics) {
        this.qualityMetrics = qualityMetrics;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public List<MarketDataUpdatedEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * Factory method for creating crypto instruments
     */
    public static MarketInstrument crypto(String symbol, String name) {
        return new MarketInstrument(symbol, name, symbol, "USD");
    }

    /**
     * Factory method for Bitcoin
     */
    public static MarketInstrument bitcoin() {
        return crypto("BTC", "Bitcoin");
    }

    /**
     * Factory method for Ethereum
     */
    public static MarketInstrument ethereum() {
        return crypto("ETH", "Ethereum");
    }

    /**
     * Adds historical price data to this instrument
     */
    public void addPriceData(List<OHLCV> ohlcvData) {
        Objects.requireNonNull(ohlcvData, "OHLCV data cannot be null");

        if (ohlcvData.isEmpty()) {
            return;
        }

        // Validate currency consistency
        validateCurrencyConsistency(ohlcvData);

        // Sort by timestamp and remove duplicates
        List<OHLCV> sortedData = ohlcvData.stream()
                .sorted(Comparator.comparing(OHLCV::timestamp))
                .distinct()
                .collect(Collectors.toList());

        // Merge with existing data
        mergePriceData(sortedData);

        // Update quality metrics
        updateQualityMetrics("EXTERNAL_API");

        // Raise domain event
        raiseMarketDataUpdatedEvent(sortedData);

        this.lastUpdated = Instant.now();
    }

    /**
     *
     *
     * Adds a single OHLCV data point
     */
    public void addPriceData(OHLCV ohlcv) {
        addPriceData(List.of(ohlcv));
    }

    /**
     * Returns price history for a specific time range
     */
    public List<OHLCV> getPriceHistory(TimeRange timeRange) {
        Objects.requireNonNull(timeRange, "Time range cannot be null");

        return priceHistory.stream()
                .filter(ohlcv -> timeRange.contains(ohlcv.timestamp()))
                .sorted(Comparator.comparing(OHLCV::timestamp))
                .toList();
    }

    /**
     * Returns all price history sorted by timestamp
     */
    public List<OHLCV> getPriceHistory() {
        return priceHistory.stream()
                .sorted(Comparator.comparing(OHLCV::timestamp))
                .toList();
    }

    /**
     * Returns the latest price point
     */
    public Optional<OHLCV> getLatestPrice() {
        return priceHistory.stream()
                .max(Comparator.comparing(OHLCV::timestamp));
    }

    /**
     * Calculates simple returns for the price history
     */
    public List<Double> calculateReturns() {
        List<OHLCV> sortedHistory = getPriceHistory();
        if (sortedHistory.size() < 2) {
            return Collections.emptyList();
        }

        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < sortedHistory.size(); i++) {
            OHLCV previous = sortedHistory.get(i - 1);
            OHLCV current = sortedHistory.get(i);

            double previousClose = previous.close().amount().doubleValue();
            double currentClose = current.close().amount().doubleValue();

            if (previousClose > 0) {
                double returnRate = (currentClose - previousClose) / previousClose;
                returns.add(returnRate);
            }
        }

        return returns;
    }

    /**
     * Calculates volatility (standard deviation of returns)
     */
    public double calculateVolatility() {
        List<Double> returns = calculateReturns();
        if (returns.size() < 2) {
            return 0.0;
        }

        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = returns.stream()
                .mapToDouble(r -> Math.pow(r - mean, 2))
                .average()
                .orElse(0.0);

        return Math.sqrt(variance);
    }

    /**
     * Checks if instrument has sufficient data for analysis
     */
    public boolean hasSufficientData() {
        return priceHistory.size() >= 30 && // At least 30 data points
                qualityMetrics != null &&
                qualityMetrics.isAcceptable();
    }

    /**
     * Validates that historical data is within expected date range
     */
    public boolean validateHistoricalDataRange() {
        if (priceHistory.isEmpty()) {
            return false;
        }

        TimeRange expectedRange = TimeRange.forHistoricalData();
        Optional<OHLCV> earliest = priceHistory.stream()
                .min(Comparator.comparing(OHLCV::timestamp));
        Optional<OHLCV> latest = priceHistory.stream()
                .max(Comparator.comparing(OHLCV::timestamp));

        return earliest.isPresent() && expectedRange.contains(earliest.get().timestamp()) && expectedRange.contains(latest.get().timestamp());
    }

    private void validateCurrencyConsistency(List<OHLCV> ohlcvData) {
        boolean hasInconsistentCurrency = ohlcvData.stream()
                .anyMatch(ohlcv -> !ohlcv.close().currency().equals(quoteCurrency));

        if (hasInconsistentCurrency) {
            throw new IllegalArgumentException(
                    String.format("OHLCV data currency must match instrument quote currency: %s", quoteCurrency));
        }
    }

    private void mergePriceData(List<OHLCV> newData) {
        // Create a map of existing data by timestamp for efficient lookup
        Map<Instant, OHLCV> existingDataMap = priceHistory.stream()
                .collect(Collectors.toMap(OHLCV::timestamp, ohlcv -> ohlcv));

        // Add or update data points
        for (OHLCV newOhlcv : newData) {
            existingDataMap.put(newOhlcv.timestamp(), newOhlcv);
        }

        // Replace price history with merged data
        priceHistory.clear();
        priceHistory.addAll(existingDataMap.values());
    }

    private void updateQualityMetrics(String dataSource) {
        // Count duplicates by timestamp
        long totalPoints = priceHistory.size();
        long uniqueTimestamps = priceHistory.stream()
                .map(OHLCV::timestamp)
                .distinct()
                .count();
        int duplicates = (int) (totalPoints - uniqueTimestamps);

        this.qualityMetrics = DataQualityMetrics.create(
                (int) totalPoints, 0, duplicates, dataSource);
    }

    private void raiseMarketDataUpdatedEvent(List<OHLCV> newData) {
        MarketDataUpdatedEvent event = new MarketDataUpdatedEvent(
                this.symbol,
                newData.size(),
                Instant.now());
        this.domainEvents.add(event);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    public int getDataPointCount() {
        return priceHistory.size();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MarketInstrument that = (MarketInstrument) obj;
        return Objects.equals(symbol, that.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }

    @Override
    public String toString() {
        return String.format("MarketInstrument[symbol=%s, name=%s, dataPoints=%d, quality=%s]",
                symbol, name, priceHistory.size(),
                qualityMetrics != null ? qualityMetrics.getQualityLevel() : "UNKNOWN");
    }
}