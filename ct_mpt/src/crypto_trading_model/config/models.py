"""
Updated Configuration Models with ARIMA Parameters
=================================================

File: crypto_trading_model/config/models.py

Updated to include ARIMA configuration parameters.
"""

from dataclasses import dataclass, field
from typing import List, Dict, Tuple, Optional, Any

class ConfigValidationError(Exception):
    """Exception raised when configuration validation fails."""
    pass

@dataclass
class RiskFreeConfig:
    annual_rates: List[float]
    default_rate: float
    
    def __post_init__(self):
        """Validate the configuration after initialization."""
        if not isinstance(self.annual_rates, list):
            raise ConfigValidationError("annual_rates must be a list of float values")
        if not isinstance(self.default_rate, (int, float)):
            raise ConfigValidationError("default_rate must be a numeric value")

@dataclass
class RiskProfileConfig:
    available: List[str]
    default: str
    
    def __post_init__(self):
        """Validate the configuration after initialization."""
        if not isinstance(self.available, list) or not all(isinstance(x, str) for x in self.available):
            raise ConfigValidationError("available risk profiles must be a list of strings")
        if not isinstance(self.default, str):
            raise ConfigValidationError("default risk profile must be a string")
        if self.default not in self.available:
            raise ConfigValidationError(f"default risk profile '{self.default}' must be one of: {self.available}")

@dataclass
class ArimaConfig:
    """Configuration for ARIMA model parameters."""
    p_range: Tuple[int, int] = (0, 8)      # (min, max) for autoregressive order
    d_range: Tuple[int, int] = (0, 1)      # (min, max) for differencing order
    q_range: Tuple[int, int] = (0, 8)      # (min, max) for moving average order
    significance_level: float = 0.1        # Significance level for coefficient testing
    max_iterations: int = 1000             # Maximum optimization iterations
    convergence_tolerance: float = 1e-9    # Convergence tolerance
    
    def __post_init__(self):
        """Validate ARIMA configuration parameters."""
        # Validate p_range
        if not isinstance(self.p_range, (tuple, list)) or len(self.p_range) != 2:
            raise ConfigValidationError("p_range must be a tuple/list of (min, max)")
        if not all(isinstance(x, int) and x >= 0 for x in self.p_range):
            raise ConfigValidationError("p_range values must be non-negative integers")
        if self.p_range[0] > self.p_range[1]:
            raise ConfigValidationError("p_range: min cannot be greater than max")
        
        # Validate d_range
        if not isinstance(self.d_range, (tuple, list)) or len(self.d_range) != 2:
            raise ConfigValidationError("d_range must be a tuple/list of (min, max)")
        if not all(isinstance(x, int) and x >= 0 for x in self.d_range):
            raise ConfigValidationError("d_range values must be non-negative integers")
        if self.d_range[0] > self.d_range[1]:
            raise ConfigValidationError("d_range: min cannot be greater than max")
        
        # Validate q_range
        if not isinstance(self.q_range, (tuple, list)) or len(self.q_range) != 2:
            raise ConfigValidationError("q_range must be a tuple/list of (min, max)")
        if not all(isinstance(x, int) and x >= 0 for x in self.q_range):
            raise ConfigValidationError("q_range values must be non-negative integers")
        if self.q_range[0] > self.q_range[1]:
            raise ConfigValidationError("q_range: min cannot be greater than max")
        
        # Validate other parameters
        if not isinstance(self.significance_level, (int, float)) or not 0 < self.significance_level < 1:
            raise ConfigValidationError("significance_level must be a float between 0 and 1")
        if not isinstance(self.max_iterations, int) or self.max_iterations <= 0:
            raise ConfigValidationError("max_iterations must be a positive integer")
        if not isinstance(self.convergence_tolerance, (int, float)) or self.convergence_tolerance <= 0:
            raise ConfigValidationError("convergence_tolerance must be a positive number")
    
    def get_p_range(self) -> range:
        """Get p parameter range as Python range object."""
        return range(self.p_range[0], self.p_range[1] + 1)
    
    def get_d_range(self) -> range:
        """Get d parameter range as Python range object."""
        return range(self.d_range[0], self.d_range[1] + 1)
    
    def get_q_range(self) -> range:
        """Get q parameter range as Python range object."""
        return range(self.q_range[0], self.q_range[1] + 1)
    
    def get_total_combinations(self) -> int:
        """Get total number of parameter combinations to test."""
        p_count = self.p_range[1] - self.p_range[0] + 1
        d_count = self.d_range[1] - self.d_range[0] + 1
        q_count = self.q_range[1] - self.q_range[0] + 1
        return p_count * d_count * q_count

@dataclass
class StrategyConfig:
    lookback_period: int
    rebalancing_frequency: int
    strategy_bounds: Dict[str, Tuple[float, float]] = field(default_factory=dict)
    weight_precision: int = 2  # Number of decimal places for portfolio weights (default: 2 for 0.01 precision)
    
    def __post_init__(self):
        """Validate the configuration after initialization."""
        if not isinstance(self.lookback_period, int) or self.lookback_period <= 0:
            raise ConfigValidationError("lookback_period must be a positive integer")
        if not isinstance(self.rebalancing_frequency, int) or self.rebalancing_frequency <= 0:
            raise ConfigValidationError("rebalancing_frequency must be a positive integer")
        if not isinstance(self.weight_precision, int) or self.weight_precision < 0 or self.weight_precision > 16:
            raise ConfigValidationError("weight_precision must be an integer between 0 and 16")
            
        # Validate strategy bounds
        if not isinstance(self.strategy_bounds, dict):
            raise ConfigValidationError("strategy_bounds must be a dictionary")
        for strategy, bounds in self.strategy_bounds.items():
            if not isinstance(bounds, (tuple, list)) or len(bounds) != 2:
                raise ConfigValidationError(f"Bounds for strategy '{strategy}' must be a tuple of (lower, upper)")
            if bounds[0] > bounds[1]:
                raise ConfigValidationError(f"Strategy '{strategy}' bounds: lower bound {bounds[0]} cannot be greater than upper bound {bounds[1]}")
    
    def get_strategy_bounds(self, strategy_id: str) -> Optional[Tuple[float, float]]:
        """Get bounds for a specific strategy."""
        return self.strategy_bounds.get(strategy_id.lower())

@dataclass
class BacktestConfig:
    initial_capital: float
    run_all_combinations: bool
    
    def __post_init__(self):
        """Validate the configuration after initialization."""
        if not isinstance(self.initial_capital, (int, float)) or self.initial_capital <= 0:
            raise ConfigValidationError("initial_capital must be a positive number")
        if not isinstance(self.run_all_combinations, bool):
            raise ConfigValidationError("run_all_combinations must be a boolean")

@dataclass
class OutputConfig:
    export_excel: bool
    excel_path: str
    output_dir: str
    
    def __post_init__(self):
        """Validate the configuration after initialization."""
        if not isinstance(self.export_excel, bool):
            raise ConfigValidationError("export_excel must be a boolean")
        if not isinstance(self.excel_path, str) or not self.excel_path:
            raise ConfigValidationError("excel_path must be a non-empty string")
        if not isinstance(self.output_dir, str) or not self.output_dir:
            raise ConfigValidationError("output_dir must be a non-empty string")

@dataclass
class Config:
    data_file_path: str
    risk_free: RiskFreeConfig
    risk_profiles: RiskProfileConfig
    strategy: StrategyConfig
    backtest: BacktestConfig
    output: OutputConfig
    arima: ArimaConfig = field(default_factory=ArimaConfig)  # Add ARIMA config with defaults
    
    def __post_init__(self):
        """Validate the configuration after initialization."""
        if not isinstance(self.data_file_path, str) or not self.data_file_path:
            raise ConfigValidationError("data_file_path must be a non-empty string")
        
        # Other components have their own validation