/**
 * Application Layer - Use cases and application services for Market Data operations.
 *
 * Contains:
 * - Use Cases: Business workflows and orchestration
 * - DTOs: Data transfer objects for cross-boundary communication
 * - Application Services: Coordination of domain objects and infrastructure
 *
 * This layer orchestrates domain operations but contains no business logic itself.
 *
 * INTERNAL ONLY: This layer is not exposed outside the module.
 * Other modules must access functionality through the interfaces.api package.
 */
@org.springframework.lang.NonNullApi
package com.ahd.trading_platform.marketdata.application;