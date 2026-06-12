"""
Updated Config Manager with ARIMA Support
=========================================

File: crypto_trading_model/config/config_manager.py

Updated to handle ARIMA configuration parameters.
"""

from pathlib import Path
import yaml
from typing import Dict, Optional, Tuple, Any
from .models import Config, RiskFreeConfig, RiskProfileConfig, StrategyConfig, BacktestConfig, OutputConfig, ArimaConfig, ConfigValidationError
from ..custom_logging.logger import CustomLogger

class ConfigurationError(Exception):
    """Exception raised for configuration-related errors."""
    pass

class ConfigManager:
    """Manages loading and validation of configuration from YAML."""
    
    CONFIG_LOCATIONS = [
        Path("config.yaml"),              # Current directory
        Path("../config.yaml"),           # Parent directory
        Path("../../config.yaml"),        # Project root
    ]
    
    _instance = None  # Singleton instance
    _config = None    # Cached config
    
    def __new__(cls, logger: Optional[CustomLogger] = None):
        """Create or return the singleton instance."""
        if cls._instance is None:
            cls._instance = super(ConfigManager, cls).__new__(cls)
            cls._instance._initialized = False
        return cls._instance
    
    def __init__(self, logger: Optional[CustomLogger] = None):
        """Initialize the ConfigManager."""
        if self._initialized:
            return
            
        self.logger = logger
        self.config_path = self._find_config_file()
        if not self.config_path:
            raise ConfigurationError("Configuration file 'config.yaml' not found in any valid location")
        
        self._initialized = True

    def _find_config_file(self) -> Path:
        """Find the configuration file in valid locations."""
        base_path = Path.cwd()
        for config_path in self.CONFIG_LOCATIONS:
            full_path = base_path / config_path
            if full_path.exists():
                if self.logger:
                    self.logger.info(f"Found configuration file at: {full_path}")
                return full_path
        return None

    def load_config(self) -> Config:
        """Load and validate the configuration."""
        try:
            with self.config_path.open('r') as file:
                config_data = yaml.safe_load(file)
                return self._validate_config(config_data)
        except FileNotFoundError:
            raise ConfigurationError(f"Configuration file not found at: {self.config_path}")
        except yaml.YAMLError as e:
            raise ConfigurationError(f"Invalid YAML configuration: {e}")
    
    def get_config(self) -> Config:
        """Get the configuration, loading it if necessary."""
        if self._config is None:
            self._config = self.load_config()
        return self._config

    def _validate_config(self, config_data: dict) -> Config:
        """Validate and convert configuration data to Config object."""
        try:
            # Extract strategy bounds - handle your exact YAML structure
            strategy_bounds = {}
            if 'strategy' in config_data and 'strategy_bounds' in config_data['strategy']:
                for strategy, bounds in config_data['strategy']['strategy_bounds'].items():
                    # Your config uses lists like: long: [-1, 2]
                    if not isinstance(bounds, list) or len(bounds) != 2:
                        raise ConfigValidationError(f"Bounds for strategy '{strategy}' must be a list of [lower, upper]")
                    strategy_bounds[strategy.lower()] = tuple(bounds)
            
            # Check required strategies
            required_strategies = ['long', 'short', 'market_neutral', 'gmvp', 'max_return', 'sharpe_max']
            missing_strategies = [s for s in required_strategies if s not in strategy_bounds]
            if missing_strategies:
                raise ConfigValidationError(
                    f"Missing bounds configuration for required strategies: {missing_strategies}. "
                    "All strategies must have bounds defined in strategy_bounds."
                )
            
            if self.logger:
                self.logger.info(f"Loaded strategy-specific bounds: {strategy_bounds}")

            # Extract and validate ARIMA configuration
            arima_config = self._extract_arima_config(config_data)
            
            # Create and validate the config object
            config = Config(
                data_file_path=config_data['data']['file_path'],
                risk_free=RiskFreeConfig(
                    annual_rates=config_data['risk_free']['annual_rates'],
                    default_rate=config_data['risk_free']['default_rate']
                ),
                risk_profiles=RiskProfileConfig(
                    available=config_data['risk_profiles']['available'],
                    default=config_data['risk_profiles']['default']
                ),
                strategy=StrategyConfig(
                    lookback_period=config_data['strategy']['lookback_period'],
                    rebalancing_frequency=config_data['strategy']['rebalancing_frequency'],
                    strategy_bounds=strategy_bounds,
                    weight_precision=config_data['strategy'].get('weight_precision', 2)  # Default to 2 decimal places
                ),
                backtest=BacktestConfig(
                    initial_capital=config_data['backtest']['initial_capital'],
                    run_all_combinations=config_data['backtest']['run_all_combinations']
                ),
                output=OutputConfig(
                    export_excel=config_data['output']['export_excel'],
                    excel_path=config_data['output']['excel_path'],
                    output_dir=config_data['output']['output_dir']
                ),
                arima=arima_config  # Add ARIMA configuration
            )
            
            if self.logger:
                self.logger.info(f"ARIMA configuration loaded:")
                self.logger.info(f"  p_range: {arima_config.p_range}")
                self.logger.info(f"  d_range: {arima_config.d_range}")
                self.logger.info(f"  q_range: {arima_config.q_range}")
                self.logger.info(f"  Total combinations: {arima_config.get_total_combinations()}")
            
            return config
            
        except ConfigValidationError as e:
            if self.logger:
                self.logger.error(f"Configuration validation error: {e}")
            raise ConfigurationError(f"Configuration validation failed: {e}")
        except KeyError as e:
            if self.logger:
                self.logger.error(f"Missing configuration key: {e}")
            raise ConfigurationError(f"Configuration missing required key: {e}")
        except Exception as e:
            if self.logger:
                self.logger.error(f"Invalid configuration: {e}")
            raise ConfigurationError(f"Configuration error: {e}")

    def _extract_arima_config(self, config_data: dict) -> ArimaConfig:
        """Extract and validate ARIMA configuration with defaults."""
        
        # Default ARIMA configuration
        default_arima = {
            'p_range': [0, 8],
            'd_range': [0, 1], 
            'q_range': [0, 8],
            'significance_level': 0.1,
            'max_iterations': 1000,
            'convergence_tolerance': 1e-9
        }
        
        # Get ARIMA config from file or use defaults
        arima_data = config_data.get('arima', {})
        
        # Merge with defaults
        for key, default_value in default_arima.items():
            if key not in arima_data:
                arima_data[key] = default_value
                if self.logger:
                    self.logger.info(f"Using default ARIMA {key}: {default_value}")
        
        # Convert range lists to tuples
        p_range = tuple(arima_data['p_range']) if isinstance(arima_data['p_range'], list) else arima_data['p_range']
        d_range = tuple(arima_data['d_range']) if isinstance(arima_data['d_range'], list) else arima_data['d_range']
        q_range = tuple(arima_data['q_range']) if isinstance(arima_data['q_range'], list) else arima_data['q_range']
        
        return ArimaConfig(
            p_range=p_range,
            d_range=d_range,
            q_range=q_range,
            significance_level=arima_data['significance_level'],
            max_iterations=arima_data['max_iterations'],
            convergence_tolerance=arima_data['convergence_tolerance']
        )

    def validate_config_completeness(self):
        """Validate that the configuration is complete with all required strategies."""
        config = self.get_config()
        
        # Define required strategies
        required_strategies = ['long', 'short', 'market_neutral', 'gmvp', 'max_return', 'sharpe_max']
        
        # Check if all required strategies have bounds defined
        missing_strategies = []
        for strategy in required_strategies:
            if not config.strategy.get_strategy_bounds(strategy):
                missing_strategies.append(strategy)
        
        if missing_strategies:
            error_msg = (
                f"Missing bounds configuration for required strategies: {missing_strategies}. "
                "All strategies must have bounds defined in 'strategy_bounds' section."
            )
            if self.logger:
                self.logger.error(error_msg)
            raise ConfigurationError(error_msg)

    def get_arima_config(self) -> ArimaConfig:
        """Get ARIMA configuration specifically."""
        return self.get_config().arima

    def update_arima_config(self, **kwargs) -> None:
        """
        Update ARIMA configuration parameters dynamically.
        
        Args:
            **kwargs: ARIMA parameters to update (p_range, d_range, q_range, etc.)
        """
        if self._config is None:
            self._config = self.load_config()
        
        # Update ARIMA config
        current_arima = self._config.arima
        
        # Create new ARIMA config with updated values
        arima_dict = {
            'p_range': kwargs.get('p_range', current_arima.p_range),
            'd_range': kwargs.get('d_range', current_arima.d_range),
            'q_range': kwargs.get('q_range', current_arima.q_range),
            'significance_level': kwargs.get('significance_level', current_arima.significance_level),
            'max_iterations': kwargs.get('max_iterations', current_arima.max_iterations),
            'convergence_tolerance': kwargs.get('convergence_tolerance', current_arima.convergence_tolerance)
        }
        
        # Create new config
        updated_arima = ArimaConfig(**arima_dict)
        
        # Update the cached config
        self._config.arima = updated_arima
        
        if self.logger:
            self.logger.info(f"Updated ARIMA configuration: {kwargs}")