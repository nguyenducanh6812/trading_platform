package com.ahd.trading_platform.marketdata.application.dto;

/**
 * Simple instrument information for UI combobox components.
 * Contains only essential data needed for instrument selection in portfolio creation.
 */
public record InstrumentInfo(
    String code,    // Instrument code (e.g., "BTC", "ETH")
    String name     // Full instrument name (e.g., "Bitcoin", "Ethereum")
) {
    /**
     * Factory method for creating instrument info from domain entity.
     */
    public static InstrumentInfo of(String code, String name) {
        return new InstrumentInfo(code, name);
    }
}
