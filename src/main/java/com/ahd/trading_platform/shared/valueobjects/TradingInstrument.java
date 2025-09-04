package com.ahd.trading_platform.shared.valueobjects;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shared kernel value object representing supported trading instruments.
 * This is a shared concept across multiple bounded contexts (Market Data, Forecasting, Analytics).
 * Provides type safety and validation for instrument codes.
 */
@Getter
public enum TradingInstrument {
    BTC("BTC", "Bitcoin", "BTC", "USD"),
    ETH("ETH", "Ethereum", "ETH", "USD");
    
    private final String code;
    private final String name;
    private final String baseCurrency;
    private final String quoteCurrency;
    
    TradingInstrument(String code, String name, String baseCurrency, String quoteCurrency) {
        this.code = code;
        this.name = name;
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
    }

    /**
     * Converts string code to enum, with validation
     */
    public static TradingInstrument fromCode(String code) {
        return Arrays.stream(values())
            .filter(instrument -> instrument.getCode().equalsIgnoreCase(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported trading instrument: " + code));
    }
    
    /**
     * Validates if a code is supported
     */
    public static boolean isSupported(String code) {
        return Arrays.stream(values())
            .anyMatch(instrument -> instrument.getCode().equalsIgnoreCase(code));
    }
    
    /**
     * Returns list of supported codes
     */
    public static List<String> getSupportedCodes() {
        return Arrays.stream(values())
            .map(TradingInstrument::getCode)
            .collect(Collectors.toList());
    }
    
    /**
     * Converts list of string codes to list of enums with validation
     */
    public static List<TradingInstrument> fromCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            throw new IllegalArgumentException("Instrument codes cannot be null or empty");
        }
        
        return codes.stream()
            .map(TradingInstrument::fromCode)
            .collect(Collectors.toList());
    }
    
    /**
     * Returns a display name for the instrument
     */
    public String getDisplayName() {
        return String.format("%s (%s)", name, code);
    }
    
    /**
     * Returns the trading pair name
     */
    public String getTradingPair() {
        return String.format("%s/%s", baseCurrency, quoteCurrency);
    }
}