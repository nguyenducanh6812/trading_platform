/**
 * Interface Layer - External system integrations and API endpoints for Market Data.
 * 
 * Contains:
 * - REST Controllers: HTTP API endpoints for external clients
 * - Camunda External Task Workers: Workflow integration  
 * - Event Handlers: Domain event processing
 * - DTOs: External communication data structures
 * 
 * This layer handles external communication and translates between external protocols and internal domain.
 */
@org.springframework.lang.NonNullApi
package com.ahd.trading_platform.marketdata.interfaces;