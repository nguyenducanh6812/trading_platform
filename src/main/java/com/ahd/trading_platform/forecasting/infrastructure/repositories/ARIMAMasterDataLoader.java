package com.ahd.trading_platform.forecasting.infrastructure.repositories;

import com.ahd.trading_platform.forecasting.domain.entities.ARIMAModel;
import com.ahd.trading_platform.forecasting.domain.repositories.ARIMAModelRepository;
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
 * Supports all trading symbols across all markets (SPOT, LINEAR, INVERSE, OPTION).
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
    public Optional<ARIMAModel> findActiveModelBySymbol(String symbol) {
        // Find the first (any) model for this symbol - this is for backward compatibility
        // In a real system, you might want to implement a proper "active" model concept
        Optional<ARIMAModel> model = modelCache.entrySet().stream()
            .filter(entry -> entry.getKey().symbol.equals(symbol))
            .map(Map.Entry::getValue)
            .findFirst();

        if (model.isPresent()) {
            log.debug("Retrieved ARIMA model for {} from cache", symbol);
        } else {
            log.warn("ARIMA model not found for symbol: {}", symbol);
        }
        return model;
    }

    @Override
    public ARIMAModel save(ARIMAModel model) {
        ModelKey key = new ModelKey(model.getSymbol(), model.getModelVersion());
        modelCache.put(key, model);
        log.debug("Cached ARIMA model for {} version {}", model.getSymbol(), model.getModelVersion());
        return model;
    }

    @Override
    public boolean existsActiveModelForSymbol(String symbol) {
        return modelCache.keySet().stream()
            .anyMatch(key -> key.symbol.equals(symbol));
    }

    @Override
    public List<ARIMAModel> findAllActiveModels() {
        return modelCache.values().stream().toList();
    }

    @Override
    public List<ARIMAModel> findAllModelsBySymbol(String symbol) {
        return modelCache.entrySet().stream()
            .filter(entry -> entry.getKey().symbol.equals(symbol))
            .map(Map.Entry::getValue)
            .toList();
    }

    @Override
    public void delete(ARIMAModel model) {
        ModelKey key = new ModelKey(model.getSymbol(), model.getModelVersion());
        modelCache.remove(key);
        log.debug("Removed ARIMA model for {} version {} from cache", model.getSymbol(), model.getModelVersion());
    }

    @Override
    public Optional<ARIMAModel> findById(Long id) {
        // Since we're using in-memory cache, we don't have IDs
        throw new UnsupportedOperationException("findById not supported for in-memory ARIMA models");
    }

    @Override
    public List<ARIMAModel> findLatestModelForEachSymbol() {
        return findAllActiveModels();
    }

    @Override
    public Optional<ARIMAModel> findBySymbolAndVersion(String symbol, String modelVersion) {
        ModelKey key = new ModelKey(symbol, modelVersion);
        ARIMAModel model = modelCache.get(key);

        if (model != null) {
            log.debug("Retrieved ARIMA model for {} with version {} from cache", symbol, modelVersion);
            return Optional.of(model);
        } else {
            log.warn("ARIMA model not found for symbol: {} with version: {}", symbol, modelVersion);

            // Log available versions for this symbol for debugging
            List<String> availableVersions = modelCache.keySet().stream()
                .filter(k -> k.symbol.equals(symbol))
                .map(k -> k.modelVersion)
                .toList();
            if (!availableVersions.isEmpty()) {
                log.debug("Available versions for {}: {}", symbol, availableVersions);
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
     * Symbol names are extracted from filenames (e.g., btc_arima_model.json -> BTC symbol).
     */
    private void loadAllAvailableModels() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        // Pattern to match ARIMA model files: {symbol}_arima_model[_yyyyMMdd].json
        Resource[] resources = resolver.getResources("classpath:forecasting/*_arima_model*.json");

        Pattern filePattern = Pattern.compile("(\\w+)_arima_model(?:_(\\d{8}))?\\.json");

        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null) continue;

            Matcher matcher = filePattern.matcher(filename);
            if (matcher.matches()) {
                String symbolCode = matcher.group(1).toUpperCase(); // btc -> BTC, eth -> ETH
                String dateVersion = matcher.group(2); // 20250904 or null for legacy files

                loadModelFromResource(symbolCode, resource, dateVersion);
            } else {
                log.debug("Skipping file that doesn't match ARIMA model pattern: {}", filename);
            }
        }
    }

    /**
     * Loads a single ARIMA model from a resource
     */
    private void loadModelFromResource(String symbol, Resource resource, String dateVersion) {
        try {
            log.debug("Loading ARIMA model for {} from {}", symbol, resource.getFilename());

            // Load JSON data from resource
            Map<String, Object> masterData = loadJsonData(resource.getInputStream());

            // Create ARIMA model from master data
            ARIMAModel model = createModelFromMasterData(symbol, masterData, dateVersion);

            // Cache the model with composite key (symbol + version)
            ModelKey key = new ModelKey(symbol, model.getModelVersion());
            modelCache.put(key, model);

            log.info("Loaded ARIMA model for {}: {} coefficients, p={}, version={}",
                symbol, model.getCoefficients().size(), model.getPOrder(), model.getModelVersion());

        } catch (IOException e) {
            log.error("Failed to load ARIMA model for {} from {}: {}", symbol, resource.getFilename(), e.getMessage());
            throw new RuntimeException("Failed to load ARIMA model for " + symbol, e);
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

        Map<String, List<String>> modelsBySymbol = modelCache.keySet().stream()
            .collect(Collectors.groupingBy(
                key -> key.symbol,
                Collectors.mapping(key -> key.modelVersion, Collectors.toList())
            ));

        modelsBySymbol.forEach((symbol, versions) -> {
            log.info("Loaded {} version(s) for {}: {}", versions.size(), symbol, versions);
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
    
    private ARIMAModel createModelFromMasterData(String symbol, Map<String, Object> masterData, String dateVersion) {
        // Generate version string based on date or use default for legacy files
        String version = generateVersionString(dateVersion);

        // Use the general factory method for any symbol
        return ARIMAModel.fromMasterData(symbol, masterData, version);
    }

    /**
     * Generates a version string from the date version extracted from filename.
     * Uses just the date for consistency with user expectations.
     */
    private String generateVersionString(String dateVersion) {
        if (dateVersion != null) {
            // For date-based files: use just the date (20250904)
            // This matches what users expect when calling findBySymbolAndVersion
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
        stats.put("symbols", modelCache.keySet().stream()
            .map(key -> key.symbol)
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
            stats.put(key.symbol.toLowerCase() + "_v" + key.modelVersion, modelStats);
        });

        return stats;
    }

    /**
     * Composite key for caching models by symbol and version
     */
    private static final class ModelKey {
        final String symbol;
        final String modelVersion;

        ModelKey(String symbol, String modelVersion) {
            this.symbol = symbol;
            this.modelVersion = modelVersion;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ModelKey modelKey = (ModelKey) obj;
            return Objects.equals(symbol, modelKey.symbol) && Objects.equals(modelVersion, modelKey.modelVersion);
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, modelVersion);
        }

        @Override
        public String toString() {
            return symbol + ":" + modelVersion;
        }
    }
}