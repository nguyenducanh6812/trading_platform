package com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories;

import com.ahd.trading_platform.marketdata.domain.services.MarketResolver;
import com.ahd.trading_platform.marketdata.domain.valueobjects.BybitMarketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Factory for routing prediction repository calls to the correct market-based repository.
 * Uses MarketResolver to determine which market (SPOT, LINEAR, INVERSE, OPTION) a symbol belongs to.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ExpectedReturnPredictionRepositoryFactory {

    private final MarketResolver marketResolver;
    private final SpotExpectedReturnPredictionJpaRepository spotRepository;
    private final LinearExpectedReturnPredictionJpaRepository linearRepository;
    private final InverseExpectedReturnPredictionJpaRepository inverseRepository;
    private final OptionExpectedReturnPredictionJpaRepository optionRepository;

    /**
     * Find predictions for a specific symbol within a date range
     */
    public List<?> findBySymbolAndForecastDateBetween(String symbol, Instant startDate, Instant endDate) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);
        log.debug("Routing symbol '{}' to {} market prediction repository", symbol, marketType);

        return switch (marketType) {
            case SPOT -> spotRepository.findBySymbolAndForecastDateBetween(symbol, startDate, endDate);
            case LINEAR -> linearRepository.findBySymbolAndForecastDateBetween(symbol, startDate, endDate);
            case INVERSE -> inverseRepository.findBySymbolAndForecastDateBetween(symbol, startDate, endDate);
            case OPTION -> optionRepository.findBySymbolAndForecastDateBetween(symbol, startDate, endDate);
        };
    }

    /**
     * Find predictions by symbol and model version
     */
    public List<?> findBySymbolAndModelVersion(String symbol, String modelVersion) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.findBySymbolAndModelVersion(symbol, modelVersion);
            case LINEAR -> linearRepository.findBySymbolAndModelVersion(symbol, modelVersion);
            case INVERSE -> inverseRepository.findBySymbolAndModelVersion(symbol, modelVersion);
            case OPTION -> optionRepository.findBySymbolAndModelVersion(symbol, modelVersion);
        };
    }

    /**
     * Find prediction for specific symbol, date and model version
     */
    public Optional<?> findBySymbolAndForecastDateAndModelVersion(
            String symbol, Instant forecastDate, String modelVersion) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.findBySymbolAndForecastDateAndModelVersion(symbol, forecastDate, modelVersion);
            case LINEAR -> linearRepository.findBySymbolAndForecastDateAndModelVersion(symbol, forecastDate, modelVersion);
            case INVERSE -> inverseRepository.findBySymbolAndForecastDateAndModelVersion(symbol, forecastDate, modelVersion);
            case OPTION -> optionRepository.findBySymbolAndForecastDateAndModelVersion(symbol, forecastDate, modelVersion);
        };
    }

    /**
     * Find latest prediction for a symbol and model version
     */
    public Optional<?> findLatestBySymbolAndModelVersion(String symbol, String modelVersion) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.findLatestBySymbolAndModelVersion(symbol, modelVersion);
            case LINEAR -> linearRepository.findLatestBySymbolAndModelVersion(symbol, modelVersion);
            case INVERSE -> inverseRepository.findLatestBySymbolAndModelVersion(symbol, modelVersion);
            case OPTION -> optionRepository.findLatestBySymbolAndModelVersion(symbol, modelVersion);
        };
    }

    /**
     * Find successful predictions only for a specific symbol
     */
    public List<?> findSuccessfulPredictionsBySymbol(String symbol) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.findSuccessfulPredictionsBySymbol(symbol);
            case LINEAR -> linearRepository.findSuccessfulPredictionsBySymbol(symbol);
            case INVERSE -> inverseRepository.findSuccessfulPredictionsBySymbol(symbol);
            case OPTION -> optionRepository.findSuccessfulPredictionsBySymbol(symbol);
        };
    }

    /**
     * Count predictions by symbol and model version
     */
    public long countBySymbolAndModelVersion(String symbol, String modelVersion) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.countBySymbolAndModelVersion(symbol, modelVersion);
            case LINEAR -> linearRepository.countBySymbolAndModelVersion(symbol, modelVersion);
            case INVERSE -> inverseRepository.countBySymbolAndModelVersion(symbol, modelVersion);
            case OPTION -> optionRepository.countBySymbolAndModelVersion(symbol, modelVersion);
        };
    }

    /**
     * Count successful predictions by symbol and model version within date range
     */
    public long countBySymbolAndModelVersionAndPredictionStatusAndForecastDateBetween(
            String symbol, String modelVersion, String predictionStatus, Instant startDate, Instant endDate) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.countBySymbolAndModelVersionAndPredictionStatusAndForecastDateBetween(
                    symbol, modelVersion, predictionStatus, startDate, endDate);
            case LINEAR -> linearRepository.countBySymbolAndModelVersionAndPredictionStatusAndForecastDateBetween(
                    symbol, modelVersion, predictionStatus, startDate, endDate);
            case INVERSE -> inverseRepository.countBySymbolAndModelVersionAndPredictionStatusAndForecastDateBetween(
                    symbol, modelVersion, predictionStatus, startDate, endDate);
            case OPTION -> optionRepository.countBySymbolAndModelVersionAndPredictionStatusAndForecastDateBetween(
                    symbol, modelVersion, predictionStatus, startDate, endDate);
        };
    }

    /**
     * Find successful predictions by symbol and model version within date range
     */
    public List<?> findBySymbolAndModelVersionAndPredictionStatusAndForecastDateBetweenOrderByForecastDate(
            String symbol, String modelVersion, String predictionStatus, Instant startDate, Instant endDate) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.findBySymbolAndModelVersionAndPredictionStatusAndForecastDateBetweenOrderByForecastDate(
                    symbol, modelVersion, predictionStatus, startDate, endDate);
            case LINEAR -> linearRepository.findBySymbolAndModelVersionAndPredictionStatusAndForecastDateBetweenOrderByForecastDate(
                    symbol, modelVersion, predictionStatus, startDate, endDate);
            case INVERSE -> inverseRepository.findBySymbolAndModelVersionAndPredictionStatusAndForecastDateBetweenOrderByForecastDate(
                    symbol, modelVersion, predictionStatus, startDate, endDate);
            case OPTION -> optionRepository.findBySymbolAndModelVersionAndPredictionStatusAndForecastDateBetweenOrderByForecastDate(
                    symbol, modelVersion, predictionStatus, startDate, endDate);
        };
    }

    /**
     * Find predictions by execution ID
     */
    public List<?> findByExecutionId(String executionId) {
        // For execution ID, we need to check all markets since we don't know which market the symbol belongs to
        log.debug("Finding predictions across all markets for execution ID: {}", executionId);

        List<?> results = new java.util.ArrayList<>();
        ((java.util.ArrayList) results).addAll(spotRepository.findByExecutionId(executionId));
        ((java.util.ArrayList) results).addAll(linearRepository.findByExecutionId(executionId));
        ((java.util.ArrayList) results).addAll(inverseRepository.findByExecutionId(executionId));
        ((java.util.ArrayList) results).addAll(optionRepository.findByExecutionId(executionId));

        return results;
    }

    /**
     * Check if prediction exists for symbol, date and model version
     */
    public boolean existsBySymbolAndForecastDateAndModelVersion(
            String symbol, Instant forecastDate, String modelVersion) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        return switch (marketType) {
            case SPOT -> spotRepository.existsBySymbolAndForecastDateAndModelVersion(symbol, forecastDate, modelVersion);
            case LINEAR -> linearRepository.existsBySymbolAndForecastDateAndModelVersion(symbol, forecastDate, modelVersion);
            case INVERSE -> inverseRepository.existsBySymbolAndForecastDateAndModelVersion(symbol, forecastDate, modelVersion);
            case OPTION -> optionRepository.existsBySymbolAndForecastDateAndModelVersion(symbol, forecastDate, modelVersion);
        };
    }

    /**
     * Saves a prediction entity to the appropriate repository
     */
    public void save(String symbol, Object entity) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        switch (marketType) {
            case SPOT -> spotRepository.save((com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.SpotExpectedReturnPredictionEntity) entity);
            case LINEAR -> linearRepository.save((com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.LinearExpectedReturnPredictionEntity) entity);
            case INVERSE -> inverseRepository.save((com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.InverseExpectedReturnPredictionEntity) entity);
            case OPTION -> optionRepository.save((com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.OptionExpectedReturnPredictionEntity) entity);
        }
    }

    /**
     * Saves all prediction entities to the appropriate repository
     */
    public void saveAll(String symbol, List<?> entities) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);

        switch (marketType) {
            case SPOT -> spotRepository.saveAll((List<com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.SpotExpectedReturnPredictionEntity>) entities);
            case LINEAR -> linearRepository.saveAll((List<com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.LinearExpectedReturnPredictionEntity>) entities);
            case INVERSE -> inverseRepository.saveAll((List<com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.InverseExpectedReturnPredictionEntity>) entities);
            case OPTION -> optionRepository.saveAll((List<com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.OptionExpectedReturnPredictionEntity>) entities);
        }
    }
}
