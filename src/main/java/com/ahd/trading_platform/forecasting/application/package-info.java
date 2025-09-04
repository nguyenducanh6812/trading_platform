/**
 * Forecasting Application Layer
 * 
 * Orchestrates the execution of forecasting use cases and manages the flow
 * between domain services and external integrations.
 * 
 * Key Components:
 * - Use Cases: Execute forecasting operations with business logic
 * - DTOs: Data Transfer Objects for API communication
 * - Application Services: High-level orchestration
 * 
 * This layer acts as a facade between the domain layer and external interfaces,
 * ensuring proper transaction boundaries and error handling.
 */
package com.ahd.trading_platform.forecasting.application;