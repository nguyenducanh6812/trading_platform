package com.ahd.trading_platform.backtesting;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the backtesting module.
 * Configures beans and module-specific properties.
 */
@Configuration
@ComponentScan(basePackages = "com.ahd.trading_platform.backtesting")
@Slf4j
public class BacktestingConfiguration {
    
    @Bean
    @ConfigurationProperties(prefix = "trading-platform.backtesting")
    public BacktestingProperties backtestingProperties() {
        return new BacktestingProperties();
    }
    
    /**
     * Configuration properties for backtesting module
     */
    public static class BacktestingProperties {
        
        /**
         * Minimum backtest period in days (default: 30)
         */
        private int minimumBacktestPeriodDays = 30;
        
        /**
         * Minimum prediction coverage percentage for validation (default: 0.8 = 80%)
         */
        private double minimumPredictionCoverage = 0.8;
        
        /**
         * Maximum backtest period in days to prevent excessive resource usage (default: 1825 = 5 years)
         */
        private int maximumBacktestPeriodDays = 1825;
        
        public int getMinimumBacktestPeriodDays() {
            return minimumBacktestPeriodDays;
        }
        
        public void setMinimumBacktestPeriodDays(int minimumBacktestPeriodDays) {
            this.minimumBacktestPeriodDays = minimumBacktestPeriodDays;
        }
        
        public double getMinimumPredictionCoverage() {
            return minimumPredictionCoverage;
        }
        
        public void setMinimumPredictionCoverage(double minimumPredictionCoverage) {
            this.minimumPredictionCoverage = minimumPredictionCoverage;
        }
        
        public int getMaximumBacktestPeriodDays() {
            return maximumBacktestPeriodDays;
        }
        
        public void setMaximumBacktestPeriodDays(int maximumBacktestPeriodDays) {
            this.maximumBacktestPeriodDays = maximumBacktestPeriodDays;
        }
    }
}