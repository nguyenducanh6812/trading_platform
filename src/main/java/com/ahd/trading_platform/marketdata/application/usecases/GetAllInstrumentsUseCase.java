package com.ahd.trading_platform.marketdata.application.usecases;

import com.ahd.trading_platform.marketdata.application.dto.AvailableInstrumentsResponse;
import com.ahd.trading_platform.marketdata.application.dto.InstrumentInfo;
import com.ahd.trading_platform.marketdata.domain.entities.MarketInstrument;
import com.ahd.trading_platform.marketdata.domain.repositories.MarketDataRepository;
import com.ahd.trading_platform.marketdata.domain.services.ExternalDataClientFactory;
import com.ahd.trading_platform.marketdata.domain.services.ExternalDataClientStrategy;
import com.ahd.trading_platform.marketdata.domain.valueobjects.DataSourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for retrieving all available trading instruments.
 * Implements database-first strategy with optional external API refresh.
 *
 * Flow:
 * 1. Query from database first (fast, master data)
 * 2. If empty: Fetch from external API (Bybit) as fallback
 * 3. Trigger async refresh from API in background (optional)
 */
@Service
@Slf4j
public class GetAllInstrumentsUseCase {

    private final ExternalDataClientFactory clientFactory;
    private final MarketDataRepository marketDataRepository;

    public GetAllInstrumentsUseCase(
        ExternalDataClientFactory clientFactory,
        MarketDataRepository marketDataRepository
    ) {
        this.clientFactory = clientFactory;
        this.marketDataRepository = marketDataRepository;
    }

    /**
     * Executes the use case to get available instruments.
     * Database-first strategy for better performance.
     *
     * @param sourceCode Data source provider code (e.g., "bybit")
     * @return Response containing instruments and metadata
     */
    public AvailableInstrumentsResponse execute(String sourceCode) {
        DataSourceType source = DataSourceType.fromCode(sourceCode);

        log.info("Fetching available instruments from source: {}", source.getCode());

        // Step 1: Try database first (fast, master data rarely changes)
        List<MarketInstrument> dbInstruments = marketDataRepository.findAll();

        if (!dbInstruments.isEmpty()) {
            log.info("Retrieved {} instruments from database (cached)", dbInstruments.size());

            List<InstrumentInfo> instruments = dbInstruments.stream()
                .map(instrument -> InstrumentInfo.of(
                    instrument.getSymbol(),
                    instrument.getName()
                ))
                .toList();

            // Step 2: Return cached data immediately
            return AvailableInstrumentsResponse.fromDatabase(instruments, source.getCode());
        }

        // Step 3: Database empty - fetch from external API as fallback
        log.warn("Database is empty, fetching from external API: {}", source.getCode());

        try {
            List<InstrumentInfo> instruments = fetchFromExternalAPI(source);

            log.info("Successfully fetched {} instruments from external API ({})",
                instruments.size(), source.getCode());

            // Note: Async sync removed - this use case will be deprecated in favor of market-specific queries
            // TODO: Remove this use case once frontend migrates to GetInstrumentsByMarketUseCase

            return AvailableInstrumentsResponse.fromExternalApi(instruments, source.getCode());

        } catch (Exception e) {
            log.error("Both database and external API failed for source {}: {}",
                source.getCode(), e.getMessage());

            throw new RuntimeException("Unable to fetch instruments from database or external API", e);
        }
    }

    /**
     * Fetches instruments from external API.
     *
     * @param source Data source type
     * @return List of instrument info
     */
    private List<InstrumentInfo> fetchFromExternalAPI(DataSourceType source) {
        ExternalDataClientStrategy client = clientFactory.getStrategy(source.getCode());
        return client.fetchAvailableInstruments();
    }

}
