/**
 * Market Data Module - Self-contained Spring Modulith module for handling market data operations.
 * 
 * This module implements a complete DDD architecture with the following layers:
 * - Domain: Pure business logic with entities, value objects, and domain services
 * - Application: Use cases and application services
 * - Infrastructure: Technical implementations and external integrations  
 * - Interface: REST controllers and external system adapters
 * 
 * Module follows asset-specific storage strategy with separate tables for different instruments (BTC, ETH).
 * Designed to be microservices-ready for future extraction.
 * 
 * @since 1.0
 * @author Trading Platform Team
 */
@org.springframework.modulith.ApplicationModule
package com.ahd.trading_platform.marketdata;