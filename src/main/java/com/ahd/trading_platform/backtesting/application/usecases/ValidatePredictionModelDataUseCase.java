package com.ahd.trading_platform.backtesting.application.usecases;

import com.ahd.trading_platform.backtesting.domain.valueobjects.BacktestPeriod;
import com.ahd.trading_platform.backtesting.domain.valueobjects.InstrumentPair;
import com.ahd.trading_platform.backtesting.domain.valueobjects.PredictionModelVersion;
import com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories.AssetSpecificPredictionRepository;
import com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories.AssetSpecificPredictionRepositoryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Use case for validating that prediction model has sufficient expected return data
 * for both instruments in the pair across the specified backtest period.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValidatePredictionModelDataUseCase {
    
    private final AssetSpecificPredictionRepositoryFactory predictionRepositoryFactory;
    
    /**
     * Validates that prediction model has sufficient data for both instruments
     * across the backtest period.
     */
    public PredictionModelValidationResult execute(
            InstrumentPair instrumentPair,
            BacktestPeriod backtestPeriod,
            PredictionModelVersion modelVersion) {
        
        log.info("Validating prediction model {} for instrument pair {} over period {} to {}", 
            modelVersion.version(), instrumentPair, 
            backtestPeriod.getStartLocalDate(), backtestPeriod.getEndLocalDate());
        
        try {
            // Get prediction repositories for both instruments
            AssetSpecificPredictionRepository firstInstrumentRepo = 
                predictionRepositoryFactory.getRepository(instrumentPair.firstInstrument());
            AssetSpecificPredictionRepository secondInstrumentRepo = 
                predictionRepositoryFactory.getRepository(instrumentPair.secondInstrument());
            
            // Check if prediction data exists for the backtest period and model version
            var firstInstrumentPredictions = firstInstrumentRepo.findByDateRange(
                backtestPeriod.startDate(), backtestPeriod.endDate()
            ).stream()
            .filter(pred -> modelVersion.version().equals(pred.modelVersion()))
            .count();
            
            var secondInstrumentPredictions = secondInstrumentRepo.findByDateRange(
                backtestPeriod.startDate(), backtestPeriod.endDate()
            ).stream()
            .filter(pred -> modelVersion.version().equals(pred.modelVersion()))
            .count();
            
            boolean hasFirstInstrumentPredictions = firstInstrumentPredictions > 0;
            boolean hasSecondInstrumentPredictions = secondInstrumentPredictions > 0;
            
            if (hasFirstInstrumentPredictions && hasSecondInstrumentPredictions) {
                String message = String.format(
                    "Prediction data validation successful. Found %d predictions for %s and %d predictions for %s with model version %s", 
                    firstInstrumentPredictions,
                    instrumentPair.firstInstrument().getCode(),
                    secondInstrumentPredictions,
                    instrumentPair.secondInstrument().getCode(),
                    modelVersion.version()
                );
                log.info("Prediction data validation successful for pair {} with model {}", instrumentPair, modelVersion.version());
                return PredictionModelValidationResult.success(message);
            } else {
                String failureReason = buildFailureReasonForPredictions(
                    instrumentPair, hasFirstInstrumentPredictions, hasSecondInstrumentPredictions, 
                    firstInstrumentPredictions, secondInstrumentPredictions, modelVersion
                );
                log.warn("Prediction data validation failed for pair {} with model {}: {}", 
                    instrumentPair, modelVersion.version(), failureReason);
                return PredictionModelValidationResult.failure(failureReason);
            }
            
        } catch (Exception e) {
            String errorMessage = "Failed to validate prediction model data: " + e.getMessage();
            log.error("Error validating prediction model {} for pair {}: {}", 
                modelVersion.version(), instrumentPair, errorMessage, e);
            return PredictionModelValidationResult.failure(errorMessage);
        }
    }
    
    private String buildFailureReasonForPredictions(InstrumentPair instrumentPair,
                                                  boolean hasFirstPredictions, boolean hasSecondPredictions,
                                                  long firstCount, long secondCount,
                                                  PredictionModelVersion modelVersion) {
        StringBuilder reason = new StringBuilder();
        
        if (!hasFirstPredictions) {
            reason.append(String.format("No prediction data found for %s with model version %s in the backtest period",
                instrumentPair.firstInstrument().getCode(), modelVersion.version()));
        }
        
        if (!hasSecondPredictions) {
            if (!reason.isEmpty()) {
                reason.append("; ");
            }
            reason.append(String.format("No prediction data found for %s with model version %s in the backtest period",
                instrumentPair.secondInstrument().getCode(), modelVersion.version()));
        }
        
        return reason.toString();
    }
    
    /**
     * Result of prediction model validation
     */
    public record PredictionModelValidationResult(
        boolean isValid,
        String message
    ) {
        
        public static PredictionModelValidationResult success(String message) {
            return new PredictionModelValidationResult(true, message);
        }
        
        public static PredictionModelValidationResult failure(String message) {
            return new PredictionModelValidationResult(false, message);
        }
    }
}