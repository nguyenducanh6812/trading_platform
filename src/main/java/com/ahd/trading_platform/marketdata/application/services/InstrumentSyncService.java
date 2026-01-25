package com.ahd.trading_platform.marketdata.application.services;

import com.ahd.trading_platform.marketdata.domain.entities.MarketInstrument;
import com.ahd.trading_platform.marketdata.domain.repositories.MarketDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service responsible for asynchronous instrument synchronization.
 * Separated from use case to enable proper Spring @Async proxy mechanism.
 * Uses batch processing for optimal performance.
 * Market details provided for logging; lazy proxy used at repository layer for JPA relationship.
 */
@Service
@Slf4j
public class InstrumentSyncService {

    private final MarketDataRepository marketDataRepository;

    public InstrumentSyncService(MarketDataRepository marketDataRepository) {
        this.marketDataRepository = marketDataRepository;
    }

    /**
     * Asynchronously syncs instruments to database using batch processing.
     * Best practice: Process in batches instead of one-by-one for better performance.
     *
     * @param instruments List of instruments from external API
     * @param marketId The market ID
     * @param marketCode The market code (e.g., SPOT, LINEAR)
     * @param marketName The market name (e.g., Spot Trading)
     */
    @Async
    @Transactional
    public void syncInstrumentsAsync(List<MarketInstrument> instruments, Long marketId, String marketCode, String marketName) {
        log.info("Starting async batch sync for market {} (ID: {}, total: {} instruments)",
            marketCode, marketId, instruments.size());

        // Filter out instruments that already exist
        List<MarketInstrument> newInstruments = instruments.stream()
            .filter(instrument -> {
                Optional<MarketInstrument> existing =
                    marketDataRepository.findInstrumentMetadataBySymbol(instrument.getSymbol());
                return existing.isEmpty();
            })
            .toList();

        if (newInstruments.isEmpty()) {
            log.info("No new instruments to sync for market: {}", marketCode);
            return;
        }

        // Batch save - process in chunks of 100
        // Market reference will be set using lazy proxy (no query!)
        int batchSize = 100;
        int totalSaved = 0;

        for (int i = 0; i < newInstruments.size(); i += batchSize) {
            int end = Math.min(i + batchSize, newInstruments.size());
            List<MarketInstrument> batch = newInstruments.subList(i, end);

            try {
                // Pass marketId to create lazy proxy reference (no database query!)
                marketDataRepository.saveAll(batch, marketId);
                totalSaved += batch.size();
                log.debug("Saved batch {}/{} ({} instruments) for market: {}",
                    (i / batchSize) + 1,
                    (newInstruments.size() + batchSize - 1) / batchSize,
                    batch.size(),
                    marketCode);
            } catch (Exception e) {
                log.error("Failed to save batch for market {}: {}", marketCode, e.getMessage());
            }
        }

        int existingCount = instruments.size() - newInstruments.size();
        log.info("Async batch sync completed for market {}: {} new saved, {} already existed",
            marketCode, totalSaved, existingCount);
    }
}
