"""
Actual Trading Backtest Module

This module provides realistic backtesting with trading fees, account rebalancing,
and detailed simulation of actual trading behavior.
"""

from .config import ActualTradingConfig, TradingAccountConfig, RebalancingConfig, RebalancingFrequency
from .trading_account import TradingAccount
from .rebalancing import RebalancingStrategy, MonthlyRebalancing, QuarterlyRebalancing, YearlyRebalancing
from .backtest_simulator import ActualTradingBacktest
from .results_exporter import ActualTradingResultsExporter
from .exceptions import (
    ActualTradingBacktestError,
    ConfigurationError,
    InsufficientFundsError,
    InvalidTradeError,
    DataValidationError,
    RebalancingError,
    ExportError,
    CalculationError
)
from .validators import DataValidator, ConfigValidator, FinancialValidator
from .ui import ActualTradingBacktestUI

__all__ = [
    'ActualTradingConfig',
    'TradingAccountConfig', 
    'RebalancingConfig',
    'RebalancingFrequency',
    'TradingAccount',
    'RebalancingStrategy',
    'MonthlyRebalancing',
    'QuarterlyRebalancing', 
    'YearlyRebalancing',
    'ActualTradingBacktest',
    'ActualTradingResultsExporter',
    'ActualTradingBacktestUI',
    'ActualTradingBacktestError',
    'ConfigurationError',
    'InsufficientFundsError',
    'InvalidTradeError',
    'DataValidationError',
    'RebalancingError',
    'ExportError',
    'CalculationError',
    'DataValidator',
    'ConfigValidator',
    'FinancialValidator'
]