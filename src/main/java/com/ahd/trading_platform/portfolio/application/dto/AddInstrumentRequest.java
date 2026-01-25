package com.ahd.trading_platform.portfolio.application.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Request DTO for adding symbol to portfolio.
 */
public record AddInstrumentRequest(
    @NotBlank(message = "Symbol is required")
    String symbol,

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.00000001", message = "Quantity must be positive")
    BigDecimal quantity,

    @NotNull(message = "Entry price is required")
    @DecimalMin(value = "0.01", message = "Entry price must be positive")
    BigDecimal entryPrice
) {}
