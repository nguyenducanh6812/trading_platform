package com.ahd.trading_platform.marketdata.application.usecases;

import com.ahd.trading_platform.marketdata.application.dto.MarketDataRequest;
import com.ahd.trading_platform.marketdata.application.dto.MarketDataResponse;
import com.ahd.trading_platform.marketdata.domain.entities.MarketInstrument;
import com.ahd.trading_platform.marketdata.domain.repositories.MarketDataRepository;
import com.ahd.trading_platform.marketdata.domain.services.DataValidationService;
import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
import com.ahd.trading_platform.marketdata.domain.services.ExternalDataClientFactory;
import com.ahd.trading_platform.marketdata.domain.services.ExternalDataClientStrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import com.ahd.trading_platform.marketdata.domain.constants.TradingConstants;

/**
 * Use case for fetching historical market data from external sources.
 * 
 * Business Logic:
 * - Always fetches data from external source (e.g., Bybit) for the requested time range
 * - Does NOT check existing data or perform gap detection
 * - Inserts fetched data if not exists (gracefully handles duplicates)
 * - This is specifically for data ingestion from external APIs, not for querying existing data
 */
@Component
public class FetchHistoricalDataUseCase {
    
    private static final int BATCH_SIZE = TradingConstants.BATCH_SIZE;
    private static final int CHUNK_SIZE_DAYS = TradingConstants.CHUNK_SIZE_DAYS;
    
    private static final Logger logger = LoggerFactory.getLogger(FetchHistoricalDataUseCase.class);
    
    private final ExternalDataClientFactory clientFactory;
    private final MarketDataRepository repository;
    private final DataValidationService validationService;
    
    public FetchHistoricalDataUseCase(
        ExternalDataClientFactory clientFactory,
        MarketDataRepository repository,
        DataValidationService validationService) {
        
        this.clientFactory = Objects.requireNonNull(clientFactory, "Client factory cannot be null");
        this.repository = Objects.requireNonNull(repository, "Repository cannot be null");
        this.validationService = Objects.requireNonNull(validationService, "Validation service cannot be null");
    }
    
    /**
     * Executes the historical data fetch process for multiple instruments
     */
    @Transactional
    public CompletableFuture<MarketDataResponse> execute(MarketDataRequest request, String executionId) {
        Objects.requireNonNull(request, "Request cannot be null");
        Objects.requireNonNull(executionId, "Execution ID cannot be null");
        
        logger.info("Starting historical data fetch for instruments: {} (execution: {})", 
            request.instruments(), executionId);
        
        return CompletableFuture.supplyAsync(() -> {
            Map<String, MarketDataResponse.InstrumentDataSummary> results = new ConcurrentHashMap<>();
            TimeRange timeRange = TimeRange.fromDates(request.fromDate(), request.toDate());
            
            // Process each instrument in parallel
            List<CompletableFuture<Void>> instrumentTasks = request.instruments().stream()
                .map(symbol -> processInstrumentAsync(symbol, timeRange, results, executionId, request.source()))
                .toList();
            
            // Wait for all instruments to complete
            CompletableFuture.allOf(instrumentTasks.toArray(new CompletableFuture[0])).join();
            
            logger.info("Completed historical data fetch (execution: {}). Results: {}", 
                executionId, results.keySet());
            
            return MarketDataResponse.success(results, executionId);
        }).exceptionally(throwable -> {
            logger.error("Failed to fetch historical data (execution: {})", executionId, throwable);
            return MarketDataResponse.failure(
                "Failed to fetch historical data: " + throwable.getMessage(), 
                executionId
            );
        });
    }
    
    private CompletableFuture<Void> processInstrumentAsync(
        String symbol, 
        TimeRange timeRange, 
        Map<String, MarketDataResponse.InstrumentDataSummary> results,
        String executionId,
        String dataSource) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                logger.debug("Processing instrument {} (execution: {})", symbol, executionId);
                
                // Create or retrieve instrument with error handling
                MarketInstrument instrument;
                try {
                    instrument = getOrCreateInstrument(symbol);
                    logger.debug("Retrieved/created instrument {} (execution: {})", symbol, executionId);
                } catch (Exception e) {
                    logger.error("Database error retrieving/creating instrument {} (execution: {})", symbol, executionId, e);
                    results.put(symbol, MarketDataResponse.InstrumentDataSummary.failure(
                        symbol, symbol, "INSTRUMENT_RETRIEVAL_ERROR: " + e.getMessage()));
                    return;
                }
                
                logger.info("Fetching data from external source for {} in range {} to {} (execution: {})",
                    symbol, timeRange.from(), timeRange.to(), executionId);
                
                // Get strategy for the specified data source
                ExternalDataClientStrategy strategy = clientFactory.getStrategy(dataSource);
                
                logger.debug("Using {} data source for symbol {} (execution: {})", 
                    strategy.getDataSource(), symbol, executionId);
                
                // Process data in chunks to avoid memory issues - fetch entire requested range
                AtomicInteger totalProcessed = new AtomicInteger(0);
                AtomicLong earliestTimestamp = new AtomicLong(Long.MAX_VALUE);
                AtomicLong latestTimestamp = new AtomicLong(Long.MIN_VALUE);
                
                boolean success = processBulkDataInChunks(
                    strategy, symbol, timeRange, instrument, executionId, 
                    totalProcessed, earliestTimestamp, latestTimestamp);
                
                if (!success) {
                    results.put(symbol, MarketDataResponse.InstrumentDataSummary.failure(
                        symbol, instrument.getName(), "BULK_PROCESSING_FAILED"));
                    return;
                }
                
                // Final save after all chunks processed
                try {
                    repository.save(instrument);
                    logger.debug("Successfully saved final instrument data for {} (execution: {})", symbol, executionId);
                } catch (Exception e) {
                    logger.error("Database error during final save for {} (execution: {})", symbol, executionId, e);
                    results.put(symbol, MarketDataResponse.InstrumentDataSummary.failure(
                        symbol, instrument.getName(), "DATABASE_SAVE_ERROR: " + e.getMessage()));
                    return;
                }
                
                // Create success summary
                results.put(symbol, MarketDataResponse.InstrumentDataSummary.success(
                    symbol,
                    instrument.getName(),
                    totalProcessed.get(),
                    instrument.getQualityMetrics(),
                    earliestTimestamp.get() != Long.MAX_VALUE ? 
                        Instant.ofEpochMilli(earliestTimestamp.get()) : null,
                    latestTimestamp.get() != Long.MIN_VALUE ? 
                        Instant.ofEpochMilli(latestTimestamp.get()) : null
                ));
                
                logger.info("Successfully processed {} data points for {} in chunks (execution: {})", 
                    totalProcessed.get(), symbol, executionId);
                
            } catch (Exception e) {
                logger.error("Error processing instrument {} (execution: {})", symbol, executionId, e);
                results.put(symbol, MarketDataResponse.InstrumentDataSummary.failure(
                    symbol, symbol, "ERROR: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Processes historical data in chunks to avoid memory issues with large datasets.
     * Splits time range into manageable chunks and processes each batch separately.
     */
    private boolean processBulkDataInChunks(
        ExternalDataClientStrategy strategy,
        String symbol,
        TimeRange timeRange,
        MarketInstrument instrument,
        String executionId,
        AtomicInteger totalProcessed,
        AtomicLong earliestTimestamp,
        AtomicLong latestTimestamp) {
        
        try {
            logger.info("Starting bulk processing for {} from {} to {} (execution: {})",
                symbol, timeRange.from(), timeRange.to(), executionId);
            
            // Split large time ranges into chunks
            TimeRange[] chunks = timeRange.splitIntoDays(CHUNK_SIZE_DAYS);
            
            logger.info("Processing {} time chunks for {} (execution: {})", 
                chunks.length, symbol, executionId);
            
            for (int i = 0; i < chunks.length; i++) {
                TimeRange chunk = chunks[i];
                
                logger.debug("Processing chunk {}/{} for {} from {} to {} (execution: {})",
                    i + 1, chunks.length, symbol, chunk.from(), chunk.to(), executionId);
                
                try {
                    // Fetch data for this chunk with error handling
                    List<OHLCV> chunkData;
                    try {
                        chunkData = strategy.fetchHistoricalData(symbol, chunk);
                    } catch (Exception apiException) {
                        logger.error("External API error in chunk {}/{} for {} (execution: {}): {}",
                            i + 1, chunks.length, symbol, executionId, apiException.getMessage());
                        // Fail fast on API errors for critical data fetching
                        throw new RuntimeException("External API failed for " + symbol + ": " + apiException.getMessage(), apiException);
                    }
                    
                    if (chunkData.isEmpty()) {
                        logger.warn("No data in chunk {}/{} for {} (execution: {})", 
                            i + 1, chunks.length, symbol, executionId);
                        continue;
                    }
                    
                    // Process chunk data in batches
                    boolean chunkSuccess = processBatchesInChunk(
                        chunkData, instrument, symbol, executionId, i + 1, chunks.length,
                        totalProcessed, earliestTimestamp, latestTimestamp);
                    
                    if (!chunkSuccess) {
                        logger.error("Failed to process chunk {}/{} for {} (execution: {})",
                            i + 1, chunks.length, symbol, executionId);
                        return false;
                    }
                    
                    // Force garbage collection to free memory
                    chunkData.clear();
                    
                    // Small delay between chunks to be respectful of external APIs
                    Thread.sleep(TradingConstants.CHUNK_DELAY_MS);
                    
                } catch (Exception e) {
                    logger.error("Error processing chunk {}/{} for {} (execution: {}): {}",
                        i + 1, chunks.length, symbol, executionId, e.getMessage(), e);
                    // Continue with next chunk rather than failing completely
                }
            }
            
            logger.info("Completed bulk processing for {} - total data points: {} (execution: {})",
                symbol, totalProcessed.get(), executionId);
            
            return totalProcessed.get() > 0;
            
        } catch (Exception e) {
            logger.error("Failed bulk processing for {} (execution: {}): {}", 
                symbol, executionId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Processes a chunk of data in smaller batches to avoid memory spikes.
     */
    private boolean processBatchesInChunk(
        List<OHLCV> chunkData,
        MarketInstrument instrument,
        String symbol,
        String executionId,
        int chunkNumber,
        int totalChunks,
        AtomicInteger totalProcessed,
        AtomicLong earliestTimestamp,
        AtomicLong latestTimestamp) {
        
        try {
            // Process data in batches
            for (int i = 0; i < chunkData.size(); i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, chunkData.size());
                List<OHLCV> batch = chunkData.subList(i, endIndex);
                
                logger.debug("Processing batch {}-{} of chunk {}/{} for {} (execution: {})",
                    i, endIndex - 1, chunkNumber, totalChunks, symbol, executionId);
                
                // Validate batch quality with error handling
                DataValidationService.ValidationResult validation;
                try {
                    validation = validationService.validateOHLCVData(batch);
                } catch (Exception validationException) {
                    logger.error("Validation service error in batch {}-{} for {} (execution: {}): {}",
                        i, endIndex - 1, symbol, executionId, validationException.getMessage());
                    // Fail fast on validation service errors
                    throw new RuntimeException("Data validation failed for " + symbol + ": " + validationException.getMessage(), validationException);
                }
                
                if (!validation.isValid()) {
                    logger.warn("Skipping invalid batch {}-{} for {} (execution: {}): {}",
                        i, endIndex - 1, symbol, executionId, validation.errors());
                    continue;
                }
                
                if (validation.hasWarnings()) {
                    logger.warn("Data quality warnings in batch {}-{} for {} (execution: {}): {}",
                        i, endIndex - 1, symbol, executionId, validation.warnings());
                }
                
                // Add batch to instrument with error handling
                // Note: This is "add if not exists" - duplicates will be handled gracefully
                try {
                    instrument.addPriceData(batch);
                    logger.debug("Added batch {}-{} to instrument {} (execution: {})",
                        i, endIndex - 1, symbol, executionId);
                } catch (Exception domainException) {
                    logger.error("Domain error adding batch {}-{} to instrument {} (execution: {}): {}",
                        i, endIndex - 1, symbol, executionId, domainException.getMessage());
                    // Fail fast on domain logic errors
                    throw new RuntimeException("Failed to add data to instrument " + symbol + ": " + domainException.getMessage(), domainException);
                }
                
                // Update tracking variables
                totalProcessed.addAndGet(batch.size());
                
                batch.stream()
                    .mapToLong(ohlcv -> ohlcv.timestamp().toEpochMilli())
                    .forEach(timestamp -> {
                        earliestTimestamp.updateAndGet(current -> Math.min(current, timestamp));
                        latestTimestamp.updateAndGet(current -> Math.max(current, timestamp));
                    });
                
                // Periodic saves to avoid losing too much work
                if (totalProcessed.get() % TradingConstants.INTERMEDIATE_SAVE_FREQUENCY == 0) {
                    try {
                        repository.save(instrument);
                        logger.debug("Intermediate save after {} data points for {} (execution: {})",
                            totalProcessed.get(), symbol, executionId);
                    } catch (Exception e) {
                        logger.error("Database error during intermediate save for {} (execution: {}): {}", 
                            symbol, executionId, e.getMessage(), e);
                        // Fail fast on database errors
                        throw new RuntimeException("Database save failed for " + symbol + ": " + e.getMessage(), e);
                    }
                }
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error processing batches in chunk {}/{} for {} (execution: {}): {}",
                chunkNumber, totalChunks, symbol, executionId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Gets or creates instrument for DATA INGESTION purposes only.
     * 
     * IMPORTANT: This method uses findInstrumentMetadataBySymbol() which does NOT load
     * existing price data from the database. This is intentional because:
     * 
     * - This use case is for DATA INGESTION from external sources (e.g., Bybit)
     * - We don't need existing price data when ingesting new data
     * - Loading all existing price data would be expensive and contradict the business logic
     * - Separate query APIs should be used when you need to retrieve existing data
     */
    private MarketInstrument getOrCreateInstrument(String symbol) {
        try {
            // Try to find existing instrument metadata only (no price data loading)
            // This is for data ingestion - we don't need existing price data
            Optional<MarketInstrument> existingInstrument = 
                repository.findInstrumentMetadataBySymbol(symbol);
            
            if (existingInstrument.isPresent()) {
                logger.debug("Found existing instrument metadata for symbol: {} (no price data loaded)", symbol);
                return existingInstrument.get();
            }
            
            // Create new instrument if not found
            MarketInstrument newInstrument = createDefaultInstrument(symbol);
            logger.debug("Created new instrument for symbol: {}", symbol);
            
            // Save immediately to detect database errors early
            repository.save(newInstrument);
            logger.debug("Successfully saved new instrument for symbol: {}", symbol);
            
            return newInstrument;
            
        } catch (Exception e) {
            logger.error("Database error in getOrCreateInstrument for symbol: {}", symbol, e);
            throw new RuntimeException("Failed to retrieve or create instrument " + symbol + ": " + e.getMessage(), e);
        }
    }
    
    private MarketInstrument createDefaultInstrument(String symbol) {
        return switch (symbol.toUpperCase()) {
            case "BTC" -> MarketInstrument.bitcoin();
            case "ETH" -> MarketInstrument.ethereum();
            default -> MarketInstrument.crypto(symbol, symbol + " Token");
        };
    }
    
    
    
}