/**
 * Forecasting Module - Phase 3 of Modern Portfolio Theory Implementation
 * 
 * This module implements ARIMA-based financial forecasting to predict expected returns for trading instruments.
 * It follows Domain-Driven Design principles and integrates with the Market Data and Analytics modules.
 * 
 * Architecture:
 * - Domain Layer: ARIMA models, calculation services, value objects
 * - Application Layer: Use cases, DTOs, orchestration services
 * - Infrastructure Layer: Master data loading, external integrations
 * - Interface Layer: REST API, Camunda workers, event handlers
 * 
 * Key Features:
 * - 5-step ARIMA forecasting process (as per specification)
 * - BTC and ETH master data models with 30 AR coefficients
 * - Comprehensive calculation audit trail for monitoring
 * - Event-driven integration with other modules
 * - Production-ready error handling and validation
 * 
 * Integration:
 * - Consumes: Market Data Module (historical price data)
 * - Produces: Expected returns for Analytics Module
 * - Orchestration: Camunda 7 workflows
 * 
 * @author Trading Platform Team
 * @version 1.0
 * @since 2025-01-01
 */
@org.springframework.modulith.NamedInterface("forecasting")
package com.ahd.trading_platform.forecasting;