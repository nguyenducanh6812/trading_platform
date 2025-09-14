package com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories;

import com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.EthDemeanDiffOCMasterDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for ETH-specific Demean_Diff_OC master data.
 * Provides optimized queries for Ethereum master data operations.
 */
@Repository
public interface EthDemeanDiffOCMasterDataJpaRepository extends JpaRepository<EthDemeanDiffOCMasterDataEntity, Long> {
    
    /**
     * Finds master data within a time range, ordered by timestamp
     */
    @Query("SELECT ed FROM EthDemeanDiffOCMasterDataEntity ed WHERE ed.timestamp >= :fromTime AND ed.timestamp <= :toTime ORDER BY ed.timestamp ASC")
    List<EthDemeanDiffOCMasterDataEntity> findByTimestampBetweenOrderByTimestampAsc(
        @Param("fromTime") Instant fromTime, 
        @Param("toTime") Instant toTime);
    
    /**
     * Finds the latest master data point
     */
    Optional<EthDemeanDiffOCMasterDataEntity> findTopByOrderByTimestampDesc();
    
    /**
     * Finds the earliest master data point
     */
    Optional<EthDemeanDiffOCMasterDataEntity> findTopByOrderByTimestampAsc();
    
    /**
     * Finds master data by specific timestamp
     */
    Optional<EthDemeanDiffOCMasterDataEntity> findByTimestamp(Instant timestamp);
    
    /**
     * Checks if master data exists for a specific timestamp
     */
    boolean existsByTimestamp(Instant timestamp);
    
    /**
     * Counts data points within a time range
     */
    @Query("SELECT COUNT(ed) FROM EthDemeanDiffOCMasterDataEntity ed WHERE ed.timestamp >= :fromTime AND ed.timestamp <= :toTime")
    long countByTimestampBetween(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);
    
    /**
     * Finds master data after a specific timestamp (for incremental updates)
     */
    @Query("SELECT ed FROM EthDemeanDiffOCMasterDataEntity ed WHERE ed.timestamp > :afterTime ORDER BY ed.timestamp ASC")
    List<EthDemeanDiffOCMasterDataEntity> findByTimestampAfterOrderByTimestampAsc(@Param("afterTime") Instant afterTime);
    
    /**
     * Finds only timestamps within a time range for gap detection
     * This is much more efficient than loading full entities when we only need timestamps
     */
    @Query("SELECT ed.timestamp FROM EthDemeanDiffOCMasterDataEntity ed WHERE ed.timestamp >= :fromTime AND ed.timestamp <= :toTime ORDER BY ed.timestamp ASC")
    List<Instant> findTimestampsByDateRange(
        @Param("fromTime") Instant fromTime, 
        @Param("toTime") Instant toTime);
    
    /**
     * Finds master data with calculated differences within a time range
     */
    @Query("SELECT ed FROM EthDemeanDiffOCMasterDataEntity ed WHERE ed.timestamp >= :fromTime AND ed.timestamp <= :toTime AND ed.hasDifferences = true ORDER BY ed.timestamp ASC")
    List<EthDemeanDiffOCMasterDataEntity> findByTimestampBetweenAndHasDifferencesOrderByTimestampAsc(
        @Param("fromTime") Instant fromTime, 
        @Param("toTime") Instant toTime);
    
    /**
     * Counts master data points with calculated differences within a time range
     */
    @Query("SELECT COUNT(ed) FROM EthDemeanDiffOCMasterDataEntity ed WHERE ed.timestamp >= :fromTime AND ed.timestamp <= :toTime AND ed.hasDifferences = true")
    long countByTimestampBetweenAndHasDifferences(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);
    
    /**
     * Deletes data points older than specified timestamp
     */
    void deleteByTimestampBefore(Instant timestamp);
    
    /**
     * Gets paginated data for large datasets
     */
    @Query("SELECT ed FROM EthDemeanDiffOCMasterDataEntity ed WHERE ed.timestamp >= :fromTime AND ed.timestamp <= :toTime ORDER BY ed.timestamp ASC")
    org.springframework.data.domain.Page<EthDemeanDiffOCMasterDataEntity> findByTimestampBetween(
        @Param("fromTime") Instant fromTime, 
        @Param("toTime") Instant toTime,
        org.springframework.data.domain.Pageable pageable);
}