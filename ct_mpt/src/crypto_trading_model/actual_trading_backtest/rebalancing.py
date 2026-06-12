"""
Rebalancing strategies for actual trading backtest.

Implements Strategy pattern for different rebalancing frequencies.
"""

from abc import ABC, abstractmethod
from datetime import datetime, timedelta
from typing import Dict, Any
import pandas as pd
from .config import RebalancingFrequency, RebalancingConfig


class RebalancingStrategy(ABC):
    """
    Abstract base class for rebalancing strategies.
    
    Follows Strategy pattern and Open/Closed principle.
    """
    
    def __init__(self, config: RebalancingConfig):
        """
        Initialize rebalancing strategy.
        
        Args:
            config: Rebalancing configuration
        """
        self.config = config
        self._last_rebalance_date: pd.Timestamp = None
    
    @abstractmethod
    def should_rebalance(self, current_date: pd.Timestamp) -> bool:
        """
        Determine if rebalancing should occur on the given date.
        
        Args:
            current_date: Current simulation date
            
        Returns:
            True if rebalancing should occur
        """
        pass
    
    @abstractmethod
    def get_next_rebalance_date(self, current_date: pd.Timestamp) -> pd.Timestamp:
        """
        Calculate the next rebalancing date.
        
        Args:
            current_date: Current simulation date
            
        Returns:
            Next rebalancing date
        """
        pass
    
    def record_rebalance(self, date: pd.Timestamp) -> None:
        """Record that rebalancing occurred on the given date."""
        self._last_rebalance_date = date
    
    @property
    def last_rebalance_date(self) -> pd.Timestamp:
        """Get the last rebalancing date."""
        return self._last_rebalance_date
    
    def get_rebalancing_info(self) -> Dict[str, Any]:
        """Get information about the rebalancing strategy."""
        return {
            "strategy_type": self.__class__.__name__,
            "frequency": self.config.frequency.value,
            "last_rebalance": self._last_rebalance_date
        }


class MonthlyRebalancing(RebalancingStrategy):
    """Monthly rebalancing strategy."""
    
    def should_rebalance(self, current_date: pd.Timestamp) -> bool:
        """Check if rebalancing should occur (first day of month)."""
        if self._last_rebalance_date is None:
            # First rebalancing - should happen on first day of any month
            return current_date.day == 1
        
        # Rebalance on first day of new month
        return (current_date.day == 1 and 
                current_date.month != self._last_rebalance_date.month)
    
    def get_next_rebalance_date(self, current_date: pd.Timestamp) -> pd.Timestamp:
        """Calculate next month's first day."""
        if current_date.month == 12:
            next_month = current_date.replace(year=current_date.year + 1, month=1, day=1)
        else:
            next_month = current_date.replace(month=current_date.month + 1, day=1)
        return next_month


class QuarterlyRebalancing(RebalancingStrategy):
    """Quarterly rebalancing strategy."""
    
    QUARTER_START_MONTHS = [1, 4, 7, 10]
    
    def should_rebalance(self, current_date: pd.Timestamp) -> bool:
        """Check if rebalancing should occur (first day of quarter)."""
        if self._last_rebalance_date is None:
            return True
        
        # Rebalance on first day of new quarter
        is_quarter_start = (
            current_date.month in self.QUARTER_START_MONTHS and 
            current_date.day == 1
        )
        
        return is_quarter_start or (
            self._get_quarter(current_date) != self._get_quarter(self._last_rebalance_date)
        )
    
    def get_next_rebalance_date(self, current_date: pd.Timestamp) -> pd.Timestamp:
        """Calculate next quarter's first day."""
        current_quarter = self._get_quarter(current_date)
        
        if current_quarter == 4:
            # Next quarter is Q1 of next year
            return current_date.replace(year=current_date.year + 1, month=1, day=1)
        else:
            # Next quarter in same year
            next_quarter_month = self.QUARTER_START_MONTHS[current_quarter]
            return current_date.replace(month=next_quarter_month, day=1)
    
    def _get_quarter(self, date: pd.Timestamp) -> int:
        """Get quarter number (1-4) for given date."""
        return ((date.month - 1) // 3) + 1


class YearlyRebalancing(RebalancingStrategy):
    """Yearly rebalancing strategy."""
    
    def should_rebalance(self, current_date: pd.Timestamp) -> bool:
        """Check if rebalancing should occur (first day of year)."""
        if self._last_rebalance_date is None:
            return True
        
        # Rebalance on first day of new year
        return (current_date.month == 1 and current_date.day == 1) or (
            current_date.year != self._last_rebalance_date.year
        )
    
    def get_next_rebalance_date(self, current_date: pd.Timestamp) -> pd.Timestamp:
        """Calculate next year's first day."""
        return current_date.replace(year=current_date.year + 1, month=1, day=1)


class CustomRebalancing(RebalancingStrategy):
    """Custom rebalancing strategy with user-defined period."""
    
    def __init__(self, config: RebalancingConfig):
        """
        Initialize custom rebalancing.
        
        Args:
            config: Rebalancing configuration with custom_days set
        """
        super().__init__(config)
        if config.custom_days is None:
            raise ValueError("custom_days must be set for CustomRebalancing")
        self.rebalance_period_days = config.custom_days
    
    def should_rebalance(self, current_date: pd.Timestamp) -> bool:
        """Check if rebalancing should occur based on custom period."""
        if self._last_rebalance_date is None:
            return True
        
        days_since_rebalance = (current_date - self._last_rebalance_date).days
        return days_since_rebalance >= self.rebalance_period_days
    
    def get_next_rebalance_date(self, current_date: pd.Timestamp) -> pd.Timestamp:
        """Calculate next rebalancing date based on custom period."""
        return current_date + pd.Timedelta(days=self.rebalance_period_days)


class RebalancingStrategyFactory:
    """
    Factory for creating rebalancing strategies.
    
    Follows Factory pattern and Open/Closed principle.
    """
    
    _strategies = {
        RebalancingFrequency.MONTHLY: MonthlyRebalancing,
        RebalancingFrequency.QUARTERLY: QuarterlyRebalancing,
        RebalancingFrequency.YEARLY: YearlyRebalancing,
        RebalancingFrequency.CUSTOM: CustomRebalancing
    }
    
    @classmethod
    def create_strategy(cls, config: RebalancingConfig) -> RebalancingStrategy:
        """
        Create appropriate rebalancing strategy.
        
        Args:
            config: Rebalancing configuration
            
        Returns:
            Configured rebalancing strategy
            
        Raises:
            ValueError: If frequency is not supported
        """
        strategy_class = cls._strategies.get(config.frequency)
        if strategy_class is None:
            raise ValueError(f"Unsupported rebalancing frequency: {config.frequency}")
        
        return strategy_class(config)
    
    @classmethod
    def register_strategy(
        cls,
        frequency: RebalancingFrequency,
        strategy_class: type
    ) -> None:
        """
        Register a new rebalancing strategy.
        
        Args:
            frequency: Rebalancing frequency enum
            strategy_class: Strategy class to register
        """
        if not issubclass(strategy_class, RebalancingStrategy):
            raise ValueError("Strategy class must inherit from RebalancingStrategy")
        
        cls._strategies[frequency] = strategy_class
    
    @classmethod
    def get_available_frequencies(cls) -> list:
        """Get list of available rebalancing frequencies."""
        return list(cls._strategies.keys())


def create_rebalancing_strategy(config: RebalancingConfig) -> RebalancingStrategy:
    """
    Convenience function to create rebalancing strategy.
    
    Args:
        config: Rebalancing configuration
        
    Returns:
        Configured rebalancing strategy
    """
    return RebalancingStrategyFactory.create_strategy(config)