package com.ahd.trading_platform.portfolio.application.usecases;

import com.ahd.trading_platform.portfolio.application.dto.UpdatePortfolioRequest;
import com.ahd.trading_platform.portfolio.domain.entities.Portfolio;
import com.ahd.trading_platform.portfolio.domain.repositories.PortfolioRepository;
import com.ahd.trading_platform.portfolio.domain.valueobjects.Leverage;
import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class UpdatePortfolioUseCase {

    private final PortfolioRepository portfolioRepository;

    public UpdatePortfolioUseCase(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    @Transactional
    public Portfolio execute(Long portfolioId, UpdatePortfolioRequest request) {
        log.info("Updating portfolio {}", portfolioId);

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));

        // Update details
        if (request.name() != null || request.description() != null) {
            portfolio.updateDetails(request.name(), request.description());
        }

        // Update strategy
        if (request.strategyType() != null || request.riskTolerance() != null || request.rebalancingFrequency() != null) {
            StrategyConfig currentConfig = portfolio.getStrategyConfig();
            StrategyConfig newConfig = new StrategyConfig(
                request.strategyType() != null ? request.strategyType() : currentConfig.strategyType(),
                request.riskTolerance() != null ? request.riskTolerance() : currentConfig.riskTolerance(),
                request.rebalancingFrequency() != null ? request.rebalancingFrequency() : currentConfig.rebalancingFrequency(),
                request.autoRebalance() != null ? request.autoRebalance() : currentConfig.autoRebalance(),
                currentConfig.targetRiskFreeRate()
            );
            portfolio.updateStrategy(newConfig);
        }

        // Update leverage
        if (request.leverageRatio() != null || request.leverageEnabled() != null) {
            Leverage currentLeverage = portfolio.getLeverage();
            int ratio = request.leverageRatio() != null ? request.leverageRatio() : currentLeverage.leverageRatio();
            boolean enabled = request.leverageEnabled() != null ? request.leverageEnabled() : currentLeverage.enabled();

            Leverage newLeverage = enabled ? Leverage.of(ratio) : Leverage.none();
            portfolio.updateLeverage(newLeverage);
        }

        return portfolioRepository.save(portfolio);
    }
}
