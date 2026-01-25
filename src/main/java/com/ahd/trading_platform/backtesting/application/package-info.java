/**
 * Backtesting Application Layer
 *
 * Orchestrates the execution of backtesting validation use cases and manages the flow
 * between domain services and external integrations.
 *
 * Key Components:
 * - Use Cases: Execute validation and orchestration operations
 * - DTOs: Data Transfer Objects for API communication
 * - Application Services: High-level orchestration
 *
 * This layer acts as a facade between the domain layer and external interfaces,
 * ensuring proper transaction boundaries and error handling.
 *
 * INTERNAL ONLY: This layer is not exposed outside the module.
 * Other modules must access functionality through the interfaces.api package.
 */
@org.springframework.lang.NonNullApi
package com.ahd.trading_platform.backtesting.application;
