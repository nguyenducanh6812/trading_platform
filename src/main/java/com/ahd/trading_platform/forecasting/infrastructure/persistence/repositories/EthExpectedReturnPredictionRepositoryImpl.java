package com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories;

import com.ahd.trading_platform.forecasting.domain.valueobjects.ExpectedReturnPrediction;
import com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.EthExpectedReturnPredictionEntity;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * ETH-specific implementation of expected return prediction repository
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class EthExpectedReturnPredictionRepositoryImpl implements AssetSpecificPredictionRepository {
    
    private final EthExpectedReturnPredictionJpaRepository jpaRepository;
    
    @Override
    public ExpectedReturnPrediction save(ExpectedReturnPrediction prediction) {
        log.debug("Saving ETH expected return prediction for date {} with model version {}", 
            prediction.forecastDate(), prediction.modelVersion());
        
        EthExpectedReturnPredictionEntity entity = toEntity(prediction);
        EthExpectedReturnPredictionEntity saved = jpaRepository.save(entity);
        
        log.debug("Successfully saved ETH prediction with ID {}", saved.getId());
        return toDomain(saved);
    }
    
    @Override
    public ExpectedReturnPrediction upsert(ExpectedReturnPrediction prediction) {
        log.debug("Upserting ETH expected return prediction for date {} with model version {}", 
            prediction.forecastDate(), prediction.modelVersion());
        
        // Check if prediction already exists for this date and model version
        Optional<EthExpectedReturnPredictionEntity> existingEntity = 
            jpaRepository.findByForecastDateAndModelVersion(prediction.forecastDate(), prediction.modelVersion());
        
        EthExpectedReturnPredictionEntity entityToSave;
        
        if (existingEntity.isPresent()) {
            // Update existing prediction
            log.debug("Updating existing ETH prediction for date {} with model version {}", 
                prediction.forecastDate(), prediction.modelVersion());
            entityToSave = existingEntity.get();
            
            // Update all fields with new values
            entityToSave.setExecutionId(prediction.executionId());
            entityToSave.setExpectedReturn(prediction.expectedReturn());
            entityToSave.setConfidenceLevel(prediction.confidenceLevel());
            entityToSave.setPredictionStatus(prediction.predictionStatus());
            entityToSave.setSummary(prediction.summary());
            entityToSave.setDataPointsUsed(prediction.dataPointsUsed());
            entityToSave.setArOrder(prediction.arOrder());
            entityToSave.setMeanSquaredError(prediction.meanSquaredError());
            entityToSave.setStandardError(prediction.standardError());
            entityToSave.setExecutionTimeMs(prediction.executionTimeMs());
            entityToSave.setDataRangeStart(prediction.dataRangeStart());
            entityToSave.setDataRangeEnd(prediction.dataRangeEnd());
            entityToSave.setHasSufficientQuality(prediction.hasSufficientQuality());
            entityToSave.setErrorMessage(prediction.errorMessage());
            entityToSave.setPredictDiffOC(prediction.predictDiffOC());
            entityToSave.setPredictOC(prediction.predictOC());
            // Keep original createdAt, update updatedAt automatically via @LastModifiedDate
        } else {
            // Create new prediction
            log.debug("Creating new ETH prediction for date {} with model version {}", 
                prediction.forecastDate(), prediction.modelVersion());
            entityToSave = toEntity(prediction);
        }
        
        try {
            EthExpectedReturnPredictionEntity saved = jpaRepository.save(entityToSave);
            log.debug("Successfully upserted ETH prediction with ID {}", saved.getId());
            return toDomain(saved);
        } catch (Exception e) {
            log.error("Failed to upsert ETH prediction for date {} with model version {}: {}", 
                prediction.forecastDate(), prediction.modelVersion(), e.getMessage(), e);
            throw new RuntimeException("Failed to upsert ETH prediction: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<ExpectedReturnPrediction> saveAll(List<ExpectedReturnPrediction> predictions) {
        log.debug("Saving {} ETH expected return predictions", predictions.size());
        
        List<EthExpectedReturnPredictionEntity> entities = predictions.stream()
            .map(this::toEntity)
            .toList();
        
        List<EthExpectedReturnPredictionEntity> saved = jpaRepository.saveAll(entities);
        log.debug("Successfully saved {} ETH predictions", saved.size());
        
        return saved.stream()
            .map(this::toDomain)
            .toList();
    }
    
    @Override
    public List<ExpectedReturnPrediction> findByDateRange(Instant startDate, Instant endDate) {
        List<EthExpectedReturnPredictionEntity> entities = 
            jpaRepository.findByForecastDateBetween(startDate, endDate);
        
        return entities.stream()
            .map(this::toDomain)
            .toList();
    }
    
    @Override
    public List<ExpectedReturnPrediction> findByModelVersion(String modelVersion) {
        List<EthExpectedReturnPredictionEntity> entities = 
            jpaRepository.findByModelVersion(modelVersion);
        
        return entities.stream()
            .map(this::toDomain)
            .toList();
    }
    
    @Override
    public Optional<ExpectedReturnPrediction> findByDateAndModelVersion(Instant forecastDate, String modelVersion) {
        return jpaRepository.findByForecastDateAndModelVersion(forecastDate, modelVersion)
            .map(this::toDomain);
    }
    
    @Override
    public Optional<ExpectedReturnPrediction> findLatestByModelVersion(String modelVersion) {
        return jpaRepository.findLatestByModelVersion(modelVersion)
            .map(this::toDomain);
    }
    
    @Override
    public List<ExpectedReturnPrediction> findSuccessfulPredictions() {
        List<EthExpectedReturnPredictionEntity> entities = 
            jpaRepository.findSuccessfulPredictions();
        
        return entities.stream()
            .map(this::toDomain)
            .toList();
    }
    
    @Override
    public List<ExpectedReturnPrediction> findByExecutionId(String executionId) {
        List<EthExpectedReturnPredictionEntity> entities = 
            jpaRepository.findByExecutionId(executionId);
        
        return entities.stream()
            .map(this::toDomain)
            .toList();
    }
    
    @Override
    public long countByModelVersion(String modelVersion) {
        return jpaRepository.countByModelVersion(modelVersion);
    }
    
    @Override
    public boolean existsByDateAndModelVersion(Instant forecastDate, String modelVersion) {
        return jpaRepository.existsByForecastDateAndModelVersion(forecastDate, modelVersion);
    }
    
    @Override
    public void deleteOlderThan(Instant cutoffDate) {
        jpaRepository.deleteByCreatedAtBefore(cutoffDate);
        log.info("Deleted ETH predictions older than {}", cutoffDate);
    }
    
    @Override
    public String getAssetSymbol() {
        return "ETH";
    }
    
    /**
     * Converts domain object to entity
     */
    private EthExpectedReturnPredictionEntity toEntity(ExpectedReturnPrediction prediction) {
        EthExpectedReturnPredictionEntity entity = new EthExpectedReturnPredictionEntity();
        entity.setExecutionId(prediction.executionId());
        entity.setForecastDate(prediction.forecastDate());
        entity.setExpectedReturn(prediction.expectedReturn());
        entity.setConfidenceLevel(prediction.confidenceLevel());
        entity.setModelVersion(prediction.modelVersion());
        entity.setPredictionStatus(prediction.predictionStatus());
        entity.setSummary(prediction.summary());
        entity.setDataPointsUsed(prediction.dataPointsUsed());
        entity.setArOrder(prediction.arOrder());
        entity.setMeanSquaredError(prediction.meanSquaredError());
        entity.setStandardError(prediction.standardError());
        entity.setExecutionTimeMs(prediction.executionTimeMs());
        entity.setDataRangeStart(prediction.dataRangeStart());
        entity.setDataRangeEnd(prediction.dataRangeEnd());
        entity.setHasSufficientQuality(prediction.hasSufficientQuality());
        entity.setErrorMessage(prediction.errorMessage());
        entity.setPredictDiffOC(prediction.predictDiffOC());
        entity.setPredictOC(prediction.predictOC());
        return entity;
    }
    
    /**
     * Converts entity to domain object
     */
    private ExpectedReturnPrediction toDomain(EthExpectedReturnPredictionEntity entity) {
        return new ExpectedReturnPrediction(
            entity.getExecutionId(),
            TradingInstrument.ETH,
            entity.getForecastDate(),
            entity.getExpectedReturn(),
            entity.getConfidenceLevel(),
            entity.getModelVersion(),
            entity.getPredictionStatus(),
            entity.getSummary(),
            entity.getDataPointsUsed(),
            entity.getArOrder(),
            entity.getMeanSquaredError(),
            entity.getStandardError(),
            entity.getExecutionTimeMs(),
            entity.getDataRangeStart(),
            entity.getDataRangeEnd(),
            entity.getHasSufficientQuality(),
            entity.getErrorMessage(),
            entity.getPredictDiffOC(),
            entity.getPredictOC(),
            entity.getCreatedAt()
        );
    }
}