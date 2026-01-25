package com.ahd.trading_platform.marketdata.domain.repositories;

import com.ahd.trading_platform.marketdata.domain.entities.Market;
import org.springframework.modulith.NamedInterface;

import java.util.List;
import java.util.Optional;

/**
 * Repository contract for Market persistence operations.
 * This is a domain interface that should be implemented in the infrastructure layer.
 * Exposed as a named interface for cross-module access.
 */
@NamedInterface
public interface MarketRepository {

    /**
     * Finds a market by its code (e.g., "LINEAR", "SPOT")
     */
    Optional<Market> findByCode(String code);

    /**
     * Finds a market by its ID
     */
    Optional<Market> findById(Long id);

    /**
     * Finds all available markets
     */
    List<Market> findAll();

    /**
     * Saves or updates a market
     */
    void save(Market market);
}
