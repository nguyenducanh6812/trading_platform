package com.ahd.trading_platform.portfolio.interfaces.rest;

import com.ahd.trading_platform.portfolio.application.dto.ConfigurePortfolioStrategyRequest;
import com.ahd.trading_platform.portfolio.application.dto.StrategyResponse;
import com.ahd.trading_platform.portfolio.application.usecases.ConfigurePortfolioStrategyUseCase;
import com.ahd.trading_platform.portfolio.application.usecases.GetAvailableStrategiesUseCase;
import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for strategy management.
 * Provides endpoints for retrieving available strategies and configuring portfolio strategies.
 */
@RestController
@RequestMapping("/api/v1/strategies")
@Tag(name = "Strategies", description = "Trading strategy management and configuration")
@RequiredArgsConstructor
@Slf4j
public class StrategyController {

    private final GetAvailableStrategiesUseCase getAvailableStrategiesUseCase;
    private final ConfigurePortfolioStrategyUseCase configurePortfolioStrategyUseCase;

    /**
     * Gets all available strategies
     */
    @GetMapping
    @Operation(
        summary = "Get all available strategies",
        description = "Returns list of all active trading strategies that users can choose for their portfolios. " +
                      "Each strategy includes its parameter schema and dependency requirements."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved strategies")
    public ResponseEntity<List<StrategyResponse>> getAllStrategies() {
        log.info("Fetching all available strategies");

        List<StrategyResponse> strategies = getAvailableStrategiesUseCase.execute();

        return ResponseEntity.ok(strategies);
    }

    /**
     * Gets strategies by category
     */
    @GetMapping("/category/{category}")
    @Operation(
        summary = "Get strategies by category",
        description = "Returns strategies filtered by category (PORTFOLIO_OPTIMIZATION, FORECASTING, RISK_MANAGEMENT, EXECUTION)"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved strategies")
    @ApiResponse(responseCode = "400", description = "Invalid category")
    public ResponseEntity<List<StrategyResponse>> getStrategiesByCategory(
        @Parameter(description = "Strategy category", example = "PORTFOLIO_OPTIMIZATION")
        @PathVariable String category
    ) {
        log.info("Fetching strategies for category: {}", category);

        try {
            StrategyCategory strategyCategory = StrategyCategory.valueOf(category.toUpperCase());
            List<StrategyResponse> strategies = getAvailableStrategiesUseCase.executeByCategory(strategyCategory);

            return ResponseEntity.ok(strategies);
        } catch (IllegalArgumentException e) {
            log.error("Invalid category: {}", category);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Configures strategy for a portfolio
     */
    @PostMapping("/configure")
    @Operation(
        summary = "Configure portfolio strategy",
        description = "Configures a trading strategy for a portfolio with user-specified parameters. " +
                      "Supports hierarchical configuration (e.g., MPT with ARIMA forecasting). " +
                      "Main strategy and nested dependencies are configured together."
    )
    @ApiResponse(responseCode = "200", description = "Strategy configured successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or parameters")
    @ApiResponse(responseCode = "404", description = "Strategy or portfolio not found")
    public ResponseEntity<ConfigurationResult> configureStrategy(
        @Valid @RequestBody ConfigurePortfolioStrategyRequest request
    ) {
        log.info("Configuring strategy {} for portfolio {}",
            request.strategyCode(), request.portfolioId());

        try {
            configurePortfolioStrategyUseCase.execute(request);

            ConfigurationResult result = new ConfigurationResult(
                true,
                "Strategy configured successfully",
                request.portfolioId(),
                request.strategyCode()
            );

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("Invalid configuration request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ConfigurationResult(false, e.getMessage(), null, null));
        } catch (Exception e) {
            log.error("Failed to configure strategy: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(new ConfigurationResult(false, "Internal server error", null, null));
        }
    }

    /**
     * Response DTO for strategy configuration
     */
    public record ConfigurationResult(
        boolean success,
        String message,
        Long portfolioId,
        String strategyCode
    ) {}
}
