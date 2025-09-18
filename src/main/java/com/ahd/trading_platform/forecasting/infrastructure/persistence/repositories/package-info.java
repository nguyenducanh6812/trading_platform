/**
 * Prediction data repository interfaces for cross-module access.
 * 
 * This package exposes repository interfaces that can be safely used
 * by other modules for accessing prediction/forecast data.
 * 
 * Exposed interfaces:
 * - AssetSpecificPredictionRepository: Repository for accessing prediction data
 * - AssetSpecificPredictionRepositoryFactory: Factory for getting asset-specific repositories
 */
@org.springframework.modulith.NamedInterface("prediction-repositories")
package com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories;