/**
 * Shared Kernel - Common value objects and concepts used across multiple bounded contexts.
 * 
 * This package contains domain concepts that are shared between multiple modules:
 * - Market Data Module: Uses TradingInstrument for data storage and retrieval
 * - Forecasting Module: Uses TradingInstrument for ARIMA model selection
 * - Analytics Module: Uses TradingInstrument for portfolio optimization
 * 
 * Following DDD principles, these are stable, well-defined concepts that form
 * the core vocabulary of the trading domain.
 * 
 * @author Trading Platform Team
 * @version 1.0
 * @since 2025-01-01
 */
package com.ahd.trading_platform.shared;