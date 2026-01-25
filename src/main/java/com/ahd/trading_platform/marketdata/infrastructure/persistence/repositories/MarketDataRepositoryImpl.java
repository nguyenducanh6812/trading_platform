package com.ahd.trading_platform.marketdata.infrastructure.persistence.repositories;

import com.ahd.trading_platform.marketdata.domain.entities.Market;
import com.ahd.trading_platform.marketdata.domain.entities.MarketInstrument;
import com.ahd.trading_platform.marketdata.domain.repositories.MarketDataRepository;
import com.ahd.trading_platform.marketdata.domain.services.MarketResolver;
import com.ahd.trading_platform.marketdata.domain.valueobjects.BybitMarketType;
import com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.*;
import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.Price;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Full implementation of MarketDataRepository with price data persistence.
 * Uses PriceDataRepositoryFactory for market-based routing (SPOT, LINEAR, INVERSE, OPTION).
 */
@Repository
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MarketDataRepositoryImpl implements MarketDataRepository {

    private final MarketInstrumentJpaRepository instrumentRepository;
    private final MarketJpaRepository marketJpaRepository;
    private final PriceDataRepositoryFactory priceDataFactory;
    private final MarketResolver marketResolver;

    @Override
    public void save(MarketInstrument instrument) {
        log.debug("Saving market instrument: {}", instrument.getSymbol());

        // Find or create instrument entity
        MarketInstrumentEntity entity = instrumentRepository.findBySymbolIgnoreCase(instrument.getSymbol())
                .orElseGet(() -> createNewInstrumentEntity(instrument));

        // Update entity from domain
        updateEntityFromDomain(entity, instrument);

        // Save instrument metadata
        instrumentRepository.save(entity);

        // Save price history if present
        if (!instrument.getPriceHistory().isEmpty()) {
            saveHistoricalData(instrument.getSymbol(), instrument.getPriceHistory());
        }

        log.debug("Saved market instrument: {} with {} price data points",
                instrument.getSymbol(), instrument.getDataPointCount());
    }

    @Override
    public void saveAll(List<MarketInstrument> instruments, Long marketId) {
        if (instruments == null || instruments.isEmpty()) {
            log.debug("No instruments to save");
            return;
        }

        log.debug("Batch saving {} market instruments for market ID: {}", instruments.size(), marketId);

        // Get lazy reference to Market entity (NO database query - just a proxy!)
        // This proxy contains only the ID and will be used for the foreign key relationship
        MarketEntity marketReference = marketJpaRepository.getReferenceById(marketId);

        // Convert all domain objects to entities
        List<MarketInstrumentEntity> entities = instruments.stream()
                .map(instrument -> {
                    // Find or create entity
                    MarketInstrumentEntity entity = instrumentRepository.findBySymbolIgnoreCase(instrument.getSymbol())
                            .orElseGet(() -> createNewInstrumentEntity(instrument));

                    // Update from domain
                    updateEntityFromDomain(entity, instrument);

                    // Set market reference using lazy proxy (no query triggered!)
                    entity.setMarket(marketReference);

                    return entity;
                })
                .toList();

        // Batch save all entities
        instrumentRepository.saveAll(entities);

        log.debug("Batch saved {} market instruments using lazy proxy reference", entities.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MarketInstrument> findBySymbol(String symbol) {
        log.debug("Finding market instrument with price data for symbol: {}", symbol);

        return instrumentRepository.findBySymbolIgnoreCase(symbol)
                .map(entity -> {
                    MarketInstrument instrument = mapEntityToDomain(entity);

                    // Load all price history
                    List<OHLCV> priceData = findAllHistoricalData(symbol);
                    if (!priceData.isEmpty()) {
                        instrument.addPriceData(priceData);
                    }

                    return instrument;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MarketInstrument> findInstrumentMetadataBySymbol(String symbol) {
        log.debug("Finding instrument metadata (without price data) for symbol: {}", symbol);

        return instrumentRepository.findBySymbolIgnoreCase(symbol)
                .map(this::mapEntityToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketInstrument> findAll() {
        log.debug("Finding all market instruments");

        return instrumentRepository.findAll().stream()
                .map(this::mapEntityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketInstrument> findByMarketCode(String marketCode) {
        log.debug("Finding market instruments by market code: {}", marketCode);

        return instrumentRepository.findByMarketCode(marketCode).stream()
                .map(this::mapEntityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketInstrument> findByMarketId(Long marketId) {
        log.debug("Finding market instruments by market ID: {}", marketId);

        return instrumentRepository.findByMarket_Id(marketId).stream()
                .map(this::mapEntityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void saveHistoricalData(String symbol, List<OHLCV> ohlcvData) {
        if (ohlcvData == null || ohlcvData.isEmpty()) {
            log.debug("No price data to save for symbol: {}", symbol);
            return;
        }

        log.debug("Saving {} price data points for symbol: {}", ohlcvData.size(), symbol);

        // Convert OHLCV to price entities based on market type
        List<?> entities = ohlcvData.stream()
                .map(ohlcv -> convertToPriceEntity(symbol, ohlcv))
                .collect(Collectors.toList());

        // Save using factory (routes to correct repository based on market)
        priceDataFactory.saveAll(symbol, entities);

        log.debug("Successfully saved {} price data points for {}", entities.size(), symbol);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OHLCV> findHistoricalData(String symbol, TimeRange timeRange) {
        log.debug("Finding historical data for {} in time range: {} to {}",
                symbol, timeRange.from(), timeRange.to());

        List<?> entities = priceDataFactory.findBySymbolAndTimestampBetweenOrderByTimestampAsc(
                symbol, timeRange.from(), timeRange.to());

        return entities.stream()
                .map(this::convertToOHLCV)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OHLCV> findAllHistoricalData(String symbol) {
        log.debug("Finding all historical data for {}", symbol);

        // Query from earliest to latest possible time
        Instant earliest = Instant.ofEpochMilli(0);
        Instant latest = Instant.now().plus(Duration.ofDays(1));

        List<?> entities = priceDataFactory.findBySymbolAndTimestampBetweenOrderByTimestampAsc(
                symbol, earliest, latest);

        return entities.stream()
                .map(this::convertToOHLCV)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasHistoricalData(String symbol, TimeRange timeRange) {
        log.debug("Checking if historical data exists for {} in range", symbol);

        long count = priceDataFactory.countBySymbolAndTimestampBetween(
                symbol, timeRange.from(), timeRange.to());

        return count > 0;
    }

    @Override
    public void deleteBySymbol(String symbol) {
        log.warn("Deleting all data for symbol: {}", symbol);

        // Note: This is a destructive operation
        // Find the instrument first, then delete
        instrumentRepository.findBySymbolIgnoreCase(symbol).ifPresent(entity -> {
            instrumentRepository.delete(entity);
            log.info("Deleted all data for symbol: {}", symbol);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public long getDataPointCount(String symbol) {
        log.debug("Getting data point count for {}", symbol);

        Instant earliest = Instant.ofEpochMilli(0);
        Instant latest = Instant.now().plus(Duration.ofDays(1));

        return priceDataFactory.countBySymbolAndTimestampBetween(symbol, earliest, latest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeRange> findDataRanges(String symbol, TimeRange searchRange) {
        log.debug("Finding data ranges for {} in search range", symbol);

        // Get all timestamps in the range
        List<Instant> timestamps = priceDataFactory.findTimestampsBySymbolAndDateRange(
                symbol, searchRange.from(), searchRange.to());

        if (timestamps.isEmpty()) {
            return List.of();
        }

        // Group consecutive timestamps into ranges
        List<TimeRange> ranges = new ArrayList<>();
        Instant rangeStart = timestamps.get(0);
        Instant rangeEnd = timestamps.get(0);

        for (int i = 1; i < timestamps.size(); i++) {
            Instant current = timestamps.get(i);
            Instant previous = timestamps.get(i - 1);

            // If gap is more than 2 days, start a new range
            Duration gap = Duration.between(previous, current);
            if (gap.toDays() > 2) {
                ranges.add(new TimeRange(rangeStart, rangeEnd));
                rangeStart = current;
            }
            rangeEnd = current;
        }

        // Add final range
        ranges.add(new TimeRange(rangeStart, rangeEnd));

        return ranges;
    }

    /**
     * Creates a new instrument entity from domain object
     */
    private MarketInstrumentEntity createNewInstrumentEntity(MarketInstrument instrument) {
        MarketInstrumentEntity entity = new MarketInstrumentEntity();
        entity.setSymbol(instrument.getSymbol());
        entity.setName(instrument.getName());
        entity.setBaseCurrency(instrument.getBaseCurrency());
        entity.setQuoteCurrency(instrument.getQuoteCurrency());

        // Set market if present
        if (instrument.getMarket() != null) {
            MarketEntity marketEntity = new MarketEntity();
            marketEntity.setCode(instrument.getMarket().getCode());
            marketEntity.setName(instrument.getMarket().getName());
            marketEntity.setDescription(instrument.getMarket().getDescription());
            entity.setMarket(marketEntity);
        }

        return entity;
    }

    /**
     * Updates entity fields from domain object
     */
    private void updateEntityFromDomain(MarketInstrumentEntity entity, MarketInstrument instrument) {
        entity.setName(instrument.getName());
        entity.setBaseCurrency(instrument.getBaseCurrency());
        entity.setQuoteCurrency(instrument.getQuoteCurrency());
        entity.setContractType(instrument.getContractType());
        entity.setSettleCoin(instrument.getSettleCoin());
        entity.setLaunchTime(instrument.getLaunchTime());
        entity.setDeliveryTime(instrument.getDeliveryTime());
        entity.setMinLeverage(instrument.getMinLeverage());
        entity.setMaxLeverage(instrument.getMaxLeverage());
        entity.setMinOrderQty(instrument.getMinOrderQty());
        entity.setMaxOrderQty(instrument.getMaxOrderQty());
        entity.setQtyStep(instrument.getQtyStep());
        entity.setTickSize(instrument.getTickSize());

        // Set first trading date if present
        instrument.getFirstTradingDate().ifPresent(entity::setFirstTradingDate);

        // Update market if present
        if (instrument.getMarket() != null) {
            if (entity.getMarket() == null) {
                entity.setMarket(new MarketEntity());
            }
            entity.getMarket().setCode(instrument.getMarket().getCode());
            entity.getMarket().setName(instrument.getMarket().getName());
            entity.getMarket().setDescription(instrument.getMarket().getDescription());
        }
    }

    /**
     * Maps entity to domain object (without price data)
     */
    private MarketInstrument mapEntityToDomain(MarketInstrumentEntity entity) {
        Market market = entity.getMarket() != null
                ? new Market(
                    entity.getMarket().getId(),
                    entity.getMarket().getCode(),
                    entity.getMarket().getName(),
                    entity.getMarket().getDescription())
                : null;

        MarketInstrument instrument = new MarketInstrument(
                entity.getSymbol(),
                entity.getName(),
                entity.getBaseCurrency(),
                entity.getQuoteCurrency(),
                market,
                entity.getFirstTradingDate()
        );

        // Set additional fields
        instrument.setContractType(entity.getContractType());
        instrument.setSettleCoin(entity.getSettleCoin());
        instrument.setLaunchTime(entity.getLaunchTime());
        instrument.setDeliveryTime(entity.getDeliveryTime());
        instrument.setMinLeverage(entity.getMinLeverage());
        instrument.setMaxLeverage(entity.getMaxLeverage());
        instrument.setMinOrderQty(entity.getMinOrderQty());
        instrument.setMaxOrderQty(entity.getMaxOrderQty());
        instrument.setQtyStep(entity.getQtyStep());
        instrument.setTickSize(entity.getTickSize());

        return instrument;
    }

    /**
     * Converts OHLCV value object to appropriate price entity based on market type
     */
    private Object convertToPriceEntity(String symbol, OHLCV ohlcv) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> SpotPriceDataEntity.builder()
                    .symbol(symbol)
                    .timestamp(ohlcv.timestamp())
                    .openPrice(ohlcv.open().amount())
                    .highPrice(ohlcv.high().amount())
                    .lowPrice(ohlcv.low().amount())
                    .closePrice(ohlcv.close().amount())
                    .volume(ohlcv.volume())
                    .currency(ohlcv.close().currency())
                    .build();
            case LINEAR -> LinearPriceDataEntity.builder()
                    .symbol(symbol)
                    .timestamp(ohlcv.timestamp())
                    .openPrice(ohlcv.open().amount())
                    .highPrice(ohlcv.high().amount())
                    .lowPrice(ohlcv.low().amount())
                    .closePrice(ohlcv.close().amount())
                    .volume(ohlcv.volume())
                    .currency(ohlcv.close().currency())
                    .build();
            case INVERSE -> InversePriceDataEntity.builder()
                    .symbol(symbol)
                    .timestamp(ohlcv.timestamp())
                    .openPrice(ohlcv.open().amount())
                    .highPrice(ohlcv.high().amount())
                    .lowPrice(ohlcv.low().amount())
                    .closePrice(ohlcv.close().amount())
                    .volume(ohlcv.volume())
                    .currency(ohlcv.close().currency())
                    .build();
            case OPTION -> OptionPriceDataEntity.builder()
                    .symbol(symbol)
                    .timestamp(ohlcv.timestamp())
                    .openPrice(ohlcv.open().amount())
                    .highPrice(ohlcv.high().amount())
                    .lowPrice(ohlcv.low().amount())
                    .closePrice(ohlcv.close().amount())
                    .volume(ohlcv.volume())
                    .currency(ohlcv.close().currency())
                    .build();
        };
    }

    /**
     * Converts price entity to OHLCV value object
     */
    private OHLCV convertToOHLCV(Object entity) {
        return switch (entity) {
            case SpotPriceDataEntity e -> createOHLCV(
                    e.getOpenPrice(), e.getHighPrice(), e.getLowPrice(),
                    e.getClosePrice(), e.getVolume(), e.getTimestamp(), e.getCurrency());
            case LinearPriceDataEntity e -> createOHLCV(
                    e.getOpenPrice(), e.getHighPrice(), e.getLowPrice(),
                    e.getClosePrice(), e.getVolume(), e.getTimestamp(), e.getCurrency());
            case InversePriceDataEntity e -> createOHLCV(
                    e.getOpenPrice(), e.getHighPrice(), e.getLowPrice(),
                    e.getClosePrice(), e.getVolume(), e.getTimestamp(), e.getCurrency());
            case OptionPriceDataEntity e -> createOHLCV(
                    e.getOpenPrice(), e.getHighPrice(), e.getLowPrice(),
                    e.getClosePrice(), e.getVolume(), e.getTimestamp(), e.getCurrency());
            default -> throw new IllegalArgumentException(
                    "Unsupported price entity type: " + entity.getClass().getName());
        };
    }

    /**
     * Helper to create OHLCV from BigDecimal values
     */
    private OHLCV createOHLCV(BigDecimal open, BigDecimal high, BigDecimal low,
                               BigDecimal close, BigDecimal volume, Instant timestamp, String currency) {
        return new OHLCV(
                new Price(open, currency),
                new Price(high, currency),
                new Price(low, currency),
                new Price(close, currency),
                volume,
                timestamp
        );
    }
}
