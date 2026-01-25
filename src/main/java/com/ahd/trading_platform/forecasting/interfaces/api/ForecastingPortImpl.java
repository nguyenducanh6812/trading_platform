package com.ahd.trading_platform.forecasting.interfaces.api;

import com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.*;
import com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories.ExpectedReturnPredictionRepositoryFactory;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Implementation of ForecastingPort that provides access to prediction data
 * for other modules.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ForecastingPortImpl implements ForecastingPort {

    private final ExpectedReturnPredictionRepositoryFactory repositoryFactory;

    @Override
    public long countSuccessfulPredictions(
            TradingInstrument instrument,
            String modelVersion,
            Instant startDate,
            Instant endDate) {

        String symbol = instrument.getCode();
        log.debug("Counting successful predictions for symbol: {}, modelVersion: {}, dateRange: {} to {}",
                symbol, modelVersion, startDate, endDate);

        return repositoryFactory.countBySymbolAndModelVersionAndPredictionStatusAndForecastDateBetween(
                symbol, modelVersion, "SUCCESS", startDate, endDate
        );
    }

    @Override
    public List<PredictionInfoDto> findSuccessfulPredictions(
            TradingInstrument instrument,
            String modelVersion,
            Instant startDate,
            Instant endDate) {

        String symbol = instrument.getCode();
        log.debug("Finding successful predictions for symbol: {}, modelVersion: {}, dateRange: {} to {}",
                symbol, modelVersion, startDate, endDate);

        List<?> entities = repositoryFactory
                .findBySymbolAndModelVersionAndPredictionStatusAndForecastDateBetweenOrderByForecastDate(
                        symbol, modelVersion, "SUCCESS", startDate, endDate
                );

        return entities.stream()
                .map(this::convertToDto)
                .toList();
    }

    @Override
    public boolean hasCompletePredictionCoverage(
            TradingInstrument instrument,
            String modelVersion,
            Instant startDate,
            Instant endDate) {

        String symbol = instrument.getCode();
        log.debug("Checking prediction coverage for symbol: {}, modelVersion: {}, dateRange: {} to {}",
                symbol, modelVersion, startDate, endDate);

        long predictionCount = countSuccessfulPredictions(instrument, modelVersion, startDate, endDate);

        // Calculate expected number of daily predictions
        long daysBetween = java.time.Duration.between(startDate, endDate).toDays() + 1;

        boolean hasCoverage = predictionCount >= daysBetween;
        log.debug("Prediction coverage check - Expected: {}, Actual: {}, Result: {}",
                daysBetween, predictionCount, hasCoverage);

        return hasCoverage;
    }

    /**
     * Converts prediction entity to DTO.
     * Uses pattern matching to handle all market-specific entity types.
     */
    private PredictionInfoDto convertToDto(Object entity) {
        return switch (entity) {
            case SpotExpectedReturnPredictionEntity e -> new PredictionInfoDto(
                    e.getForecastDate(),
                    e.getExpectedReturn(),
                    e.getConfidenceLevel(),
                    e.getModelVersion(),
                    e.getPredictionStatus(),
                    e.getArOrder() != null ? e.getArOrder() : 0,
                    e.getMeanSquaredError(),
                    e.getDataPointsUsed() != null ? e.getDataPointsUsed() : 0
            );
            case LinearExpectedReturnPredictionEntity e -> new PredictionInfoDto(
                    e.getForecastDate(),
                    e.getExpectedReturn(),
                    e.getConfidenceLevel(),
                    e.getModelVersion(),
                    e.getPredictionStatus(),
                    e.getArOrder() != null ? e.getArOrder() : 0,
                    e.getMeanSquaredError(),
                    e.getDataPointsUsed() != null ? e.getDataPointsUsed() : 0
            );
            case InverseExpectedReturnPredictionEntity e -> new PredictionInfoDto(
                    e.getForecastDate(),
                    e.getExpectedReturn(),
                    e.getConfidenceLevel(),
                    e.getModelVersion(),
                    e.getPredictionStatus(),
                    e.getArOrder() != null ? e.getArOrder() : 0,
                    e.getMeanSquaredError(),
                    e.getDataPointsUsed() != null ? e.getDataPointsUsed() : 0
            );
            case OptionExpectedReturnPredictionEntity e -> new PredictionInfoDto(
                    e.getForecastDate(),
                    e.getExpectedReturn(),
                    e.getConfidenceLevel(),
                    e.getModelVersion(),
                    e.getPredictionStatus(),
                    e.getArOrder() != null ? e.getArOrder() : 0,
                    e.getMeanSquaredError(),
                    e.getDataPointsUsed() != null ? e.getDataPointsUsed() : 0
            );
            default -> throw new IllegalArgumentException(
                    "Unsupported prediction entity type: " + entity.getClass().getName()
            );
        };
    }
}
