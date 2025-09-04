package com.ahd.trading_platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI configuration for the Trading Platform API.
 * 
 * Provides comprehensive API documentation for:
 * - Market Data endpoints
 * - Portfolio Management endpoints  
 * - Trading Strategy endpoints
 * - Risk Management endpoints
 * - Workflow/Camunda endpoints
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI tradingPlatformOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Trading Platform API")
                .description("""
                    Advanced Trading Platform with Modern Portfolio Theory (MPT) implementation.
                    
                    ## Features
                    - Real-time market data from Bybit
                    - Portfolio optimization using Sharpe ratio
                    - Risk-adjusted returns calculation
                    - ARIMA forecasting for price prediction
                    - Automated workflow orchestration via Camunda BPM
                    - Comprehensive audit trails
                    
                    ## Architecture
                    - Spring Boot 3.4+ with Modulith architecture
                    - Domain-Driven Design (DDD) principles
                    - Event-driven communication between modules
                    - High-performance data processing with MapStruct
                    """)
                .version("v1.0.0")
                .contact(new Contact()
                    .name("Trading Platform Team")
                    .email("support@tradingplatform.com")
                    .url("https://tradingplatform.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Development server"),
                new Server()
                    .url("https://api.tradingplatform.com")
                    .description("Production server")
            ));
    }
}