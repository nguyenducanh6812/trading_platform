from typing import Dict, Type, Tuple, Optional
import numpy as np
from .objectives import OptimizationObjective, MaxReturnObjective, MaxSharpeObjective, MinVarianceObjective
from .constraint_strategies import ConstraintStrategy, LongConstraintStrategy, ShortConstraintStrategy, MarketNeutralConstraintStrategy
from ..custom_logging.logger import CustomLogger
from ..config.config_manager import ConfigManager

class StrategyFactory:
    """Factory for creating strategy combinations with strict configuration."""
    
    def __init__(self, logger: CustomLogger, config_manager: ConfigManager):
        """Initialize the factory."""
        self.logger = logger
        self.config_manager = config_manager
        
        # Register constraint strategies
        self._constraint_strategies: Dict[str, Type[ConstraintStrategy]] = {
            'long': LongConstraintStrategy,
            'short': ShortConstraintStrategy,
            'market_neutral': MarketNeutralConstraintStrategy
        }
        
        # Register objectives
        self._objectives: Dict[str, Type[OptimizationObjective]] = {
            'max_return': MaxReturnObjective,
            'max_sharpe': MaxSharpeObjective,
            'min_variance': MinVarianceObjective
        }
    
    def get_available_constraints(self) -> list:
        """Get available constraint strategy names."""
        return list(self._constraint_strategies.keys())
    
    def get_available_objectives(self) -> list:
        """Get available objective names."""
        return list(self._objectives.keys())
    
    def create_constraint_strategy(
        self, 
        constraint_type: str,
        predicted_returns: np.ndarray,
        cov_matrix: np.ndarray,
        rf_rate: float
    ) -> ConstraintStrategy:
        """Create a constraint strategy."""
        constraint_type = constraint_type.lower()
        
        if constraint_type not in self._constraint_strategies:
            raise ValueError(f"Unknown constraint type: {constraint_type}")
        
        # 🟢 STRICT: Get bounds from config (no overrides)
        config = self.config_manager.get_config()
        strategy_bounds = config.strategy.get_strategy_bounds(constraint_type)
        
        if strategy_bounds is None:
            raise ValueError(f"No explicit bounds configured for strategy: {constraint_type}")
        
        # Get weight precision from config
        config = self.config_manager.get_config()
        weight_precision = config.strategy.weight_precision
        
        strategy_class = self._constraint_strategies[constraint_type]
        return strategy_class(
            logger=self.logger,
            predicted_returns=predicted_returns,
            cov_matrix=cov_matrix,
            rf_rate=rf_rate,
            strategy_bounds=strategy_bounds,
            weight_precision=weight_precision
        )
    
    def create_objective(self, objective_type: str) -> OptimizationObjective:
        """Create an optimization objective."""
        objective_type = objective_type.lower()
        
        if objective_type not in self._objectives:
            raise ValueError(f"Unknown objective type: {objective_type}")
        
        objective_class = self._objectives[objective_type]
        return objective_class()
    
    def optimize_strategy_combination(
        self,
        constraint_type: str,
        objective_type: str,
        predicted_returns: np.ndarray,
        cov_matrix: np.ndarray,
        rf_rate: float
    ) -> Tuple[np.ndarray, float, float, float]:
        """
        Optimize a specific strategy-objective combination.
        
        Args:
            constraint_type: Type of constraint ('long', 'short', 'market_neutral')
            objective_type: Type of objective ('max_return', 'max_sharpe', 'min_variance')
            predicted_returns: Predicted daily asset returns (e.g., from ARIMA models)
            cov_matrix: Covariance matrix
            rf_rate: Risk-free rate
            
        Returns:
            Tuple of (weights, expected_return, volatility, sharpe_ratio)
        """
        # Create constraint strategy
        constraint_strategy = self.create_constraint_strategy(
            constraint_type, predicted_returns, cov_matrix, rf_rate
        )
        
        # Create objective
        objective = self.create_objective(objective_type)
        
        # Optimize
        result = constraint_strategy.optimize_with_objective(objective)
        
        return result