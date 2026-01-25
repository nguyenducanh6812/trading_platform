package com.ahd.trading_platform.portfolio.application.usecases;

import com.ahd.trading_platform.marketdata.domain.entities.MarketInstrument;
import com.ahd.trading_platform.marketdata.domain.repositories.MarketDataRepository;
import com.ahd.trading_platform.portfolio.application.dto.AddInstrumentRequest;
import com.ahd.trading_platform.portfolio.domain.entities.Portfolio;
import com.ahd.trading_platform.portfolio.domain.repositories.PortfolioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for adding instrument to portfolio.
 */
@Service
@Slf4j
public class AddInstrumentToPortfolioUseCase {

    private final PortfolioRepository portfolioRepository;
    private final MarketDataRepository marketDataRepository;

    public AddInstrumentToPortfolioUseCase(
        PortfolioRepository portfolioRepository,
        MarketDataRepository marketDataRepository
    ) {
        this.portfolioRepository = portfolioRepository;
        this.marketDataRepository = marketDataRepository;
    }

    @Transactional
    public Portfolio execute(Long portfolioId, AddInstrumentRequest request) {
        log.info("Adding symbol {} to portfolio {}", request.symbol(), portfolioId);

        // Find portfolio
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));

        // Validate symbol exists in market data
        MarketInstrument marketInstrument = marketDataRepository.findInstrumentMetadataBySymbol(request.symbol())
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Symbol '%s' not found in market data", request.symbol())
            ));

        // Validate symbol is in portfolio's selected symbols (handled by portfolio.addInstrument)
        log.info("Adding symbol {} to portfolio {}, selected symbols: {}",
            request.symbol(), portfolioId,
            portfolio.getSelectedSymbols());

        // Add instrument
        portfolio.addInstrument(request.symbol(), request.quantity(), request.entryPrice());

        // Save
        Portfolio savedPortfolio = portfolioRepository.save(portfolio);

        log.info("Symbol {} added successfully to portfolio {}", request.symbol(), portfolioId);

        return savedPortfolio;
    }
}
