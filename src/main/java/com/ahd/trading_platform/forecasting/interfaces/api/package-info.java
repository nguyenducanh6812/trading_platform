/**
 * Forecasting Module Public API
 *
 * This package contains the public API contracts (ports) that other modules should use
 * to interact with the Forecasting module.
 *
 * Key Components:
 * - ForecastingPort: Main port interface for accessing prediction data
 * - PredictionInfoDto: DTO for prediction information
 *
 * All cross-module dependencies should go through these port interfaces rather than
 * directly accessing domain, application, or infrastructure layers.
 *
 * This is an ANTI-CORRUPTION LAYER that:
 * - Prevents external modules from depending on internal domain models
 * - Provides stable contracts that can evolve independently
 * - Converts domain value objects to DTOs for cross-module communication
 */
@org.springframework.lang.NonNullApi
package com.ahd.trading_platform.forecasting.interfaces.api;
