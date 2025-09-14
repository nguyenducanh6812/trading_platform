package com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories;

import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Factory for getting asset-specific expected return prediction repositories.
 * Uses strategy pattern to provide the correct repository implementation for each trading instrument.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssetSpecificPredictionRepositoryFactory {
    
    private final BtcExpectedReturnPredictionRepositoryImpl btcRepository;
    private final EthExpectedReturnPredictionRepositoryImpl ethRepository;
    
    /**
     * Gets the appropriate prediction repository for the given trading instrument
     */
    public AssetSpecificPredictionRepository getRepository(TradingInstrument instrument) {
        log.debug("Getting prediction repository for instrument: {}", instrument.getCode());
        
        return switch (instrument) {
            case BTC -> {
                log.debug("Returning BTC prediction repository");
                yield btcRepository;
            }
            case ETH -> {
                log.debug("Returning ETH prediction repository");
                yield ethRepository;
            }
            default -> throw new IllegalArgumentException(
                "Unsupported trading instrument for prediction storage: " + instrument.getCode());
        };
    }
    
    /**
     * Gets repository by instrument code string
     */
    public AssetSpecificPredictionRepository getRepository(String instrumentCode) {
        TradingInstrument instrument = TradingInstrument.fromCode(instrumentCode);
        return getRepository(instrument);
    }
    
    /**
     * Checks if prediction storage is supported for the given instrument
     */
    public boolean isSupported(TradingInstrument instrument) {
        try {
            getRepository(instrument);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Checks if prediction storage is supported for the given instrument code
     */
    public boolean isSupported(String instrumentCode) {
        try {
            getRepository(instrumentCode);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}