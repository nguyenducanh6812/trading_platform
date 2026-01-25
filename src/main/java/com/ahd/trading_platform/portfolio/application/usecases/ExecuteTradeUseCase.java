package com.ahd.trading_platform.portfolio.application.usecases;

import com.ahd.trading_platform.marketdata.domain.entities.MarketInstrument;
import com.ahd.trading_platform.marketdata.domain.repositories.MarketDataRepository;
import com.ahd.trading_platform.portfolio.application.dto.ExecuteTradeRequest;
import com.ahd.trading_platform.portfolio.domain.entities.Portfolio;
import com.ahd.trading_platform.portfolio.domain.repositories.PortfolioRepository;
import com.ahd.trading_platform.portfolio.domain.valueobjects.TradeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for executing trades (buy/sell).
 */
@Service
@Slf4j
public class ExecuteTradeUseCase {

    private final PortfolioRepository portfolioRepository;
    private final MarketDataRepository marketDataRepository;

    public ExecuteTradeUseCase(
        PortfolioRepository portfolioRepository,
        MarketDataRepository marketDataRepository
    ) {
        this.portfolioRepository = portfolioRepository;
        this.marketDataRepository = marketDataRepository;
    }

    @Transactional
    public Portfolio execute(Long portfolioId, ExecuteTradeRequest request) {
        log.info("Executing {} trade for portfolio {}: {} {} @ {}",
            request.tradeType(), portfolioId, request.quantity(), request.symbol(), request.price());

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));

        // Validate symbol exists in market data for BUY trades
        if (request.tradeType() == TradeType.BUY) {
            MarketInstrument marketInstrument = marketDataRepository.findInstrumentMetadataBySymbol(request.symbol())
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Symbol '%s' not found in market data", request.symbol())
                ));

            log.info("Executing BUY trade for symbol {} in portfolio {}, selected symbols: {}",
                request.symbol(), portfolioId,
                portfolio.getSelectedSymbols());

            portfolio.increasePosition(request.symbol(), request.quantity(), request.price());
        } else {
            log.info("Executing SELL trade for symbol {} in portfolio {}", request.symbol(), portfolioId);
            portfolio.decreasePosition(request.symbol(), request.quantity(), request.price());
        }

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);

        log.info("Trade executed successfully for portfolio {}", portfolioId);

        return savedPortfolio;
    }
}
