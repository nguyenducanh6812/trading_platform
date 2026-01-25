/**
 * Interface Layer - External system integrations and API endpoints for Market Data.
 *
 * Contains:
 * - REST Controllers: HTTP API endpoints for external clients
 * - Camunda External Task Workers: Workflow integration
 * - Event Handlers: Domain event processing
 * - API Ports: Public contracts for cross-module communication
 * - DTOs: External communication data structures
 *
 * This layer handles external communication and translates between external protocols and internal domain.
 *
 * PUBLIC API: This package is exposed to other modules via @NamedInterface.
 * The api subpackage contains port interfaces that other modules should use.
 */
@org.springframework.lang.NonNullApi
@org.springframework.modulith.NamedInterface("api")
package com.ahd.trading_platform.marketdata.interfaces;