package com.ahd.trading_platform.marketdata.infrastructure.config;

import com.ahd.trading_platform.marketdata.domain.services.DataValidationService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Spring configuration for the Market Data module.
 * Defines beans and configuration for the entire module.
 */
@Configuration
@EnableConfigurationProperties(MarketDataProperties.class)
public class MarketDataConfiguration {
    
    /**
     * WebClient bean for external API calls
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(16 * 1024 * 1024)) // 16MB buffer
            .defaultHeader("User-Agent", "TradingPlatform/1.0")
            .defaultHeader("Accept", "application/json")
            .defaultHeader("Content-Type", "application/json");
    }
    
    /**
     * Data validation service bean
     */
    @Bean
    public DataValidationService dataValidationService() {
        return new DataValidationService();
    }
}