package com.ahd.trading_platform.portfolio.application.dto;

import com.ahd.trading_platform.portfolio.domain.valueobjects.TradeType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Request DTO for executing a trade.
 */
public record ExecuteTradeRequest(
    @NotBlank(message = "Symbol is required")
    String symbol,

    @NotNull(message = "Trade type is required")
    TradeType tradeType,

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.00000001", message = "Quantity must be positive")
    BigDecimal quantity,

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be positive")
    BigDecimal price
) {}
