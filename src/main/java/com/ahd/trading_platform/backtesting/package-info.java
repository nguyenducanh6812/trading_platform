/**
 * Backtesting module for Modern Portfolio Theory (MPT) trading strategies.
 *
 * This module provides validation and orchestration capabilities for backtesting operations
 * including instrument pair validation, prediction model data availability checks,
 * and coordination with external Python-based backtesting services.
 *
 * Architecture follows DDD principles with clear separation between:
 * - Domain: Core business logic and entities (INTERNAL)
 * - Application: Use cases and service orchestration (INTERNAL)
 * - Infrastructure: External system integration (INTERNAL)
 * - Interfaces: Public API exposed to other modules (PUBLIC via @NamedInterface)
 *
 * Only the interfaces package is exposed to other modules. Domain, application, and infrastructure
 * layers are internal and should never be accessed directly from outside this module.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"shared", "marketdata :: api", "forecasting :: api"}
)
package com.ahd.trading_platform.backtesting;