/**
 * Backtesting Interface Layer
 *
 * External interfaces for the backtesting module including:
 * - Camunda service delegates for workflow integration
 * - REST API endpoints for backtest operations (future)
 * - Event listeners for inter-module communication
 * - API Ports: Public contracts for cross-module communication
 *
 * This layer handles all external communication while delegating
 * business logic to the application and domain layers.
 *
 * PUBLIC API: This package is exposed to other modules via @NamedInterface.
 * The api subpackage contains port interfaces that other modules should use.
 */
@org.springframework.lang.NonNullApi
@org.springframework.modulith.NamedInterface("api")
package com.ahd.trading_platform.backtesting.interfaces;
