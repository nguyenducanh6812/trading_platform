package com.ahd.trading_platform.portfolio.interfaces.rest;

import com.ahd.trading_platform.portfolio.application.dto.*;
import com.ahd.trading_platform.portfolio.application.mappers.PortfolioMapper;
import com.ahd.trading_platform.portfolio.application.services.PortfolioApplicationService;
import com.ahd.trading_platform.portfolio.domain.entities.Portfolio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for portfolio management operations.
 */
@RestController
@RequestMapping("/api/v1/portfolios")
@Tag(name = "Portfolio Management", description = "Endpoints for managing trading portfolios")
@Slf4j
public class PortfolioController {

    private final PortfolioApplicationService portfolioService;
    private final PortfolioMapper portfolioMapper;

    public PortfolioController(
        PortfolioApplicationService portfolioService,
        PortfolioMapper portfolioMapper
    ) {
        this.portfolioService = portfolioService;
        this.portfolioMapper = portfolioMapper;
    }

    @PostMapping
    @Operation(
        summary = "Create new portfolio",
        description = "Creates a new trading portfolio with specified configuration. " +
                      "Use 'status' field in request body to create DRAFT or ACTIVE portfolio. " +
                      "If status is not provided, defaults to ACTIVE."
    )
    public ResponseEntity<PortfolioResponse> createPortfolio(
        @Valid @RequestBody CreatePortfolioRequest request,
        @RequestParam String userId
    ) {
        log.info("Creating portfolio for user: {}", userId);

        Portfolio portfolio = portfolioService.createPortfolio(request, userId);
        PortfolioResponse response = portfolioMapper.toResponse(portfolio);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get portfolio by ID")
    public ResponseEntity<PortfolioResponse> getPortfolio(@PathVariable Long id) {
        Portfolio portfolio = portfolioService.getPortfolio(id);
        return ResponseEntity.ok(portfolioMapper.toResponse(portfolio));
    }

    @GetMapping
    @Operation(summary = "Get user's portfolios")
    public ResponseEntity<List<PortfolioResponse>> getUserPortfolios(@RequestParam String userId) {
        List<Portfolio> portfolios = portfolioService.getUserPortfolios(userId);
        List<PortfolioResponse> response = portfolios.stream()
            .map(portfolioMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    @Operation(summary = "Get user's active portfolios")
    public ResponseEntity<List<PortfolioResponse>> getActivePortfolios(@RequestParam String userId) {
        List<Portfolio> portfolios = portfolioService.getActivePortfolios(userId);
        List<PortfolioResponse> response = portfolios.stream()
            .map(portfolioMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/drafts")
    @Operation(summary = "Get user's draft portfolios", description = "Retrieves all portfolios in DRAFT status for the specified user")
    public ResponseEntity<List<PortfolioResponse>> getDraftPortfolios(@RequestParam String userId) {
        List<Portfolio> portfolios = portfolioService.getDraftPortfolios(userId);
        List<PortfolioResponse> response = portfolios.stream()
            .map(portfolioMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate draft portfolio", description = "Activates a portfolio from DRAFT status to ACTIVE status")
    public ResponseEntity<PortfolioResponse> activatePortfolio(@PathVariable Long id) {
        log.info("Activating portfolio: {}", id);
        Portfolio portfolio = portfolioService.activatePortfolio(id);
        return ResponseEntity.ok(portfolioMapper.toResponse(portfolio));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update portfolio")
    public ResponseEntity<PortfolioResponse> updatePortfolio(
        @PathVariable Long id,
        @Valid @RequestBody UpdatePortfolioRequest request
    ) {
        Portfolio portfolio = portfolioService.updatePortfolio(id, request);
        return ResponseEntity.ok(portfolioMapper.toResponse(portfolio));
    }

    @PostMapping("/{id}/instruments")
    @Operation(summary = "Add instrument to portfolio")
    public ResponseEntity<PortfolioResponse> addInstrument(
        @PathVariable Long id,
        @Valid @RequestBody AddInstrumentRequest request
    ) {
        Portfolio portfolio = portfolioService.addInstrument(id, request);
        return ResponseEntity.ok(portfolioMapper.toResponse(portfolio));
    }

    @PostMapping("/{id}/trades")
    @Operation(summary = "Execute trade")
    public ResponseEntity<PortfolioResponse> executeTrade(
        @PathVariable Long id,
        @Valid @RequestBody ExecuteTradeRequest request
    ) {
        Portfolio portfolio = portfolioService.executeTrade(id, request);
        return ResponseEntity.ok(portfolioMapper.toResponse(portfolio));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete portfolio")
    public ResponseEntity<Void> deletePortfolio(@PathVariable Long id) {
        portfolioService.deletePortfolio(id);
        return ResponseEntity.noContent().build();
    }
}
