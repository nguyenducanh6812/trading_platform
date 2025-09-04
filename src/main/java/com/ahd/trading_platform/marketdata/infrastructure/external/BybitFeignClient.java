package com.ahd.trading_platform.marketdata.infrastructure.external;

import com.ahd.trading_platform.marketdata.infrastructure.external.dto.bybit.BybitKlineResponse;
import com.ahd.trading_platform.marketdata.infrastructure.external.dto.bybit.BybitTickerResponse;
import com.ahd.trading_platform.marketdata.infrastructure.external.dto.bybit.BybitInstrumentsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for Bybit API integration.
 * Provides access to Bybit's comprehensive cryptocurrency market data.
 * 
 * Bybit advantages:
 * - High-quality OHLCV data with actual open/high/low/close prices
 * - Real-time data with minimal latency
 * - Excellent API rate limits (120 requests/minute for public endpoints)
 * - Supports both spot and derivatives markets
 * - No API key required for public market data
 * - Professional trading platform data quality
 */
@FeignClient(
    name = "bybit-client",
    url = "${market-data.external.bybit.baseUrl:https://api.bybit.com}",
    configuration = BybitFeignConfig.class
)
public interface BybitFeignClient {
    
    /**
     * Get historical kline/candlestick data (OHLCV).
     * This provides actual OHLC data unlike some APIs that only provide closing prices.
     * 
     * @param category Product category: spot, linear, inverse, option
     * @param symbol Trading pair symbol (e.g., BTCUSDT, ETHUSDT)
     * @param interval Kline interval: 1,3,5,15,30,60,120,240,360,720,D,M,W
     * @param start Start timestamp in milliseconds
     * @param end End timestamp in milliseconds  
     * @param limit Number of data points to return (max 1000)
     * @return Kline/candlestick data response
     */
    @GetMapping("/v5/market/kline")
    BybitKlineResponse getKlineData(
        @RequestParam("category") String category,
        @RequestParam("symbol") String symbol,
        @RequestParam("interval") String interval,
        @RequestParam("start") long start,
        @RequestParam("end") long end,
        @RequestParam(value = "limit", defaultValue = "1000") int limit
    );
    
    /**
     * Get real-time ticker information including current price, 24h volume, etc.
     * 
     * @param category Product category: spot, linear, inverse, option
     * @param symbol Trading pair symbol (optional - if not provided, returns all symbols)
     * @return Ticker data response
     */
    @GetMapping("/v5/market/tickers")
    BybitTickerResponse getTickerInfo(
        @RequestParam("category") String category,
        @RequestParam(value = "symbol", required = false) String symbol
    );
    
    /**
     * Get information about trading instruments/symbols.
     * Useful for getting available trading pairs and their specifications.
     * 
     * @param category Product category: spot, linear, inverse, option
     * @param symbol Specific symbol to query (optional)
     * @param status Instrument status: Trading, Closed (optional)  
     * @param baseCoin Base coin (optional, e.g., BTC)
     * @param limit Number of results (max 1000)
     * @param cursor Pagination cursor
     * @return Instruments information response
     */
    @GetMapping("/v5/market/instruments-info") 
    BybitInstrumentsResponse getInstrumentsInfo(
        @RequestParam("category") String category,
        @RequestParam(value = "symbol", required = false) String symbol,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "baseCoin", required = false) String baseCoin,
        @RequestParam(value = "limit", defaultValue = "1000") int limit,
        @RequestParam(value = "cursor", required = false) String cursor
    );
}