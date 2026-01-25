/**
 * Forecasting Domain Layer
 *
 * Contains the core business logic for ARIMA-based financial forecasting.
 *
 * Key Domain Concepts:
 * - ARIMA Models: Statistical models for time series forecasting
 * - Time Series Data: Historical price movements and derived calculations
 * - Forecast Results: Predictions for future price movements and returns
 *
 * Domain Services:
 * - ARIMA calculation engine
 * - Statistical data preparation services
 * - Forecast validation services
 *
 * This module follows Domain-Driven Design principles with clear separation
 * between domain logic and infrastructure concerns.
 *
 * INTERNAL ONLY: This layer is not exposed outside the module.
 * Other modules must access functionality through the interfaces.api package.
 */
@org.springframework.lang.NonNullApi
package com.ahd.trading_platform.forecasting.domain;