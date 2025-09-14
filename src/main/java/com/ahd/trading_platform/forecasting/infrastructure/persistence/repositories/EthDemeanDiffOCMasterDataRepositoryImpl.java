package com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories;

import com.ahd.trading_platform.forecasting.domain.valueobjects.DemeanDiffOCMasterData;
import com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.EthDemeanDiffOCMasterDataEntity;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Ethereum-specific master data repository implementation.
 * Handles persistence operations for ETH master data using dedicated ETH table.
 */
@Repository
public class EthDemeanDiffOCMasterDataRepositoryImpl implements AssetSpecificMasterDataRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(EthDemeanDiffOCMasterDataRepositoryImpl.class);
    private static final String ASSET_SYMBOL = "ETH";
    
    private final EthDemeanDiffOCMasterDataJpaRepository jpaRepository;
    
    public EthDemeanDiffOCMasterDataRepositoryImpl(EthDemeanDiffOCMasterDataJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public List<DemeanDiffOCMasterData> findByTimeRange(TimeRange timeRange) {
        if (timeRange == null) {
            return List.of();
        }
        
        logger.debug("Finding ETH master data for time range: {}", timeRange);
        
        List<EthDemeanDiffOCMasterDataEntity> entities = jpaRepository.findByTimestampBetweenOrderByTimestampAsc(
            timeRange.from(), timeRange.to());
        
        List<DemeanDiffOCMasterData> result = entities.stream()
            .map(this::toDomain)
            .toList();
            
        logger.debug("Found {} ETH master data points for time range", result.size());
        return result;
    }
    
    @Override
    public List<DemeanDiffOCMasterData> findFromTimestamp(Instant fromTimestamp) {
        if (fromTimestamp == null) {
            return List.of();
        }
        
        logger.debug("Finding ETH master data from timestamp: {}", fromTimestamp);
        
        List<EthDemeanDiffOCMasterDataEntity> entities = jpaRepository.findByTimestampAfterOrderByTimestampAsc(fromTimestamp);
        
        List<DemeanDiffOCMasterData> result = entities.stream()
            .map(this::toDomain)
            .toList();
            
        logger.debug("Found {} ETH master data points from timestamp", result.size());
        return result;
    }
    
    @Override
    public boolean existsForTimeRange(TimeRange timeRange) {
        if (timeRange == null) {
            return false;
        }
        
        long count = countByTimeRange(timeRange);
        return count > 0;
    }
    
    @Override
    public Optional<Instant> getLatestTimestamp() {
        logger.debug("Finding latest ETH master data timestamp");
        
        return jpaRepository.findTopByOrderByTimestampDesc()
            .map(EthDemeanDiffOCMasterDataEntity::getTimestamp);
    }
    
    @Override
    @Transactional
    public List<DemeanDiffOCMasterData> saveAll(List<DemeanDiffOCMasterData> masterData) {
        if (masterData == null || masterData.isEmpty()) {
            return List.of();
        }
        
        logger.debug("Saving {} ETH master data points", masterData.size());
        
        List<EthDemeanDiffOCMasterDataEntity> entities = masterData.stream()
            .map(this::toEntity)
            .toList();
        
        try {
            // Filter out existing data to avoid duplicate key errors
            List<EthDemeanDiffOCMasterDataEntity> newEntities = entities.stream()
                .filter(entity -> !jpaRepository.existsByTimestamp(entity.getTimestamp()))
                .toList();
            
            if (newEntities.isEmpty()) {
                logger.info("All {} ETH master data points already exist, skipping save", masterData.size());
                return masterData;
            }
            
            List<EthDemeanDiffOCMasterDataEntity> savedEntities = jpaRepository.saveAll(newEntities);
            logger.info("Successfully saved {} new ETH master data points (filtered {} duplicates)", 
                newEntities.size(), entities.size() - newEntities.size());
                
            return savedEntities.stream()
                .map(this::toDomain)
                .toList();
        } catch (Exception e) {
            logger.error("Failed to save ETH master data", e);
            throw new RuntimeException("Failed to save ETH master data: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public DemeanDiffOCMasterData save(DemeanDiffOCMasterData masterData) {
        if (masterData == null) {
            throw new IllegalArgumentException("Master data cannot be null");
        }
        
        EthDemeanDiffOCMasterDataEntity entity = toEntity(masterData);
        EthDemeanDiffOCMasterDataEntity savedEntity = jpaRepository.save(entity);
        
        return toDomain(savedEntity);
    }
    
    @Override
    @Transactional
    public DemeanDiffOCMasterData upsert(DemeanDiffOCMasterData masterData) {
        if (masterData == null) {
            throw new IllegalArgumentException("Master data cannot be null");
        }
        
        logger.debug("Upserting ETH master data for timestamp: {}", masterData.timestamp());
        
        // Check if record already exists
        Optional<EthDemeanDiffOCMasterDataEntity> existingEntity = 
            jpaRepository.findByTimestamp(masterData.timestamp());
        
        EthDemeanDiffOCMasterDataEntity entityToSave;
        
        if (existingEntity.isPresent()) {
            // Update existing record
            logger.debug("Updating existing ETH master data for timestamp: {}", masterData.timestamp());
            entityToSave = existingEntity.get();
            
            // Update all fields with new values
            entityToSave.setOpenPrice(masterData.openPrice());
            entityToSave.setClosePrice(masterData.closePrice());
            entityToSave.setOc(masterData.oc());
            entityToSave.setDiffOC(masterData.diffOC());
            entityToSave.setDemeanDiffOC(masterData.demeanDiffOC());
            entityToSave.setHasDifferences(masterData.hasDifferences());
            // Keep original createdAt, don't update it
        } else {
            // Create new record
            logger.debug("Creating new ETH master data for timestamp: {}", masterData.timestamp());
            entityToSave = toEntity(masterData);
        }
        
        try {
            EthDemeanDiffOCMasterDataEntity savedEntity = jpaRepository.save(entityToSave);
            logger.debug("Successfully upserted ETH master data for timestamp: {}", masterData.timestamp());
            return toDomain(savedEntity);
        } catch (Exception e) {
            logger.error("Failed to upsert ETH master data for timestamp {}: {}", masterData.timestamp(), e.getMessage(), e);
            throw new RuntimeException("Failed to upsert ETH master data: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public void deleteAll() {
        logger.warn("Deleting all ETH master data");
        jpaRepository.deleteAll();
        logger.info("All ETH master data deleted");
    }
    
    @Override
    public long countByTimeRange(TimeRange timeRange) {
        if (timeRange == null) {
            return 0L;
        }
        
        long count = jpaRepository.countByTimestampBetween(timeRange.from(), timeRange.to());
        logger.debug("ETH master data count in time range {}: {}", timeRange, count);
        return count;
    }
    
    @Override
    public long count() {
        long count = jpaRepository.count();
        logger.debug("ETH master data total count: {}", count);
        return count;
    }
    
    @Override
    public String getAssetSymbol() {
        return ASSET_SYMBOL;
    }
    
    @Override
    public List<Instant> findTimestampsInRange(Instant from, Instant to) {
        if (from == null || to == null) {
            return List.of();
        }
        
        logger.debug("Finding ETH master data timestamps for time range: {} to {}", from, to);
        
        List<Instant> timestamps = jpaRepository.findTimestampsByDateRange(from, to);
        
        logger.debug("Found {} ETH master data timestamp entries for time range", timestamps.size());
        return timestamps;
    }
    
    @Override
    public List<DemeanDiffOCMasterData> findByTimeRangeWithDifferences(TimeRange timeRange) {
        if (timeRange == null) {
            return List.of();
        }
        
        logger.debug("Finding ETH master data with differences for time range: {}", timeRange);
        
        List<EthDemeanDiffOCMasterDataEntity> entities = jpaRepository.findByTimestampBetweenAndHasDifferencesOrderByTimestampAsc(
            timeRange.from(), timeRange.to());
        
        List<DemeanDiffOCMasterData> result = entities.stream()
            .map(this::toDomain)
            .toList();
            
        logger.debug("Found {} ETH master data points with differences for time range", result.size());
        return result;
    }
    
    @Override
    public long countByTimeRangeWithDifferences(TimeRange timeRange) {
        if (timeRange == null) {
            return 0L;
        }
        
        long count = jpaRepository.countByTimestampBetweenAndHasDifferences(timeRange.from(), timeRange.to());
        logger.debug("ETH master data count with differences in time range {}: {}", timeRange, count);
        return count;
    }
    
    /**
     * Converts domain object to entity
     */
    private EthDemeanDiffOCMasterDataEntity toEntity(DemeanDiffOCMasterData domain) {
        return EthDemeanDiffOCMasterDataEntity.builder()
            .timestamp(domain.timestamp())
            .openPrice(domain.openPrice())
            .closePrice(domain.closePrice())
            .oc(domain.oc())                        // Add OC field mapping
            .diffOC(domain.diffOC())
            .demeanDiffOC(domain.demeanDiffOC())
            .hasDifferences(domain.hasDifferences())
            .createdAt(domain.calculatedAt())
            .build();
    }
    
    /**
     * Converts entity to domain object
     */
    private DemeanDiffOCMasterData toDomain(EthDemeanDiffOCMasterDataEntity entity) {
        // Calculate meanDiffOC only if we have difference data
        BigDecimal meanDiffOC = (entity.getDiffOC() != null && entity.getDemeanDiffOC() != null) ? 
            entity.getDiffOC().subtract(entity.getDemeanDiffOC()) : null;
        
        return new DemeanDiffOCMasterData(
            TradingInstrument.ETH,  // Fixed instrument for ETH repository
            entity.getTimestamp(),
            entity.getOpenPrice(),
            entity.getClosePrice(),
            entity.getOc(),          // Use stored OC field instead of calculating
            entity.getDiffOC(),
            entity.getDemeanDiffOC(),
            meanDiffOC,
            "v1.0", // Default calculation version
            entity.getCreatedAt()
        );
    }
}