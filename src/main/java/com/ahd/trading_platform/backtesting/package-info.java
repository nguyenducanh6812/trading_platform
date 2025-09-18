/**
 * Backtesting module for Modern Portfolio Theory (MPT) trading strategies.
 * 
 * This module provides validation and orchestration capabilities for backtesting operations
 * including instrument pair validation, prediction model data availability checks,
 * and coordination with external Python-based backtesting services.
 * 
 * Architecture follows DDD principles with clear separation between:
 * - Domain: Core business logic and entities
 * - Application: Use cases and service orchestration  
 * - Infrastructure: External system integration
 * - Interface: Camunda service delegates and controllers
 */
@org.springframework.modulith.NamedInterface("backtesting")
package com.ahd.trading_platform.backtesting;