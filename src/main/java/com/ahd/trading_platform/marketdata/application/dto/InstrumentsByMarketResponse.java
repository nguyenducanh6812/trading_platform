package com.ahd.trading_platform.marketdata.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for instruments belonging to a specific market.
 * Provides instrument list for a selected market.
 */
@Schema(description = "Instruments for a specific market")
public record InstrumentsByMarketResponse(

    @Schema(description = "Market code", example = "LINEAR")
    String marketCode,

    @Schema(description = "Market name", example = "Linear Futures")
    String marketName,

    @Schema(description = "List of instruments in this market")
    List<InstrumentInfo> instruments,

    @Schema(description = "Total number of instruments", example = "2")
    int totalInstruments
) {

    /**
     * Simplified instrument information for UI combobox display
     */
    @Schema(description = "Instrument information for combobox")
    public record InstrumentInfo(

        @Schema(description = "Instrument symbol", example = "BTCUSDT")
        String symbol,

        @Schema(description = "Contract type", example = "LinearPerpetual")
        String contractType
    ) {}

    /**
     * Factory method for creating response with instruments
     */
    public static InstrumentsByMarketResponse from(
        String marketCode,
        String marketName,
        List<com.ahd.trading_platform.marketdata.domain.entities.MarketInstrument> instruments
    ) {
        List<InstrumentInfo> instrumentInfos = instruments.stream()
            .map(instrument -> new InstrumentInfo(
                instrument.getSymbol(),
                instrument.getContractType()
            ))
            .toList();

        return new InstrumentsByMarketResponse(
            marketCode,
            marketName,
            instrumentInfos,
            instrumentInfos.size()
        );
    }
}
