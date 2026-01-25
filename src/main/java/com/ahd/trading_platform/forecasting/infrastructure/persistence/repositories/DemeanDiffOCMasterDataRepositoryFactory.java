package com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories;

import com.ahd.trading_platform.marketdata.domain.services.MarketResolver;
import com.ahd.trading_platform.marketdata.domain.valueobjects.BybitMarketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Factory for routing ARIMA master data repository calls to the correct market-based repository.
 * Uses MarketResolver to determine which market (SPOT, LINEAR, INVERSE, OPTION) a symbol belongs to.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DemeanDiffOCMasterDataRepositoryFactory {

    private final MarketResolver marketResolver;
    private final SpotDemeanDiffOCMasterDataJpaRepository spotRepository;
    private final LinearDemeanDiffOCMasterDataJpaRepository linearRepository;
    private final InverseDemeanDiffOCMasterDataJpaRepository inverseRepository;
    private final OptionDemeanDiffOCMasterDataJpaRepository optionRepository;

    /**
     * Finds master data for a specific symbol within a time range, ordered by timestamp
     */
    public List<?> findBySymbolAndTimestampBetweenOrderByTimestampAsc(
            String symbol, Instant fromTime, Instant toTime) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);
        log.debug("Routing symbol '{}' to {} market ARIMA repository", symbol, marketType);

        return switch (marketType) {
            case SPOT -> spotRepository.findBySymbolAndTimestampBetweenOrderByTimestampAsc(symbol, fromTime, toTime);
            case LINEAR -> linearRepository.findBySymbolAndTimestampBetweenOrderByTimestampAsc(symbol, fromTime, toTime);
            case INVERSE -> inverseRepository.findBySymbolAndTimestampBetweenOrderByTimestampAsc(symbol, fromTime, toTime);
            case OPTION -> optionRepository.findBySymbolAndTimestampBetweenOrderByTimestampAsc(symbol, fromTime, toTime);
        };
    }

    /**
     * Finds the latest master data point for a specific symbol
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
     * Finds the earliest master data point for a specific symbol
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
     * Finds master data by specific symbol and timestamp
     */
    public Optional<?> findBySymbolAndTimestamp(String symbol, Instant timestamp) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.findBySymbolAndTimestamp(symbol, timestamp);
            case LINEAR -> linearRepository.findBySymbolAndTimestamp(symbol, timestamp);
            case INVERSE -> inverseRepository.findBySymbolAndTimestamp(symbol, timestamp);
            case OPTION -> optionRepository.findBySymbolAndTimestamp(symbol, timestamp);
        };
    }

    /**
     * Checks if master data exists for a specific symbol and timestamp
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
     * Finds master data after a specific timestamp for a symbol (for incremental updates)
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
     * Finds master data with calculated differences within a time range for a specific symbol
     */
    public List<?> findBySymbolAndTimestampBetweenAndHasDifferencesOrderByTimestampAsc(
            String symbol, Instant fromTime, Instant toTime) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.findBySymbolAndTimestampBetweenAndHasDifferencesOrderByTimestampAsc(symbol, fromTime, toTime);
            case LINEAR -> linearRepository.findBySymbolAndTimestampBetweenAndHasDifferencesOrderByTimestampAsc(symbol, fromTime, toTime);
            case INVERSE -> inverseRepository.findBySymbolAndTimestampBetweenAndHasDifferencesOrderByTimestampAsc(symbol, fromTime, toTime);
            case OPTION -> optionRepository.findBySymbolAndTimestampBetweenAndHasDifferencesOrderByTimestampAsc(symbol, fromTime, toTime);
        };
    }

    /**
     * Counts master data points with calculated differences within a time range for a specific symbol
     */
    public long countBySymbolAndTimestampBetweenAndHasDifferences(String symbol, Instant fromTime, Instant toTime) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.countBySymbolAndTimestampBetweenAndHasDifferences(symbol, fromTime, toTime);
            case LINEAR -> linearRepository.countBySymbolAndTimestampBetweenAndHasDifferences(symbol, fromTime, toTime);
            case INVERSE -> inverseRepository.countBySymbolAndTimestampBetweenAndHasDifferences(symbol, fromTime, toTime);
            case OPTION -> optionRepository.countBySymbolAndTimestampBetweenAndHasDifferences(symbol, fromTime, toTime);
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
     * Saves a master data entity to the appropriate repository
     */
    public void save(String symbol, Object entity) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        switch (marketType) {
            case SPOT -> spotRepository.save((com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.SpotDemeanDiffOCMasterDataEntity) entity);
            case LINEAR -> linearRepository.save((com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.LinearDemeanDiffOCMasterDataEntity) entity);
            case INVERSE -> inverseRepository.save((com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.InverseDemeanDiffOCMasterDataEntity) entity);
            case OPTION -> optionRepository.save((com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.OptionDemeanDiffOCMasterDataEntity) entity);
        }
    }

    /**
     * Saves all master data entities to the appropriate repository
     */
    public void saveAll(String symbol, List<?> entities) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        switch (marketType) {
            case SPOT -> spotRepository.saveAll((List<com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.SpotDemeanDiffOCMasterDataEntity>) entities);
            case LINEAR -> linearRepository.saveAll((List<com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.LinearDemeanDiffOCMasterDataEntity>) entities);
            case INVERSE -> inverseRepository.saveAll((List<com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.InverseDemeanDiffOCMasterDataEntity>) entities);
            case OPTION -> optionRepository.saveAll((List<com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.OptionDemeanDiffOCMasterDataEntity>) entities);
        }
    }
}
