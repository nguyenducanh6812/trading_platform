package com.ahd.trading_platform.forecasting.application.dto;

import com.ahd.trading_platform.forecasting.domain.valueobjects.ForecastStep;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * DTO representing a single calculation step in the ARIMA process
 */
public record CalculationStepDto(
    
    @Schema(description = "Forecast step identifier")
    ForecastStep step,
    
    @Schema(description = "Timestamp for this calculation point")
    Instant timestamp,
    
    @Schema(description = "Original open price")
    double openPrice,
    
    @Schema(description = "Original close price")
    double closePrice,
    
    @Schema(description = "Calculated OC (Open - Close)")
    double oc,
    
    @Schema(description = "Calculated Diff_OC")
    Double diffOC,
    
    @Schema(description = "Calculated Demean_Diff_OC")
    Double demeanDiffOC,
    
    @Schema(description = "AR lag values")
    List<Double> arLags,
    
    @Schema(description = "Predicted difference")
    Double predictedDiffOC,
    
    @Schema(description = "Predicted OC value")
    Double predictedOC,
    
    @Schema(description = "Final predicted return")
    Double predictedReturn
    
) {}