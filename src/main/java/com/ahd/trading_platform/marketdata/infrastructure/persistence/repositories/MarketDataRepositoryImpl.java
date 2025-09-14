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
        
        // Save or update instrument metadata - use saveOrUpdate to avoid duplicate key errors
        MarketInstrumentEntity entity = saveOrUpdateInstrument(instrument);
        MarketInstrumentEntity savedEntity = entity;
        
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
    
    /**
     * Saves or updates instrument metadata without causing duplicate key violations
     */
    private MarketInstrumentEntity saveOrUpdateInstrument(MarketInstrument instrument) {
        // Check if instrument already exists
        Optional<MarketInstrumentEntity> existingEntityOpt = 
            instrumentRepository.findBySymbolIgnoreCase(instrument.getSymbol());
        
        if (existingEntityOpt.isPresent()) {
            // Update existing instrument
            MarketInstrumentEntity existingEntity = existingEntityOpt.get();
            updateInstrumentEntity(existingEntity, instrument);
            return instrumentRepository.save(existingEntity);
        } else {
            // Create new instrument
            MarketInstrumentEntity newEntity = mapper.toEntity(instrument);
            return instrumentRepository.save(newEntity);
        }
    }
    
    /**
     * Updates existing instrument entity with data from domain object
     */
    private void updateInstrumentEntity(MarketInstrumentEntity entity, MarketInstrument instrument) {
        entity.setName(instrument.getName());
        entity.setDataPointCount(instrument.getDataPointCount());
        entity.setQualityScore(instrument.getQualityMetrics().getQualityScore());
        entity.setQualityLevel(instrument.getQualityMetrics().getQualityLevel());
        entity.setDataSource(instrument.getQualityMetrics().dataSource());
        // Don't update symbol as it's the primary key
        // Don't update audit fields (created_at, created_by) as they should remain unchanged
        entity.getAuditInfo().setUpdatedAt(java.time.Instant.now());
        entity.getAuditInfo().setUpdatedBy("SYSTEM");
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
    
    /**
     * Finds instrument by symbol WITHOUT loading price data.
     * Used for data ingestion scenarios where we only need instrument metadata.
     */
    public Optional<MarketInstrument> findInstrumentMetadataBySymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return Optional.empty();
        }
        
        logger.debug("Finding instrument metadata by symbol: {}", symbol);
        
        Optional<MarketInstrumentEntity> entityOpt = instrumentRepository.findBySymbolIgnoreCase(symbol);
        if (entityOpt.isEmpty()) {
            logger.debug("Instrument metadata not found: {}", symbol);
            return Optional.empty();
        }
        
        // Return instrument without loading price data - for data ingestion only
        MarketInstrument instrument = mapper.toDomain(entityOpt.get());
        logger.debug("Found instrument metadata for {}", symbol);
        
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
    
    @Override
    public List<TimeRange> findDataRanges(String symbol, TimeRange searchRange) {
        if (symbol == null || symbol.isBlank() || searchRange == null) {
            return List.of();
        }
        
        logger.debug("Finding existing data ranges for {} within {}", symbol, searchRange);
        
        // Check if we have an asset-specific repository for this symbol
        if (!repositoryFactory.supportsAsset(symbol)) {
            logger.debug("No asset-specific repository found for {}", symbol);
            return List.of();
        }
        
        try {
            AssetSpecificPriceRepository priceRepo = repositoryFactory.getRepository(symbol);
            
            // Get existing timestamps within the search range
            List<java.time.Instant> existingTimestamps = priceRepo.findTimestampsInRange(
                searchRange.from(), searchRange.to());
            
            if (existingTimestamps.isEmpty()) {
                logger.debug("No existing data found for {} in range", symbol);
                return List.of();
            }
            
            // Convert timestamps to continuous ranges
            List<TimeRange> dataRanges = convertTimestampsToRanges(existingTimestamps);
            
            logger.debug("Found {} existing data ranges for {}", dataRanges.size(), symbol);
            return dataRanges;
            
        } catch (Exception e) {
            logger.error("Error finding data ranges for {} in range {}: {}", 
                symbol, searchRange, e.getMessage(), e);
            // Return empty list if we can't determine existing data
            return List.of();
        }
    }
    
    /**
     * Converts a list of timestamps to continuous time ranges
     */
    private List<TimeRange> convertTimestampsToRanges(List<java.time.Instant> timestamps) {
        if (timestamps.isEmpty()) {
            return List.of();
        }
        
        // Sort timestamps
        List<java.time.Instant> sorted = timestamps.stream()
            .sorted()
            .toList();
        
        List<TimeRange> ranges = new java.util.ArrayList<>();
        java.time.Instant rangeStart = sorted.get(0);
        java.time.Instant rangeEnd = sorted.get(0);
        
        // 1 day gap tolerance (data is daily)
        java.time.Duration gapTolerance = java.time.Duration.ofDays(2);
        
        for (int i = 1; i < sorted.size(); i++) {
            java.time.Instant current = sorted.get(i);
            
            if (java.time.Duration.between(rangeEnd, current).compareTo(gapTolerance) <= 0) {
                // Continue current range
                rangeEnd = current;
            } else {
                // Gap found - close current range and start new one
                ranges.add(new TimeRange(rangeStart, rangeEnd.plus(java.time.Duration.ofDays(1))));
                rangeStart = current;
                rangeEnd = current;
            }
        }
        
        // Add the final range
        ranges.add(new TimeRange(rangeStart, rangeEnd.plus(java.time.Duration.ofDays(1))));
        
        return ranges;
    }
}