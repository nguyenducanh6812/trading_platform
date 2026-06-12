"""
Configuration classes for actual trading backtest simulation.

Following SOLID principles with comprehensive validation and type safety.
"""

from dataclasses import dataclass, field
from typing import Dict, Optional
from enum import Enum


class RebalancingFrequency(Enum):
    """Rebalancing frequency options."""
    MONTHLY = "monthly"
    QUARTERLY = "quarterly"
    YEARLY = "yearly"
    CUSTOM = "custom"


@dataclass
class TradingAccountConfig:
    """Configuration for trading account setup."""
    
    initial_trading_balance: float = 500.0
    initial_saving_balance: float = 500.0
    target_trading_balance: float = 500.0
    
    def __post_init__(self):
        """Validate trading account configuration."""
        if self.initial_trading_balance <= 0:
            raise ValueError("initial_trading_balance must be positive")
        if self.initial_saving_balance < 0:
            raise ValueError("initial_saving_balance cannot be negative")
        if self.target_trading_balance <= 0:
            raise ValueError("target_trading_balance must be positive")
    
    @property
    def total_capital(self) -> float:
        """Calculate total capital."""
        return self.initial_trading_balance + self.initial_saving_balance


@dataclass
class RebalancingConfig:
    """Configuration for account rebalancing strategy."""
    
    frequency: RebalancingFrequency = RebalancingFrequency.MONTHLY
    custom_days: Optional[int] = None
    
    def __post_init__(self):
        """Validate rebalancing configuration."""
        if self.frequency == RebalancingFrequency.CUSTOM:
            if self.custom_days is None or self.custom_days <= 0:
                raise ValueError("custom_days must be positive when using CUSTOM frequency")
        elif self.custom_days is not None:
            raise ValueError("custom_days should only be set when using CUSTOM frequency")


@dataclass
class AssetConfig:
    """Configuration for asset-specific parameters."""
    
    btc_decimal_places: int = 3
    eth_decimal_places: int = 2
    
    def __post_init__(self):
        """Validate asset configuration."""
        if not (0 <= self.btc_decimal_places <= 8):
            raise ValueError("btc_decimal_places must be between 0 and 8")
        if not (0 <= self.eth_decimal_places <= 8):
            raise ValueError("eth_decimal_places must be between 0 and 8")


@dataclass
class TradingConfig:
    """Configuration for trading parameters."""
    
    trading_fee: float = 0.0015
    leverage_scale: float = 1.5
    
    def __post_init__(self):
        """Validate trading configuration."""
        if not (0 <= self.trading_fee <= 1):
            raise ValueError("trading_fee must be between 0 and 1")
        if self.leverage_scale <= 0:
            raise ValueError("leverage_scale must be positive")


@dataclass
class ActualTradingConfig:
    """
    Main configuration class for actual trading backtest.
    
    Aggregates all sub-configurations following composition pattern.
    """
    
    account: TradingAccountConfig = field(default_factory=TradingAccountConfig)
    rebalancing: RebalancingConfig = field(default_factory=RebalancingConfig)
    assets: AssetConfig = field(default_factory=AssetConfig)
    trading: TradingConfig = field(default_factory=TradingConfig)
    
    def __post_init__(self):
        """Validate the complete configuration."""
        # Additional cross-validation can be added here
        pass
    
    @classmethod
    def create_default(cls) -> 'ActualTradingConfig':
        """Create configuration with default values matching requirements."""
        return cls(
            account=TradingAccountConfig(
                initial_trading_balance=500.0,
                initial_saving_balance=500.0,
                target_trading_balance=500.0
            ),
            rebalancing=RebalancingConfig(
                frequency=RebalancingFrequency.MONTHLY
            ),
            assets=AssetConfig(
                btc_decimal_places=3,
                eth_decimal_places=2
            ),
            trading=TradingConfig(
                trading_fee=0.0015,
                leverage_scale=1.5
            )
        )
    
    @classmethod
    def create_custom(
        cls,
        total_capital: float = 1000.0,
        trading_portion: float = 0.5,
        rebalancing_frequency: RebalancingFrequency = RebalancingFrequency.MONTHLY,
        custom_rebalancing_days: Optional[int] = None,
        trading_fee: float = 0.0015,
        leverage_scale: float = 1.5
    ) -> 'ActualTradingConfig':
        """
        Create custom configuration with calculated account balances.
        
        Args:
            total_capital: Total available capital
            trading_portion: Portion allocated to trading (0-1)
            rebalancing_frequency: How often to rebalance
            custom_rebalancing_days: Custom rebalancing period in days
            trading_fee: Trading fee percentage
            leverage_scale: Leverage multiplier
            
        Returns:
            Configured ActualTradingConfig instance
        """
        if not (0 < trading_portion < 1):
            raise ValueError("trading_portion must be between 0 and 1")
        if total_capital <= 0:
            raise ValueError("total_capital must be positive")
        
        trading_balance = total_capital * trading_portion
        saving_balance = total_capital * (1 - trading_portion)
        
        return cls(
            account=TradingAccountConfig(
                initial_trading_balance=trading_balance,
                initial_saving_balance=saving_balance,
                target_trading_balance=trading_balance
            ),
            rebalancing=RebalancingConfig(
                frequency=rebalancing_frequency,
                custom_days=custom_rebalancing_days
            ),
            trading=TradingConfig(
                trading_fee=trading_fee,
                leverage_scale=leverage_scale
            )
        )