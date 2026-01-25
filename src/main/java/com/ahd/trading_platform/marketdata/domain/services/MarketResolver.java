package com.ahd.trading_platform.marketdata.domain.services;

import com.ahd.trading_platform.marketdata.domain.valueobjects.BybitMarketType;
import com.ahd.trading_platform.marketdata.infrastructure.persistence.repositories.MarketInstrumentJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to resolve which market (SPOT, LINEAR, INVERSE, OPTION) a trading symbol belongs to.
 * Queries the market_instruments table directly to avoid circular dependencies.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MarketResolver {

    private final MarketInstrumentJpaRepository instrumentRepository;

    /**
     * Determines which market a symbol belongs to by querying the market_instruments table.
     *
     * @param symbol Trading symbol (e.g., "BTCUSDT")
     * @return The market type for this symbol
     * @throws IllegalArgumentException if symbol not found
     */
    public BybitMarketType resolveMarket(String symbol) {
        return instrumentRepository.findBySymbolIgnoreCase(symbol)
            .map(entity -> {
                String marketCode = entity.getMarket().getCode();
                return BybitMarketType.valueOf(marketCode);
            })
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Symbol '%s' not found in market instruments. " +
                    "Please fetch market data for this symbol first.", symbol)
            ));
    }

    /**
     * Checks if a symbol exists in the market instruments.
     */
    public boolean symbolExists(String symbol) {
        return instrumentRepository.findBySymbolIgnoreCase(symbol).isPresent();
    }
}
