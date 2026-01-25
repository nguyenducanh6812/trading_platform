package com.ahd.trading_platform.marketdata.domain.entities;

import lombok.Getter;
import org.springframework.modulith.NamedInterface;

import java.util.Objects;

/**
 * Market entity representing a trading market category (e.g., Spot, Linear Futures, etc.).
 * This is a reference data entity that defines the available markets where instruments can be traded.
 * Exposed to other modules through the domain-api interface.
 */
@NamedInterface("domain-api")
public class Market {

    @Getter
    private final Long id;

    @Getter
    private final String code;

    @Getter
    private final String name;

    @Getter
    private final String description;

    /**
     * Full constructor for reconstructing Market from persistence
     */
    public Market(Long id, String code, String name, String description) {
        this.id = id;
        this.code = Objects.requireNonNull(code, "Market code cannot be null");
        this.name = Objects.requireNonNull(name, "Market name cannot be null");
        this.description = description;
    }

    /**
     * Factory method for creating new Market (before persistence)
     */
    public static Market create(String code, String name, String description) {
        return new Market(null, code, name, description);
    }

    /**
     * Factory method for Spot market
     */
    public static Market spot() {
        return create("SPOT", "Spot Trading", "Regular spot trading pairs");
    }

    /**
     * Factory method for Linear Futures market
     */
    public static Market linear() {
        return create("LINEAR", "Linear Futures", "USDT-margined perpetual contracts");
    }

    /**
     * Factory method for Inverse Futures market
     */
    public static Market inverse() {
        return create("INVERSE", "Inverse Futures", "Coin-margined perpetual contracts");
    }

    /**
     * Factory method for Options market
     */
    public static Market option() {
        return create("OPTION", "Options", "Options contracts");
    }

    /**
     * Checks if this market is for futures trading
     */
    public boolean isFutures() {
        return "LINEAR".equals(code) || "INVERSE".equals(code);
    }

    /**
     * Checks if this market is for spot trading
     */
    public boolean isSpot() {
        return "SPOT".equals(code);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Market market = (Market) obj;
        return Objects.equals(code, market.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return String.format("Market[code=%s, name=%s]", code, name);
    }
}
