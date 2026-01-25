package com.ahd.trading_platform.portfolio.application.services;

import com.ahd.trading_platform.portfolio.application.dto.*;
import com.ahd.trading_platform.portfolio.application.usecases.*;
import com.ahd.trading_platform.portfolio.domain.entities.Portfolio;
import com.ahd.trading_platform.portfolio.domain.repositories.PortfolioRepository;
import com.ahd.trading_platform.portfolio.domain.valueobjects.PortfolioStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service coordinating portfolio operations.
 */
@Service
@Slf4j
public class PortfolioApplicationService {

    private final CreatePortfolioUseCase createPortfolioUseCase;
    private final AddInstrumentToPortfolioUseCase addInstrumentUseCase;
    private final ExecuteTradeUseCase executeTradeUseCase;
    private final UpdatePortfolioUseCase updatePortfolioUseCase;
    private final PortfolioRepository portfolioRepository;

    public PortfolioApplicationService(
        CreatePortfolioUseCase createPortfolioUseCase,
        AddInstrumentToPortfolioUseCase addInstrumentUseCase,
        ExecuteTradeUseCase executeTradeUseCase,
        UpdatePortfolioUseCase updatePortfolioUseCase,
        PortfolioRepository portfolioRepository
    ) {
        this.createPortfolioUseCase = createPortfolioUseCase;
        this.addInstrumentUseCase = addInstrumentUseCase;
        this.executeTradeUseCase = executeTradeUseCase;
        this.updatePortfolioUseCase = updatePortfolioUseCase;
        this.portfolioRepository = portfolioRepository;
    }

    public Portfolio createPortfolio(CreatePortfolioRequest request, String userId) {
        return createPortfolioUseCase.execute(request, userId);
    }

    public Portfolio addInstrument(Long portfolioId, AddInstrumentRequest request) {
        return addInstrumentUseCase.execute(portfolioId, request);
    }

    public Portfolio executeTrade(Long portfolioId, ExecuteTradeRequest request) {
        return executeTradeUseCase.execute(portfolioId, request);
    }

    public Portfolio updatePortfolio(Long portfolioId, UpdatePortfolioRequest request) {
        return updatePortfolioUseCase.execute(portfolioId, request);
    }

    public Portfolio getPortfolio(Long portfolioId) {
        return portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));
    }

    public List<Portfolio> getUserPortfolios(String userId) {
        return portfolioRepository.findByUserId(userId);
    }

    public List<Portfolio> getActivePortfolios(String userId) {
        return portfolioRepository.findByUserIdAndStatus(userId, PortfolioStatus.ACTIVE);
    }

    public List<Portfolio> getDraftPortfolios(String userId) {
        return portfolioRepository.findByUserIdAndStatus(userId, PortfolioStatus.DRAFT);
    }

    public Portfolio activatePortfolio(Long portfolioId) {
        log.info("Activating draft portfolio: {}", portfolioId);
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));

        portfolio.activate();
        return portfolioRepository.save(portfolio);
    }

    public void deletePortfolio(Long portfolioId) {
        portfolioRepository.deleteById(portfolioId);
    }
}
