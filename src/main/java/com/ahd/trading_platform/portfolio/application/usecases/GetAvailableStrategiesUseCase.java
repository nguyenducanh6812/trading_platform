package com.ahd.trading_platform.portfolio.application.usecases;

import com.ahd.trading_platform.portfolio.application.dto.StrategyResponse;
import com.ahd.trading_platform.portfolio.domain.entities.Strategy;
import com.ahd.trading_platform.portfolio.domain.repositories.StrategyRepository;
import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for retrieving available trading strategies.
 * Returns list of strategies that users can choose for their portfolios.
 */
@Component
public class GetAvailableStrategiesUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetAvailableStrategiesUseCase.class);

    private final StrategyRepository strategyRepository;

    public GetAvailableStrategiesUseCase(StrategyRepository strategyRepository) {
        this.strategyRepository = strategyRepository;
    }

    /**
     * Gets all active strategies
     */
    public List<StrategyResponse> execute() {
        logger.info("Fetching all available strategies");

        // For now, return hardcoded strategies (MPT and ARIMA)
        // TODO: Replace with database query once persistence is implemented
        List<Strategy> strategies = getHardcodedStrategies();

        List<StrategyResponse> response = strategies.stream()
            .filter(Strategy::isActive)
            .map(StrategyResponse::from)
            .collect(Collectors.toList());

        logger.info("Found {} active strategies", response.size());
        return response;
    }

    /**
     * Gets strategies by category
     */
    public List<StrategyResponse> executeByCategory(StrategyCategory category) {
        logger.info("Fetching strategies for category: {}", category);

        // For now, filter hardcoded strategies
        // TODO: Replace with database query
        List<Strategy> strategies = getHardcodedStrategies();

        List<StrategyResponse> response = strategies.stream()
            .filter(Strategy::isActive)
            .filter(s -> s.getCategory() == category)
            .map(StrategyResponse::from)
            .collect(Collectors.toList());

        logger.info("Found {} strategies for category: {}", response.size(), category);
        return response;
    }

    /**
     * Temporary method returning hardcoded strategies
     * TODO: Remove once database persistence is implemented
     */
    private List<Strategy> getHardcodedStrategies() {
        List<Strategy> strategies = new ArrayList<>();
        strategies.add(Strategy.mpt());
        strategies.add(Strategy.arima());
        return strategies;
    }
}
