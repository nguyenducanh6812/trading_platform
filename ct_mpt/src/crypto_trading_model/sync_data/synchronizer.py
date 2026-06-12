"""
Data Synchronization Service for Cryptocurrency Market Data
==========================================================

This module provides the core data synchronization functionality for the sync_data module.
It orchestrates API calls, data processing, and export operations following the
modular monolith architecture principles.
"""

import logging
from typing import List, Dict, Optional, Tuple
from datetime import datetime, timedelta, timezone
from concurrent.futures import ThreadPoolExecutor, as_completed
import time

from .api_client import BybitApiClient, BybitApiError, KlineData
from .exporter import CsvExporter, ExportError
from .models import (
    SyncDataConfig, SyncRequest, AssetData, AssetType, 
    IntervalType, ConfigValidationError
)

logger = logging.getLogger(__name__)

class SynchronizationError(Exception):
    """Custom exception for synchronization-related errors."""
    pass

class DataSynchronizer:
    """
    Core data synchronization service.
    
    This class orchestrates the entire data synchronization process including:
    - API data retrieval
    - Data validation and processing
    - CSV export
    - Error handling and retry logic
    """
    
    def __init__(self, config: SyncDataConfig):
        """
        Initialize the data synchronizer.
        
        Args:
            config: Synchronization configuration
        """
        self.config = config
        self.api_client = BybitApiClient(
            base_url=config.api.base_url,
            rate_limit_delay=config.api.rate_limit_delay
        )
        self.csv_exporter = CsvExporter(config.export)
        
        # Setup logging
        if config.enable_logging:
            self._setup_logging()
    
    def _setup_logging(self) -> None:
        """Configure logging for the synchronizer."""
        log_level = getattr(logging, self.config.log_level.upper())
        logging.basicConfig(
            level=log_level,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        )
        logger.setLevel(log_level)
    
    def sync_asset_data(self, request: SyncRequest) -> Tuple[List[AssetData], Optional[str]]:
        """
        Synchronize data for a single asset.
        
        Args:
            request: Synchronization request parameters
            
        Returns:
            Tuple of (asset_data_list, csv_file_path)
            
        Raises:
            SynchronizationError: If synchronization fails
        """
        try:
            # Validate the request
            self.config.validate_date_range(request.start_date, request.end_date)
            
            # Get trading symbol
            trading_symbol = request.get_trading_symbol()
            
            logger.info(f"Starting sync for {request.asset_code} ({trading_symbol}) from {request.start_date} to {request.end_date}")
            
            # Fetch data from API with retry logic
            kline_data = self._fetch_with_retry(
                trading_symbol, 
                request.start_date, 
                request.end_date,
                request.interval,
                request.category.value
            )
            
            if not kline_data:
                logger.warning(f"No data received for {trading_symbol}")
                return [], None
            
            # Convert to AssetData objects
            asset_data = self._convert_kline_to_asset_data(kline_data, request.asset_code)
            
            # Check if we got data from the requested start date
            if asset_data and asset_data[0].timestamp.date() > request.start_date.date():
                earliest_date = asset_data[0].timestamp.date()
                requested_date = request.start_date.date()
                logger.warning(f"Data for {request.asset_code} not available from {requested_date}. "
                              f"Earliest available data starts from {earliest_date}. "
                              f"Consider updating your start date to {earliest_date} or later.")
            
            # Export to CSV if requested
            csv_file_path = None
            if request.export_csv:
                csv_file_path = self.csv_exporter.export_asset_data(
                    asset_data=asset_data,
                    asset_code=request.asset_code,
                    start_date=request.start_date,
                    end_date=request.end_date
                )
            
            logger.info(f"Successfully synchronized {len(asset_data)} records for {request.asset_code}")
            return asset_data, csv_file_path
            
        except Exception as e:
            error_msg = f"Failed to sync data for {request.asset_code}: {str(e)}"
            logger.error(error_msg)
            raise SynchronizationError(error_msg) from e
    
    def sync_multiple_assets(self, 
                           asset_codes: List[str],
                           start_date: datetime,
                           end_date: datetime,
                           interval: IntervalType = IntervalType.DAILY,
                           category: AssetType = AssetType.LINEAR,
                           export_csv: bool = True,
                           create_combined_csv: bool = False) -> Dict[str, Tuple[List[AssetData], Optional[str]]]:
        """
        Synchronize data for multiple assets concurrently.
        
        Args:
            asset_codes: List of asset codes to synchronize
            start_date: Start date for data retrieval
            end_date: End date for data retrieval
            interval: Time interval for data
            category: Asset category
            export_csv: Whether to export individual CSV files
            create_combined_csv: Whether to create a combined CSV file
            
        Returns:
            Dictionary mapping asset codes to (asset_data, csv_path) tuples
            
        Raises:
            SynchronizationError: If synchronization fails for all assets
        """
        try:
            # Validate inputs
            if not asset_codes:
                raise SynchronizationError("No asset codes provided")
            
            self.config.validate_date_range(start_date, end_date)
            
            logger.info(f"Starting sync for {len(asset_codes)} assets: {', '.join(asset_codes)}")
            
            # Create sync requests
            sync_requests = []
            for asset_code in asset_codes:
                request = SyncRequest(
                    asset_code=asset_code,
                    start_date=start_date,
                    end_date=end_date,
                    interval=interval,
                    category=category,
                    export_csv=export_csv
                )
                sync_requests.append(request)
            
            # Execute synchronization concurrently
            results = {}
            failed_assets = []
            
            with ThreadPoolExecutor(max_workers=self.config.max_concurrent_requests) as executor:
                # Submit all tasks
                future_to_request = {
                    executor.submit(self.sync_asset_data, request): request 
                    for request in sync_requests
                }
                
                # Collect results
                for future in as_completed(future_to_request):
                    request = future_to_request[future]
                    try:
                        asset_data, csv_path = future.result()
                        results[request.asset_code] = (asset_data, csv_path)
                        logger.info(f"Completed sync for {request.asset_code}")
                    except Exception as e:
                        failed_assets.append((request.asset_code, str(e)))
                        logger.error(f"Failed to sync {request.asset_code}: {str(e)}")
            
            # Report results
            successful_count = len(results)
            failed_count = len(failed_assets)
            
            logger.info(f"Sync completed: {successful_count} successful, {failed_count} failed")
            
            if failed_assets:
                failed_asset_names = [asset for asset, _ in failed_assets]
                logger.warning(f"Failed assets: {', '.join(failed_asset_names)}")
            
            # Create combined CSV if requested and we have data
            if create_combined_csv and results:
                try:
                    assets_data = {asset_code: data for asset_code, (data, _) in results.items() if data}
                    if assets_data:
                        combined_path = self.csv_exporter.create_combined_csv(
                            assets_data=assets_data,
                            start_date=start_date,
                            end_date=end_date
                        )
                        logger.info(f"Created combined CSV: {combined_path}")
                except Exception as e:
                    logger.error(f"Failed to create combined CSV: {str(e)}")
            
            # Raise error if all assets failed
            if not results:
                raise SynchronizationError(f"All assets failed to sync. First error: {failed_assets[0][1] if failed_assets else 'Unknown error'}")
            
            return results
            
        except Exception as e:
            if isinstance(e, SynchronizationError):
                raise
            error_msg = f"Failed to sync multiple assets: {str(e)}"
            logger.error(error_msg)
            raise SynchronizationError(error_msg) from e
    
    def _fetch_with_retry(self, 
                         symbol: str, 
                         start_date: datetime, 
                         end_date: datetime,
                         interval: IntervalType,
                         category: str = "linear") -> List[KlineData]:
        """
        Fetch kline data with retry logic.
        
        Args:
            symbol: Trading symbol
            start_date: Start date
            end_date: End date
            interval: Time interval
            category: Market category (spot, linear, inverse)
            
        Returns:
            List of KlineData objects
            
        Raises:
            SynchronizationError: If all retries fail
        """
        last_error = None
        
        for attempt in range(self.config.api.max_retries + 1):
            try:
                if attempt > 0:
                    logger.info(f"Retry attempt {attempt} for {symbol}")
                    time.sleep(self.config.api.retry_delay * attempt)
                
                return self.api_client.get_daily_data_range(
                    symbol=symbol,
                    start_date=start_date,
                    end_date=end_date,
                    category=category
                )
                
            except BybitApiError as e:
                last_error = e
                logger.warning(f"API error for {symbol} (attempt {attempt + 1}): {str(e)}")
                
                # Don't retry for certain types of errors
                if "symbol" in str(e).lower() and "not found" in str(e).lower():
                    raise SynchronizationError(f"Symbol {symbol} not found") from e
                
            except Exception as e:
                last_error = e
                logger.warning(f"Unexpected error for {symbol} (attempt {attempt + 1}): {str(e)}")
        
        # All retries failed
        raise SynchronizationError(f"Failed to fetch data for {symbol} after {self.config.api.max_retries + 1} attempts: {str(last_error)}")
    
    def _convert_kline_to_asset_data(self, kline_data: List[KlineData], asset_code: str) -> List[AssetData]:
        """
        Convert KlineData objects to AssetData objects.
        
        Args:
            kline_data: List of KlineData objects from API
            asset_code: Asset code for the data
            
        Returns:
            List of AssetData objects
        """
        asset_data = []
        
        for kline in kline_data:
            try:
                # Convert timestamp from milliseconds to datetime in UTC
                timestamp = datetime.fromtimestamp(kline.timestamp / 1000, tz=timezone.utc)
                
                asset_obj = AssetData(
                    timestamp=timestamp,
                    asset_code=asset_code,
                    open_price=kline.open_price,
                    close_price=kline.close_price,
                    high_price=kline.high_price,
                    low_price=kline.low_price,
                    volume=kline.volume,
                    turnover=kline.turnover
                )
                asset_data.append(asset_obj)
                
            except Exception as e:
                logger.warning(f"Failed to convert kline data: {kline}, error: {str(e)}")
                continue
        
        logger.debug(f"Converted {len(kline_data)} kline records to {len(asset_data)} asset data records")
        return asset_data
    
    def get_available_symbols(self, category: AssetType = AssetType.LINEAR) -> List[str]:
        """
        Get list of available trading symbols from the API.
        
        Args:
            category: Asset category to query
            
        Returns:
            List of available trading symbols
            
        Raises:
            SynchronizationError: If unable to fetch symbols
        """
        try:
            logger.info(f"Fetching available symbols for category: {category.value}")
            
            symbols = self.api_client.get_supported_symbols(category.value)
            
            logger.info(f"Found {len(symbols)} available symbols")
            return symbols
            
        except BybitApiError as e:
            error_msg = f"Failed to fetch available symbols: {str(e)}"
            logger.error(error_msg)
            raise SynchronizationError(error_msg) from e
    
    def test_connection(self) -> bool:
        """
        Test the connection to the data provider API.
        
        Returns:
            True if connection is successful, False otherwise
        """
        try:
            logger.info("Testing API connection...")
            result = self.api_client.test_connection()
            
            if result:
                logger.info("API connection test successful")
            else:
                logger.warning("API connection test failed")
            
            return result
            
        except Exception as e:
            logger.error(f"API connection test error: {str(e)}")
            return False
    
    def get_sync_summary(self, results: Dict[str, Tuple[List[AssetData], Optional[str]]]) -> Dict[str, any]:
        """
        Generate a summary of synchronization results.
        
        Args:
            results: Results from sync_multiple_assets
            
        Returns:
            Dictionary with synchronization summary
        """
        total_assets = len(results)
        total_records = sum(len(data) for data, _ in results.values())
        
        assets_with_data = sum(1 for data, _ in results.values() if data)
        assets_with_csv = sum(1 for _, csv_path in results.values() if csv_path)
        
        # Get date range from data
        all_timestamps = []
        for data, _ in results.values():
            all_timestamps.extend([record.timestamp for record in data])
        
        earliest_date = min(all_timestamps) if all_timestamps else None
        latest_date = max(all_timestamps) if all_timestamps else None
        
        return {
            "total_assets": total_assets,
            "assets_with_data": assets_with_data,
            "assets_with_csv": assets_with_csv,
            "total_records": total_records,
            "earliest_date": earliest_date,
            "latest_date": latest_date,
            "success_rate": (assets_with_data / total_assets * 100) if total_assets > 0 else 0
        }
    
    def cleanup_resources(self) -> None:
        """Clean up resources used by the synchronizer."""
        try:
            if hasattr(self.api_client, 'session'):
                self.api_client.session.close()
            logger.info("Synchronizer resources cleaned up")
        except Exception as e:
            logger.warning(f"Error during cleanup: {str(e)}")
    
    def __enter__(self):
        """Context manager entry."""
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit."""
        self.cleanup_resources()