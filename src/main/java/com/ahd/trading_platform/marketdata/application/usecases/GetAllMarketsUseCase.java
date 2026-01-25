package com.ahd.trading_platform.marketdata.application.usecases;

import com.ahd.trading_platform.marketdata.application.dto.MarketResponse;
import com.ahd.trading_platform.marketdata.domain.entities.Market;
import com.ahd.trading_platform.marketdata.domain.repositories.MarketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Use case for retrieving all available markets.
 * Returns list of markets that users can select from.
 */
@Component
public class GetAllMarketsUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetAllMarketsUseCase.class);

    private final MarketRepository marketRepository;

    public GetAllMarketsUseCase(MarketRepository marketRepository) {
        this.marketRepository = marketRepository;
    }

    /**
     * Executes the use case to get all markets.
     *
     * @return List of market responses
     */
    public List<MarketResponse> execute() {
        logger.debug("Fetching all available markets");

        List<Market> markets = marketRepository.findAll();

        List<MarketResponse> responses = markets.stream()
            .map(MarketResponse::from)
            .toList();

        logger.info("Retrieved {} markets", responses.size());

        return responses;
    }
}
