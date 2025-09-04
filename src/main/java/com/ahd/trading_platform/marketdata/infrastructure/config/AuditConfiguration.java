package com.ahd.trading_platform.marketdata.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Configuration for JPA auditing in the Market Data module.
 * Enables automatic population of audit fields in embedded AuditInfo objects.
 */
@Configuration
@EnableJpaAuditing
public class AuditConfiguration {
    
    /**
     * Provides the current auditor for Spring Data auditing.
     * In a real application, this would typically return the current user's ID.
     * For now, it returns a system identifier.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }
    
    private static class AuditorAwareImpl implements AuditorAware<String> {
        @Override
        public Optional<String> getCurrentAuditor() {
            // TODO: In production, get this from Spring Security context
            // SecurityContextHolder.getContext().getAuthentication().getName()
            return Optional.of("SYSTEM");
        }
    }
}