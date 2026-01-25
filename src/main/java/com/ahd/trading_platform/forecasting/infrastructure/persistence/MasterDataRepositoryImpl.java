package com.ahd.trading_platform.forecasting.infrastructure.persistence;

import com.ahd.trading_platform.forecasting.domain.repositories.MasterDataRepository;
import com.ahd.trading_platform.forecasting.domain.valueobjects.DemeanDiffOCMasterData;
import com.ahd.trading_platform.forecasting.infrastructure.persistence.entities.*;
import com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories.DemeanDiffOCMasterDataRepositoryFactory;
import com.ahd.trading_platform.marketdata.domain.services.MarketResolver;
import com.ahd.trading_platform.marketdata.domain.valueobjects.BybitMarketType;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Infrastructure implementation of MasterDataRepository.
 *
 * Delegates to market-specific JPA repositories via DemeanDiffOCMasterDataRepositoryFactory
 * and handles conversion between domain objects and JPA entities.
 *
 * This implementation:
 * - Lives in the infrastructure layer
 * - Depends on infrastructure components (factories, JPA repositories)
 * - Implements domain interface
 * - Handles entity-domain conversions
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class MasterDataRepositoryImpl implements MasterDataRepository {

    private final DemeanDiffOCMasterDataRepositoryFactory repositoryFactory;
    private final MarketResolver marketResolver;

    @Override
    public List<DemeanDiffOCMasterData> findByTimeRange(String symbol, TimeRange timeRange) {
        List<?> entities = repositoryFactory.findBySymbolAndTimestampBetweenOrderByTimestampAsc(
            symbol, timeRange.from(), timeRange.to());
        return convertEntitiesToDomain(entities);
    }

    @Override
    public List<DemeanDiffOCMasterData> findFromTimestamp(String symbol, Instant fromTimestamp) {
        List<?> entities = repositoryFactory.findBySymbolAndTimestampAfterOrderByTimestampAsc(
            symbol, fromTimestamp);
        return convertEntitiesToDomain(entities);
    }

    @Override
    public boolean existsForTimeRange(String symbol, TimeRange timeRange) {
        long count = repositoryFactory.countBySymbolAndTimestampBetween(
            symbol, timeRange.from(), timeRange.to());
        return count > 0;
    }

    @Override
    public Optional<Instant> getLatestTimestamp(String symbol) {
        Optional<?> entity = repositoryFactory.findTopBySymbolOrderByTimestampDesc(symbol);
        return entity.map(this::extractTimestamp);
    }

    @Override
    public List<DemeanDiffOCMasterData> saveAll(String symbol, List<DemeanDiffOCMasterData> masterDataList) {
        List<?> entities = convertDomainToEntities(symbol, masterDataList);
        repositoryFactory.saveAll(symbol, entities);
        return masterDataList;
    }

    @Override
    public DemeanDiffOCMasterData save(String symbol, DemeanDiffOCMasterData masterData) {
        Object entity = convertDomainToEntity(symbol, masterData);
        repositoryFactory.save(symbol, entity);
        return masterData;
    }

    @Override
    public long countByTimeRange(String symbol, TimeRange timeRange) {
        return repositoryFactory.countBySymbolAndTimestampBetween(
            symbol, timeRange.from(), timeRange.to());
    }

    @Override
    public List<Instant> findTimestampsInRange(String symbol, Instant from, Instant to) {
        return repositoryFactory.findTimestampsBySymbolAndDateRange(symbol, from, to);
    }

    @Override
    public List<DemeanDiffOCMasterData> findByTimeRangeWithDifferences(String symbol, TimeRange timeRange) {
        List<?> entities = repositoryFactory.findBySymbolAndTimestampBetweenAndHasDifferencesOrderByTimestampAsc(
            symbol, timeRange.from(), timeRange.to());
        return convertEntitiesToDomain(entities);
    }

    @Override
    public long countByTimeRangeWithDifferences(String symbol, TimeRange timeRange) {
        return repositoryFactory.countBySymbolAndTimestampBetweenAndHasDifferences(
            symbol, timeRange.from(), timeRange.to());
    }

    // Private conversion methods

    private List<DemeanDiffOCMasterData> convertEntitiesToDomain(List<?> entities) {
        return entities.stream()
            .map(this::convertEntityToDomain)
            .toList();
    }

    private DemeanDiffOCMasterData convertEntityToDomain(Object entity) {
        return switch (entity) {
            case SpotDemeanDiffOCMasterDataEntity e -> DemeanDiffOCMasterData.builder()
                .symbol(e.getSymbol())
                .timestamp(e.getTimestamp())
                .openPrice(e.getOpenPrice())
                .closePrice(e.getClosePrice())
                .oc(e.getOc())
                .diffOC(e.getDiffOC())
                .demeanDiffOC(e.getDemeanDiffOC())
                .build();
            case LinearDemeanDiffOCMasterDataEntity e -> DemeanDiffOCMasterData.builder()
                .symbol(e.getSymbol())
                .timestamp(e.getTimestamp())
                .openPrice(e.getOpenPrice())
                .closePrice(e.getClosePrice())
                .oc(e.getOc())
                .diffOC(e.getDiffOC())
                .demeanDiffOC(e.getDemeanDiffOC())
                .build();
            case InverseDemeanDiffOCMasterDataEntity e -> DemeanDiffOCMasterData.builder()
                .symbol(e.getSymbol())
                .timestamp(e.getTimestamp())
                .openPrice(e.getOpenPrice())
                .closePrice(e.getClosePrice())
                .oc(e.getOc())
                .diffOC(e.getDiffOC())
                .demeanDiffOC(e.getDemeanDiffOC())
                .build();
            case OptionDemeanDiffOCMasterDataEntity e -> DemeanDiffOCMasterData.builder()
                .symbol(e.getSymbol())
                .timestamp(e.getTimestamp())
                .openPrice(e.getOpenPrice())
                .closePrice(e.getClosePrice())
                .oc(e.getOc())
                .diffOC(e.getDiffOC())
                .demeanDiffOC(e.getDemeanDiffOC())
                .build();
            default -> throw new IllegalArgumentException("Unknown entity type: " + entity.getClass());
        };
    }

    private List<?> convertDomainToEntities(String symbol, List<DemeanDiffOCMasterData> masterDataList) {
        return masterDataList.stream()
            .map(md -> convertDomainToEntity(symbol, md))
            .toList();
    }

    private Object convertDomainToEntity(String symbol, DemeanDiffOCMasterData masterData) {
        BybitMarketType marketType = marketResolver.resolveMarket(symbol);
        boolean hasDiffs = masterData.diffOC() != null && masterData.demeanDiffOC() != null;

        return switch (marketType) {
            case SPOT -> SpotDemeanDiffOCMasterDataEntity.builder()
                .symbol(masterData.symbol())
                .timestamp(masterData.timestamp())
                .openPrice(masterData.openPrice())
                .closePrice(masterData.closePrice())
                .oc(masterData.oc())
                .diffOC(masterData.diffOC())
                .demeanDiffOC(masterData.demeanDiffOC())
                .hasDifferences(hasDiffs)
                .build();
            case LINEAR -> LinearDemeanDiffOCMasterDataEntity.builder()
                .symbol(masterData.symbol())
                .timestamp(masterData.timestamp())
                .openPrice(masterData.openPrice())
                .closePrice(masterData.closePrice())
                .oc(masterData.oc())
                .diffOC(masterData.diffOC())
                .demeanDiffOC(masterData.demeanDiffOC())
                .hasDifferences(hasDiffs)
                .build();
            case INVERSE -> InverseDemeanDiffOCMasterDataEntity.builder()
                .symbol(masterData.symbol())
                .timestamp(masterData.timestamp())
                .openPrice(masterData.openPrice())
                .closePrice(masterData.closePrice())
                .oc(masterData.oc())
                .diffOC(masterData.diffOC())
                .demeanDiffOC(masterData.demeanDiffOC())
                .hasDifferences(hasDiffs)
                .build();
            case OPTION -> OptionDemeanDiffOCMasterDataEntity.builder()
                .symbol(masterData.symbol())
                .timestamp(masterData.timestamp())
                .openPrice(masterData.openPrice())
                .closePrice(masterData.closePrice())
                .oc(masterData.oc())
                .diffOC(masterData.diffOC())
                .demeanDiffOC(masterData.demeanDiffOC())
                .hasDifferences(hasDiffs)
                .build();
        };
    }

    private Instant extractTimestamp(Object entity) {
        return switch (entity) {
            case SpotDemeanDiffOCMasterDataEntity e -> e.getTimestamp();
            case LinearDemeanDiffOCMasterDataEntity e -> e.getTimestamp();
            case InverseDemeanDiffOCMasterDataEntity e -> e.getTimestamp();
            case OptionDemeanDiffOCMasterDataEntity e -> e.getTimestamp();
            default -> throw new IllegalArgumentException("Unknown entity type: " + entity.getClass());
        };
    }
}
