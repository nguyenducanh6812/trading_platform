/**
 * Domain Layer - Pure business logic for Portfolio operations.
 *
 * Contains:
 * - Entities: Portfolio (aggregate root), Trade
 * - Value Objects: Capital, Position, StrategyConfig, Leverage
 * - Enums: StrategyType, RiskTolerance, RebalancingFrequency, PortfolioStatus, TradeType, TradeStatus
 * - Repository Contracts: PortfolioRepository interface
 * - Domain Services: Portfolio management business rules
 *
 * The Portfolio aggregate root manages:
 * - Capital allocation and tracking
 * - Position management (add, increase, decrease)
 * - Trade execution and history
 * - Strategy configuration
 * - Leverage settings
 * - Rebalancing operations
 *
 * This package has no dependencies on infrastructure or external systems.
 * All business invariants and rules are enforced within this layer.
 *
 * INTERNAL ONLY: This layer is not exposed outside the module.
 * Other modules must access functionality through the interfaces.rest package.
 */
@org.springframework.lang.NonNullApi
package com.ahd.trading_platform.portfolio.domain;
