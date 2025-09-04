/**
 * Infrastructure Layer - Technical implementations and external integrations for Market Data.
 * 
 * Contains:
 * - Repository Implementations: JPA-based data persistence  
 * - External API Clients: Integration with Binance, Coinbase, etc.
 * - JPA Entities: Database mappings and persistence models
 * - Configuration: Infrastructure-specific configuration
 * - Repository Factory: Asset-specific repository routing
 * 
 * This layer implements the interfaces defined in the domain layer.
 */
@org.springframework.lang.NonNullApi
package com.ahd.trading_platform.marketdata.infrastructure;