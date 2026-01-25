/**
 * Market Data Module Public API
 *
 * This package contains the public API contracts (ports) that other modules should use
 * to interact with the Market Data module.
 *
 * Key Components:
 * - MarketDataPort: Main port interface for accessing market data
 * - InstrumentInfoDto: DTO for instrument information
 *
 * All cross-module dependencies should go through these port interfaces rather than
 * directly accessing domain, application, or infrastructure layers.
 *
 * This is an ANTI-CORRUPTION LAYER that:
 * - Prevents external modules from depending on internal domain models
 * - Provides stable contracts that can evolve independently
 * - Converts domain entities to DTOs for cross-module communication
 */
@org.springframework.lang.NonNullApi
package com.ahd.trading_platform.marketdata.interfaces.api;
