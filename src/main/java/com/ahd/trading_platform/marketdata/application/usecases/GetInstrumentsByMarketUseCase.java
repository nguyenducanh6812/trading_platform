package com.ahd.trading_platform.marketdata.application.usecases;

import com.ahd.trading_platform.marketdata.application.dto.GetInstrumentsByMarketRequest;
import com.ahd.trading_platform.marketdata.application.dto.InstrumentsByMarketResponse;
import com.ahd.trading_platform.marketdata.application.services.InstrumentSyncService;
import com.ahd.trading_platform.marketdata.domain.entities.Market;
import com.ahd.trading_platform.marketdata.domain.entities.MarketInstrument;
import com.ahd.trading_platform.marketdata.domain.repositories.MarketDataRepository;
import com.ahd.trading_platform.marketdata.infrastructure.external.dto.bybit.BybitInstrumentsResponse;
import com.ahd.trading_platform.marketdata.infrastructure.external.BybitFeignClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for retrieving instruments belonging to a specific market.
 * Implements database-first strategy for better performance.
 *
 * Flow:
 * 1. Query from database first (fast, master data rarely changes)
 * 2. If empty: Fetch from external API (Bybit) as fallback
 * 3. Circuit Breaker protects against API failures
 */
@Component
public class GetInstrumentsByMarketUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetInstrumentsByMarketUseCase.class);

    private final MarketDataRepository marketDataRepository;
    private final BybitFeignClient bybitClient;
    private final InstrumentSyncService instrumentSyncService;

    public GetInstrumentsByMarketUseCase(
        MarketDataRepository marketDataRepository,
        BybitFeignClient bybitClient,
        InstrumentSyncService instrumentSyncService) {
        this.marketDataRepository = marketDataRepository;
        this.bybitClient = bybitClient;
        this.instrumentSyncService = instrumentSyncService;
    }

    /**
     * Executes the use case to get instruments for a specific market.
     * Database-first strategy with single optimized query using market ID.
     * Frontend provides market details to avoid database lookup.
     *
     * @param request Request containing market ID, code, and name from frontend
     * @return Response containing instruments for the market
     */
    public InstrumentsByMarketResponse execute(GetInstrumentsByMarketRequest request) {
        logger.info("Fetching instruments for market: {} (ID: {})", request.marketCode(), request.marketId());

        // Step 1: Query instruments by market ID (single optimized query using FK index)
        List<MarketInstrument> instruments = marketDataRepository.findByMarketId(request.marketId());

        if (!instruments.isEmpty()) {
            logger.info("Retrieved {} instruments from database (cached) for market: {}",
                instruments.size(), request.marketCode());

            return InstrumentsByMarketResponse.from(
                request.marketCode(),
                request.marketName(),
                instruments
            );
        }

        // Step 2: Database empty - fetch from external API as fallback
        logger.warn("Database is empty for market {}, fetching from external API", request.marketCode());

        return fetchFromExternalAPIWithFallback(request);
    }

    /**
     * Fetches from external API with Circuit Breaker protection.
     * Falls back to empty list if API fails.
     */
    @CircuitBreaker(name = "bybitApi", fallbackMethod = "apiFailureFallback")
    private InstrumentsByMarketResponse fetchFromExternalAPIWithFallback(GetInstrumentsByMarketRequest request) {
        List<MarketInstrument> instruments = fetchFromExternalAPI(request);

        logger.info("Successfully fetched {} instruments from external API for market {}",
            instruments.size(), request.marketCode());

        // Sync to database asynchronously for future requests (using batch processing)
        instrumentSyncService.syncInstrumentsAsync(instruments, request.marketId(), request.marketCode(), request.marketName());

        return InstrumentsByMarketResponse.from(
            request.marketCode(),
            request.marketName(),
            instruments
        );
    }

    /**
     * Fallback method called by Circuit Breaker when external API fails.
     */
    private InstrumentsByMarketResponse apiFailureFallback(GetInstrumentsByMarketRequest request, Throwable throwable) {
        logger.error("External API failed for market {} - Reason: {} - Returning empty list",
            request.marketCode(), throwable.getMessage());

        return InstrumentsByMarketResponse.from(
            request.marketCode(),
            request.marketName(),
            List.of()
        );
    }

    /**
     * Fetches instruments from external API for a specific market.
     *
     * @param request Request containing market details
     * @return List of market instruments from external API
     */
    private List<MarketInstrument> fetchFromExternalAPI(GetInstrumentsByMarketRequest request) {
        String category = mapMarketCodeToBybitCategory(request.marketCode());

        logger.debug("Fetching instruments from Bybit (category: {})", category);

        // Fetch from Bybit API
        BybitInstrumentsResponse response = bybitClient.getInstrumentsInfo(
            category,      // Market category (spot, linear, etc.)
            null,          // All symbols
            "Trading",     // Only actively trading
            null,          // All base coins
            1000,          // Max limit
            null           // No pagination
        );

        if (!response.hasValidData()) {
            throw new RuntimeException("No valid instruments data from Bybit for category: " + category);
        }

        // Convert Bybit response to domain objects
        return response.getResult().getInstrumentList().stream()
            .filter(BybitInstrumentsResponse.InstrumentInfo::isTradable)
            .filter(BybitInstrumentsResponse.InstrumentInfo::isUSDTQuoted)
            .map(info -> {
                MarketInstrument instrument = MarketInstrument.crypto(
                    info.getSymbol(),    // Full symbol (e.g., BTCUSDT)
                    info.getBaseCoin()   // Name (e.g., BTC)
                );
                // Note: Market will be set in syncSingleInstrument() to avoid detached entity issues

                // Set contract and trading specifications
                instrument.setContractType(info.getContractType());
                instrument.setSettleCoin(info.getSettleCoin());
                instrument.setLaunchTime(parseLong(info.getLaunchTime()));
                instrument.setDeliveryTime(parseLong(info.getDeliveryTime()));

                // Set leverage limits
                if (info.getLeverageFilter() != null) {
                    instrument.setMinLeverage(parseBigDecimal(info.getLeverageFilter().getMinLeverage()));
                    instrument.setMaxLeverage(parseBigDecimal(info.getLeverageFilter().getMaxLeverage()));
                }

                // Set order quantity limits
                if (info.getLotSizeFilter() != null) {
                    instrument.setMinOrderQty(parseBigDecimal(info.getLotSizeFilter().getMinOrderQty()));
                    instrument.setMaxOrderQty(parseBigDecimal(info.getLotSizeFilter().getMaxOrderQty()));
                    instrument.setQtyStep(parseBigDecimal(info.getLotSizeFilter().getQtyStep()));
                }

                // Set price tick size
                if (info.getPriceFilter() != null) {
                    instrument.setTickSize(parseBigDecimal(info.getPriceFilter().getTickSize()));
                }

                return instrument;
            })
            .collect(Collectors.toList());
    }

    /**
     * Maps market code to Bybit category.
     *
     * @param marketCode Market code (SPOT, LINEAR, INVERSE, OPTION)
     * @return Bybit category string
     */
    private String mapMarketCodeToBybitCategory(String marketCode) {
        return switch (marketCode.toUpperCase()) {
            case "SPOT" -> "spot";
            case "LINEAR" -> "linear";
            case "INVERSE" -> "inverse";
            case "OPTION" -> "option";
            default -> throw new IllegalArgumentException("Unknown market code: " + marketCode);
        };
    }


    /**
     * Helper method to safely parse Long from String.
     */
    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse Long value: {}", value);
            return null;
        }
    }

    /**
     * Helper method to safely parse BigDecimal from String.
     */
    private java.math.BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new java.math.BigDecimal(value);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse BigDecimal value: {}", value);
            return null;
        }
    }
}
