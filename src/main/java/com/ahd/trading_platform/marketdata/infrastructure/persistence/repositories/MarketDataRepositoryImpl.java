package com.ahd.trading_platform.marketdata.infrastructure.persistence.repositories;

import com.ahd.trading_platform.marketdata.domain.entities.MarketInstrument;
import com.ahd.trading_platform.marketdata.domain.repositories.MarketDataRepository;
import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
import com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.MarketInstrumentEntity;
import com.ahd.trading_platform.marketdata.infrastructure.persistence.mappers.MarketDataMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Main repository implementation that coordinates between instrument metadata
 * and asset-specific price data repositories using the factory pattern.
 */
@Repository
public class MarketDataRepositoryImpl implements MarketDataRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataRepositoryImpl.class);
    
    private final MarketInstrumentJpaRepository instrumentRepository;
    private final AssetSpecificRepositoryFactory repositoryFactory;
    private final MarketDataMapper mapper;
    
    public MarketDataRepositoryImpl(
        MarketInstrumentJpaRepository instrumentRepository,
        AssetSpecificRepositoryFactory repositoryFactory,
        MarketDataMapper mapper) {
        
        this.instrumentRepository = instrumentRepository;
        this.repositoryFactory = repositoryFactory;
        this.mapper = mapper;
    }
    
    @Override
    @Transactional
    public void save(MarketInstrument instrument) {
        if (instrument == null) {
            throw new IllegalArgumentException("Instrument cannot be null");
        }
        
        logger.debug("Saving market instrument: {}", instrument.getSymbol());
        
        // Save or update instrument metadata
        MarketInstrumentEntity entity = mapper.toEntity(instrument);
        MarketInstrumentEntity savedEntity = instrumentRepository.save(entity);
        
        // Save price data using asset-specific repository
        List<OHLCV> priceHistory = instrument.getPriceHistory();
        if (!priceHistory.isEmpty()) {
            if (repositoryFactory.supportsAsset(instrument.getSymbol())) {
                AssetSpecificPriceRepository priceRepo = repositoryFactory.getRepository(instrument.getSymbol());
                priceRepo.saveAll(priceHistory);
                logger.info("Saved {} price data points for {}", priceHistory.size(), instrument.getSymbol());
            } else {
                logger.warn("No asset-specific repository found for {}, price data not saved", instrument.getSymbol());
            }
        }
        
        logger.info("Successfully saved market instrument: {}", instrument.getSymbol());
    }
    
    @Override
    public Optional<MarketInstrument> findBySymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return Optional.empty();
        }
        
        logger.debug("Finding market instrument by symbol: {}", symbol);
        
        Optional<MarketInstrumentEntity> entityOpt = instrumentRepository.findBySymbolIgnoreCase(symbol);
        if (entityOpt.isEmpty()) {
            logger.debug("Market instrument not found: {}", symbol);
            return Optional.empty();
        }
        
        MarketInstrumentEntity entity = entityOpt.get();
        MarketInstrument instrument = mapper.toDomain(entity);
        
        // Load price history if asset-specific repository exists
        if (repositoryFactory.supportsAsset(symbol)) {
            try {
                AssetSpecificPriceRepository priceRepo = repositoryFactory.getRepository(symbol);
                List<OHLCV> priceHistory = priceRepo.findAll();
                instrument.addPriceData(priceHistory);
                logger.debug("Loaded {} price data points for {}", priceHistory.size(), symbol);
            } catch (Exception e) {
                logger.warn("Failed to load price data for {}: {}", symbol, e.getMessage());
            }
        }
        
        return Optional.of(instrument);
    }
    
    @Override
    public List<MarketInstrument> findAll() {
        logger.debug("Finding all market instruments");
        
        List<MarketInstrumentEntity> entities = instrumentRepository.findAll();
        
        return entities.stream()
            .map(entity -> {
                MarketInstrument instrument = mapper.toDomain(entity);
                
                // Load price history if available
                if (repositoryFactory.supportsAsset(entity.getSymbol())) {
                    try {
                        AssetSpecificPriceRepository priceRepo = repositoryFactory.getRepository(entity.getSymbol());
                        List<OHLCV> priceHistory = priceRepo.findAll();
                        instrument.addPriceData(priceHistory);
                    } catch (Exception e) {
                        logger.warn("Failed to load price data for {}: {}", entity.getSymbol(), e.getMessage());
                    }
                }
                
                return instrument;
            })
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void saveHistoricalData(String symbol, List<OHLCV> ohlcvData) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        if (ohlcvData == null) {
            throw new IllegalArgumentException("OHLCV data cannot be null");
        }
        
        logger.debug("Saving {} historical data points for {}", ohlcvData.size(), symbol);
        
        if (!repositoryFactory.supportsAsset(symbol)) {
            throw new UnsupportedOperationException("No repository configured for asset: " + symbol);
        }
        
        AssetSpecificPriceRepository priceRepo = repositoryFactory.getRepository(symbol);
        priceRepo.saveAll(ohlcvData);
        
        logger.info("Successfully saved {} historical data points for {}", ohlcvData.size(), symbol);
    }
    
    @Override
    public List<OHLCV> findHistoricalData(String symbol, TimeRange timeRange) {
        if (symbol == null || symbol.isBlank()) {
            return List.of();
        }
        if (timeRange == null) {
            return List.of();
        }
        
        logger.debug("Finding historical data for {} in time range: {}", symbol, timeRange);
        
        if (!repositoryFactory.supportsAsset(symbol)) {
            logger.warn("No repository configured for asset: {}", symbol);
            return List.of();
        }
        
        AssetSpecificPriceRepository priceRepo = repositoryFactory.getRepository(symbol);
        List<OHLCV> result = priceRepo.findByTimeRange(timeRange);
        
        logger.debug("Found {} historical data points for {} in time range", result.size(), symbol);
        return result;
    }
    
    @Override
    public List<OHLCV> findAllHistoricalData(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return List.of();
        }
        
        logger.debug("Finding all historical data for {}", symbol);
        
        if (!repositoryFactory.supportsAsset(symbol)) {
            logger.warn("No repository configured for asset: {}", symbol);
            return List.of();
        }
        
        AssetSpecificPriceRepository priceRepo = repositoryFactory.getRepository(symbol);
        List<OHLCV> result = priceRepo.findAll();
        
        logger.debug("Found {} total historical data points for {}", result.size(), symbol);
        return result;
    }
    
    @Override
    public boolean hasHistoricalData(String symbol, TimeRange timeRange) {
        if (symbol == null || symbol.isBlank() || timeRange == null) {
            return false;
        }
        
        if (!repositoryFactory.supportsAsset(symbol)) {
            return false;
        }
        
        AssetSpecificPriceRepository priceRepo = repositoryFactory.getRepository(symbol);
        return priceRepo.hasDataInTimeRange(timeRange);
    }
    
    @Override
    @Transactional
    public void deleteBySymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        
        logger.warn("Deleting all data for symbol: {}", symbol);
        
        // Delete price data if repository exists
        if (repositoryFactory.supportsAsset(symbol)) {
            AssetSpecificPriceRepository priceRepo = repositoryFactory.getRepository(symbol);
            priceRepo.deleteAll();
        }
        
        // Delete instrument metadata
        Optional<MarketInstrumentEntity> entityOpt = instrumentRepository.findBySymbolIgnoreCase(symbol);
        entityOpt.ifPresent(instrumentRepository::delete);
        
        logger.info("Deleted all data for symbol: {}", symbol);
    }
    
    @Override
    public long getDataPointCount(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return 0L;
        }
        
        if (!repositoryFactory.supportsAsset(symbol)) {
            return 0L;
        }
        
        AssetSpecificPriceRepository priceRepo = repositoryFactory.getRepository(symbol);
        return priceRepo.count();
    }
}