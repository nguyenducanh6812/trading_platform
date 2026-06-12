"""
Pydantic Models for API Requests and Responses
==============================================

Data models for request validation and response serialization.
"""

from pydantic import BaseModel, Field
from typing import List, Dict, Optional, Any, Union
from datetime import datetime
from enum import Enum


class RiskProfile(str, Enum):
    """Risk profile options."""
    AVERSE = "averse"
    NEUTRAL = "neutral"
    LOVER = "lover"


class OptimizationMethod(str, Enum):
    """Optimization method options."""
    TRADITIONAL = "traditional"
    SMART_GRID = "smart_grid"
    COMPARE = "compare"


class RebalancingFrequency(str, Enum):
    """Rebalancing frequency options."""
    MONTHLY = "monthly"
    QUARTERLY = "quarterly"
    YEARLY = "yearly"
    CUSTOM = "custom"


# Portfolio Optimization Models (Step 1)

class PortfolioOptimizationRequest(BaseModel):
    """Request model for portfolio optimization."""

    # Asset configuration - file uploads will be handled separately
    asset_codes: List[str] = Field(
        default=["BTC", "ETH"],
        description="List of asset codes (e.g., ['BTC', 'ETH'])",
        min_items=1,
        max_items=10
    )

    # Optimization parameters
    risk_profile: RiskProfile = Field(
        default=RiskProfile.NEUTRAL,
        description="Risk profile for optimization"
    )

    optimization_method: OptimizationMethod = Field(
        default=OptimizationMethod.TRADITIONAL,
        description="Optimization method to use"
    )

    rebalance_frequency: int = Field(
        default=1,
        ge=1,
        le=30,
        description="Rebalance frequency in days"
    )

    lookback_period: int = Field(
        default=7,
        ge=5,
        le=30,
        description="Lookback period for covariance calculation"
    )

    smart_grid_precision: Optional[int] = Field(
        default=2,
        ge=1,
        le=4,
        description="Precision for Smart Grid optimization (if applicable)"
    )

    use_custom_precision: bool = Field(
        default=False,
        description="Override weight precision"
    )

    weight_precision: Optional[int] = Field(
        default=16,
        ge=0,
        le=16,
        description="Weight precision override"
    )

    # Date range (optional)
    date_range: Optional[Dict[str, str]] = Field(
        default=None,
        description="Optional date range {'start': 'YYYY-MM-DD', 'end': 'YYYY-MM-DD'}"
    )

    # Risk-free rate
    risk_free_rate: float = Field(
        default=0.0001075,
        ge=0.0,
        le=0.1,
        description="Risk-free rate for optimization"
    )

    class Config:
        schema_extra = {
            "example": {
                "asset_codes": ["BTC", "ETH"],
                "risk_profile": "neutral",
                "optimization_method": "traditional",
                "rebalance_frequency": 1,
                "lookback_period": 7,
                "smart_grid_precision": 2,
                "use_custom_precision": False,
                "weight_precision": 16,
                "date_range": {
                    "start": "2021-04-15",
                    "end": "2023-12-31"
                },
                "risk_free_rate": 0.0001075
            }
        }


class PortfolioOptimizationSummary(BaseModel):
    """Summary statistics for portfolio optimization."""
    total_days: int
    decision_days: int
    final_portfolio_value: float
    total_return_pct: float
    annualized_return_pct: float
    annualized_volatility_pct: float
    sharpe_ratio: float
    prediction_usage_days: str
    asset_codes: List[str]


class PortfolioOptimizationMetadata(BaseModel):
    """Metadata for portfolio optimization results."""
    asset_codes: List[str]
    total_days: int
    decision_days: int
    first_prediction_date: str
    decision_start_date: str
    optimization_method: str
    risk_profile: str


class PortfolioOptimizationJobResponse(BaseModel):
    """Response model for portfolio optimization job submission."""
    job_id: str
    status: str
    message: str
    estimated_processing_time_minutes: int
    check_status_url: str
    download_url: Optional[str] = None


class PortfolioOptimizationStatusResponse(BaseModel):
    """Response model for portfolio optimization job status."""
    job_id: str
    status: str  # "pending", "processing", "completed", "failed"
    progress_percentage: Optional[float] = None
    current_step: Optional[str] = None
    message: Optional[str] = None
    started_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None
    download_url: Optional[str] = None
    error: Optional[str] = None


class PortfolioOptimizationResponse(BaseModel):
    """Response model for portfolio optimization (legacy - for backwards compatibility)."""
    status: str
    results: List[Dict[str, Any]]
    summary: PortfolioOptimizationSummary
    output_file: str
    metadata: PortfolioOptimizationMetadata
    processing_time_seconds: float


# Trading Simulation Models (Step 2)

class TradingSimulationRequest(BaseModel):
    """Request model for trading simulation."""

    # File configuration
    backtest_results_filename: Optional[str] = Field(
        default="portfolio_optimization_BTC_ETH_20250917_235212.xlsx",
        description="Filename of backtest results in trading_microservice/results/ directory"
    )

    # Account configuration
    total_capital: float = Field(
        default=1000.0,
        gt=0.0,
        description="Total capital available for trading"
    )

    trading_portion: float = Field(
        default=0.5,
        gt=0.0,
        lt=1.0,
        description="Portion of capital allocated to trading (0-1)"
    )

    # Trading parameters
    trading_fee: float = Field(
        default=0.0015,
        ge=0.0,
        le=0.01,
        description="Trading fee percentage (0.0015 = 0.15%)"
    )

    leverage_scale: float = Field(
        default=1.5,
        gt=0.0,
        le=10.0,
        description="Leverage multiplier for positions"
    )

    # Asset configuration
    btc_decimal_places: int = Field(
        default=3,
        ge=0,
        le=8,
        description="Decimal places for BTC quantities"
    )

    eth_decimal_places: int = Field(
        default=2,
        ge=0,
        le=8,
        description="Decimal places for ETH quantities"
    )

    # Rebalancing configuration
    rebalancing_frequency: RebalancingFrequency = Field(
        default=RebalancingFrequency.MONTHLY,
        description="Account rebalancing frequency"
    )

    custom_rebalancing_days: Optional[int] = Field(
        default=None,
        ge=1,
        le=365,
        description="Custom rebalancing period in days (if frequency is 'custom')"
    )

    # Output options
    include_summary: bool = Field(
        default=True,
        description="Include summary statistics in output"
    )

    class Config:
        schema_extra = {
            "example": {
                "total_capital": 1000.0,
                "trading_portion": 0.5,
                "trading_fee": 0.0015,
                "leverage_scale": 1.5,
                "btc_decimal_places": 3,
                "eth_decimal_places": 2,
                "rebalancing_frequency": "monthly",
                "custom_rebalancing_days": None,
                "include_summary": True
            }
        }


class TradingSimulationSummary(BaseModel):
    """Summary statistics for trading simulation."""
    initial_trading_balance: float
    final_trading_balance: float
    total_profit_loss: float
    total_return_pct: float
    total_fees_paid: float
    total_rebalancing_events: int
    average_daily_return: float
    volatility: float
    max_daily_return: float
    min_daily_return: float
    total_trading_days: int


class TradingSimulationMetadata(BaseModel):
    """Metadata for trading simulation results."""
    input_rows: int
    output_rows: int
    trading_config: Dict[str, Any]


class TradingSimulationJobResponse(BaseModel):
    """Response model for trading simulation job submission."""
    job_id: str
    status: str
    message: str
    estimated_processing_time_minutes: int
    check_status_url: str
    download_url: Optional[str] = None


class TradingSimulationStatusResponse(BaseModel):
    """Response model for trading simulation job status."""
    job_id: str
    status: str
    progress_percentage: Optional[int] = None
    message: str
    created_at: str
    updated_at: str
    processing_time_seconds: Optional[float] = None
    estimated_completion_time: Optional[str] = None
    download_url: Optional[str] = None
    error_details: Optional[str] = None


class TradingSimulationResponse(BaseModel):
    """Response model for trading simulation."""
    status: str
    results: List[Dict[str, Any]]
    summary: TradingSimulationSummary
    output_file: str
    metadata: TradingSimulationMetadata
    processing_time_seconds: float


# Common Models

class HealthCheckResponse(BaseModel):
    """Health check response model."""
    status: str
    service: str
    version: str
    environment: str
    timestamp: datetime


class ErrorResponse(BaseModel):
    """Error response model."""
    error: str
    message: str
    details: Optional[Dict[str, Any]] = None
    request_id: Optional[str] = None


class FileUploadResponse(BaseModel):
    """File upload response model."""
    filename: str
    size: int
    content_type: str
    asset_code: Optional[str] = None
    validation_status: str
    validation_details: Optional[Dict[str, Any]] = None


class ValidationResult(BaseModel):
    """Validation result model."""
    is_valid: bool
    errors: List[str] = []
    warnings: List[str] = []
    details: Optional[Dict[str, Any]] = None