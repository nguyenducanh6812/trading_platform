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
 */
package com.ahd.trading_platform.forecasting.infrastructure;