package com.ahd.trading_platform.marketdata.infrastructure.persistence.repositories;

import com.ahd.trading_platform.marketdata.domain.services.MarketResolver;
import com.ahd.trading_platform.marketdata.domain.valueobjects.BybitMarketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Factory for routing price data repository calls to the correct market-based repository.
 * Uses MarketResolver to determine which market (SPOT, LINEAR, INVERSE, OPTION) a symbol belongs to.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PriceDataRepositoryFactory {

    private final MarketResolver marketResolver;
    private final SpotPriceDataJpaRepository spotRepository;
    private final LinearPriceDataJpaRepository linearRepository;
    private final InversePriceDataJpaRepository inverseRepository;
    private final OptionPriceDataJpaRepository optionRepository;

    /**
     * Gets the appropriate repository for a given symbol
     */
    private JpaRepository<?, Long> getRepository(String symbol) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);
        log.debug("Routing symbol '{}' to {} market repository", symbol, marketType);

        return switch (marketType) {
            case SPOT -> spotRepository;
            case LINEAR -> linearRepository;
            case INVERSE -> inverseRepository;
            case OPTION -> optionRepository;
        };
    }

    /**
     * Finds price data for a specific symbol within a time range, ordered by timestamp
     */
    public List<?> findBySymbolAndTimestampBetweenOrderByTimestampAsc(
            String symbol, Instant fromTime, Instant toTime) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.findBySymbolAndTimestampBetweenOrderByTimestampAsc(symbol, fromTime, toTime);
            case LINEAR -> linearRepository.findBySymbolAndTimestampBetweenOrderByTimestampAsc(symbol, fromTime, toTime);
            case INVERSE -> inverseRepository.findBySymbolAndTimestampBetweenOrderByTimestampAsc(symbol, fromTime, toTime);
            case OPTION -> optionRepository.findBySymbolAndTimestampBetweenOrderByTimestampAsc(symbol, fromTime, toTime);
        };
    }

    /**
     * Finds the latest price data point for a specific symbol
     */
    public Optional<?> findTopBySymbolOrderByTimestampDesc(String symbol) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.findTopBySymbolOrderByTimestampDesc(symbol);
            case LINEAR -> linearRepository.findTopBySymbolOrderByTimestampDesc(symbol);
            case INVERSE -> inverseRepository.findTopBySymbolOrderByTimestampDesc(symbol);
            case OPTION -> optionRepository.findTopBySymbolOrderByTimestampDesc(symbol);
        };
    }

    /**
     * Finds the earliest price data point for a specific symbol
     */
    public Optional<?> findTopBySymbolOrderByTimestampAsc(String symbol) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.findTopBySymbolOrderByTimestampAsc(symbol);
            case LINEAR -> linearRepository.findTopBySymbolOrderByTimestampAsc(symbol);
            case INVERSE -> inverseRepository.findTopBySymbolOrderByTimestampAsc(symbol);
            case OPTION -> optionRepository.findTopBySymbolOrderByTimestampAsc(symbol);
        };
    }

    /**
     * Checks if price data exists for a specific symbol and timestamp
     */
    public boolean existsBySymbolAndTimestamp(String symbol, Instant timestamp) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.existsBySymbolAndTimestamp(symbol, timestamp);
            case LINEAR -> linearRepository.existsBySymbolAndTimestamp(symbol, timestamp);
            case INVERSE -> inverseRepository.existsBySymbolAndTimestamp(symbol, timestamp);
            case OPTION -> optionRepository.existsBySymbolAndTimestamp(symbol, timestamp);
        };
    }

    /**
     * Counts data points for a specific symbol within a time range
     */
    public long countBySymbolAndTimestampBetween(String symbol, Instant fromTime, Instant toTime) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.countBySymbolAndTimestampBetween(symbol, fromTime, toTime);
            case LINEAR -> linearRepository.countBySymbolAndTimestampBetween(symbol, fromTime, toTime);
            case INVERSE -> inverseRepository.countBySymbolAndTimestampBetween(symbol, fromTime, toTime);
            case OPTION -> optionRepository.countBySymbolAndTimestampBetween(symbol, fromTime, toTime);
        };
    }

    /**
     * Finds price data after a specific timestamp for a symbol (for incremental updates)
     */
    public List<?> findBySymbolAndTimestampAfterOrderByTimestampAsc(String symbol, Instant afterTime) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.findBySymbolAndTimestampAfterOrderByTimestampAsc(symbol, afterTime);
            case LINEAR -> linearRepository.findBySymbolAndTimestampAfterOrderByTimestampAsc(symbol, afterTime);
            case INVERSE -> inverseRepository.findBySymbolAndTimestampAfterOrderByTimestampAsc(symbol, afterTime);
            case OPTION -> optionRepository.findBySymbolAndTimestampAfterOrderByTimestampAsc(symbol, afterTime);
        };
    }

    /**
     * Finds the highest price within a time range for a specific symbol
     */
    public Optional<BigDecimal> findMaxHighPriceInTimeRange(String symbol, Instant fromTime, Instant toTime) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.findMaxHighPriceInTimeRange(symbol, fromTime, toTime);
            case LINEAR -> linearRepository.findMaxHighPriceInTimeRange(symbol, fromTime, toTime);
            case INVERSE -> inverseRepository.findMaxHighPriceInTimeRange(symbol, fromTime, toTime);
            case OPTION -> optionRepository.findMaxHighPriceInTimeRange(symbol, fromTime, toTime);
        };
    }

    /**
     * Finds the lowest price within a time range for a specific symbol
     */
    public Optional<BigDecimal> findMinLowPriceInTimeRange(String symbol, Instant fromTime, Instant toTime) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.findMinLowPriceInTimeRange(symbol, fromTime, toTime);
            case LINEAR -> linearRepository.findMinLowPriceInTimeRange(symbol, fromTime, toTime);
            case INVERSE -> inverseRepository.findMinLowPriceInTimeRange(symbol, fromTime, toTime);
            case OPTION -> optionRepository.findMinLowPriceInTimeRange(symbol, fromTime, toTime);
        };
    }

    /**
     * Calculates total volume within a time range for a specific symbol
     */
    public BigDecimal getTotalVolumeInTimeRange(String symbol, Instant fromTime, Instant toTime) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.getTotalVolumeInTimeRange(symbol, fromTime, toTime);
            case LINEAR -> linearRepository.getTotalVolumeInTimeRange(symbol, fromTime, toTime);
            case INVERSE -> inverseRepository.getTotalVolumeInTimeRange(symbol, fromTime, toTime);
            case OPTION -> optionRepository.getTotalVolumeInTimeRange(symbol, fromTime, toTime);
        };
    }

    /**
     * Finds only timestamps within a time range for gap detection for a specific symbol
     */
    public List<Instant> findTimestampsBySymbolAndDateRange(String symbol, Instant fromTime, Instant toTime) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.findTimestampsBySymbolAndDateRange(symbol, fromTime, toTime);
            case LINEAR -> linearRepository.findTimestampsBySymbolAndDateRange(symbol, fromTime, toTime);
            case INVERSE -> inverseRepository.findTimestampsBySymbolAndDateRange(symbol, fromTime, toTime);
            case OPTION -> optionRepository.findTimestampsBySymbolAndDateRange(symbol, fromTime, toTime);
        };
    }

    /**
     * Gets paginated data for large datasets for a specific symbol
     */
    public Page<?> findBySymbolAndTimestampBetween(
            String symbol, Instant fromTime, Instant toTime, Pageable pageable) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.findBySymbolAndTimestampBetween(symbol, fromTime, toTime, pageable);
            case LINEAR -> linearRepository.findBySymbolAndTimestampBetween(symbol, fromTime, toTime, pageable);
            case INVERSE -> inverseRepository.findBySymbolAndTimestampBetween(symbol, fromTime, toTime, pageable);
            case OPTION -> optionRepository.findBySymbolAndTimestampBetween(symbol, fromTime, toTime, pageable);
        };
    }

    /**
     * Saves a price data entity to the appropriate repository
     */
    public void save(String symbol, Object entity) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        switch (marketType) {
            case SPOT -> spotRepository.save((com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.SpotPriceDataEntity) entity);
            case LINEAR -> linearRepository.save((com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.LinearPriceDataEntity) entity);
            case INVERSE -> inverseRepository.save((com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.InversePriceDataEntity) entity);
            case OPTION -> optionRepository.save((com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.OptionPriceDataEntity) entity);
        }
    }

    /**
     * Saves all price data entities to the appropriate repository
     */
    public void saveAll(String symbol, List<?> entities) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        switch (marketType) {
            case SPOT -> spotRepository.saveAll((List<com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.SpotPriceDataEntity>) entities);
            case LINEAR -> linearRepository.saveAll((List<com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.LinearPriceDataEntity>) entities);
            case INVERSE -> inverseRepository.saveAll((List<com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.InversePriceDataEntity>) entities);
            case OPTION -> optionRepository.saveAll((List<com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.OptionPriceDataEntity>) entities);
        }
    }
}
