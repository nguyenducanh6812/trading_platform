package com.ahd.trading_platform.portfolio.application.usecases;

import com.ahd.trading_platform.portfolio.application.dto.CreatePortfolioRequest;
import com.ahd.trading_platform.portfolio.domain.entities.Portfolio;
import com.ahd.trading_platform.portfolio.domain.repositories.PortfolioRepository;
import com.ahd.trading_platform.portfolio.domain.valueobjects.Leverage;
import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Use case for creating a new portfolio.
 */
@Service
@Slf4j
public class CreatePortfolioUseCase {

    private final PortfolioRepository portfolioRepository;

    public CreatePortfolioUseCase(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    @Transactional
    public Portfolio execute(CreatePortfolioRequest request, String userId) {
        log.info("Creating portfolio for user: {}, name: {}, symbols: {}",
            userId, request.name(), request.symbols());

        // Check if portfolio with same name already exists
        if (portfolioRepository.existsByUserIdAndName(userId, request.name())) {
            throw new IllegalArgumentException(
                String.format("Portfolio with name '%s' already exists for user", request.name())
            );
        }

        // Use symbols directly
        Set<String> symbols = new HashSet<>(request.symbols());

        // Create strategy config
        StrategyConfig strategyConfig = StrategyConfig.custom(
            request.strategyType(),
            request.riskTolerance(),
            request.rebalancingFrequency(),
            request.autoRebalance()
        );

        // Create leverage
        Leverage leverage = request.leverageEnabled()
            ? Leverage.of(request.leverageRatio())
            : Leverage.none();

        // Create portfolio with optional status (defaults to ACTIVE if not provided)
        Portfolio portfolio = new Portfolio(
            request.name(),
            request.description(),
            userId,
            symbols,
            request.initialCapital(),
            request.currency(),
            strategyConfig,
            leverage,
            request.status()  // Can be DRAFT or ACTIVE (null defaults to ACTIVE)
        );

        // Save
        Portfolio savedPortfolio = portfolioRepository.save(portfolio);

        log.info("Portfolio created successfully: id={}, name={}, symbols={}",
            savedPortfolio.getId(), savedPortfolio.getName(),
            savedPortfolio.getSelectedSymbols());

        return savedPortfolio;
    }
}
