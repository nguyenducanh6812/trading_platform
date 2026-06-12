"""
Simplified Constraint Strategies for Microservice
=================================================

Simplified version of the constraint strategies from the original implementation.
"""

from abc import ABC, abstractmethod
import numpy as np
from typing import Dict, Any, Optional, Tuple, List
from scipy.optimize import minimize

from app.optimization.objectives import OptimizationObjective
from app.utils.logger import get_logger

logger = get_logger(__name__)

class ConstraintStrategy(ABC):
    """Base class for constraint strategies."""

    def __init__(self, predicted_returns: np.ndarray, cov_matrix: np.ndarray,
                 rf_rate: float, weight_bounds: Tuple[float, float]):
        self.predicted_returns = predicted_returns
        self.cov_matrix = cov_matrix
        self.rf_rate = rf_rate
        self.num_assets = len(predicted_returns)
        self.weight_bounds = weight_bounds

    @abstractmethod
    def get_constraints(self) -> list:
        """Get the constraint functions for this strategy."""
        pass

    @abstractmethod
    def get_initial_weights(self) -> np.ndarray:
        """Get initial weights for optimization."""
        pass

    @abstractmethod
    def get_strategy_name(self) -> str:
        """Get the name of this constraint strategy."""
        pass

    def get_multiple_initial_guesses(self) -> List[np.ndarray]:
        """Get multiple initial guesses for robust optimization."""
        # Default implementation returns single guess
        return [self.get_initial_weights()]

    def optimize_with_objective(self, objective: OptimizationObjective) -> Tuple[np.ndarray, float, float, float]:
        """Optimize portfolio with given objective using multiple initial guesses."""
        objective_func = objective.get_objective_function(self.predicted_returns, self.cov_matrix, self.rf_rate)
        bounds = [self.weight_bounds] * self.num_assets
        constraints = self.get_constraints()

        # Try multiple initial guesses to avoid local optima
        initial_guesses = self.get_multiple_initial_guesses()

        best_result = None
        best_objective_value = float('inf')

        for i, initial_weights in enumerate(initial_guesses):
            try:
                result = minimize(
                    objective_func,
                    initial_weights,
                    method='SLSQP',
                    bounds=bounds,
                    constraints=constraints,
                    options={'maxiter': 1000, 'ftol': 1e-9, 'disp': False}
                )

                if result.success and result.fun < best_objective_value:
                    best_objective_value = result.fun
                    best_result = result

            except Exception as e:
                logger.warning(f"{self.get_strategy_name()}-{objective.get_name()} optimization failed for guess {i+1}: {e}")
                continue

        if best_result is None or not best_result.success:
            raise ValueError(f"All optimization attempts failed for {self.get_strategy_name()}-{objective.get_name()}")

        weights = best_result.x
        port_return = np.sum(weights * self.predicted_returns)
        port_vol = np.sqrt(np.dot(weights.T, np.dot(self.cov_matrix, weights)))
        sharpe = (port_return - self.rf_rate) / port_vol if port_vol > 0 else float('inf')

        return weights, port_return, port_vol, sharpe

class LongConstraintStrategy(ConstraintStrategy):
    """Long strategy with strategic initial guesses covering full feasible space."""

    def get_constraints(self) -> list:
        """Weights must sum to 1."""
        return [{'type': 'eq', 'fun': lambda w: np.sum(w) - 1}]

    def get_initial_weights(self) -> np.ndarray:
        """Equal weights summing to 1."""
        return np.ones(self.num_assets) / self.num_assets

    def get_multiple_initial_guesses(self) -> List[np.ndarray]:
        """Get 5 strategic initial guesses covering full feasible space like the original."""
        guesses = []
        lower, upper = self.weight_bounds

        if self.num_assets == 2:
            # THE ESSENTIAL 5 for Long Strategy (sum = 1) - exactly like original
            candidates = [
                # 1. BALANCED TRADITIONAL - Center of feasible space
                [0.5, 0.5],

                # 2. EXTREME CORNER 1 - Max first asset (leveraged if upper > 1)
                [min(upper, 1.5), max(1 - min(upper, 1.5), -0.5)],

                # 3. EXTREME CORNER 2 - Max second asset (leveraged if upper > 1)
                [max(1 - min(upper, 1.5), -0.5), min(upper, 1.5)],

                # 4. TRADITIONAL DIRECTIONAL - BTC heavy
                [min(0.7, upper), max(0.3, lower)],

                # 5. TRADITIONAL DIRECTIONAL - ETH heavy
                [max(0.3, lower), min(0.7, upper)]
            ]

            # Validate candidates and ensure sum = 1
            for candidate in candidates:
                w1, w2 = candidate
                # Adjust to ensure exact constraint satisfaction
                if abs(w1 + w2 - 1) > 1e-10:
                    # Proportionally adjust to sum to 1
                    total = w1 + w2
                    if total > 0:
                        w1, w2 = w1/total, w2/total
                    else:
                        w1, w2 = 0.5, 0.5

                weights = np.array([w1, w2])

                # Check bounds and constraint
                if (lower <= w1 <= upper and lower <= w2 <= upper and
                    abs(np.sum(weights) - 1) < 1e-10):
                    guesses.append(weights)

                # Stop at 5 guesses max
                if len(guesses) >= 5:
                    break
        else:
            # Multi-asset: fallback to equal weights
            guesses.append(self.get_initial_weights())

        # Ensure we have at least one guess
        if not guesses:
            guesses.append(self.get_initial_weights())

        logger.info(f"Long Strategy: Using {len(guesses)} strategic initial guesses")
        for i, guess in enumerate(guesses):
            logger.info(f"  Long Guess {i+1}: {guess} (sum: {np.sum(guess):.6f})")

        return guesses

    def get_strategy_name(self) -> str:
        return "Long"

class ShortConstraintStrategy(ConstraintStrategy):
    """Short strategy with strategic initial guesses covering full feasible space."""

    def get_constraints(self) -> list:
        """Weights must sum to -1."""
        return [{'type': 'eq', 'fun': lambda w: np.sum(w) + 1}]

    def get_initial_weights(self) -> np.ndarray:
        """Equal weights summing to -1."""
        return np.ones(self.num_assets) * (-1 / self.num_assets)

    def get_multiple_initial_guesses(self) -> List[np.ndarray]:
        """Get 5 strategic initial guesses covering full feasible space like the original."""
        guesses = []
        lower, upper = self.weight_bounds

        if self.num_assets == 2:
            # THE ESSENTIAL 5 for Short Strategy (sum = -1) - exactly like original
            candidates = [
                # 1. BALANCED TRADITIONAL SHORT - Center of feasible space
                [-0.5, -0.5],

                # 2. EXTREME CORNER 1 - Max short first asset (leveraged if lower < -1)
                [max(lower, -1.5), min(-1 - max(lower, -1.5), 0.5)],

                # 3. EXTREME CORNER 2 - Max short second asset (leveraged if lower < -1)
                [min(-1 - max(lower, -1.5), 0.5), max(lower, -1.5)],

                # 4. TRADITIONAL DIRECTIONAL - BTC heavy short
                [max(-0.7, lower), min(-0.3, upper)],

                # 5. TRADITIONAL DIRECTIONAL - ETH heavy short
                [min(-0.3, upper), max(-0.7, lower)]
            ]

            # Validate candidates and ensure sum = -1
            for candidate in candidates:
                w1, w2 = candidate
                # Adjust to ensure exact constraint satisfaction
                if abs(w1 + w2 + 1) > 1e-10:
                    # Proportionally adjust to sum to -1
                    total = w1 + w2
                    if total != 0:
                        factor = -1 / total
                        w1, w2 = w1 * factor, w2 * factor
                    else:
                        w1, w2 = -0.5, -0.5

                weights = np.array([w1, w2])

                # Check bounds and constraint
                if (lower <= w1 <= upper and lower <= w2 <= upper and
                    abs(np.sum(weights) + 1) < 1e-10):
                    guesses.append(weights)

                # Stop at 5 guesses max
                if len(guesses) >= 5:
                    break
        else:
            # Multi-asset: fallback to equal weights
            guesses.append(self.get_initial_weights())

        # Ensure we have at least one guess
        if not guesses:
            guesses.append(self.get_initial_weights())

        logger.info(f"Short Strategy: Using {len(guesses)} strategic initial guesses")
        for i, guess in enumerate(guesses):
            logger.info(f"  Short Guess {i+1}: {guess} (sum: {np.sum(guess):.6f})")

        return guesses

    def get_strategy_name(self) -> str:
        return "Short"

class MarketNeutralConstraintStrategy(ConstraintStrategy):
    """Market neutral strategy: weights must sum to 0."""

    def get_constraints(self) -> list:
        return [{'type': 'eq', 'fun': lambda w: np.sum(w)}]

    def get_initial_weights(self) -> np.ndarray:
        if self.num_assets == 2:
            return np.array([-0.5, 0.5])
        else:
            weights = np.zeros(self.num_assets)
            half = self.num_assets // 2
            weights[:half] = 0.5 / half
            weights[half:] = -0.5 / (self.num_assets - half)
            return weights

    def get_strategy_name(self) -> str:
        return "Market_neutral"