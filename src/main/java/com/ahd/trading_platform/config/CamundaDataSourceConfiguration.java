package com.ahd.trading_platform.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Camunda BPM separate datasource.
 * <p>
 * Separates Camunda workflow engine database from business data database:
 * - Camunda database: Workflow state, history, and engine tables (act_*)
 * - Trading platform database: Business data (market data, forecasts, predictions)
 * <p>
 * This separation follows microservices best practices for:
 * - Independent scaling
 * - Different backup/retention policies
 * - Clear separation of concerns
 * <p>
 * Configuration is loaded from application.yaml under camunda.bpm.datasource prefix.
 */
@Configuration
public class CamundaDataSourceConfiguration {

    /**
     * Loads Camunda datasource properties from application.yaml.
     * <p>
     * Binds properties from:
     * - camunda.bpm.datasource.url
     * - camunda.bpm.datasource.username
     * - camunda.bpm.datasource.password
     * - camunda.bpm.datasource.driver-class-name
     *
     * @return DataSourceProperties configured from YAML
     */
    @Bean
    @ConfigurationProperties("camunda.bpm.datasource")
    public DataSourceProperties camundaDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Creates a separate HikariCP datasource for Camunda BPM.
     * <p>
     * Uses properties loaded from application.yaml and applies HikariCP-specific
     * configuration from camunda.bpm.datasource.hikari prefix.
     * <p>
     * Bean name "camundaBpmDataSource" is recognized by Camunda Spring Boot Starter
     * and will be used instead of the primary datasource.
     *
     * @return Configured HikariDataSource for Camunda
     */
    @Bean(name = "camundaBpmDataSource")
    @Qualifier("camundaBpmDataSource")
    @ConfigurationProperties("camunda.bpm.datasource.hikari")
    public HikariDataSource camundaDataSource() {
        HikariDataSource dataSource = camundaDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        dataSource.setPoolName("CamundaHikariPool");
        return dataSource;
    }
}
