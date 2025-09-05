package com.ahd.trading_platform.marketdata;

import com.ahd.trading_platform.marketdata.application.services.MarketDataApplicationService;
import com.ahd.trading_platform.marketdata.domain.entities.MarketInstrument;
import com.ahd.trading_platform.marketdata.domain.repositories.MarketDataRepository;
import com.ahd.trading_platform.shared.valueobjects.OHLCV;
import com.ahd.trading_platform.shared.valueobjects.Price;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the Market Data module.
 * Verifies that the complete module works correctly with Spring Boot context.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "logging.level.com.ahd.trading_platform.marketdata=DEBUG"
})
@Transactional
class MarketDataModuleIntegrationTest {
    
    @Autowired
    private MarketDataApplicationService applicationService;
    
    @Autowired
    private MarketDataRepository repository;
    
    @Test
    void shouldCreateAndPersistMarketInstrument() {
        // Given
        MarketInstrument btc = MarketInstrument.bitcoin();
        
        // Create some test OHLCV data
        OHLCV ohlcv1 = OHLCV.fromUsdValues(
            45000.0, 46000.0, 44000.0, 45500.0, 
            1000.0, Instant.now().minusSeconds(3600));
        OHLCV ohlcv2 = OHLCV.fromUsdValues(
            45500.0, 46500.0, 45000.0, 46000.0, 
            1200.0, Instant.now());
        
        btc.addPriceData(List.of(ohlcv1, ohlcv2));
        
        // When
        repository.save(btc);
        
        // Then
        Optional<MarketInstrument> savedInstrument = repository.findBySymbol("BTC");
        assertThat(savedInstrument).isPresent();
        assertThat(savedInstrument.get().getSymbol()).isEqualTo("BTC");
        assertThat(savedInstrument.get().getName()).isEqualTo("Bitcoin");
        assertThat(savedInstrument.get().getDataPointCount()).isEqualTo(2);
        assertThat(savedInstrument.get().hasSufficientData()).isFalse(); // Only 2 data points, needs 30+
    }
    
    @Test
    void shouldRetrieveAllMarketInstruments() {
        // Given
        MarketInstrument btc = MarketInstrument.bitcoin();
        MarketInstrument eth = MarketInstrument.ethereum();
        
        repository.save(btc);
        repository.save(eth);
        
        // When
        List<MarketInstrument> instruments = applicationService.getAllInstruments();
        
        // Then
        assertThat(instruments).hasSize(2);
        assertThat(instruments.stream().map(MarketInstrument::getSymbol))
            .containsExactlyInAnyOrder("BTC", "ETH");
    }
    
    @Test
    void shouldCalculateReturnsCorrectly() {
        // Given
        MarketInstrument btc = MarketInstrument.bitcoin();
        
        // Create test data with known return
        OHLCV day1 = OHLCV.fromUsdValues(
            40000.0, 41000.0, 39000.0, 40000.0, 
            1000.0, Instant.now().minusSeconds(86400));
        OHLCV day2 = OHLCV.fromUsdValues(
            40000.0, 42000.0, 40000.0, 42000.0, // 5% increase
            1200.0, Instant.now());
        
        btc.addPriceData(List.of(day1, day2));
        
        // When
        List<Double> returns = btc.calculateReturns();
        
        // Then
        assertThat(returns).hasSize(1);
        assertThat(returns.get(0)).isEqualTo(0.05); // 5% return
    }
    
    @Test
    void shouldValidatePriceObjectCorrectly() {
        // Given & When & Then
        Price price = Price.usd(45000.50);
        
        assertThat(price.amount()).isEqualTo(BigDecimal.valueOf(45000.50).setScale(8));
        assertThat(price.currency()).isEqualTo("USD");
        assertThat(price.isZero()).isFalse();
        
        Price addedPrice = price.add(Price.usd(1000.0));
        assertThat(addedPrice.amount().doubleValue()).isEqualTo(46000.50);
    }
    
    @Test
    void shouldCreateOHLCVWithValidation() {
        // Given & When
        OHLCV ohlcv = OHLCV.fromUsdValues(
            45000.0, 46000.0, 44000.0, 45500.0, 
            1000.0, Instant.now());
        
        // Then
        assertThat(ohlcv.open().amount().doubleValue()).isEqualTo(45000.0);
        assertThat(ohlcv.high().amount().doubleValue()).isEqualTo(46000.0);
        assertThat(ohlcv.low().amount().doubleValue()).isEqualTo(44000.0);
        assertThat(ohlcv.close().amount().doubleValue()).isEqualTo(45500.0);
        assertThat(ohlcv.volume().doubleValue()).isEqualTo(1000.0);
        assertThat(ohlcv.isBullish()).isTrue(); // close > open
        
        Price typical = ohlcv.getTypicalPrice();
        double expectedTypical = (46000.0 + 44000.0 + 45500.0) / 3.0;
        assertThat(typical.amount().doubleValue()).isCloseTo(expectedTypical, org.assertj.core.data.Offset.offset(0.001));
    }
}