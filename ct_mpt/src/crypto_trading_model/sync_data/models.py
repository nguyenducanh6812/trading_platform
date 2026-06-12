"""
Data Models and Configuration for Sync Data Module
=================================================

This module contains data models and configuration classes for the sync_data module.
It defines structures for API configuration, asset data, and export settings.
"""

from dataclasses import dataclass, field
from typing import List, Dict, Optional, Any
from datetime import datetime, timezone
from enum import Enum

class ConfigValidationError(Exception):
    """Exception raised when sync data configuration validation fails."""
    pass

class AssetType(Enum):
    """Supported asset types for data synchronization."""
    SPOT = "spot"
    LINEAR = "linear"
    OPTION = "option"

class IntervalType(Enum):
    """Supported time intervals for kline data."""
    MINUTE_1 = "1"
    MINUTE_3 = "3" 
    MINUTE_5 = "5"
    MINUTE_15 = "15"
    MINUTE_30 = "30"
    HOUR_1 = "60"
    HOUR_2 = "120"
    HOUR_4 = "240"
    HOUR_6 = "360"
    HOUR_12 = "720"
    DAILY = "D"
    WEEKLY = "W"
    MONTHLY = "M"

@dataclass
class AssetData:
    """
    Represents cryptocurrency asset market data.
    
    This matches the output format specified in the requirements with
    dynamic column naming based on asset code.
    """
    timestamp: datetime
    asset_code: str
    open_price: float
    close_price: float
    high_price: float
    low_price: float
    volume: float
    turnover: float
    
    def to_dict(self) -> Dict[str, Any]:
        """
        Convert asset data to dictionary with proper column naming.
        
        Returns:
            Dictionary with columns formatted as: TIMESTAMP, OPEN_PRICE_{ASSET}, etc.
        """
        asset_upper = self.asset_code.upper()
        return {
            "TIMESTAMP": self.timestamp,
            f"OPEN_PRICE_{asset_upper}": self.open_price,
            f"CLOSE_PRICE_{asset_upper}": self.close_price,
            f"HIGH_{asset_upper}": self.high_price,
            f"LOW_{asset_upper}": self.low_price,
            f"VOLUME_{asset_upper}": self.volume,
            f"TURNOVER_{asset_upper}": self.turnover
        }
    
    @classmethod
    def get_column_headers(cls, asset_code: str) -> List[str]:
        """
        Get CSV column headers for a specific asset.
        
        Args:
            asset_code: The asset code (e.g., 'BTC', 'ETH')
            
        Returns:
            List of column header names
        """
        asset_upper = asset_code.upper()
        return [
            "TIMESTAMP",
            f"OPEN_PRICE_{asset_upper}",
            f"CLOSE_PRICE_{asset_upper}",
            f"HIGH_{asset_upper}",
            f"LOW_{asset_upper}",
            f"VOLUME_{asset_upper}",
            f"TURNOVER_{asset_upper}"
        ]

@dataclass
class ApiConfig:
    """Configuration for external API connections."""
    base_url: str = "https://api.bybit.com"
    rate_limit_delay: float = 0.1
    timeout: int = 30
    max_retries: int = 3
    retry_delay: float = 1.0
    
    def __post_init__(self):
        """Validate API configuration."""
        if not isinstance(self.base_url, str) or not self.base_url.strip():
            raise ConfigValidationError("base_url must be a non-empty string")
        
        if not isinstance(self.rate_limit_delay, (int, float)) or self.rate_limit_delay < 0:
            raise ConfigValidationError("rate_limit_delay must be a non-negative number")
        
        if not isinstance(self.timeout, int) or self.timeout <= 0:
            raise ConfigValidationError("timeout must be a positive integer")
        
        if not isinstance(self.max_retries, int) or self.max_retries < 0:
            raise ConfigValidationError("max_retries must be a non-negative integer")
        
        if not isinstance(self.retry_delay, (int, float)) or self.retry_delay < 0:
            raise ConfigValidationError("retry_delay must be a non-negative number")

@dataclass
class ExportConfig:
    """Configuration for data export settings."""
    output_dir: str = "./sync_data_output"
    file_name_template: str = "{asset_code}.csv"
    include_headers: bool = True
    date_format: str = "%Y-%m-%d"
    timestamp_format: str = "%Y-%m-%d %H:%M:%S"
    
    def __post_init__(self):
        """Validate export configuration."""
        if not isinstance(self.output_dir, str) or not self.output_dir.strip():
            raise ConfigValidationError("output_dir must be a non-empty string")
        
        if not isinstance(self.file_name_template, str) or not self.file_name_template.strip():
            raise ConfigValidationError("file_name_template must be a non-empty string")
        
        if not isinstance(self.include_headers, bool):
            raise ConfigValidationError("include_headers must be a boolean")
        
        if not isinstance(self.date_format, str) or not self.date_format.strip():
            raise ConfigValidationError("date_format must be a non-empty string")
        
        if not isinstance(self.timestamp_format, str) or not self.timestamp_format.strip():
            raise ConfigValidationError("timestamp_format must be a non-empty string")
    
    def generate_filename(self, asset_code: str, start_date: datetime, end_date: datetime) -> str:
        """
        Generate filename based on template and parameters.
        
        Args:
            asset_code: Asset code (e.g., 'BTC', 'ETH')
            start_date: Start date for the data
            end_date: End date for the data
            
        Returns:
            Generated filename
        """
        return self.file_name_template.format(
            asset_code=asset_code.upper(),
            start_date=start_date.strftime(self.date_format),
            end_date=end_date.strftime(self.date_format)
        )

@dataclass
class SyncDataConfig:
    """
    Main configuration class for the sync_data module.
    
    This configuration supports the modular monolith architecture by centralizing
    all sync_data related settings in one place.
    """
    api: ApiConfig = field(default_factory=ApiConfig)
    export: ExportConfig = field(default_factory=ExportConfig)
    default_symbols: List[str] = field(default_factory=lambda: ["BTCUSDT", "ETHUSDT"])
    default_interval: IntervalType = IntervalType.DAILY
    default_category: AssetType = AssetType.LINEAR
    max_concurrent_requests: int = 5
    enable_logging: bool = True
    log_level: str = "INFO"
    
    def __post_init__(self):
        """Validate sync data configuration."""
        if not isinstance(self.default_symbols, list):
            raise ConfigValidationError("default_symbols must be a list")
        
        if not all(isinstance(symbol, str) and symbol.strip() for symbol in self.default_symbols):
            raise ConfigValidationError("default_symbols must contain non-empty strings")
        
        if not isinstance(self.default_interval, IntervalType):
            raise ConfigValidationError("default_interval must be an IntervalType enum")
        
        if not isinstance(self.default_category, AssetType):
            raise ConfigValidationError("default_category must be an AssetType enum")
        
        if not isinstance(self.max_concurrent_requests, int) or self.max_concurrent_requests <= 0:
            raise ConfigValidationError("max_concurrent_requests must be a positive integer")
        
        if not isinstance(self.enable_logging, bool):
            raise ConfigValidationError("enable_logging must be a boolean")
        
        valid_log_levels = ["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"]
        if not isinstance(self.log_level, str) or self.log_level.upper() not in valid_log_levels:
            raise ConfigValidationError(f"log_level must be one of: {valid_log_levels}")
    
    def get_symbol_without_usdt(self, symbol: str) -> str:
        """
        Extract base asset code from trading symbol.
        
        Args:
            symbol: Trading symbol (e.g., 'BTCUSDT', 'ETHUSDT')
            
        Returns:
            Base asset code (e.g., 'BTC', 'ETH')
        """
        if symbol.endswith('USDT'):
            return symbol[:-4]
        elif symbol.endswith('USD'):
            return symbol[:-3]
        else:
            # For other quote currencies, try to intelligently extract
            # This is a simple heuristic - may need refinement for other pairs
            return symbol[:3] if len(symbol) >= 6 else symbol
    
    def validate_date_range(self, start_date: datetime, end_date: datetime) -> None:
        """
        Validate that the date range is valid.
        
        Args:
            start_date: Start date
            end_date: End date
            
        Raises:
            ConfigValidationError: If date range is invalid
        """
        if not isinstance(start_date, datetime):
            raise ConfigValidationError("start_date must be a datetime object")
        
        if not isinstance(end_date, datetime):
            raise ConfigValidationError("end_date must be a datetime object")
        
        if start_date >= end_date:
            raise ConfigValidationError("start_date must be before end_date")
        
        # Handle timezone-aware comparison properly
        now = datetime.now(timezone.utc)
        end_date_utc = end_date
        
        # Convert to UTC if timezone-naive (assume local timezone)
        if end_date.tzinfo is None:
            end_date_utc = end_date.replace(tzinfo=timezone.utc)
        elif end_date.tzinfo != timezone.utc:
            end_date_utc = end_date.astimezone(timezone.utc)
            
        if end_date_utc > now:
            raise ConfigValidationError("end_date cannot be in the future")

@dataclass
class SyncRequest:
    """
    Represents a data synchronization request.
    
    This encapsulates all parameters needed for a sync operation.
    """
    asset_code: str
    start_date: datetime
    end_date: datetime
    interval: IntervalType = IntervalType.DAILY
    category: AssetType = AssetType.LINEAR
    export_csv: bool = True
    
    def __post_init__(self):
        """Validate sync request parameters."""
        if not isinstance(self.asset_code, str) or not self.asset_code.strip():
            raise ConfigValidationError("asset_code must be a non-empty string")
        
        if not isinstance(self.interval, IntervalType):
            raise ConfigValidationError("interval must be an IntervalType enum")
        
        if not isinstance(self.category, AssetType):
            raise ConfigValidationError("category must be an AssetType enum")
        
        if not isinstance(self.export_csv, bool):
            raise ConfigValidationError("export_csv must be a boolean")
    
    def get_trading_symbol(self) -> str:
        """
        Get the trading symbol for API requests.
        
        Returns:
            Trading symbol (e.g., 'BTCUSDT' for asset_code 'BTC')
        """
        # Simple logic - append USDT for most assets
        # This can be made more sophisticated based on requirements
        asset_upper = self.asset_code.upper()
        if asset_upper.endswith('USDT'):
            return asset_upper
        else:
            return f"{asset_upper}USDT"