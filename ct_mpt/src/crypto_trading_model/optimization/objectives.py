from abc import ABC, abstractmethod
import numpy as np
from typing import Callable

class OptimizationObjective(ABC):
    """Base class for optimization objectives."""
    
    @abstractmethod
    def get_objective_function(self, predicted_returns: np.ndarray, cov_matrix: np.ndarray, rf_rate: float) -> Callable[[np.ndarray], float]:
        """
        Get the objective function to minimize.
        
        Args:
            predicted_returns: Array of predicted daily asset returns (e.g., from ARIMA models)
            cov_matrix: Covariance matrix
            rf_rate: Risk-free rate
            
        Returns:
            Callable that takes weights and returns objective value (to minimize)
        """
        pass
    
    @abstractmethod
    def get_name(self) -> str:
        """Get the name of this objective."""
        pass

class MaxReturnObjective(OptimizationObjective):
    """Objective to maximize portfolio return."""
    
    def get_objective_function(self, predicted_returns: np.ndarray, cov_matrix: np.ndarray, rf_rate: float) -> Callable[[np.ndarray], float]:
        """Return negative portfolio return for minimization."""
        def objective(weights: np.ndarray) -> float:
            return -np.sum(weights * predicted_returns)
        return objective
    
    def get_name(self) -> str:
        return "MaxReturn"

class MaxSharpeObjective(OptimizationObjective):
    """Objective to maximize Sharpe ratio."""
    
    def get_objective_function(self, predicted_returns: np.ndarray, cov_matrix: np.ndarray, rf_rate: float) -> Callable[[np.ndarray], float]:
        """Return negative Sharpe ratio for minimization."""
        def objective(weights: np.ndarray) -> float:
            port_return = np.sum(weights * predicted_returns)
            port_vol = np.sqrt(np.dot(weights.T, np.dot(cov_matrix, weights)))
            if port_vol == 0:
                return float('inf')
            sharpe = (port_return - rf_rate) / port_vol
            return -sharpe
        return objective
    
    def get_name(self) -> str:
        return "MaxSharpe"

class MinVarianceObjective(OptimizationObjective):
    """Objective to minimize portfolio variance."""
    
    def get_objective_function(self, predicted_returns: np.ndarray, cov_matrix: np.ndarray, rf_rate: float) -> Callable[[np.ndarray], float]:
        """Return portfolio variance for minimization."""
        def objective(weights: np.ndarray) -> float:
            return np.dot(weights.T, np.dot(cov_matrix, weights))
        return objective
    
    def get_name(self) -> str:
        return "MinVariance"