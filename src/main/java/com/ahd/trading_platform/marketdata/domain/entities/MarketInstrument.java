package com.ahd.trading_platform.marketdata.domain.entities;

import com.ahd.trading_platform.shared.valueobjects.*;
import com.ahd.trading_platform.marketdata.domain.valueobjects.*;
import com.ahd.trading_platform.marketdata.domain.valueobjects.DataQualityMetrics;
import com.ahd.trading_platform.marketdata.domain.events.MarketDataUpdatedEvent;
import lombok.*;
import org.springframework.modulith.NamedInterface;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MarketInstrument aggregate root representing a tradable financial instrument (BTC, ETH, etc.).
 * Manages historical price data and provides domain operations for data analysis.
 * Exposed to other modules through the domain-api interface.
 */
@NamedInterface("domain-api")
public class MarketInstrument {
    // Basic identification
    @Getter
    private final String symbol;
    @Getter
    @Setter
    private String contractType;
    @Getter
    private final String name;
    @Getter
    private final String baseCurrency;
    @Getter
    private final String quoteCurrency;
    @Getter
    @Setter
    private String settleCoin;
    @Getter
    @Setter
    private Market market;

    // Trading specifications
    @Getter
    @Setter
    private Long launchTime;
    @Getter
    @Setter
    private Long deliveryTime;
    @Getter
    @Setter
    private BigDecimal minLeverage;
    @Getter
    @Setter
    private BigDecimal maxLeverage;
    @Getter
    @Setter
    private BigDecimal minOrderQty;
    @Getter
    @Setter
    private BigDecimal maxOrderQty;
    @Getter
    @Setter
    private BigDecimal qtyStep;
    @Getter
    @Setter
    private BigDecimal tickSize;

    // Price data and quality
    private final List<OHLCV> priceHistory;
    @Setter
    @Getter
    private DataQualityMetrics qualityMetrics;
    @Getter
    private Instant lastUpdated;
    private Instant firstTradingDate;
    private final List<MarketDataUpdatedEvent> domainEvents;

    public MarketInstrument(String symbol, String name, String baseCurrency, String quoteCurrency) {
        this(symbol, name, baseCurrency, quoteCurrency, null);
    }

    public MarketInstrument(String symbol, String name, String baseCurrency, String quoteCurrency, Market market) {
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.baseCurrency = Objects.requireNonNull(baseCurrency, "Base currency cannot be null");
        this.quoteCurrency = Objects.requireNonNull(quoteCurrency, "Quote currency cannot be null");
        this.market = market;  // Can be null initially, set later
        this.priceHistory = new ArrayList<>();
        this.domainEvents = new ArrayList<>();
        this.lastUpdated = Instant.now();
    }

    public MarketInstrument(String symbol, String name, String baseCurrency, String quoteCurrency, Market market, Instant firstTradingDate) {
        this(symbol, name, baseCurrency, quoteCurrency, market);
        this.firstTradingDate = firstTradingDate;
    }

    public Optional<Instant> getFirstTradingDate() {
        return Optional.ofNullable(firstTradingDate);
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

        // Replace price history with merged data, sorted by timestamp
        priceHistory.clear();
        List<OHLCV> sortedMergedData = existingDataMap.values().stream()
                .sorted(Comparator.comparing(OHLCV::timestamp))
                .toList();
        priceHistory.addAll(sortedMergedData);
    }

    /**
     * Sets the first trading date for this instrument.
     * This should only be called when we discover the actual first trading date
     * through API error responses indicating no data is available for earlier dates.
     * 
     * @param firstTradingDate The confirmed first trading date for this instrument
     */
    public void setFirstTradingDate(Instant firstTradingDate) {
        if (firstTradingDate == null) {
            throw new IllegalArgumentException("First trading date cannot be null");
        }
        
        // Only set if not already set, or if the new date is earlier (more accurate)
        if (this.firstTradingDate == null || firstTradingDate.isBefore(this.firstTradingDate)) {
            this.firstTradingDate = firstTradingDate;
        }
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