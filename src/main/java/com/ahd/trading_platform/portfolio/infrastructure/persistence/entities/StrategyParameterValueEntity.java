package com.ahd.trading_platform.portfolio.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity for strategy parameter values.
 * Stores the actual parameter values configured by users for their portfolio strategies.
 */
@Entity
@Table(name = "strategy_parameter_values", indexes = {
    @Index(name = "idx_param_value_config_id", columnList = "configuration_id"),
    @Index(name = "idx_param_value_param_code", columnList = "parameter_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StrategyParameterValueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "configuration_id", nullable = false)
    private Long configurationId;

    @Column(name = "parameter_code", nullable = false, length = 50)
    private String parameterCode;

    @Column(name = "parameter_value", nullable = false, length = 200)
    private String parameterValue;
}
