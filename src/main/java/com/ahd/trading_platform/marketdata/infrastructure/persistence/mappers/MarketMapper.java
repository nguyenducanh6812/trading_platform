package com.ahd.trading_platform.marketdata.infrastructure.persistence.mappers;

import com.ahd.trading_platform.marketdata.domain.entities.Market;
import com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.MarketEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Market domain objects and JPA entities.
 * Handles the translation between the domain layer and persistence layer.
 */
@Component
public class MarketMapper {

    /**
     * Converts Market domain entity to JPA entity
     */
    public MarketEntity toEntity(Market market) {
        if (market == null) return null;

        return MarketEntity.builder()
                .id(market.getId())
                .code(market.getCode())
                .name(market.getName())
                .description(market.getDescription())
                .build();
    }

    /**
     * Converts MarketEntity JPA entity to domain object
     */
    public Market toDomain(MarketEntity entity) {
        if (entity == null) return null;

        return new Market(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription()
        );
    }
}
