package com.ahd.trading_platform.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Configuration for primary datasource (trading_platform database).
 *
 * This datasource is used by:
 * - JPA/Hibernate for business entities (market data, forecasts, predictions)
 * - Liquibase for database migrations
 * - Spring Modulith for event publication
 *
 * Marked as @Primary to ensure JPA and Liquibase use this datasource
 * instead of the Camunda datasource.
 */
@Configuration
public class PrimaryDataSourceConfiguration {

    /**
     * Loads primary datasource properties from application.yaml.
     *
     * Binds properties from:
     * - spring.datasource.url
     * - spring.datasource.username
     * - spring.datasource.password
     * - spring.datasource.driver-class-name
     *
     * @return DataSourceProperties configured from YAML
     */
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Creates the primary HikariCP datasource for business data.
     *
     * Uses properties loaded from application.yaml and applies HikariCP-specific
     * configuration from spring.datasource.hikari prefix.
     *
     * Marked as @Primary to be the default datasource for JPA and Liquibase.
     *
     * @return Configured HikariDataSource for trading_platform database
     */
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource() {
        return primaryDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }
}
