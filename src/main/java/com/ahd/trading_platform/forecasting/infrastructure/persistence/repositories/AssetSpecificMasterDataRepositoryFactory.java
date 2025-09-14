package com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories;

import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Factory for selecting asset-specific master data repositories.
 * Follows the strategy pattern to route requests to the appropriate
 * instrument-specific repository implementation.
 */
@Component
public class AssetSpecificMasterDataRepositoryFactory {
    
    private final AssetSpecificMasterDataRepository btcRepository;
    private final AssetSpecificMasterDataRepository ethRepository;
    
    public AssetSpecificMasterDataRepositoryFactory(
            @Qualifier("btcDemeanDiffOCMasterDataRepositoryImpl") AssetSpecificMasterDataRepository btcRepository,
            @Qualifier("ethDemeanDiffOCMasterDataRepositoryImpl") AssetSpecificMasterDataRepository ethRepository) {
        this.btcRepository = btcRepository;
        this.ethRepository = ethRepository;
    }
    
    /**
     * Gets the appropriate repository for the specified trading instrument
     */
    public AssetSpecificMasterDataRepository getRepository(TradingInstrument instrument) {
        return switch (instrument) {
            case BTC -> btcRepository;
            case ETH -> ethRepository;
            default -> throw new IllegalArgumentException(
                "Unsupported trading instrument: " + instrument + 
                ". Supported instruments: " + getSupportedInstruments());
        };
    }
    
    /**
     * Gets all supported trading instruments
     */
    public String getSupportedInstruments() {
        return "[BTC, ETH]";
    }
    
    /**
     * Checks if the trading instrument is supported
     */
    public boolean isSupported(TradingInstrument instrument) {
        return instrument == TradingInstrument.BTC || instrument == TradingInstrument.ETH;
    }
}