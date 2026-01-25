/**
 * Market Data Module - Self-contained Spring Modulith module for handling market data operations.
 *
 * This module implements a complete DDD architecture with the following layers:
 * - Domain: Pure business logic with entities, value objects, and domain services (INTERNAL)
 * - Application: Use cases and application services (INTERNAL)
 * - Infrastructure: Technical implementations and external integrations (INTERNAL)
 * - Interfaces: Public API exposed to other modules (PUBLIC via @NamedInterface)
 *
 * Module follows asset-specific storage strategy with separate tables for different instruments (BTC, ETH).
 * Designed to be microservices-ready for future extraction.
 *
 * Only the interfaces package is exposed to other modules. Domain, application, and infrastructure
 * layers are internal and should never be accessed directly from outside this module.
 *
 * @since 1.0
 * @author Trading Platform Team
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = "shared"
)
package com.ahd.trading_platform.marketdata;