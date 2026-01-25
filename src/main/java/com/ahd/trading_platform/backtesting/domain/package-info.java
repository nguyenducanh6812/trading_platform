/**
 * Backtesting Domain Layer
 *
 * Contains the core business logic for backtesting validation and orchestration.
 *
 * Key Domain Concepts:
 * - BacktestRequest: Aggregate root for backtest validation
 * - InstrumentPair: Value object representing trading instrument pairs
 * - BacktestPeriod: Value object representing time periods for backtesting
 * - PredictionModelVersion: Value object for model version tracking
 * - MissingPredictionRange: Value object for gap analysis
 *
 * Domain Services:
 * - InstrumentPairValidator: Validates instrument pair availability
 *
 * This module follows Domain-Driven Design principles with clear separation
 * between domain logic and infrastructure concerns.
 *
 * INTERNAL ONLY: This layer is not exposed outside the module.
 * Other modules must access functionality through the interfaces.api package.
 */
@org.springframework.lang.NonNullApi
package com.ahd.trading_platform.backtesting.domain;
