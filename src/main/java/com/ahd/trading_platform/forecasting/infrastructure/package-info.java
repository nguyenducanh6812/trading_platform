/**
 * Forecasting Infrastructure Layer
 *
 * Provides concrete implementations for external integrations including:
 * - Database persistence via JPA repositories
 * - ARIMA master data loading from configuration
 * - Integration with Market Data module
 *
 * This layer implements the repository contracts defined in the domain layer
 * and handles all external system interactions.
 *
 * INTERNAL ONLY: This layer is not exposed outside the module.
 * Other modules must access functionality through the interfaces.api package.
 */
@org.springframework.lang.NonNullApi
package com.ahd.trading_platform.forecasting.infrastructure;