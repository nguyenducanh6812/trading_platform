package com.ahd.trading_platform.marketdata.domain.valueobjects;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enum representing supported trading instruments.
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
     * Converts list of string codes to enums with validation
     */
    public static List<TradingInstrument> fromCodes(List<String> codes) {
        return codes.stream()
            .map(TradingInstrument::fromCode)
            .collect(Collectors.toList());
    }
}