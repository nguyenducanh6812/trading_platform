package com.ahd.trading_platform.marketdata;

import com.ahd.trading_platform.marketdata.infrastructure.external.BybitDataClientStrategy;
import com.ahd.trading_platform.marketdata.infrastructure.external.BybitFeignClient;
import com.ahd.trading_platform.marketdata.domain.services.ExternalDataClientStrategy;
import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.TimeRange;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Bybit API client.
 * 
 * These tests are disabled by default as they make real API calls.
 * Enable them for manual testing during development.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "market-data.external.provider=bybit",
    "logging.level.com.ahd.trading_platform.marketdata=DEBUG"
})
@Slf4j
class BybitIntegrationTest {
    
    @MockBean
    private BybitFeignClient mockBybitClient; // Mock for unit tests
    
    @Test
    @Disabled("Integration test - enable for manual testing")
    void testFetchHistoricalData() {
        // This would require actual Bybit client
        ExternalDataClientStrategy client = createRealBybitClient();
        
        LocalDate fromDate = LocalDate.of(2024, 8, 1);
        LocalDate toDate = LocalDate.of(2024, 8, 31);
        TimeRange timeRange = TimeRange.fromDates(fromDate, toDate);
        
        List<OHLCV> btcData = client.fetchHistoricalData("BTC", timeRange);
        
        assertThat(btcData).isNotEmpty();
        assertThat(btcData.size()).isGreaterThan(20); // August has ~31 days
        
        // Verify data quality
        OHLCV firstDataPoint = btcData.get(0);
        assertThat(firstDataPoint.open().amount()).isPositive();
        assertThat(firstDataPoint.high().amount()).isGreaterThanOrEqualTo(firstDataPoint.low().amount());
        assertThat(firstDataPoint.close().amount()).isPositive();
        assertThat(firstDataPoint.volume()).isPositive();
        // Note: OHLCV doesn't have symbol field, it's implicit in the request
        
        log.info("✅ Successfully fetched {} BTC data points", btcData.size());
        log.info("First data point: {}", firstDataPoint);
        log.info("Last data point: {}", btcData.get(btcData.size() - 1));
    }
    
    @Test
    @Disabled("Integration test - enable for manual testing")
    void testGetCurrentPrice() {
        ExternalDataClientStrategy client = createRealBybitClient();
        
        OHLCV btcData = client.fetchLatestData("BTC");
        OHLCV ethData = client.fetchLatestData("ETH");
        
        assertThat(btcData.close().amount()).isPositive();
        assertThat(ethData.close().amount()).isPositive();
        
        // BTC should be more expensive than ETH
        assertThat(btcData.close().amount()).isGreaterThan(ethData.close().amount());
        
        BigDecimal btcPrice = btcData.close().amount();
        BigDecimal ethPrice = ethData.close().amount();
        
        log.info("✅ Current BTC price: ${}", btcPrice);
        log.info("✅ Current ETH price: ${}", ethPrice);
    }
    
    @Test
    @Disabled("Integration test - enable for manual testing") 
    void testApiHealth() {
        ExternalDataClientStrategy client = createRealBybitClient();
        
        boolean isHealthy = client.isHealthy();
        
        assertThat(isHealthy).isTrue();
        log.info("✅ Bybit API is healthy");
    }
    
    @Test
    @Disabled("Integration test - enable for manual testing")
    void testGetSupportedSymbols() {
        ExternalDataClientStrategy client = createRealBybitClient();
        
        List<String> symbols = client.getSupportedSymbols();
        
        assertThat(symbols).contains("BTC", "ETH");
        assertThat(symbols).hasSizeGreaterThanOrEqualTo(2);
        
        log.info("✅ Supported symbols: {}", symbols);
    }
    
    /**
     * Creates a real Bybit client for integration testing.
     * In actual tests, this would be injected by Spring.
     */
    private ExternalDataClientStrategy createRealBybitClient() {
        // This is just a placeholder - in real tests, Spring would inject the bean
        // return new BybitDataClientStrategy(realBybitFeignClient, bybitMapper, rateLimiter);
        throw new UnsupportedOperationException("Use Spring injection in real tests");
    }
}