package com.ahd.trading_platform.marketdata.domain.services;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for creating and managing external data client strategies.
 * Implements the Strategy pattern to allow runtime selection of data providers.
 */
@Component
public class ExternalDataClientFactory {
    
    private final Map<String, ExternalDataClientStrategy> strategies;
    
    public ExternalDataClientFactory(List<ExternalDataClientStrategy> strategies) {
        this.strategies = strategies.stream()
            .collect(Collectors.toMap(
                strategy -> strategy.getDataSource().toLowerCase(),
                Function.identity()
            ));
    }
    
    /**
     * Gets a strategy by data source name
     * @param dataSource The data source identifier (e.g., "bybit", "binance")
     * @return The corresponding strategy
     * @throws IllegalArgumentException if no strategy found for the given source
     */
    public ExternalDataClientStrategy getStrategy(String dataSource) {
        String normalizedSource = dataSource.toLowerCase();
        ExternalDataClientStrategy strategy = strategies.get(normalizedSource);
        
        if (strategy == null) {
            throw new IllegalArgumentException(
                String.format("No external data client strategy found for source: %s. Available sources: %s", 
                    dataSource, strategies.keySet())
            );
        }
        
        return strategy;
    }
    
    /**
     * Gets the default strategy (first available)
     * @return The default strategy
     * @throws IllegalStateException if no strategies are available
     */
    public ExternalDataClientStrategy getDefaultStrategy() {
        return strategies.values().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No external data client strategies available"));
    }
    
    /**
     * Gets all available data source names
     * @return List of available data source identifiers
     */
    public List<String> getAvailableDataSources() {
        return List.copyOf(strategies.keySet());
    }
    
    /**
     * Checks if a data source is supported
     * @param dataSource The data source to check
     * @return true if supported, false otherwise
     */
    public boolean isDataSourceSupported(String dataSource) {
        return strategies.containsKey(dataSource.toLowerCase());
    }
}