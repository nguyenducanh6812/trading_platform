package com.ahd.trading_platform.forecasting.application.usecases;

import com.ahd.trading_platform.forecasting.domain.entities.ARIMAModel;
import com.ahd.trading_platform.forecasting.domain.valueobjects.DemeanDiffOCMasterData;
import com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories.AssetSpecificMasterDataRepository;
import com.ahd.trading_platform.forecasting.infrastructure.persistence.repositories.AssetSpecificMasterDataRepositoryFactory;
import com.ahd.trading_platform.marketdata.application.ports.MarketDataPort;
import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
import com.ahd.trading_platform.shared.valueobjects.TradingInstrument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Use case for preparing DemeanDiffOC master data for forecasting models.
 * 
 * This use case is model-agnostic and can be reused by:
 * - ARIMA models
 * - LSTM models  
 * - Prophet models
 * - Any other time series forecasting model requiring DemeanDiffOC master data
 * 
 * Architecture:
 * - Follows DDD principles with clean separation of concerns
 * - Uses pipeline pattern for efficient data preparation
 * - Implements smart caching to reuse existing calculations
 * - Supports dynamic data requirements based on model parameters
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PrepareMasterDataUseCase {
    
    private final CalculateDemeanDiffOCUseCase calculateDemeanDiffOCUseCase;
    private final MarketDataPort marketDataPort;
    private final AssetSpecificMasterDataRepositoryFactory repositoryFactory;
    
    /**
     * Prepares master data for forecasting models with the specified data requirements.
     * 
     * @param instrument The trading instrument
     * @param historicalDataRange Required historical data time range
     * @param arOrder Minimum number of data points require for one day calculation
     * @param arimaModel ARIMA model containing meanDiffOC configuration
     * @param executionId Execution ID for tracking and logging
     * @return List of prepared master data points
     */
    public List<DemeanDiffOCMasterData> prepareMasterData(
            TradingInstrument instrument,
            TimeRange historicalDataRange,
            int arOrder,
            ARIMAModel arimaModel,
            String executionId) {
        int minimumDataPoints = Math.toIntExact(historicalDataRange.getDurationDays());
        log.info("Starting master data preparation for {} (execution: {}, required points: {})", 
            instrument.getCode(), executionId, minimumDataPoints);
        
        return new MasterDataPreparationPipeline(instrument, historicalDataRange, minimumDataPoints, arimaModel, executionId)
            .checkExistingMasterData()      // Step 1: Check for existing master data
            .calculateMissingData()         // Step 2: Calculate missing data if needed
            .ensureSufficientData()         // Step 3: Expand range if still insufficient
            .getMasterData();               // Step 4: Return final master data
    }
    
    /**
     * Pipeline for master data preparation that can be used by any forecasting model.
     * Handles the complete master data lifecycle: check existing → calculate missing → ensure sufficient.
     */
    private class MasterDataPreparationPipeline {
        
        private final TradingInstrument instrument;
        private final TimeRange historicalDataRange;
        private final int minimumDataPoints;
        private final ARIMAModel arimaModel;
        private final String executionId;
        
        private List<DemeanDiffOCMasterData> masterData;
        private boolean needsCalculation;
        private final TimeRange effectiveDataRange;
        
        public MasterDataPreparationPipeline(TradingInstrument instrument, TimeRange historicalDataRange, 
                int minimumDataPoints, ARIMAModel arimaModel, String executionId) {
            this.instrument = instrument;
            this.historicalDataRange = historicalDataRange;
            this.minimumDataPoints = minimumDataPoints;
            this.arimaModel = arimaModel;
            this.executionId = executionId;
            this.effectiveDataRange = historicalDataRange;
            this.needsCalculation = false;
        }
        
        /**
         * Step 1: Check for existing master data (optimized with count-first approach)
         */
        public MasterDataPreparationPipeline checkExistingMasterData() {
            log.debug("Pipeline Step 1: Checking existing master data for {} in range {} - {} (execution: {})", 
                instrument.getCode(), historicalDataRange.from(), historicalDataRange.to(), executionId);
            
            if (calculateDemeanDiffOCUseCase.masterDataExists(instrument, historicalDataRange)) {
                // First check count to optimize performance - faster than loading all data
                long existingCount = calculateDemeanDiffOCUseCase.getMasterDataCount(instrument, historicalDataRange);
                log.info("Found {} existing master data points for {} (execution: {})", 
                    existingCount, instrument.getCode(), executionId);
                
                // Check if existing data count is sufficient
                if (existingCount >= minimumDataPoints) {
                    log.debug("Existing master data count is sufficient: {} >= {} (required by model)", 
                        existingCount, minimumDataPoints);
                    // Only load actual data when we know it's sufficient
                    this.masterData = calculateDemeanDiffOCUseCase.getMasterData(instrument, historicalDataRange);
                } else {
                    log.info("Existing master data count insufficient: {} < {} (required by model), need to calculate more", 
                        existingCount, minimumDataPoints);
                    // Load existing data for merging with newly calculated data
                    this.masterData = calculateDemeanDiffOCUseCase.getMasterData(instrument, historicalDataRange);
                    this.needsCalculation = true;
                }
            } else {
                log.info("No existing master data found for {} in range, will calculate from scratch (execution: {})", 
                    instrument.getCode(), executionId);
                this.masterData = new ArrayList<>();
                this.needsCalculation = true;
            }
            
            return this;
        }
        
        /**
         * Step 2: Calculate missing master data if needed using intelligent pipeline
         */
        public MasterDataPreparationPipeline calculateMissingData() {
            if (!needsCalculation) {
                log.debug("Pipeline Step 2: Skipping calculation - sufficient master data already exists (execution: {})", 
                    executionId);
                return this;
            }
            
            log.debug("Pipeline Step 2: Starting intelligent missing data calculation for {} (execution: {})", 
                instrument.getCode(), executionId);
            
            // Use sophisticated pipeline to calculate only missing parts
            new MissingDataCalculationPipeline(this)
                .identifyMissingRanges()           // Sub-step 1: Find what's actually missing
                .validatePriceDataSufficiency()    // Sub-step 2: Ensure price data is available
                .fetchMissingPriceData()           // Sub-step 3: Fetch missing price data if needed
                .calculateOnlyMissingParts()       // Sub-step 4: Calculate only missing master data
                .mergeMasterData();                // Sub-step 5: Merge with existing data
            
            log.info("Completed intelligent missing data calculation: {} total master data points for {} (execution: {})", 
                masterData.size(), instrument.getCode(), executionId);
            
            return this;
        }
        
        /**
         * Step 3: Validate sufficient data (fail fast after step 2 processing)
         */
        public MasterDataPreparationPipeline ensureSufficientData() {
            if (masterData.size() >= minimumDataPoints) {
                log.debug("Pipeline Step 3: Sufficient master data available: {} >= {} (required by model)", 
                    masterData.size(), minimumDataPoints);
                return this;
            }
            
            // After step 2 intelligent missing data calculation, if still insufficient, fail fast
            log.error("Pipeline Step 3: INSUFFICIENT DATA after intelligent gap detection and calculation. " +
                "Have {} points, need {} (required by model). Cannot proceed with prediction (execution: {})", 
                masterData.size(), minimumDataPoints, executionId);
            
            throw new IllegalStateException(String.format(
                "Insufficient master data for %s prediction: have %d points, need %d (AR order). " +
                "Even after intelligent gap detection and calculation, cannot obtain sufficient data for time range %s to %s. " +
                "This suggests fundamental data availability issues that cannot be resolved by expanding date ranges.",
                instrument.getCode(), masterData.size(), minimumDataPoints, 
                historicalDataRange.from(), historicalDataRange.to()));
        }
        
        /**
         * Step 4: Return final master data
         */
        public List<DemeanDiffOCMasterData> getMasterData() {
            log.debug("Pipeline Step 4: Returning {} master data points for model calculation (execution: {})", 
                masterData.size(), executionId);
            return masterData;
        }
    }
    
    /**
     * Sophisticated pipeline for calculating only missing master data parts.
     * Follows the principle of minimal computation by identifying and processing only what's missing.
     */
    private class MissingDataCalculationPipeline {
        
        private final MasterDataPreparationPipeline parent;
        private List<TimeRange> missingRanges;
        private final Map<TimeRange, List<OHLCV>> priceDataByRange;
        private final List<DemeanDiffOCMasterData> newlyCalculatedData;
        
        public MissingDataCalculationPipeline(MasterDataPreparationPipeline parent) {
            this.parent = parent;
            this.priceDataByRange = new HashMap<>();
            this.newlyCalculatedData = new ArrayList<>();
        }
        
        /**
         * Sub-step 1: Identify exactly which time ranges are missing master data
         */
        public MissingDataCalculationPipeline identifyMissingRanges() {
            log.debug("Sub-pipeline 1: Identifying missing master data ranges for {} (execution: {})", 
                parent.instrument.getCode(), parent.executionId);
            
            // Get existing master data time coverage
            Set<LocalDate> existingDates = parent.masterData.stream()
                .map(data -> data.timestamp().atOffset(java.time.ZoneOffset.UTC).toLocalDate())
                .collect(Collectors.toSet());
            
            // Calculate required date range
            LocalDate rangeStart = parent.historicalDataRange.from().atOffset(java.time.ZoneOffset.UTC).toLocalDate();
            LocalDate rangeEnd = parent.historicalDataRange.to().atOffset(java.time.ZoneOffset.UTC).toLocalDate().minusDays(1);
            
            log.debug("Sub-pipeline 1: Adjusted rangeEnd from {} to {} (minus 1 day) because we need historical master data for prediction, not prediction date itself (execution: {})",
                parent.historicalDataRange.to().atOffset(java.time.ZoneOffset.UTC).toLocalDate(), rangeEnd, parent.executionId);
            
            // Find missing date ranges
            this.missingRanges = findMissingDateRanges(existingDates, rangeStart, rangeEnd);
            
            log.info("Sub-pipeline 1: Found {} missing time ranges for {} (execution: {})", 
                missingRanges.size(), parent.instrument.getCode(), parent.executionId);
            // Log detailed list of missing date ranges for debugging
            if (!missingRanges.isEmpty()) {
                for (int i = 0; i < missingRanges.size(); i++) {
                    TimeRange range = missingRanges.get(i);
                    LocalDate missingFromDate = range.from().atOffset(java.time.ZoneOffset.UTC).toLocalDate();
                    LocalDate missingToDate = range.to().atOffset(java.time.ZoneOffset.UTC).toLocalDate();
                    long dayCount = java.time.temporal.ChronoUnit.DAYS.between(missingFromDate, missingToDate) + 1;
                    
                    log.info("Sub-pipeline 1: Missing range {}: {} to {} ({} days) (execution: {})", 
                        i + 1, missingFromDate, missingToDate, dayCount, parent.executionId);
                }
            } else {
                log.debug("Sub-pipeline 1: No missing date ranges found - all required data already exists (execution: {})", 
                    parent.executionId);
            }
            
            return this;
        }
        
        /**
         * Sub-step 2: Validate that sufficient price data exists for missing ranges
         * IMPORTANT: For DiffOC calculation, we need price data starting from one day BEFORE the missing range
         * to calculate the first difference: DiffOC(day_1) = OC(day_1) - OC(day_0)
         */
        public MissingDataCalculationPipeline validatePriceDataSufficiency() {
            log.debug("Sub-pipeline 2: Validating price data sufficiency for {} missing ranges (execution: {})", 
                missingRanges.size(), parent.executionId);
            
            for (TimeRange missingRange : missingRanges) {
                // Expand the range to include one day before for DiffOC calculation
                Instant expandedFrom = missingRange.from().minus(1, java.time.temporal.ChronoUnit.DAYS);
                TimeRange expandedRange = new TimeRange(expandedFrom, missingRange.to());
                
                LocalDate missingFromDate = missingRange.from().atOffset(java.time.ZoneOffset.UTC).toLocalDate();
                LocalDate missingToDate = missingRange.to().atOffset(java.time.ZoneOffset.UTC).toLocalDate();
                LocalDate expandedFromDate = expandedRange.from().atOffset(java.time.ZoneOffset.UTC).toLocalDate();
                LocalDate expandedToDate = expandedRange.to().atOffset(java.time.ZoneOffset.UTC).toLocalDate();
                
                log.info("Sub-pipeline 2: Processing missing range {} to {} (execution: {})", 
                    missingFromDate, missingToDate, parent.executionId);
                log.info("Sub-pipeline 2: Expanded price data fetch range from {} to {} (requesting {} days) (execution: {})", 
                    expandedFromDate, expandedToDate, 
                    java.time.temporal.ChronoUnit.DAYS.between(expandedFromDate, expandedToDate) + 1, 
                    parent.executionId);
                
                List<OHLCV> priceData = marketDataPort.getHistoricalData(parent.instrument, expandedRange);
                
                if (priceData.isEmpty()) {
                    log.warn("Sub-pipeline 2: No price data available for expanded range {} - {} (execution: {})", 
                        expandedFromDate, expandedToDate, parent.executionId);
                    // Store empty list to indicate this range needs price data fetching
                    priceDataByRange.put(missingRange, new ArrayList<>());
                } else {
                    // Validate that we have sufficient data for DiffOC calculation
                    long expectedDays = java.time.temporal.ChronoUnit.DAYS.between(
                        expandedRange.from(), expandedRange.to()) + 1;
                    
                    // Log detailed information about the fetched price data
                    List<LocalDate> fetchedDates = priceData.stream()
                        .map(ohlcv -> ohlcv.timestamp().atOffset(java.time.ZoneOffset.UTC).toLocalDate())
                        .sorted()
                        .distinct()
                        .toList();
                    
                    log.info("Sub-pipeline 2: Fetched {} price data points for expanded range {} - {}, dates: {} to {} (execution: {})", 
                        priceData.size(), expandedFromDate, expandedToDate,
                        fetchedDates.isEmpty() ? "none" : fetchedDates.getFirst(),
                        fetchedDates.isEmpty() ? "none" : fetchedDates.getLast(),
                        parent.executionId);
                    
                    if (priceData.size() < expectedDays) { // Need exact number of days for DiffOC calculation
                        // Identify which specific dates are missing
                        Set<LocalDate> expectedDates = expandedFromDate.datesUntil(expandedToDate.plusDays(1))
                            .collect(Collectors.toSet());
                        Set<LocalDate> actualDates = fetchedDates.stream().collect(Collectors.toSet());
                        Set<LocalDate> missingDates = expectedDates.stream()
                            .filter(date -> !actualDates.contains(date))
                            .collect(Collectors.toSet());
                        
                        log.warn("Sub-pipeline 2: Insufficient price data - found {} points, expected ~{} for expanded range {} - {}. Missing dates: {} (execution: {})", 
                            priceData.size(), expectedDays, expandedFromDate, expandedToDate, 
                            missingDates.stream().sorted().toList(), parent.executionId);
                        priceDataByRange.put(missingRange, new ArrayList<>()); // Mark as needing fetch
                    } else {
                        log.info("Sub-pipeline 2: Found {} price data points for expanded range {} - {} (execution: {})", 
                            priceData.size(), expandedFromDate, expandedToDate, parent.executionId);
                        
                        // Log the specific timestamps in the price data for debugging
                        List<LocalDate> priceDataDates = priceData.stream()
                            .map(ohlcv -> ohlcv.timestamp().atOffset(java.time.ZoneOffset.UTC).toLocalDate())
                            .sorted()
                            .distinct()
                            .toList();
                        log.info("Sub-pipeline 2: Price data dates found: {} (execution: {})", 
                            priceDataDates, parent.executionId);
                        
                        priceDataByRange.put(missingRange, priceData);
                    }
                }
            }
            
            return this;
        }
        
        /**
         * Sub-step 3: Fetch missing price data if needed
         */
        public MissingDataCalculationPipeline fetchMissingPriceData() {
            List<TimeRange> rangesNeedingPriceFetch = priceDataByRange.entrySet().stream()
                .filter(entry -> entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .toList();
            
            if (rangesNeedingPriceFetch.isEmpty()) {
                log.debug("Sub-pipeline 3: All missing ranges have sufficient price data (execution: {})", 
                    parent.executionId);
                return this;
            }
            
            log.info("Sub-pipeline 3: Need to fetch price data for {} ranges (execution: {})", 
                rangesNeedingPriceFetch.size(), parent.executionId);
            
            for (TimeRange range : rangesNeedingPriceFetch) {
                // Expand the range to include one day before for DiffOC calculation
                Instant expandedFrom = range.from().minus(1, java.time.temporal.ChronoUnit.DAYS);
                TimeRange expandedRange = new TimeRange(expandedFrom, range.to());
                
                LocalDate originalFromDate = range.from().atOffset(java.time.ZoneOffset.UTC).toLocalDate();
                LocalDate originalToDate = range.to().atOffset(java.time.ZoneOffset.UTC).toLocalDate();
                LocalDate expandedFromDate = expandedRange.from().atOffset(java.time.ZoneOffset.UTC).toLocalDate();
                LocalDate expandedToDate = expandedRange.to().atOffset(java.time.ZoneOffset.UTC).toLocalDate();
                
                log.info("Sub-pipeline 3: Missing price data detected for {} - attempting to fetch {} days from {} to {} (original range: {} to {}) (execution: {})",
                    parent.instrument.getCode(),
                    java.time.temporal.ChronoUnit.DAYS.between(expandedFromDate, expandedToDate) + 1,
                    expandedFromDate, expandedToDate, originalFromDate, originalToDate, parent.executionId);
                
                // Attempt to fetch missing price data from external source
                boolean fetchSuccess = fetchMissingPriceDataFromExternalSource(expandedRange, range, parent.executionId);
                
                if (!fetchSuccess) {
                    throw new IllegalStateException(
                        String.format("Failed to fetch missing price data for %s: need %d days from %s to %s (original range: %s to %s). External data fetch failed. (execution: %s)", 
                            parent.instrument.getCode(),
                            java.time.temporal.ChronoUnit.DAYS.between(expandedFromDate, expandedToDate) + 1,
                            expandedFromDate, expandedToDate, originalFromDate, originalToDate, parent.executionId));
                }
            }
            
            return this;
        }
        
        /**
         * Sub-step 4: Calculate master data only for missing parts
         */
        public MissingDataCalculationPipeline calculateOnlyMissingParts() {
            log.debug("Sub-pipeline 4: Calculating master data for {} missing ranges (execution: {})", 
                missingRanges.size(), parent.executionId);
            
            for (TimeRange missingRange : missingRanges) {
                List<OHLCV> priceData = priceDataByRange.get(missingRange);
                
                if (priceData != null && !priceData.isEmpty()) {
                    log.debug("Sub-pipeline 4: Calculating master data for range {} - {} with {} price points (execution: {})", 
                        missingRange.from(), missingRange.to(), priceData.size(), parent.executionId);
                    
                    // Calculate only the missing master data points efficiently
                    List<DemeanDiffOCMasterData> missingMasterData = calculateMissingMasterDataOnly(
                        missingRange, priceData, parent.arimaModel, parent.executionId);
                    
                    newlyCalculatedData.addAll(missingMasterData);
                    
                    log.info("Sub-pipeline 4: Calculated {} master data points for original range {} - {} (execution: {})", 
                        missingMasterData.size(), missingRange.from(), missingRange.to(), parent.executionId);
                } else {
                    log.warn("Sub-pipeline 4: Skipping range {} - {} due to insufficient price data (execution: {})", 
                        missingRange.from(), missingRange.to(), parent.executionId);
                }
            }
            
            return this;
        }
        
        /**
         * Sub-step 5: Merge newly calculated data with existing master data
         */
        public void mergeMasterData() {
            log.debug("Sub-pipeline 5: Merging {} newly calculated points with {} existing points (execution: {})", 
                newlyCalculatedData.size(), parent.masterData.size(), parent.executionId);
            
            // Merge and sort by timestamp
            List<DemeanDiffOCMasterData> mergedData = new ArrayList<>(parent.masterData);
            mergedData.addAll(newlyCalculatedData);
            
            // Sort by timestamp to maintain chronological order
            mergedData.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));
            
            // Smart duplicate removal: prefer newly calculated data with differences over existing data without differences
            parent.masterData = mergedData.stream()
                .collect(Collectors.toMap(
                    DemeanDiffOCMasterData::timestamp,
                    data -> data,
                    (existing, replacement) -> {
                        // Prefer data with difference calculations (more complete)
                        if (replacement.hasDifferences() && !existing.hasDifferences()) {
                            log.debug("Sub-pipeline 5: Replacing existing data at {} with newly calculated data (has differences) (execution: {})", 
                                existing.timestamp(), parent.executionId);
                            return replacement;
                        } else if (existing.hasDifferences() && !replacement.hasDifferences()) {
                            log.debug("Sub-pipeline 5: Keeping existing data at {} (has differences) over replacement (execution: {})", 
                                existing.timestamp(), parent.executionId);
                            return existing;
                        } else {
                            // Both have same level of completeness, keep existing
                            log.debug("Sub-pipeline 5: Found duplicate timestamp {}, keeping existing data (execution: {})", 
                                existing.timestamp(), parent.executionId);
                            return existing;
                        }
                    }
                ))
                .values()
                .stream()
                .sorted((a, b) -> a.timestamp().compareTo(b.timestamp()))
                .toList();
            
            long originalSize = mergedData.size() - newlyCalculatedData.size(); // Original size before adding new data
            long actuallyMerged = parent.masterData.size();
            long expectedMerged = originalSize + newlyCalculatedData.size();
            
            if (actuallyMerged < expectedMerged) {
                log.info("Sub-pipeline 5: Merged data completed: {} total master data points (expected {}, {} duplicates removed) (execution: {})", 
                    actuallyMerged, expectedMerged, expectedMerged - actuallyMerged, parent.executionId);
            } else {
                log.info("Sub-pipeline 5: Merged data completed: {} total master data points (execution: {})", 
                    actuallyMerged, parent.executionId);
            }
        }
        
        /**
         * Simple calculation of missing master data points.
         * For each missing day: get previous OC, calculate current OC, compute DiffOC and demean.
         */
        private List<DemeanDiffOCMasterData> calculateMissingMasterDataOnly(
                TimeRange missingRange, List<OHLCV> priceData, ARIMAModel arimaModel, String executionId) {
            
            List<DemeanDiffOCMasterData> result = new ArrayList<>();
            AssetSpecificMasterDataRepository repository = repositoryFactory.getRepository(parent.instrument);
            
            // Load meanDiffOC from ARIMA model config instead of calculating
            BigDecimal meanDiffOC = arimaModel != null ? 
                BigDecimal.valueOf(arimaModel.getMeanDiffOC()) : 
                calculateMeanDiffOCFromExistingData(); // Fallback for backward compatibility
            
            log.debug("Sub-pipeline 4: Using meanDiffOC {} from {} (execution: {})", 
                meanDiffOC, arimaModel != null ? "ARIMA model config" : "calculated from existing data", executionId);
            
            // Create a map of price data by timestamp for efficient lookup
            Map<Instant, OHLCV> priceDataMap = priceData.stream()
                .collect(Collectors.toMap(OHLCV::timestamp, ohlcv -> ohlcv));
            
            // Loop through missing days (iterate by date, not by price data)
            LocalDate missingFromDate = missingRange.from().atOffset(java.time.ZoneOffset.UTC).toLocalDate();
            LocalDate missingToDate = missingRange.to().atOffset(java.time.ZoneOffset.UTC).toLocalDate();
            
            log.debug("Sub-pipeline 4: Iterating through missing days from {} to {} (execution: {})", 
                missingFromDate, missingToDate, executionId);
            
            for (LocalDate currentDate = missingFromDate; !currentDate.isAfter(missingToDate); currentDate = currentDate.plusDays(1)) {
                Instant currentTimestamp = currentDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
                
                // Step 1: Find price data for current missing day
                OHLCV currentPrice = priceDataMap.get(currentTimestamp);
                if (currentPrice == null) {
                    log.error("Sub-pipeline 4: No price data found for current master day {} (execution: {})",
                        currentDate, executionId);
                    throw new IllegalStateException(
                            String.format("Failed to get price data for %s: date %s, (execution: %s)",
                                    parent.instrument.getCode(), currentDate, parent.executionId));
                }
                
                // Step 2: Get previous day's OC
                Instant previousDay = currentTimestamp.minus(1, java.time.temporal.ChronoUnit.DAYS);
                OHLCV previousPrice = priceDataMap.get(previousDay);
                if (previousPrice == null) {
                    log.error("Sub-pipeline 4: No price data found for previous day {} (execution: {})",
                            previousDay, executionId);
                    throw new IllegalStateException(
                            String.format("Failed to get price data for %s: date %s, (execution: %s)",
                                    parent.instrument.getCode(), previousDay, parent.executionId));
                }
                BigDecimal previousOC = previousPrice.close().amount().subtract(previousPrice.open().amount());

                // Step 3: Calculate current day's OC
                BigDecimal currentOC = currentPrice.close().amount().subtract(currentPrice.open().amount());
                
                DemeanDiffOCMasterData newMasterData;
                
                if (!previousOC.equals(BigDecimal.ZERO)) {
                    // Step 4: Calculate DiffOC = Current OC - Previous OC
                    BigDecimal diffOC = currentOC.subtract(previousOC);
                    
                    // Step 5: Calculate DemeanDiffOC = DiffOC - Mean DiffOC
                    BigDecimal demeanDiffOC = diffOC.subtract(meanDiffOC);
                    
                    // Step 6: Create new master data with differences
                    newMasterData = DemeanDiffOCMasterData.withDifferences(
                        parent.instrument,
                        currentTimestamp,
                        currentPrice.open().amount(),
                        currentPrice.close().amount(),
                        previousOC,
                        meanDiffOC,
                        "v1.0.0"
                    );
                    
                    log.debug("Sub-pipeline 4: Calculated master data for {} (DiffOC: {}, DemeanDiffOC: {}) (execution: {})", 
                        currentDate, diffOC, demeanDiffOC, executionId);
                } else {
                    // No previous data - create initial master data (no differences)
                    newMasterData = DemeanDiffOCMasterData.initial(
                        parent.instrument,
                        currentTimestamp,
                        currentPrice.open().amount(),
                        currentPrice.close().amount(),
                        meanDiffOC,
                        "v1.0.0"
                    );
                    
                    log.debug("Sub-pipeline 4: Created initial master data for {} (no previous data) (execution: {})", 
                        currentDate, executionId);
                }
                
                // Step 7: Save and collect result
                DemeanDiffOCMasterData savedData = repository.save(newMasterData);
                result.add(savedData);
            }
            
            return result;
        }
        
        /**
         * Calculates mean DiffOC from existing master data (more efficient than full recalculation)
         */
        private BigDecimal calculateMeanDiffOCFromExistingData() {
            if (parent.masterData.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            // Use existing master data to calculate mean DiffOC
            List<BigDecimal> diffOCValues = parent.masterData.stream()
                .filter(DemeanDiffOCMasterData::hasDifferences)
                .map(data -> data.diffOC())
                .toList();
            
            if (diffOCValues.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            BigDecimal sum = diffOCValues.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            return sum.divide(BigDecimal.valueOf(diffOCValues.size()), 8, RoundingMode.HALF_UP);
        }
        
        /**
         * Helper method to find missing date ranges between existing data coverage
         */
        private List<TimeRange> findMissingDateRanges(Set<LocalDate> existingDates, LocalDate rangeStart, LocalDate rangeEnd) {
            List<TimeRange> missingRanges = new ArrayList<>();
            LocalDate current = rangeStart;
            LocalDate gapStart = null;
            
            while (!current.isAfter(rangeEnd)) {
                if (!existingDates.contains(current)) {
                    // Start of a gap
                    if (gapStart == null) {
                        gapStart = current;
                    }
                } else {
                    // End of a gap
                    if (gapStart != null) {
                        missingRanges.add(new TimeRange(
                            gapStart.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                            current.minusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC)
                        ));
                        gapStart = null;
                    }
                }
                current = current.plusDays(1);
            }
            
            // Handle gap that extends to the end
            if (gapStart != null) {
                missingRanges.add(new TimeRange(
                    gapStart.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                    rangeEnd.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)
                ));
            }
            
            return missingRanges;
        }
        
        /**
         * Attempts to fetch missing price data from external source using FetchHistoricalDataUseCase
         * 
         * @param expandedRange The expanded range that includes one day before for DiffOC calculation  
         * @param originalRange The original missing master data range
         * @param executionId Execution ID for tracking
         * @return true if fetch was successful and data is now available, false otherwise
         */
        private boolean fetchMissingPriceDataFromExternalSource(TimeRange expandedRange, TimeRange originalRange, String executionId) {
            try {
                log.info("Sub-pipeline 3: Attempting to fetch price data from external source for {} - range {} to {} (execution: {})",
                    parent.instrument.getCode(), 
                    expandedRange.from().atOffset(java.time.ZoneOffset.UTC).toLocalDate(),
                    expandedRange.to().atOffset(java.time.ZoneOffset.UTC).toLocalDate(), 
                    executionId);
                
                // Use MarketDataPort to fetch missing historical data
                Boolean fetchResult = marketDataPort.fetchMissingHistoricalData(parent.instrument, expandedRange, executionId).join();
                
                if (!fetchResult) {
                    log.error("Sub-pipeline 3: External data fetch failed for {} - range {} (execution: {})",
                        parent.instrument.getCode(), expandedRange, executionId);
                    return false;
                }
                
                // Re-fetch the price data to verify it's now available
                List<OHLCV> fetchedData = marketDataPort.getHistoricalData(parent.instrument, expandedRange);
                if (!fetchedData.isEmpty()) {
                    log.info("Sub-pipeline 3: Verified - {} price data points now available in local storage for {} (execution: {})",
                        fetchedData.size(), parent.instrument.getCode(), executionId);
                    
                    // Store the full expanded data for calculation (needed for DiffOC calculation)
                    // but mark it clearly that this includes expanded range data
                    priceDataByRange.put(originalRange, fetchedData);
                    return true;
                } else {
                    log.warn("Sub-pipeline 3: External fetch reported success but no data found in local storage for {} (execution: {})",
                        parent.instrument.getCode(), executionId);
                    return false;
                }
                
            } catch (Exception e) {
                log.error("Sub-pipeline 3: Exception during external price data fetch for {} - range {} to {} (execution: {}): {}",
                    parent.instrument.getCode(),
                    expandedRange.from().atOffset(java.time.ZoneOffset.UTC).toLocalDate(),
                    expandedRange.to().atOffset(java.time.ZoneOffset.UTC).toLocalDate(),
                    executionId, e.getMessage(), e);
                return false;
            }
        }
    }
}