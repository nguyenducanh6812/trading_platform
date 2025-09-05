package com.ahd.trading_platform.forecasting.infrastructure.repositories;

import com.ahd.trading_platform.forecasting.domain.repositories.DemeanDiffOCMasterDataRepository;
import com.ahd.trading_platform.forecasting.domain.valueobjects.DemeanDiffOCMasterData;
import com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.DemeanDiffOCMasterDataEntity;
import com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories.DemeanDiffOCMasterDataJpaRepository;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Infrastructure implementation of DemeanDiffOCMasterDataRepository.
 * Handles mapping between domain objects and persistence entities.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DemeanDiffOCMasterDataRepositoryImpl implements DemeanDiffOCMasterDataRepository {
    
    private final DemeanDiffOCMasterDataJpaRepository jpaRepository;
    
    @Override
    public List<DemeanDiffOCMasterData> findByInstrumentAndTimeRange(
            TradingInstrument instrument, 
            TimeRange timeRange,
            String calculationVersion) {
        
        List<DemeanDiffOCMasterDataEntity> entities = jpaRepository.findByInstrumentAndTimestampBetween(
            instrument, timeRange.from(), timeRange.to(), calculationVersion
        );
        
        return entities.stream()
            .map(this::toDomain)
            .toList();
    }
    
    @Override
    public List<DemeanDiffOCMasterData> findByInstrumentFromTimestamp(
            TradingInstrument instrument,
            Instant fromTimestamp,
            String calculationVersion) {
        
        List<DemeanDiffOCMasterDataEntity> entities = jpaRepository.findByInstrumentAndTimestampGreaterThanEqual(
            instrument, fromTimestamp, calculationVersion
        );
        
        return entities.stream()
            .map(this::toDomain)
            .toList();
    }
    
    @Override
    public boolean existsForInstrumentAndTimeRange(
            TradingInstrument instrument,
            TimeRange timeRange,
            String calculationVersion) {
        
        return jpaRepository.existsByInstrumentAndTimestampRange(
            instrument, timeRange.from(), timeRange.to(), calculationVersion
        );
    }
    
    @Override
    public Optional<Instant> getLatestTimestampForInstrument(
            TradingInstrument instrument,
            String calculationVersion) {
        
        return jpaRepository.findMaxTimestampByInstrument(instrument, calculationVersion);
    }
    
    @Override
    @Transactional
    public List<DemeanDiffOCMasterData> saveAll(List<DemeanDiffOCMasterData> masterData) {
        log.debug("Saving {} master data points", masterData.size());
        
        List<DemeanDiffOCMasterDataEntity> entities = masterData.stream()
            .map(this::toEntity)
            .toList();
        
        List<DemeanDiffOCMasterDataEntity> savedEntities = jpaRepository.saveAll(entities);
        
        return savedEntities.stream()
            .map(this::toDomain)
            .toList();
    }
    
    @Override
    @Transactional
    public DemeanDiffOCMasterData save(DemeanDiffOCMasterData masterData) {
        DemeanDiffOCMasterDataEntity entity = toEntity(masterData);
        DemeanDiffOCMasterDataEntity savedEntity = jpaRepository.save(entity);
        
        return toDomain(savedEntity);
    }
    
    @Override
    @Transactional
    public void deleteByInstrumentAndCalculationVersion(
            TradingInstrument instrument,
            String calculationVersion) {
        
        log.info("Deleting master data for instrument {} with calculation version {}", 
            instrument.getCode(), calculationVersion);
        
        jpaRepository.deleteByInstrumentAndCalculationVersion(instrument, calculationVersion);
    }
    
    @Override
    public long countByInstrumentAndTimeRange(
            TradingInstrument instrument,
            TimeRange timeRange,
            String calculationVersion) {
        
        return jpaRepository.countByInstrumentAndTimestampRange(
            instrument, timeRange.from(), timeRange.to(), calculationVersion
        );
    }
    
    @Override
    public long countByInstrument(TradingInstrument instrument, String calculationVersion) {
        return jpaRepository.countByInstrumentAndCalculationVersion(instrument, calculationVersion);
    }
    
    @Override
    public List<DemeanDiffOCMasterData> findObsoleteData(String currentCalculationVersion) {
        List<DemeanDiffOCMasterDataEntity> entities = jpaRepository.findByCalculationVersionNot(
            currentCalculationVersion
        );
        
        return entities.stream()
            .map(this::toDomain)
            .toList();
    }
    
    @Override
    public List<String> getAvailableCalculationVersions(TradingInstrument instrument) {
        return jpaRepository.findDistinctCalculationVersionsByInstrument(instrument);
    }
    
    @Override
    public Optional<String> getLatestCalculationVersion(TradingInstrument instrument) {
        return jpaRepository.findLatestCalculationVersionByInstrument(instrument);
    }
    
    /**
     * Converts domain object to entity
     */
    private DemeanDiffOCMasterDataEntity toEntity(DemeanDiffOCMasterData domain) {
        return DemeanDiffOCMasterDataEntity.builder()
            .instrument(domain.instrument())
            .timestamp(domain.timestamp())
            .openPrice(domain.openPrice())
            .closePrice(domain.closePrice())
            .oc(domain.oc())
            .diffOC(domain.diffOC())
            .demeanDiffOC(domain.demeanDiffOC())
            .meanDiffOC(domain.meanDiffOC())
            .calculationVersion(domain.calculationVersion())
            .calculatedAt(domain.calculatedAt())
            .build();
    }
    
    /**
     * Converts entity to domain object
     */
    private DemeanDiffOCMasterData toDomain(DemeanDiffOCMasterDataEntity entity) {
        return new DemeanDiffOCMasterData(
            entity.getInstrument(),
            entity.getTimestamp(),
            entity.getOpenPrice(),
            entity.getClosePrice(),
            entity.getOc(),
            entity.getDiffOC(),
            entity.getDemeanDiffOC(),
            entity.getMeanDiffOC(),
            entity.getCalculationVersion(),
            entity.getCalculatedAt()
        );
    }
}