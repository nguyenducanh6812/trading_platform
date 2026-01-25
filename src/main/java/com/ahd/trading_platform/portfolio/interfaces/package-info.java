/**
 * Interface Layer - External system integrations and API endpoints for Portfolio.
 *
 * Contains:
 * - REST Controllers: HTTP API endpoints for external clients
 * - Event Handlers: Domain event processing (future)
 * - API Ports: Public contracts for cross-module communication (future)
 * - DTOs: External communication data structures
 *
 * This layer handles external communication and translates between external protocols and internal domain.
 *
 * REST API Endpoints:
 * - POST /api/v1/portfolios - Create new portfolio
 * - GET /api/v1/portfolios/{id} - Get portfolio by ID
 * - GET /api/v1/portfolios - Get user's portfolios
 * - GET /api/v1/portfolios/active - Get active portfolios
 * - PUT /api/v1/portfolios/{id} - Update portfolio
 * - POST /api/v1/portfolios/{id}/instruments - Add instrument
 * - POST /api/v1/portfolios/{id}/trades - Execute trade
 * - DELETE /api/v1/portfolios/{id} - Delete portfolio
 *
 * PUBLIC API: This package is exposed to other modules via @NamedInterface.
 * The rest subpackage contains controllers that other modules can invoke via HTTP.
 */
@org.springframework.lang.NonNullApi
@org.springframework.modulith.NamedInterface("rest")
package com.ahd.trading_platform.portfolio.interfaces;
