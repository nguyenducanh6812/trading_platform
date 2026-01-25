/**
 * Domain Layer - Pure business logic for Market Data operations.
 *
 * Contains:
 * - Entities: MarketInstrument (aggregate root)
 * - Value Objects: Price, OHLCV, TimeRange, DataQualityMetrics
 * - Repository Contracts: Abstract interfaces
 * - Domain Services: Core business logic
 * - Domain Events: Business events
 *
 * This package has no dependencies on infrastructure or external systems.
 *
 * INTERNAL ONLY: This layer is not exposed outside the module.
 * Other modules must access functionality through the interfaces.api package.
 */
@org.springframework.lang.NonNullApi
package com.ahd.trading_platform.marketdata.domain;