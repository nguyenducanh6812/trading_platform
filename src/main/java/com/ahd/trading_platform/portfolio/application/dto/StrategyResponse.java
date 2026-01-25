package com.ahd.trading_platform.portfolio.application.dto;

import com.ahd.trading_platform.portfolio.domain.entities.Strategy;
import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyCategory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO for strategy information in API responses
 */
public record StrategyResponse(
    Long id,
    String code,
    String name,
    String description,
    String category,
    String categoryDisplayName,
    boolean active,
    List<StrategyParameterResponse> parameters,
    List<StrategyDependencyResponse> dependencies,
    boolean hasDependencies
) {
    public static StrategyResponse from(Strategy strategy) {
        return new StrategyResponse(
            strategy.getId(),
            strategy.getCode(),
            strategy.getName(),
            strategy.getDescription(),
            strategy.getCategory().name(),
            strategy.getCategory().getDisplayName(),
            strategy.isActive(),
            strategy.getParameters().stream()
                .map(StrategyParameterResponse::from)
                .collect(Collectors.toList()),
            strategy.getDependencies().stream()
                .map(StrategyDependencyResponse::from)
                .collect(Collectors.toList()),
            strategy.hasDependencies()
        );
    }

    /**
     * DTO for strategy parameter schema
     */
    public record StrategyParameterResponse(
        String code,
        String name,
        String dataType,
        String defaultValue,
        boolean required,
        String description,
        String validationRule
    ) {
        public static StrategyParameterResponse from(com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyParameter param) {
            return new StrategyParameterResponse(
                param.getCode(),
                param.getName(),
                param.getDataType(),
                param.getDefaultValue(),
                param.isRequired(),
                param.getDescription(),
                param.getValidationRule()
            );
        }
    }

    /**
     * DTO for strategy dependency
     */
    public record StrategyDependencyResponse(
        String category,
        String categoryDisplayName,
        boolean required,
        String description
    ) {
        public static StrategyDependencyResponse from(Strategy.StrategyDependency dep) {
            return new StrategyDependencyResponse(
                dep.getCategory().name(),
                dep.getCategory().getDisplayName(),
                dep.isRequired(),
                dep.getDescription()
            );
        }
    }
}
