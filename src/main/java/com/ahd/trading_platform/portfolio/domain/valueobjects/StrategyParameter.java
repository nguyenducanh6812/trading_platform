package com.ahd.trading_platform.portfolio.domain.valueobjects;

import lombok.Getter;

import java.util.Objects;

/**
 * Value object representing a strategy configuration parameter.
 * Defines the schema for strategy parameters that users can configure.
 */
@Getter
public class StrategyParameter {

    private final String code;
    private final String name;
    private final String dataType; // string, integer, double, boolean, enum
    private final String defaultValue;
    private final boolean required;
    private final String description;
    private final String validationRule; // Optional: min/max, regex, allowed values

    private StrategyParameter(
        String code,
        String name,
        String dataType,
        String defaultValue,
        boolean required,
        String description,
        String validationRule
    ) {
        this.code = Objects.requireNonNull(code, "Parameter code cannot be null");
        this.name = Objects.requireNonNull(name, "Parameter name cannot be null");
        this.dataType = Objects.requireNonNull(dataType, "Parameter dataType cannot be null");
        this.defaultValue = defaultValue;
        this.required = required;
        this.description = description;
        this.validationRule = validationRule;
    }

    /**
     * Factory: Required parameter
     */
    public static StrategyParameter required(
        String code,
        String name,
        String dataType,
        String defaultValue,
        String description
    ) {
        return new StrategyParameter(code, name, dataType, defaultValue, true, description, null);
    }

    /**
     * Factory: Optional parameter
     */
    public static StrategyParameter optional(
        String code,
        String name,
        String dataType,
        String defaultValue,
        String description
    ) {
        return new StrategyParameter(code, name, dataType, defaultValue, false, description, null);
    }

    /**
     * Factory: Parameter with validation rule
     */
    public static StrategyParameter withValidation(
        String code,
        String name,
        String dataType,
        String defaultValue,
        boolean required,
        String description,
        String validationRule
    ) {
        return new StrategyParameter(code, name, dataType, defaultValue, required, description, validationRule);
    }

    /**
     * Validates a parameter value against its data type and validation rules
     */
    public boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return !required; // null/empty is only valid for optional parameters
        }

        // Type validation
        try {
            switch (dataType.toLowerCase()) {
                case "integer" -> Integer.parseInt(value);
                case "double" -> Double.parseDouble(value);
                case "boolean" -> {
                    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                        return false;
                    }
                }
                case "string" -> {} // Any non-null string is valid
                default -> {
                    return false; // Unknown data type
                }
            }
        } catch (NumberFormatException e) {
            return false;
        }

        // TODO: Apply validation rules if present
        // e.g., "min:0,max:100" or "values:A,B,C"

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StrategyParameter that = (StrategyParameter) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return "StrategyParameter{" +
            "code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", dataType='" + dataType + '\'' +
            ", required=" + required +
            '}';
    }
}
