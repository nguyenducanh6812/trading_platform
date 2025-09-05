package com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories;

import com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.DemeanDiffOCMasterDataEntity;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for DemeanDiffOC master data persistence operations
 */
@Repository
public interface DemeanDiffOCMasterDataJpaRepository extends JpaRepository<DemeanDiffOCMasterDataEntity, Long> {
    
    /**
     * Finds master data by instrument and timestamp range
     */
    @Query("""
        SELECT d FROM DemeanDiffOCMasterDataEntity d 
        WHERE d.instrument = :instrument 
        AND d.timestamp BETWEEN :fromTimestamp AND :toTimestamp
        AND d.calculationVersion = :calculationVersion
        ORDER BY d.timestamp ASC
        """)
    List<DemeanDiffOCMasterDataEntity> findByInstrumentAndTimestampBetween(
        @Param("instrument") TradingInstrument instrument,
        @Param("fromTimestamp") Instant fromTimestamp,
        @Param("toTimestamp") Instant toTimestamp,
        @Param("calculationVersion") String calculationVersion
    );
    
    /**
     * Finds master data by instrument starting from timestamp
     */
    @Query("""
        SELECT d FROM DemeanDiffOCMasterDataEntity d 
        WHERE d.instrument = :instrument 
        AND d.timestamp >= :fromTimestamp
        AND d.calculationVersion = :calculationVersion
        ORDER BY d.timestamp ASC
        """)
    List<DemeanDiffOCMasterDataEntity> findByInstrumentAndTimestampGreaterThanEqual(
        @Param("instrument") TradingInstrument instrument,
        @Param("fromTimestamp") Instant fromTimestamp,
        @Param("calculationVersion") String calculationVersion
    );
    
    /**
     * Checks if master data exists for instrument and time range
     */
    @Query("""
        SELECT COUNT(d) > 0 FROM DemeanDiffOCMasterDataEntity d 
        WHERE d.instrument = :instrument 
        AND d.timestamp BETWEEN :fromTimestamp AND :toTimestamp
        AND d.calculationVersion = :calculationVersion
        """)
    boolean existsByInstrumentAndTimestampRange(
        @Param("instrument") TradingInstrument instrument,
        @Param("fromTimestamp") Instant fromTimestamp,
        @Param("toTimestamp") Instant toTimestamp,
        @Param("calculationVersion") String calculationVersion
    );
    
    /**
     * Gets the latest timestamp for instrument
     */
    @Query("""
        SELECT MAX(d.timestamp) FROM DemeanDiffOCMasterDataEntity d 
        WHERE d.instrument = :instrument 
        AND d.calculationVersion = :calculationVersion
        """)
    Optional<Instant> findMaxTimestampByInstrument(
        @Param("instrument") TradingInstrument instrument,
        @Param("calculationVersion") String calculationVersion
    );
    
    /**
     * Deletes by instrument and calculation version
     */
    void deleteByInstrumentAndCalculationVersion(
        TradingInstrument instrument, 
        String calculationVersion
    );
    
    /**
     * Counts by instrument and time range
     */
    @Query("""
        SELECT COUNT(d) FROM DemeanDiffOCMasterDataEntity d 
        WHERE d.instrument = :instrument 
        AND d.timestamp BETWEEN :fromTimestamp AND :toTimestamp
        AND d.calculationVersion = :calculationVersion
        """)
    long countByInstrumentAndTimestampRange(
        @Param("instrument") TradingInstrument instrument,
        @Param("fromTimestamp") Instant fromTimestamp,
        @Param("toTimestamp") Instant toTimestamp,
        @Param("calculationVersion") String calculationVersion
    );
    
    /**
     * Counts by instrument
     */
    long countByInstrumentAndCalculationVersion(
        TradingInstrument instrument, 
        String calculationVersion
    );
    
    /**
     * Finds obsolete data (older calculation versions)
     */
    @Query("""
        SELECT d FROM DemeanDiffOCMasterDataEntity d 
        WHERE d.calculationVersion != :currentVersion
        ORDER BY d.calculatedAt ASC
        """)
    List<DemeanDiffOCMasterDataEntity> findByCalculationVersionNot(
        @Param("currentVersion") String currentVersion
    );
    
    /**
     * Gets all calculation versions for instrument
     */
    @Query("""
        SELECT DISTINCT d.calculationVersion FROM DemeanDiffOCMasterDataEntity d 
        WHERE d.instrument = :instrument
        ORDER BY d.calculationVersion DESC
        """)
    List<String> findDistinctCalculationVersionsByInstrument(
        @Param("instrument") TradingInstrument instrument
    );
    
    /**
     * Gets latest calculation version for instrument
     */
    @Query("""
        SELECT d.calculationVersion FROM DemeanDiffOCMasterDataEntity d 
        WHERE d.instrument = :instrument
        ORDER BY d.calculatedAt DESC 
        LIMIT 1
        """)
    Optional<String> findLatestCalculationVersionByInstrument(
        @Param("instrument") TradingInstrument instrument
    );
}