package com.ahd.trading_platform.marketdata.infrastructure.persistence.mappers;

import com.ahd.trading_platform.marketdata.domain.entities.MarketInstrument;
import com.ahd.trading_platform.marketdata.domain.valueobjects.*;
import com.ahd.trading_platform.marketdata.infrastructure.persistence.entities.*;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between domain objects and JPA entities.
 * Handles the translation between the domain layer and persistence layer.
 */
@Component
public class MarketDataMapper {
    
    /**
     * Converts MarketInstrument domain entity to JPA entity
     */
    public MarketInstrumentEntity toEntity(MarketInstrument instrument) {
        if (instrument == null) return null;
        
        MarketInstrumentEntity entity = new MarketInstrumentEntity(
            instrument.getSymbol(),
            instrument.getName(),
            instrument.getBaseCurrency(),
            instrument.getQuoteCurrency()
        );
        
        entity.setDataPointCount(instrument.getDataPointCount());
        
        DataQualityMetrics quality = instrument.getQualityMetrics();
        if (quality != null) {
            entity.setQualityScore(quality.getQualityScore());
            entity.setQualityLevel(quality.getQualityLevel());
            entity.setDataSource(quality.dataSource());
        }
        
        if (instrument.getLastUpdated() != null) {
            entity.getAuditInfo().setUpdatedAt(instrument.getLastUpdated());
        }
        
        return entity;
    }
    
    /**
     * Converts JPA entity to MarketInstrument domain entity
     */
    public MarketInstrument toDomain(MarketInstrumentEntity entity) {
        if (entity == null) return null;
        
        MarketInstrument instrument = new MarketInstrument(
            entity.getSymbol(),
            entity.getName(),
            entity.getBaseCurrency(),
            entity.getQuoteCurrency()
        );
        
        // Note: Price history is loaded separately by the repository
        return instrument;
    }
    
    /**
     * Converts OHLCV domain value object to BTC JPA entity
     */
    public BtcPriceEntity toBtcEntity(OHLCV ohlcv) {
        if (ohlcv == null) return null;
        
        return new BtcPriceEntity(
            ohlcv.timestamp(),
            ohlcv.open().amount(),
            ohlcv.high().amount(),
            ohlcv.low().amount(),
            ohlcv.close().amount(),
            ohlcv.volume()
        );
    }
    
    /**
     * Converts BTC JPA entity to OHLCV domain value object
     */
    public OHLCV fromBtcEntity(BtcPriceEntity entity) {
        if (entity == null) return null;
        
        return new OHLCV(
            new Price(entity.getOpenPrice(), entity.getCurrency()),
            new Price(entity.getHighPrice(), entity.getCurrency()),
            new Price(entity.getLowPrice(), entity.getCurrency()),
            new Price(entity.getClosePrice(), entity.getCurrency()),
            entity.getVolume(),
            entity.getTimestamp()
        );
    }
    
    /**
     * Converts OHLCV domain value object to ETH JPA entity
     */
    public EthPriceEntity toEthEntity(OHLCV ohlcv) {
        if (ohlcv == null) return null;
        
        return new EthPriceEntity(
            ohlcv.timestamp(),
            ohlcv.open().amount(),
            ohlcv.high().amount(),
            ohlcv.low().amount(),
            ohlcv.close().amount(),
            ohlcv.volume()
        );
    }
    
    /**
     * Converts ETH JPA entity to OHLCV domain value object
     */
    public OHLCV fromEthEntity(EthPriceEntity entity) {
        if (entity == null) return null;
        
        return new OHLCV(
            new Price(entity.getOpenPrice(), entity.getCurrency()),
            new Price(entity.getHighPrice(), entity.getCurrency()),
            new Price(entity.getLowPrice(), entity.getCurrency()),
            new Price(entity.getClosePrice(), entity.getCurrency()),
            entity.getVolume(),
            entity.getTimestamp()
        );
    }
    
    /**
     * Converts list of BTC entities to OHLCV list
     */
    public List<OHLCV> fromBtcEntities(List<BtcPriceEntity> entities) {
        if (entities == null) return List.of();
        
        return entities.stream()
            .map(this::fromBtcEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * Converts list of ETH entities to OHLCV list  
     */
    public List<OHLCV> fromEthEntities(List<EthPriceEntity> entities) {
        if (entities == null) return List.of();
        
        return entities.stream()
            .map(this::fromEthEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * Converts list of OHLCV to BTC entities
     */
    public List<BtcPriceEntity> toBtcEntities(List<OHLCV> ohlcvList) {
        if (ohlcvList == null) return List.of();
        
        return ohlcvList.stream()
            .map(this::toBtcEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * Converts list of OHLCV to ETH entities
     */
    public List<EthPriceEntity> toEthEntities(List<OHLCV> ohlcvList) {
        if (ohlcvList == null) return List.of();
        
        return ohlcvList.stream()
            .map(this::toEthEntity)
            .collect(Collectors.toList());
    }
}