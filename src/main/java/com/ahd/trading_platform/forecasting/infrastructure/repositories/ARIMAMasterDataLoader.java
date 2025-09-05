package com.ahd.trading_platform.forecasting.infrastructure.repositories;

import com.ahd.trading_platform.forecasting.domain.entities.ARIMAModel;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads ARIMA master data from JSON files and maintains in-memory cache.
 * Provides high-performance access to ARIMA models for forecasting operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ARIMAMasterDataLoader implements ARIMAModelRepository {
    
    private final ObjectMapper objectMapper;
    private final Map<TradingInstrument, ARIMAModel> modelCache = new ConcurrentHashMap<>();
    
    /**
     * Loads ARIMA models on application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void loadModelsOnStartup() {
        log.info("Loading ARIMA master data on application startup");
        reloadAllModels();
    }
    
    @Override
    public Optional<ARIMAModel> findByInstrument(TradingInstrument instrument) {
        ARIMAModel model = modelCache.get(instrument);
        if (model != null) {
            log.debug("Retrieved ARIMA model for {} from cache", instrument.getCode());
        } else {
            log.warn("ARIMA model not found for instrument: {}", instrument.getCode());
        }
        return Optional.ofNullable(model);
    }
    
    @Override
    public ARIMAModel save(ARIMAModel model) {
        modelCache.put(model.getInstrument(), model);
        log.debug("Cached ARIMA model for {}", model.getInstrument().getCode());
        return model;
    }
    
    @Override
    public boolean existsByInstrument(TradingInstrument instrument) {
        return modelCache.containsKey(instrument);
    }
    
    @Override
    public void reloadAllModels() {
        try {
            modelCache.clear();
            
            // Load BTC model
            loadModelForInstrument(TradingInstrument.BTC, "btc_arima_model_20250904.json");
            
            // Load ETH model
            loadModelForInstrument(TradingInstrument.ETH, "eth_arima_model_20250904.json");
            
            log.info("Successfully loaded {} ARIMA models", modelCache.size());
            
        } catch (Exception e) {
            log.error("Failed to reload ARIMA models: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to reload ARIMA models", e);
        }
    }
    
    private void loadModelForInstrument(TradingInstrument instrument, String filename) {
        try {
            log.debug("Loading ARIMA model for {} from {}", instrument.getCode(), filename);
            
            // Load JSON data from classpath
            ClassPathResource resource = new ClassPathResource("forecasting/" + filename);
            Map<String, Object> masterData = loadJsonData(resource.getInputStream());
            
            // Create ARIMA model from master data
            ARIMAModel model = createModelFromMasterData(instrument, masterData);
            
            // Cache the model
            modelCache.put(instrument, model);
            
            log.info("Loaded ARIMA model for {}: {} coefficients, p={}, version={}", 
                instrument.getCode(), model.getCoefficients().size(), model.getPOrder(), model.getModelVersion());
                
        } catch (IOException e) {
            log.error("Failed to load ARIMA model for {} from {}: {}", instrument.getCode(), filename, e.getMessage());
            throw new RuntimeException("Failed to load ARIMA model for " + instrument.getCode(), e);
        }
    }
    
    private Map<String, Object> loadJsonData(InputStream inputStream) throws IOException {
        TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};
        Map<String, Object> data = objectMapper.readValue(inputStream, typeRef);
        
        // Validate required fields
        validateMasterData(data);
        
        return data;
    }
    
    private void validateMasterData(Map<String, Object> data) {
        // Check for required fields
        if (!data.containsKey("mean_diff_oc")) {
            throw new IllegalArgumentException("Master data missing required field: mean_diff_oc");
        }
        if (!data.containsKey("sigma2")) {
            throw new IllegalArgumentException("Master data missing required field: sigma2");
        }
        if (!data.containsKey("p")) {
            throw new IllegalArgumentException("Master data missing required field: p");
        }
        
        // Check for AR coefficients
        long arCoeffCount = data.keySet().stream()
            .filter(key -> key.startsWith("ar.L"))
            .count();
            
        if (arCoeffCount == 0) {
            throw new IllegalArgumentException("Master data missing AR coefficients (ar.L1, ar.L2, etc.)");
        }
        
        // Validate p parameter matches coefficient count
        int pOrder = ((Number) data.get("p")).intValue();
        if (arCoeffCount != pOrder) {
            throw new IllegalArgumentException(
                String.format("P order (%d) does not match AR coefficient count (%d)", pOrder, arCoeffCount));
        }
    }
    
    private ARIMAModel createModelFromMasterData(TradingInstrument instrument, Map<String, Object> masterData) {
        // Use appropriate factory method based on instrument
        return switch (instrument) {
            case BTC -> ARIMAModel.forBTC(masterData);
            case ETH -> ARIMAModel.forETH(masterData);
            default -> throw new IllegalArgumentException("Unsupported instrument for ARIMA model: " + instrument.getCode());
        };
    }
    
    /**
     * Gets cache statistics for monitoring
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalModels", modelCache.size());
        stats.put("instruments", modelCache.keySet().stream()
            .map(TradingInstrument::getCode)
            .toList());
        
        // Add model details
        modelCache.forEach((instrument, model) -> {
            Map<String, Object> modelStats = new HashMap<>();
            modelStats.put("coefficients", model.getCoefficients().size());
            modelStats.put("pOrder", model.getPOrder());
            modelStats.put("version", model.getModelVersion());
            modelStats.put("createdAt", model.getCreatedAt());
            modelStats.put("lastUsed", model.getLastUsed());
            stats.put(instrument.getCode().toLowerCase() + "Model", modelStats);
        });
        
        return stats;
    }
}