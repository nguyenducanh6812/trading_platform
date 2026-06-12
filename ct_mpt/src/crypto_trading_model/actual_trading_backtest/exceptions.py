"""
Custom exceptions for actual trading backtest module.

Provides specific exception types for better error handling.
"""


class ActualTradingBacktestError(Exception):
    """Base exception for actual trading backtest module."""
    pass


class ConfigurationError(ActualTradingBacktestError):
    """Raised when configuration is invalid or incomplete."""
    pass


class InsufficientFundsError(ActualTradingBacktestError):
    """Raised when account has insufficient funds for trading."""
    
    def __init__(self, required_amount: float, available_amount: float, message: str = None):
        self.required_amount = required_amount
        self.available_amount = available_amount
        
        if message is None:
            message = f"Insufficient funds: required {required_amount:.2f}, available {available_amount:.2f}"
        
        super().__init__(message)


class InvalidTradeError(ActualTradingBacktestError):
    """Raised when a trade is invalid or cannot be executed."""
    pass


class DataValidationError(ActualTradingBacktestError):
    """Raised when input data is invalid or incomplete."""
    
    def __init__(self, missing_columns: list = None, message: str = None):
        self.missing_columns = missing_columns or []
        
        if message is None and missing_columns:
            message = f"Missing required columns: {missing_columns}"
        elif message is None:
            message = "Data validation failed"
        
        super().__init__(message)


class RebalancingError(ActualTradingBacktestError):
    """Raised when rebalancing fails."""
    pass


class ExportError(ActualTradingBacktestError):
    """Raised when result export fails."""
    pass


class CalculationError(ActualTradingBacktestError):
    """Raised when financial calculations fail."""
    pass