"""
Bybit API Client for Cryptocurrency Data Retrieval
==================================================

This module provides a client interface for the Bybit API to fetch cryptocurrency
market data including OHLCV (Open, High, Low, Close, Volume) information.

API Documentation: https://bybit-exchange.github.io/docs/v5/market/kline
"""

import requests
from typing import List, Dict, Any, Optional
from datetime import datetime, timedelta, timezone
import time
import logging
from dataclasses import dataclass

logger = logging.getLogger(__name__)

@dataclass
class KlineData:
    """Represents a single kline (candlestick) data point."""
    timestamp: int
    open_price: float
    high_price: float
    low_price: float
    close_price: float
    volume: float
    turnover: float

class BybitApiError(Exception):
    """Custom exception for Bybit API related errors."""
    pass

class BybitApiClient:
    """
    Client for interacting with Bybit API to fetch cryptocurrency market data.
    
    This client handles rate limiting, error handling, and data formatting
    for the Bybit V5 market data API.
    """
    
    def __init__(self, base_url: str = "https://api.bybit.com", rate_limit_delay: float = 0.1):
        """
        Initialize the Bybit API client.
        
        Args:
            base_url: Base URL for the Bybit API
            rate_limit_delay: Delay between API calls to respect rate limits
        """
        self.base_url = base_url.rstrip('/')
        self.rate_limit_delay = rate_limit_delay
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json',
            'User-Agent': 'CryptoTradingModel/1.0'
        })
    
    def get_kline_data(self, 
                      symbol: str, 
                      interval: str = "D", 
                      start_time: Optional[datetime] = None,
                      end_time: Optional[datetime] = None,
                      limit: int = 1000,
                      category: str = "linear") -> List[KlineData]:
        """
        Fetch kline (candlestick) data for a given symbol.
        
        Args:
            symbol: Trading symbol (e.g., 'BTCUSDT', 'ETHUSDT')
            interval: Time interval ('1', '3', '5', '15', '30', '60', '120', '240', '360', '720', 'D', 'W', 'M')
            start_time: Start time for data retrieval
            end_time: End time for data retrieval  
            limit: Maximum number of records to return (max 1000)
            category: Market category ('spot', 'linear', 'inverse') - defaults to 'linear' for better historical data
            
        Returns:
            List of KlineData objects
            
        Raises:
            BybitApiError: If API request fails or returns invalid data
        """
        endpoint = f"{self.base_url}/v5/market/kline"
        
        params = {
            "category": category,
            "symbol": symbol,
            "interval": interval,
            "limit": min(limit, 1000)  # Bybit max limit is 1000
        }
        
        # Convert datetime to timestamp if provided
        # Ensure we're working with UTC timestamps
        if start_time:
            # If timezone-naive, assume it's UTC
            if start_time.tzinfo is None:
                start_time = start_time.replace(tzinfo=timezone.utc)
            params["start"] = int(start_time.timestamp() * 1000)
        if end_time:
            # If timezone-naive, assume it's UTC
            if end_time.tzinfo is None:
                end_time = end_time.replace(tzinfo=timezone.utc)
            params["end"] = int(end_time.timestamp() * 1000)
        
        try:
            logger.info(f"Fetching kline data for {symbol} from {start_time} to {end_time}")
            
            # Add rate limiting
            time.sleep(self.rate_limit_delay)
            
            response = self.session.get(endpoint, params=params, timeout=30)
            response.raise_for_status()
            
            data = response.json()
            
            # Check if response is successful
            if data.get("retCode") != 0:
                error_msg = data.get("retMsg", "Unknown error")
                raise BybitApiError(f"API returned error: {error_msg}")
            
            # Parse the kline data
            kline_list = data.get("result", {}).get("list", [])
            
            if not kline_list:
                logger.warning(f"No kline data found for {symbol}")
                return []
            
            # Convert raw data to KlineData objects
            kline_data = []
            for kline in kline_list:
                try:
                    kline_obj = KlineData(
                        timestamp=int(kline[0]),
                        open_price=float(kline[1]),
                        high_price=float(kline[2]),
                        low_price=float(kline[3]),
                        close_price=float(kline[4]),
                        volume=float(kline[5]),
                        turnover=float(kline[6])
                    )
                    kline_data.append(kline_obj)
                except (ValueError, IndexError) as e:
                    logger.warning(f"Failed to parse kline data: {kline}, error: {e}")
                    continue
            
            logger.info(f"Successfully fetched {len(kline_data)} kline records for {symbol}")
            return kline_data
            
        except requests.RequestException as e:
            raise BybitApiError(f"HTTP request failed: {str(e)}")
        except Exception as e:
            raise BybitApiError(f"Unexpected error fetching kline data: {str(e)}")
    
    def get_daily_data_range(self, 
                           symbol: str, 
                           start_date: datetime, 
                           end_date: datetime,
                           category: str = "linear") -> List[KlineData]:
        """
        Fetch daily kline data for a date range.
        
        This method handles pagination when the date range exceeds API limits.
        
        Args:
            symbol: Trading symbol (e.g., 'BTCUSDT', 'ETHUSDT')
            start_date: Start date for data retrieval
            end_date: End date for data retrieval
            category: Market category ('spot', 'linear', 'inverse') - defaults to 'linear' for better historical data
            
        Returns:
            List of KlineData objects sorted by timestamp
            
        Raises:
            BybitApiError: If API request fails
        """
        all_data = []
        current_start = start_date
        
        # Bybit API limit is 1000 records, so we may need to paginate
        max_days_per_request = 1000
        
        while current_start <= end_date:
            # Calculate end date for this batch
            current_end = min(
                current_start + timedelta(days=max_days_per_request - 1),
                end_date
            )
            
            # Fetch data for this batch
            batch_data = self.get_kline_data(
                symbol=symbol,
                interval="D",
                start_time=current_start,
                end_time=current_end,
                limit=1000,
                category=category
            )
            
            all_data.extend(batch_data)
            
            # Move to next batch
            current_start = current_end + timedelta(days=1)
            
            # Add delay between requests to respect rate limits
            if current_start <= end_date:
                time.sleep(self.rate_limit_delay)
        
        # Sort by timestamp to ensure chronological order
        all_data.sort(key=lambda x: x.timestamp)
        
        logger.info(f"Fetched total of {len(all_data)} daily records for {symbol} from {start_date} to {end_date}")
        return all_data
    
    def test_connection(self) -> bool:
        """
        Test the connection to Bybit API.
        
        Returns:
            True if connection is successful, False otherwise
        """
        try:
            endpoint = f"{self.base_url}/v5/market/time"
            response = self.session.get(endpoint, timeout=10)
            response.raise_for_status()
            
            data = response.json()
            return data.get("retCode") == 0
            
        except Exception as e:
            logger.error(f"Connection test failed: {str(e)}")
            return False
    
    def get_supported_symbols(self, category: str = "spot") -> List[str]:
        """
        Get list of supported trading symbols.
        
        Args:
            category: Trading category (spot, linear, option)
            
        Returns:
            List of symbol names
            
        Raises:
            BybitApiError: If API request fails
        """
        endpoint = f"{self.base_url}/v5/market/instruments-info"
        params = {"category": category}
        
        try:
            time.sleep(self.rate_limit_delay)
            response = self.session.get(endpoint, params=params, timeout=30)
            response.raise_for_status()
            
            data = response.json()
            
            if data.get("retCode") != 0:
                error_msg = data.get("retMsg", "Unknown error")
                raise BybitApiError(f"API returned error: {error_msg}")
            
            instruments = data.get("result", {}).get("list", [])
            symbols = [instrument.get("symbol") for instrument in instruments if instrument.get("symbol")]
            
            logger.info(f"Found {len(symbols)} supported symbols in {category} category")
            return symbols
            
        except requests.RequestException as e:
            raise BybitApiError(f"HTTP request failed: {str(e)}")
        except Exception as e:
            raise BybitApiError(f"Unexpected error fetching symbols: {str(e)}")
    
    def __enter__(self):
        """Context manager entry."""
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit - close session."""
        self.session.close()