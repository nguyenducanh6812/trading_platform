package com.ahd.trading_platform.marketdata.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for market information.
 * Provides market details for UI display and selection.
 */
@Schema(description = "Market information")
public record MarketResponse(

    @Schema(description = "Market unique identifier", example = "1")
    Long id,

    @Schema(description = "Market code", example = "LINEAR")
    String code,

    @Schema(description = "Market name", example = "Linear Futures")
    String name,

    @Schema(description = "Market description", example = "USDT-margined perpetual contracts")
    String description
) {

    /**
     * Factory method for creating response from domain entity
     */
    public static MarketResponse from(com.ahd.trading_platform.marketdata.domain.entities.Market market) {
        return new MarketResponse(
            market.getId(),
            market.getCode(),
            market.getName(),
            market.getDescription()
        );
    }
}
