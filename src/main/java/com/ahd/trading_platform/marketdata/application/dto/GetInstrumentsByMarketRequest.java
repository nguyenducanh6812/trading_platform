package com.ahd.trading_platform.marketdata.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for getting instruments by market.
 * Frontend sends market details to avoid extra database query.
 *
 * @param marketId Market ID (primary key)
 * @param marketCode Market code (e.g., SPOT, LINEAR, INVERSE, OPTION)
 * @param marketName Market display name (e.g., Spot Trading, Linear Perpetual)
 */
public record GetInstrumentsByMarketRequest(
    @NotNull(message = "Market ID is required")
    Long marketId,

    @NotBlank(message = "Market code is required")
    String marketCode,

    @NotBlank(message = "Market name is required")
    String marketName
) {}
