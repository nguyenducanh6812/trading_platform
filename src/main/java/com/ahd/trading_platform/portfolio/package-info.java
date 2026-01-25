/**
 * Portfolio Module - Self-contained Spring Modulith module for managing trading portfolios.
 *
 * This module implements a complete DDD architecture with the following layers:
 * - Domain: Pure business logic with entities, value objects, and domain services (INTERNAL)
 * - Application: Use cases and application services (INTERNAL)
 * - Infrastructure: Technical implementations and persistence (INTERNAL)
 * - Interfaces: Public API exposed to other modules (PUBLIC via @NamedInterface)
 *
 * The module provides comprehensive portfolio management capabilities including:
 * - Portfolio creation and configuration (MPT, Equal Weight, Custom strategies)
 * - Position tracking and management (add/increase/decrease positions)
 * - Trade execution and history (buy/sell operations)
 * - Capital management (initial, current, available, reserved)
 * - Strategy configuration (risk tolerance, rebalancing frequency)
 * - Leverage management (enabled/disabled, ratio limits)
 *
 * Designed to be microservices-ready for future extraction.
 *
 * Only the interfaces package is exposed to other modules. Domain, application, and infrastructure
 * layers are internal and should never be accessed directly from outside this module.
 *
 * @since 1.0
 * @author Trading Platform Team
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"shared", "marketdata::api"}
)
package com.ahd.trading_platform.portfolio;
