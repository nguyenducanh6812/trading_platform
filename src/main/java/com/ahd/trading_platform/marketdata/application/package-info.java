/**
 * Application Layer - Use cases and application services for Market Data operations.
 * 
 * Contains:
 * - Use Cases: Business workflows and orchestration
 * - DTOs: Data transfer objects for cross-boundary communication  
 * - Application Services: Coordination of domain objects and infrastructure
 * 
 * This layer orchestrates domain operations but contains no business logic itself.
 */
@org.springframework.modulith.NamedInterface("ports")
@org.springframework.lang.NonNullApi
package com.ahd.trading_platform.marketdata.application;