/**
 * Application Layer - Use cases and orchestration for Portfolio operations.
 *
 * Contains:
 * - Use Cases: CreatePortfolioUseCase, UpdatePortfolioUseCase, AddInstrumentToPortfolioUseCase, ExecuteTradeUseCase
 * - Application Services: PortfolioApplicationService (orchestrates use cases)
 * - DTOs: Request/Response objects for API communication
 * - Mappers: Domain to DTO conversion
 *
 * This layer orchestrates domain logic and coordinates transactions.
 * It translates external requests into domain operations and handles
 * cross-cutting concerns like transaction management.
 *
 * Key responsibilities:
 * - Orchestrate multi-step business operations
 * - Manage transaction boundaries
 * - Convert between DTOs and domain entities
 * - Coordinate with domain layer
 *
 * INTERNAL ONLY: This layer is not exposed outside the module.
 * Other modules must access functionality through the interfaces.rest package.
 */
@org.springframework.lang.NonNullApi
package com.ahd.trading_platform.portfolio.application;
