package com.ahd.trading_platform.portfolio.application.usecases;

import com.ahd.trading_platform.portfolio.application.dto.ConfigurePortfolioStrategyRequest;
import com.ahd.trading_platform.portfolio.domain.entities.PortfolioStrategyConfiguration;
import com.ahd.trading_platform.portfolio.domain.entities.Strategy;
import com.ahd.trading_platform.portfolio.domain.repositories.PortfolioStrategyConfigurationRepository;
import com.ahd.trading_platform.portfolio.domain.repositories.StrategyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Use case for configuring strategies for a portfolio.
 * Handles hierarchical strategy configuration (strategy with nested dependencies).
 *
 * Example: Configure MPT for a portfolio
 * - Main strategy: MPT with parameters (riskFreeRate, optimizationType)
 * - Nested strategy: ARIMA with parameters (p, d, q, forecastHorizon)
 */
@Component
public class ConfigurePortfolioStrategyUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurePortfolioStrategyUseCase.class);

    private final StrategyRepository strategyRepository;
    private final PortfolioStrategyConfigurationRepository configurationRepository;

    public ConfigurePortfolioStrategyUseCase(
        StrategyRepository strategyRepository,
        PortfolioStrategyConfigurationRepository configurationRepository
    ) {
        this.strategyRepository = strategyRepository;
        this.configurationRepository = configurationRepository;
    }

    /**
     * Configures a strategy for a portfolio with its parameters and dependencies
     */
    @Transactional
    public void execute(ConfigurePortfolioStrategyRequest request) {
        logger.info("Configuring strategy {} for portfolio {}",
            request.strategyCode(), request.portfolioId());

        // 1. Validate main strategy exists
        Strategy mainStrategy = strategyRepository.findByCode(request.strategyCode())
            .orElseThrow(() -> new IllegalArgumentException(
                "Strategy not found: " + request.strategyCode()));

        // 2. Validate parameters
        Map<String, String> parameters = request.parameters() != null
            ? request.parameters()
            : getDefaultParameters(mainStrategy);

        validateParameters(mainStrategy, parameters);

        // 3. Create main strategy configuration
        PortfolioStrategyConfiguration mainConfig = PortfolioStrategyConfiguration.create(
            request.portfolioId(),
            mainStrategy.getCode(),
            mainStrategy.getCategory(),
            parameters
        );

        configurationRepository.save(mainConfig);
        logger.info("Saved main strategy configuration: {}", mainStrategy.getCode());

        // 4. Configure nested strategies (dependencies)
        if (request.nestedStrategies() != null && !request.nestedStrategies().isEmpty()) {
            configureNestedStrategies(
                request.portfolioId(),
                mainConfig.getId(),
                request.nestedStrategies()
            );
        } else if (mainStrategy.hasDependencies()) {
            // Validate that required dependencies are provided
            List<Strategy.StrategyDependency> requiredDeps = mainStrategy.getRequiredDependencies();
            if (!requiredDeps.isEmpty()) {
                logger.warn("Strategy {} has {} required dependencies but none were provided",
                    mainStrategy.getCode(), requiredDeps.size());
            }
        }

        logger.info("Successfully configured strategy {} for portfolio {}",
            request.strategyCode(), request.portfolioId());
    }

    /**
     * Configures nested/dependent strategies
     */
    private void configureNestedStrategies(
        Long portfolioId,
        Long parentConfigId,
        List<ConfigurePortfolioStrategyRequest.NestedStrategyConfig> nestedConfigs
    ) {
        List<PortfolioStrategyConfiguration> nestedConfigEntities = new ArrayList<>();

        for (ConfigurePortfolioStrategyRequest.NestedStrategyConfig nestedConfig : nestedConfigs) {
            // Validate nested strategy exists
            Strategy nestedStrategy = strategyRepository.findByCode(nestedConfig.strategyCode())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Nested strategy not found: " + nestedConfig.strategyCode()));

            // Get parameters (use defaults if not provided)
            Map<String, String> params = nestedConfig.parameters() != null
                ? nestedConfig.parameters()
                : getDefaultParameters(nestedStrategy);

            validateParameters(nestedStrategy, params);

            // Create nested configuration
            PortfolioStrategyConfiguration nestedConfigEntity =
                PortfolioStrategyConfiguration.createNested(
                    portfolioId,
                    nestedStrategy.getCode(),
                    nestedStrategy.getCategory(),
                    params,
                    parentConfigId
                );

            nestedConfigEntities.add(nestedConfigEntity);
        }

        // Batch save nested configurations
        configurationRepository.saveAll(nestedConfigEntities);
        logger.info("Saved {} nested strategy configurations", nestedConfigEntities.size());
    }

    /**
     * Gets default parameter values from strategy definition
     */
    private Map<String, String> getDefaultParameters(Strategy strategy) {
        Map<String, String> defaults = new HashMap<>();
        strategy.getParameters().forEach(param ->
            defaults.put(param.getCode(), param.getDefaultValue())
        );
        return defaults;
    }

    /**
     * Validates that required parameters are provided and values are valid
     */
    private void validateParameters(Strategy strategy, Map<String, String> parameters) {
        for (var param : strategy.getParameters()) {
            if (param.isRequired()) {
                String value = parameters.get(param.getCode());
                if (value == null || value.isBlank()) {
                    throw new IllegalArgumentException(
                        "Required parameter missing: " + param.getCode() +
                        " for strategy: " + strategy.getCode()
                    );
                }

                if (!param.isValid(value)) {
                    throw new IllegalArgumentException(
                        "Invalid value for parameter: " + param.getCode() +
                        " (expected " + param.getDataType() + "): " + value
                    );
                }
            }
        }
    }
}
