package com.ahd.trading_platform.forecasting.infrastructure.repositories;

import com.ahd.trading_platform.forecasting.domain.entities.ARIMAModel;
import com.ahd.trading_platform.forecasting.domain.repositories.ARIMAModelRepository;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Loads ARIMA master data from JSON files and maintains in-memory cache.
 * Provides high-performance access to ARIMA models for forecasting operations.
 */
@Component
@Slf4j
public class ARIMAMasterDataLoader implements ARIMAModelRepository {
    
    private final ObjectMapper objectMapper;
    private final Map<ModelKey, ARIMAModel> modelCache = new ConcurrentHashMap<>();
    
    public ARIMAMasterDataLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        log.info("=== ARIMAMasterDataLoader component initialized ===");
    }
    
    /**
     * Loads ARIMA models on application startup using @PostConstruct
     */
    @PostConstruct
    public void initializeModels() {
        log.info("=== Loading ARIMA master data via @PostConstruct ===");
        try {
            reloadAllModels();
            log.info("=== ARIMA master data loading completed ===");
        } catch (Exception e) {
            log.error("=== ARIMA master data loading failed ===", e);
            throw e;
        }
    }
    
    @Override
    public Optional<ARIMAModel> findActiveModelByInstrument(TradingInstrument instrument) {
        // Find the first (any) model for this instrument - this is for backward compatibility
        // In a real system, you might want to implement a proper "active" model concept
        Optional<ARIMAModel> model = modelCache.entrySet().stream()
            .filter(entry -> entry.getKey().instrument == instrument)
            .map(Map.Entry::getValue)
            .findFirst();
            
        if (model.isPresent()) {
            log.debug("Retrieved ARIMA model for {} from cache", instrument.getCode());
        } else {
            log.warn("ARIMA model not found for instrument: {}", instrument.getCode());
        }
        return model;
    }
    
    @Override
    public ARIMAModel save(ARIMAModel model) {
        ModelKey key = new ModelKey(model.getInstrument(), model.getModelVersion());
        modelCache.put(key, model);
        log.debug("Cached ARIMA model for {} version {}", model.getInstrument().getCode(), model.getModelVersion());
        return model;
    }
    
    @Override
    public boolean existsActiveModelForInstrument(TradingInstrument instrument) {
        return modelCache.keySet().stream()
            .anyMatch(key -> key.instrument == instrument);
    }
    
    @Override
    public List<ARIMAModel> findAllActiveModels() {
        return modelCache.values().stream().toList();
    }
    
    @Override
    public List<ARIMAModel> findAllModelsByInstrument(TradingInstrument instrument) {
        return modelCache.entrySet().stream()
            .filter(entry -> entry.getKey().instrument == instrument)
            .map(Map.Entry::getValue)
            .toList();
    }
    
    @Override
    public void delete(ARIMAModel model) {
        ModelKey key = new ModelKey(model.getInstrument(), model.getModelVersion());
        modelCache.remove(key);
        log.debug("Removed ARIMA model for {} version {} from cache", model.getInstrument().getCode(), model.getModelVersion());
    }
    
    @Override
    public Optional<ARIMAModel> findById(Long id) {
        // Since we're using in-memory cache, we don't have IDs
        throw new UnsupportedOperationException("findById not supported for in-memory ARIMA models");
    }
    
    @Override
    public List<ARIMAModel> findLatestModelForEachInstrument() {
        return findAllActiveModels();
    }
    
    @Override
    public Optional<ARIMAModel> findByInstrumentAndVersion(TradingInstrument instrument, String modelVersion) {
        ModelKey key = new ModelKey(instrument, modelVersion);
        ARIMAModel model = modelCache.get(key);
        
        if (model != null) {
            log.debug("Retrieved ARIMA model for {} with version {} from cache", instrument.getCode(), modelVersion);
            return Optional.of(model);
        } else {
            log.warn("ARIMA model not found for instrument: {} with version: {}", instrument.getCode(), modelVersion);
            
            // Log available versions for this instrument for debugging
            List<String> availableVersions = modelCache.keySet().stream()
                .filter(k -> k.instrument == instrument)
                .map(k -> k.modelVersion)
                .toList();
            if (!availableVersions.isEmpty()) {
                log.debug("Available versions for {}: {}", instrument.getCode(), availableVersions);
            }
        }
        return Optional.empty();
    }
    
    public void reloadAllModels() {
        try {
            modelCache.clear();
            
            // Dynamically discover and load all ARIMA model files
            loadAllAvailableModels();
            
            log.info("Successfully loaded {} ARIMA models", modelCache.size());
            logLoadedModels();
            
        } catch (Exception e) {
            log.error("Failed to reload ARIMA models: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to reload ARIMA models", e);
        }
    }
    
    /**
     * Discovers and loads all available ARIMA model files from the classpath.
     * Supports both legacy format (btc_arima_model.json) and date-based format (btc_arima_model_20250904.json).
     */
    private void loadAllAvailableModels() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        
        // Pattern to match ARIMA model files: {instrument}_arima_model[_yyyyMMdd].json
        Resource[] resources = resolver.getResources("classpath:forecasting/*_arima_model*.json");
        
        Pattern filePattern = Pattern.compile("(\\w+)_arima_model(?:_(\\d{8}))?\\.json");
        
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null) continue;
            
            Matcher matcher = filePattern.matcher(filename);
            if (matcher.matches()) {
                String instrumentCode = matcher.group(1).toUpperCase(); // btc -> BTC, eth -> ETH
                String dateVersion = matcher.group(2); // 20250904 or null for legacy files
                
                // Convert instrument code to TradingInstrument
                TradingInstrument instrument = parseInstrument(instrumentCode);
                if (instrument != null) {
                    loadModelFromResource(instrument, resource, dateVersion);
                } else {
                    log.warn("Unknown instrument code in filename: {}", filename);
                }
            } else {
                log.debug("Skipping file that doesn't match ARIMA model pattern: {}", filename);
            }
        }
    }
    
    /**
     * Loads a single ARIMA model from a resource
     */
    private void loadModelFromResource(TradingInstrument instrument, Resource resource, String dateVersion) {
        try {
            log.debug("Loading ARIMA model for {} from {}", instrument.getCode(), resource.getFilename());
            
            // Load JSON data from resource
            Map<String, Object> masterData = loadJsonData(resource.getInputStream());
            
            // Create ARIMA model from master data
            ARIMAModel model = createModelFromMasterData(instrument, masterData, dateVersion);
            
            // Cache the model with composite key (instrument + version)
            ModelKey key = new ModelKey(instrument, model.getModelVersion());
            modelCache.put(key, model);
            
            log.info("Loaded ARIMA model for {}: {} coefficients, p={}, version={}", 
                instrument.getCode(), model.getCoefficients().size(), model.getPOrder(), model.getModelVersion());
                
        } catch (IOException e) {
            log.error("Failed to load ARIMA model for {} from {}: {}", instrument.getCode(), resource.getFilename(), e.getMessage());
            throw new RuntimeException("Failed to load ARIMA model for " + instrument.getCode(), e);
        }
    }
    
    /**
     * Parses instrument code string to TradingInstrument enum
     */
    private TradingInstrument parseInstrument(String instrumentCode) {
        try {
            return TradingInstrument.valueOf(instrumentCode);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Logs all loaded models for monitoring
     */
    private void logLoadedModels() {
        if (modelCache.isEmpty()) {
            log.warn("No ARIMA models were loaded!");
            return;
        }
        
        Map<TradingInstrument, List<String>> modelsByInstrument = modelCache.keySet().stream()
            .collect(Collectors.groupingBy(
                key -> key.instrument,
                Collectors.mapping(key -> key.modelVersion, Collectors.toList())
            ));
            
        modelsByInstrument.forEach((instrument, versions) -> {
            log.info("Loaded {} version(s) for {}: {}", versions.size(), instrument.getCode(), versions);
        });
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
    
    private ARIMAModel createModelFromMasterData(TradingInstrument instrument, Map<String, Object> masterData, String dateVersion) {
        // Generate version string based on date or use default for legacy files
        String version = generateVersionString(instrument, dateVersion);
        
        // Use appropriate factory method based on instrument with dynamic version
        return switch (instrument) {
            case BTC -> ARIMAModel.forBTC(masterData, version);
            case ETH -> ARIMAModel.forETH(masterData, version);
            default -> throw new IllegalArgumentException("Unsupported instrument for ARIMA model: " + instrument.getCode());
        };
    }
    
    /**
     * Generates a version string from the date version extracted from filename.
     * Uses just the date for consistency with user expectations.
     */
    private String generateVersionString(TradingInstrument instrument, String dateVersion) {
        if (dateVersion != null) {
            // For date-based files: use just the date (20250904)
            // This matches what users expect when calling findByInstrumentAndVersion
            return dateVersion;
        } else {
            // For legacy files without date: use "legacy" 
            return "legacy";
        }
    }
    
    /**
     * Gets cache statistics for monitoring
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalModels", modelCache.size());
        stats.put("instruments", modelCache.keySet().stream()
            .map(key -> key.instrument.getCode())
            .distinct()
            .toList());
        
        // Add model details
        modelCache.forEach((key, model) -> {
            Map<String, Object> modelStats = new HashMap<>();
            modelStats.put("coefficients", model.getCoefficients().size());
            modelStats.put("pOrder", model.getPOrder());
            modelStats.put("version", model.getModelVersion());
            modelStats.put("createdAt", model.getCreatedAt());
            modelStats.put("lastUsed", model.getLastUsed());
            stats.put(key.instrument.getCode().toLowerCase() + "_v" + key.modelVersion, modelStats);
        });
        
        return stats;
    }
    
    /**
     * Composite key for caching models by instrument and version
     */
    private static final class ModelKey {
        final TradingInstrument instrument;
        final String modelVersion;
        
        ModelKey(TradingInstrument instrument, String modelVersion) {
            this.instrument = instrument;
            this.modelVersion = modelVersion;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ModelKey modelKey = (ModelKey) obj;
            return instrument == modelKey.instrument && Objects.equals(modelVersion, modelKey.modelVersion);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(instrument, modelVersion);
        }
        
        @Override
        public String toString() {
            return instrument.getCode() + ":" + modelVersion;
        }
    }
}