package com.ahd.trading_platform.marketdata.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Data Transfer Object for market data fetch requests.
 * Used for communication between external systems and the application layer.
 */
public record MarketDataRequest(
    @NotEmpty(message = "Instruments list cannot be empty")
    List<@NotBlank(message = "Instrument symbol cannot be blank") String> instruments,
    
    @NotNull(message = "From date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate fromDate,
    
    @NotNull(message = "To date is required") 
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate toDate,
    
    String source
) {
    
    public MarketDataRequest {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From date must be before or equal to to date");
        }
    }
    
    /**
     * Creates a request for historical data (March 15, 2021 to current date)
     */
    public static MarketDataRequest forHistoricalData(List<String> instruments) {
        return new MarketDataRequest(
            instruments, 
            LocalDate.of(2021, 3, 15), 
            LocalDate.now(),
            "EXTERNAL_API"
        );
    }
    
    /**
     * Creates a request for BTC and ETH historical data
     */
    public static MarketDataRequest forBtcEthHistorical() {
        return forHistoricalData(List.of("BTC", "ETH"));
    }
}