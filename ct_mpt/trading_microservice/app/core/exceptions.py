"""
Custom Exceptions
================

Centralized exception handling for the trading microservice.
"""

from typing import Optional, Dict, Any


class TradingServiceException(Exception):
    """Base exception for trading service errors."""

    def __init__(
        self,
        message: str,
        error_code: str = "TRADING_ERROR",
        status_code: int = 400,
        details: Optional[Dict[str, Any]] = None
    ):
        self.message = message
        self.error_code = error_code
        self.status_code = status_code
        self.details = details or {}
        super().__init__(message)


class ValidationError(TradingServiceException):
    """Raised when input validation fails."""

    def __init__(self, message: str, details: Optional[Dict[str, Any]] = None):
        super().__init__(
            message=message,
            error_code="VALIDATION_ERROR",
            status_code=422,
            details=details
        )


class DataProcessingError(TradingServiceException):
    """Raised when data processing fails."""

    def __init__(self, message: str, details: Optional[Dict[str, Any]] = None):
        super().__init__(
            message=message,
            error_code="DATA_PROCESSING_ERROR",
            status_code=400,
            details=details
        )


class OptimizationError(TradingServiceException):
    """Raised when portfolio optimization fails."""

    def __init__(self, message: str, details: Optional[Dict[str, Any]] = None):
        super().__init__(
            message=message,
            error_code="OPTIMIZATION_ERROR",
            status_code=400,
            details=details
        )


class TradingSimulationError(TradingServiceException):
    """Raised when trading simulation fails."""

    def __init__(self, message: str, details: Optional[Dict[str, Any]] = None):
        super().__init__(
            message=message,
            error_code="TRADING_SIMULATION_ERROR",
            status_code=400,
            details=details
        )


class ConfigurationError(TradingServiceException):
    """Raised when configuration is invalid."""

    def __init__(self, message: str, details: Optional[Dict[str, Any]] = None):
        super().__init__(
            message=message,
            error_code="CONFIGURATION_ERROR",
            status_code=400,
            details=details
        )


class FileProcessingError(TradingServiceException):
    """Raised when file processing fails."""

    def __init__(self, message: str, details: Optional[Dict[str, Any]] = None):
        super().__init__(
            message=message,
            error_code="FILE_PROCESSING_ERROR",
            status_code=400,
            details=details
        )


class InsufficientDataError(TradingServiceException):
    """Raised when there's insufficient data for processing."""

    def __init__(self, message: str, details: Optional[Dict[str, Any]] = None):
        super().__init__(
            message=message,
            error_code="INSUFFICIENT_DATA_ERROR",
            status_code=400,
            details=details
        )


class ResourceLimitError(TradingServiceException):
    """Raised when resource limits are exceeded."""

    def __init__(self, message: str, details: Optional[Dict[str, Any]] = None):
        super().__init__(
            message=message,
            error_code="RESOURCE_LIMIT_ERROR",
            status_code=413,
            details=details
        )