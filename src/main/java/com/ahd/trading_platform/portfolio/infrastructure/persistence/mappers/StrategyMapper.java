package com.ahd.trading_platform.portfolio.infrastructure.persistence.mappers;

import com.ahd.trading_platform.portfolio.domain.entities.Strategy;
import com.ahd.trading_platform.portfolio.domain.valueobjects.StrategyParameter;
import com.ahd.trading_platform.portfolio.infrastructure.persistence.entities.StrategyDependencyEntity;
import com.ahd.trading_platform.portfolio.infrastructure.persistence.entities.StrategyEntity;
import com.ahd.trading_platform.portfolio.infrastructure.persistence.entities.StrategyParameterEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for Strategy domain <-> persistence layer.
 */
@Component
public class StrategyMapper {

    /**
     * Maps entity to domain
     */
    public Strategy toDomain(StrategyEntity entity) {
        List<StrategyParameter> parameters = entity.getParameters().stream()
            .map(this::toParameterDomain)
            .collect(Collectors.toList());

        List<Strategy.StrategyDependency> dependencies = entity.getDependencies().stream()
            .map(this::toDependencyDomain)
            .collect(Collectors.toList());

        return new Strategy(
            entity.getId(),
            null, // StrategyType is not stored in DB currently
            entity.getCategory(),
            entity.getCode(),
            entity.getName(),
            entity.getDescription(),
            entity.getActive(),
            parameters,
            dependencies
        );
    }

    /**
     * Maps domain to entity
     */
    public StrategyEntity toEntity(Strategy domain) {
        StrategyEntity entity = new StrategyEntity();
        entity.setId(domain.getId());
        entity.setCode(domain.getCode());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setCategory(domain.getCategory());
        entity.setActive(domain.isActive());

        // Map parameters
        for (StrategyParameter param : domain.getParameters()) {
            StrategyParameterEntity paramEntity = toParameterEntity(param);
            entity.addParameter(paramEntity);
        }

        // Map dependencies
        for (Strategy.StrategyDependency dep : domain.getDependencies()) {
            StrategyDependencyEntity depEntity = toDependencyEntity(dep);
            entity.addDependency(depEntity);
        }

        return entity;
    }

    /**
     * Maps parameter entity to domain
     */
    private StrategyParameter toParameterDomain(StrategyParameterEntity entity) {
        return StrategyParameter.withValidation(
            entity.getCode(),
            entity.getName(),
            entity.getDataType(),
            entity.getDefaultValue(),
            entity.getRequired(),
            entity.getDescription(),
            entity.getValidationRule()
        );
    }

    /**
     * Maps parameter domain to entity
     */
    private StrategyParameterEntity toParameterEntity(StrategyParameter domain) {
        StrategyParameterEntity entity = new StrategyParameterEntity();
        entity.setCode(domain.getCode());
        entity.setName(domain.getName());
        entity.setDataType(domain.getDataType());
        entity.setDefaultValue(domain.getDefaultValue());
        entity.setRequired(domain.isRequired());
        entity.setDescription(domain.getDescription());
        entity.setValidationRule(domain.getValidationRule());
        return entity;
    }

    /**
     * Maps dependency entity to domain
     */
    private Strategy.StrategyDependency toDependencyDomain(StrategyDependencyEntity entity) {
        return new Strategy.StrategyDependency(
            entity.getCategory(),
            entity.getRequired(),
            entity.getDescription()
        );
    }

    /**
     * Maps dependency domain to entity
     */
    private StrategyDependencyEntity toDependencyEntity(Strategy.StrategyDependency domain) {
        StrategyDependencyEntity entity = new StrategyDependencyEntity();
        entity.setCategory(domain.getCategory());
        entity.setRequired(domain.isRequired());
        entity.setDescription(domain.getDescription());
        return entity;
    }
}
