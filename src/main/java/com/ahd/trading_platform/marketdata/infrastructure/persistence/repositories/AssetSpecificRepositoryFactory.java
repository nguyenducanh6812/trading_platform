package com.ahd.trading_platform.marketdata.infrastructure.persistence.repositories;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for routing to asset-specific price repositories.
 * Implements the Repository Factory pattern to handle asset-specific storage strategy.
 */
@Component
public class AssetSpecificRepositoryFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(AssetSpecificRepositoryFactory.class);
    
    private final Map<String, AssetSpecificPriceRepository> repositories;
    
    public AssetSpecificRepositoryFactory(
        BtcPriceRepositoryImpl btcRepository,
        EthPriceRepositoryImpl ethRepository) {
        
        this.repositories = new ConcurrentHashMap<>();
        
        // Register asset-specific repositories
        repositories.put("BTC", btcRepository);
        repositories.put("ETH", ethRepository);
        
        logger.info("Initialized asset-specific repository factory with {} repositories: {}", 
            repositories.size(), repositories.keySet());
    }
    
    /**
     * Gets the appropriate repository for a given asset symbol
     */
    public AssetSpecificPriceRepository getRepository(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        
        String normalizedSymbol = symbol.toUpperCase().trim();
        AssetSpecificPriceRepository repository = repositories.get(normalizedSymbol);
        
        if (repository == null) {
            logger.error("No repository found for asset symbol: {}", normalizedSymbol);
            throw new UnsupportedOperationException(
                String.format("No repository configured for asset: %s. Supported assets: %s", 
                    normalizedSymbol, repositories.keySet()));
        }
        
        logger.debug("Retrieved repository for asset: {}", normalizedSymbol);
        return repository;
    }
    
    /**
     * Checks if a repository exists for the given symbol
     */
    public boolean supportsAsset(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return false;
        }
        
        String normalizedSymbol = symbol.toUpperCase().trim();
        return repositories.containsKey(normalizedSymbol);
    }
    
    /**
     * Returns all supported asset symbols
     */
    public java.util.Set<String> getSupportedAssets() {
        return java.util.Set.copyOf(repositories.keySet());
    }
    
    /**
     * Returns the count of registered repositories
     */
    public int getRepositoryCount() {
        return repositories.size();
    }
    
    /**
     * Registers a new asset-specific repository (for future extensibility)
     */
    public void registerRepository(String symbol, AssetSpecificPriceRepository repository) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        if (repository == null) {
            throw new IllegalArgumentException("Repository cannot be null");
        }
        
        String normalizedSymbol = symbol.toUpperCase().trim();
        AssetSpecificPriceRepository existing = repositories.put(normalizedSymbol, repository);
        
        if (existing != null) {
            logger.warn("Replaced existing repository for asset: {}", normalizedSymbol);
        } else {
            logger.info("Registered new repository for asset: {}", normalizedSymbol);
        }
    }
}