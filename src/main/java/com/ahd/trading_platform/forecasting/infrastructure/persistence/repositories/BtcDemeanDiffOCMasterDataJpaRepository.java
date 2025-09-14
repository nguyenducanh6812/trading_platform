package com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories;

import com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.BtcDemeanDiffOCMasterDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for BTC-specific Demean_Diff_OC master data.
 * Provides optimized queries for Bitcoin master data operations.
 */
@Repository
public interface BtcDemeanDiffOCMasterDataJpaRepository extends JpaRepository<BtcDemeanDiffOCMasterDataEntity, Long> {
    
    /**
     * Finds master data within a time range, ordered by timestamp
     */
    @Query("SELECT bd FROM BtcDemeanDiffOCMasterDataEntity bd WHERE bd.timestamp >= :fromTime AND bd.timestamp <= :toTime ORDER BY bd.timestamp ASC")
    List<BtcDemeanDiffOCMasterDataEntity> findByTimestampBetweenOrderByTimestampAsc(
        @Param("fromTime") Instant fromTime, 
        @Param("toTime") Instant toTime);
    
    /**
     * Finds the latest master data point
     */
    Optional<BtcDemeanDiffOCMasterDataEntity> findTopByOrderByTimestampDesc();
    
    /**
     * Finds the earliest master data point
     */
    Optional<BtcDemeanDiffOCMasterDataEntity> findTopByOrderByTimestampAsc();
    
    /**
     * Finds master data by specific timestamp
     */
    Optional<BtcDemeanDiffOCMasterDataEntity> findByTimestamp(Instant timestamp);
    
    /**
     * Checks if master data exists for a specific timestamp
     */
    boolean existsByTimestamp(Instant timestamp);
    
    /**
     * Counts data points within a time range
     */
    @Query("SELECT COUNT(bd) FROM BtcDemeanDiffOCMasterDataEntity bd WHERE bd.timestamp >= :fromTime AND bd.timestamp <= :toTime")
    long countByTimestampBetween(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);
    
    /**
     * Finds master data after a specific timestamp (for incremental updates)
     */
    @Query("SELECT bd FROM BtcDemeanDiffOCMasterDataEntity bd WHERE bd.timestamp > :afterTime ORDER BY bd.timestamp ASC")
    List<BtcDemeanDiffOCMasterDataEntity> findByTimestampAfterOrderByTimestampAsc(@Param("afterTime") Instant afterTime);
    
    /**
     * Finds only timestamps within a time range for gap detection
     * This is much more efficient than loading full entities when we only need timestamps
     */
    @Query("SELECT bd.timestamp FROM BtcDemeanDiffOCMasterDataEntity bd WHERE bd.timestamp >= :fromTime AND bd.timestamp <= :toTime ORDER BY bd.timestamp ASC")
    List<Instant> findTimestampsByDateRange(
        @Param("fromTime") Instant fromTime, 
        @Param("toTime") Instant toTime);
    
    /**
     * Finds master data with calculated differences within a time range
     */
    @Query("SELECT bd FROM BtcDemeanDiffOCMasterDataEntity bd WHERE bd.timestamp >= :fromTime AND bd.timestamp <= :toTime AND bd.hasDifferences = true ORDER BY bd.timestamp ASC")
    List<BtcDemeanDiffOCMasterDataEntity> findByTimestampBetweenAndHasDifferencesOrderByTimestampAsc(
        @Param("fromTime") Instant fromTime, 
        @Param("toTime") Instant toTime);
    
    /**
     * Counts master data points with calculated differences within a time range
     */
    @Query("SELECT COUNT(bd) FROM BtcDemeanDiffOCMasterDataEntity bd WHERE bd.timestamp >= :fromTime AND bd.timestamp <= :toTime AND bd.hasDifferences = true")
    long countByTimestampBetweenAndHasDifferences(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);
    
    /**
     * Deletes data points older than specified timestamp
     */
    void deleteByTimestampBefore(Instant timestamp);
    
    /**
     * Gets paginated data for large datasets
     */
    @Query("SELECT bd FROM BtcDemeanDiffOCMasterDataEntity bd WHERE bd.timestamp >= :fromTime AND bd.timestamp <= :toTime ORDER BY bd.timestamp ASC")
    org.springframework.data.domain.Page<BtcDemeanDiffOCMasterDataEntity> findByTimestampBetween(
        @Param("fromTime") Instant fromTime, 
        @Param("toTime") Instant toTime,
        org.springframework.data.domain.Pageable pageable);
}