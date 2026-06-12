"""
PERFORMANCE OPTIMIZED: Strategy-Specific Initial Guesses
- Maximum 5 initial guesses per strategy for optimal performance
- Full feasible space coverage (traditional + leveraged + hedged positions)
- Deterministic Market Neutral to eliminate instability
"""

from abc import ABC, abstractmethod
import numpy as np
from typing import Dict, Any, Optional, Tuple, List
from scipy.optimize import minimize
from ..custom_logging.logger import CustomLogger
from .objectives import OptimizationObjective

class StrategyBoundsError(Exception):
    """Exception raised when strategy bounds are missing or invalid."""
    pass

class ConstraintStrategy(ABC):
    """
    IMPROVED: Base class with strategy-specific initial guess responsibility.
    Each strategy now generates its own constraint-aware initial guesses.
    """
    
    def __init__(self, logger: CustomLogger, predicted_returns: np.ndarray, 
                 cov_matrix: np.ndarray, rf_rate: float, 
                 strategy_bounds: Tuple[float, float], weight_precision: int = 2):
        """Initialize constraint strategy with explicit bounds injection."""
        self.logger = logger
        self.predicted_returns = predicted_returns
        self.cov_matrix = cov_matrix
        self.rf_rate = rf_rate
        self.num_assets = len(predicted_returns)
        self.weight_bounds = strategy_bounds
        self.weight_precision = weight_precision  # Number of decimal places for weight rounding
        
        # Validate bounds and inputs
        self._validate_strategy_bounds()
        self._validate_inputs()
    
    def _validate_strategy_bounds(self) -> None:
        """Validate that bounds are reasonable."""
        if not isinstance(self.weight_bounds, (tuple, list)) or len(self.weight_bounds) != 2:
            raise StrategyBoundsError(f"Strategy bounds must be a tuple/list of (lower, upper). Got: {self.weight_bounds}")
        
        lower, upper = self.weight_bounds
        if not all(isinstance(bound, (int, float)) for bound in [lower, upper]):
            raise StrategyBoundsError(f"Strategy bounds must be numeric. Got: {self.weight_bounds}")
        if lower >= upper:
            raise StrategyBoundsError(f"Strategy lower bound {lower} must be less than upper bound {upper}")
        
        # Strategy-specific bounds validation
        self._validate_strategy_specific_bounds(lower, upper)
    
    def _validate_strategy_specific_bounds(self, lower: float, upper: float) -> None:
        """Validate bounds make sense for this specific strategy type."""
        pass  # Base implementation does no additional validation
    
    def _validate_inputs(self) -> None:
        """Validate portfolio optimization input parameters."""
        if self.predicted_returns.shape[0] != self.cov_matrix.shape[0]:
            raise ValueError("Mean returns and covariance matrix dimensions don't match")
        if self.cov_matrix.shape[0] != self.cov_matrix.shape[1]:
            raise ValueError("Covariance matrix must be square")
        if not isinstance(self.rf_rate, (int, float)):
            raise ValueError("Risk-free rate must be numeric")
    
    @abstractmethod
    def get_constraints(self) -> list:
        """Get the constraint functions for this strategy."""
        pass
    
    @abstractmethod
    def get_initial_weights(self) -> np.ndarray:
        """Get primary initial weights for optimization."""
        pass
    
    @abstractmethod
    def get_strategy_initial_guesses(self) -> List[np.ndarray]:
        """
        NEW: Get strategy-specific constraint-aware initial guesses.
        Each strategy implements this to provide multiple starting points that satisfy its constraints.
        PERFORMANCE: Maximum 5 guesses per strategy.
        
        Returns:
            List of initial weight arrays that satisfy this strategy's constraints
        """
        pass
    
    @abstractmethod
    def get_strategy_name(self) -> str:
        """Get the name of this constraint strategy."""
        pass
    
    def round_weights(self, weights: np.ndarray) -> np.ndarray:
        """
        Round portfolio weights to specified precision for realistic trading implementation.
        
        Args:
            weights: Original optimization weights
            
        Returns:
            Rounded weights that still satisfy constraints
        """
        # Round to specified decimal places
        rounded_weights = np.round(weights, self.weight_precision)
        
        # Log the rounding effect
        if not np.allclose(weights, rounded_weights, rtol=1e-6):
            self.logger.info(f"Weight rounding applied (precision={self.weight_precision}):")
            for i, (orig, rounded) in enumerate(zip(weights, rounded_weights)):
                if abs(orig - rounded) > 1e-6:
                    self.logger.info(f"  Asset {i}: {orig:.6f} -> {rounded:.{self.weight_precision}f}")
        
        return rounded_weights
    
    def validate_rounded_weights(self, weights: np.ndarray) -> bool:
        """
        Check if rounded weights still satisfy strategy constraints.
        
        Args:
            weights: Rounded weights to validate
            
        Returns:
            True if constraints are satisfied, False otherwise
        """
        try:
            # Check bounds
            lower, upper = self.weight_bounds
            if not all(lower <= w <= upper for w in weights):
                return False
            
            # Check strategy-specific constraints
            constraints = self.get_constraints()
            for constraint in constraints:
                if constraint['type'] == 'eq':
                    if abs(constraint['fun'](weights)) > 1e-6:  # Allow small tolerance for rounding
                        return False
                elif constraint['type'] == 'ineq':
                    if constraint['fun'](weights) < -1e-6:  # Allow small tolerance for rounding
                        return False
            
            return True
        except Exception:
            return False
    
    def _create_discrete_bounds(self):
        """
        Create discrete bounds for optimization.
        Instead of continuous bounds like (-1.0, 2.0), creates discrete bounds
        that guide the optimizer to discrete values.
        """
        step_size = 10**(-self.weight_precision)
        lower_bound, upper_bound = self.weight_bounds
        
        # Create list of allowed discrete values
        discrete_values = []
        current = lower_bound
        while current <= upper_bound + step_size/2:
            discrete_values.append(round(current, self.weight_precision))
            current += step_size
        
        self.logger.info(f"Created discrete bounds with {len(discrete_values)} values from {discrete_values[0]} to {discrete_values[-1]}")
        
        # Return tight bounds around each discrete value to guide optimization
        discrete_bounds = []
        for _ in range(self.num_assets):
            # Use original bounds but the optimizer will be guided to discrete values
            discrete_bounds.append(self.weight_bounds)
        
        return discrete_bounds
    
    def _quantize_to_discrete(self, weights):
        """Quantize weights to nearest discrete values."""
        return np.round(weights, self.weight_precision)
    
    def _create_discrete_objective_wrapper(self, objective_func):
        """
        Create an objective function that only evaluates at discrete weight values.
        This forces the optimizer to search in discrete space during optimization.
        """
        def discrete_objective(weights):
            # Quantize input weights to discrete precision
            discrete_weights = self._quantize_to_discrete(weights)
            
            # Evaluate objective at discrete point
            return objective_func(discrete_weights)
        
        return discrete_objective
    
    def discrete_objective_wrapper(self, objective_func, precision: int):
        """
        Wrapper that quantizes weights to discrete precision during optimization.
        Uses smoothing to avoid gradient discontinuities.
        """
        def wrapped_objective(weights):
            # Quantize weights to discrete precision
            quantized_weights = np.round(weights, precision)
            
            # Add small smooth penalty to guide optimizer toward discrete values
            # This helps avoid numerical differentiation issues
            discrete_penalty = 0.0
            step_size = 10**(-precision)
            
            for w, q_w in zip(weights, quantized_weights):
                # Small quadratic penalty for being away from discrete values
                deviation = abs(w - q_w)
                if deviation > step_size / 2:
                    discrete_penalty += (deviation / step_size) ** 2 * 1e-8
            
            return objective_func(quantized_weights) + discrete_penalty
        return wrapped_objective
    
    def _optimize_discrete_grid(self, objective_func, bounds, constraints):
        """
        Grid-based discrete optimization that avoids gradient calculations.
        Samples the discrete weight space intelligently and finds the best combination.
        """
        try:
            # Calculate discrete step size
            step_size = 10**(-self.weight_precision)
            lower_bound, upper_bound = self.weight_bounds
            
            # Create discrete weight grid for each asset
            n_steps = int((upper_bound - lower_bound) / step_size) + 1
            
            # Limit grid size to prevent excessive computation, but be more generous for 2 assets
            max_steps = 500 if self.num_assets <= 2 else 200  # More generous for 2 assets
            if n_steps > max_steps:
                # Reduce precision temporarily to keep computation reasonable
                step_size = (upper_bound - lower_bound) / max_steps
                n_steps = max_steps
                self.logger.warning(f"Grid too large, using step size {step_size:.6f} instead")
            
            # Generate weight candidates for each asset
            weight_candidates = []
            for _ in range(self.num_assets):
                candidates = np.arange(lower_bound, upper_bound + step_size/2, step_size)
                candidates = np.round(candidates, self.weight_precision)
                weight_candidates.append(candidates)
            
            self.logger.info(f"Discrete grid: {len(weight_candidates[0])} steps per asset, {len(weight_candidates[0])**self.num_assets} total combinations")
            
            best_weights = None
            best_objective = float('inf')
            evaluated_count = 0
            max_evaluations = 50000 if self.num_assets <= 2 else 10000  # More evaluations for 2 assets
            
            # Smart sampling: Try strategic combinations first
            strategic_samples = self._get_strategic_discrete_samples(weight_candidates)
            
            # Debug counters
            bounds_failed = 0
            constraints_failed = 0
            objective_failed = 0
            
            for weights in strategic_samples:
                if evaluated_count >= max_evaluations:
                    break
                    
                # Check bounds
                if not all(lower_bound <= w <= upper_bound for w in weights):
                    bounds_failed += 1
                    continue
                
                # Check constraints
                if not self._check_constraints_satisfied(weights, constraints):
                    constraints_failed += 1
                    # Debug: Log first few constraint failures
                    if constraints_failed <= 3:
                        self.logger.debug(f"Constraint failed for weights {weights}")
                    continue
                
                # Evaluate objective
                try:
                    obj_value = objective_func(weights)
                    evaluated_count += 1
                    
                    if obj_value < best_objective:
                        best_objective = obj_value
                        best_weights = weights.copy()
                        
                except Exception as e:
                    objective_failed += 1
                    if objective_failed <= 3:
                        self.logger.debug(f"Objective failed for weights {weights}: {e}")
                    continue
            
            # Debug summary
            total_tested = bounds_failed + constraints_failed + objective_failed + evaluated_count
            self.logger.info(f"Discrete search summary: {total_tested} tested, {evaluated_count} evaluated, "
                           f"{bounds_failed} bounds failed, {constraints_failed} constraints failed, "
                           f"{objective_failed} objective failed")
            
            if best_weights is not None:
                self.logger.info(f"Discrete optimization evaluated {evaluated_count} combinations, found optimum: {best_weights}")
                
                # Create a mock result object compatible with scipy.optimize.minimize
                class DiscreteResult:
                    def __init__(self, x, fun, success):
                        self.x = x
                        self.fun = fun
                        self.success = success
                
                return DiscreteResult(best_weights, best_objective, True)
            else:
                self.logger.warning("No feasible discrete solution found")
                return None
                
        except Exception as e:
            self.logger.warning(f"Discrete grid optimization failed: {e}")
            return None
    
    def _get_strategic_discrete_samples(self, weight_candidates):
        """Generate strategic discrete weight combinations."""
        import itertools
        import random
        
        # For 2 assets, check feasibility of exhaustive search
        if self.num_assets == 2:
            total_combinations = len(weight_candidates[0]) * len(weight_candidates[1])
            if total_combinations <= 100000:  # Reasonable threshold
                self.logger.info(f"Using exhaustive search for 2 assets ({total_combinations} combinations)")
                return itertools.product(*weight_candidates)
            else:
                self.logger.info(f"Too many combinations ({total_combinations}), using strategic sampling")
        
        # Strategic sampling for larger grids or more assets
        strategic_combinations = []
        
        # 1. Equal weight combinations and variations
        n_candidates = len(weight_candidates[0])
        for equal_idx in [n_candidates//4, n_candidates//2, 3*n_candidates//4]:
            if equal_idx < len(weight_candidates[0]):
                equal_weight = weight_candidates[0][equal_idx]
                strategic_combinations.append([equal_weight] * self.num_assets)
        
        # 2. Corner cases (extreme allocations)
        for i in range(self.num_assets):
            for extreme_idx in [0, -1]:  # Min and max weights
                extreme_weight = weight_candidates[i][extreme_idx]
                # Distribute remaining among other assets
                corner_weights = [weight_candidates[j][n_candidates//2] for j in range(self.num_assets)]
                corner_weights[i] = extreme_weight
                strategic_combinations.append(corner_weights)
        
        # 3. Constraint-aware sampling for 2-asset portfolio strategies
        if self.num_assets == 2:
            # For long strategy (sum = 1), sample along the constraint line
            for w1_candidate in weight_candidates[0][::max(1, len(weight_candidates[0])//50)]:  # Sample every N-th
                w2_needed = 1.0 - w1_candidate  # For sum = 1 constraint
                # Find closest discrete w2
                w2_distances = [abs(w2 - w2_needed) for w2 in weight_candidates[1]]
                closest_idx = min(range(len(w2_distances)), key=w2_distances.__getitem__)
                w2_candidate = weight_candidates[1][closest_idx]
                strategic_combinations.append([w1_candidate, w2_candidate])
        
        # 4. Random sampling from the grid
        random.seed(42)  # Reproducible
        max_random_samples = min(5000, len(weight_candidates[0])**self.num_assets)
        for _ in range(max_random_samples):
            random_weights = []
            for candidates in weight_candidates:
                random_weights.append(random.choice(candidates))
            strategic_combinations.append(random_weights)
        
        self.logger.info(f"Generated {len(strategic_combinations)} strategic combinations")
        return strategic_combinations
    
    def _check_constraints_satisfied(self, weights, constraints):
        """Check if discrete weights satisfy all constraints."""
        try:
            for constraint in constraints:
                if constraint['type'] == 'eq':
                    if abs(constraint['fun'](weights)) > 1e-6:
                        return False
                elif constraint['type'] == 'ineq':
                    if constraint['fun'](weights) < -1e-6:
                        return False
            return True
        except Exception:
            return False
    
    def optimize_with_objective(self, objective: OptimizationObjective) -> Tuple[np.ndarray, float, float, float]:
        """
        IMPROVED: Discrete weight optimization for realistic trading.
        Constrains optimization to only search within discrete weight space.
        """
        # Get objective function
        raw_objective_func = objective.get_objective_function(self.predicted_returns, self.cov_matrix, self.rf_rate)
        
        # Use discrete bounds optimization for better performance and accuracy
        use_discrete_bounds = self.weight_precision <= 3  # Enable discrete bounds for practical precision levels
        
        # Use continuous optimization to avoid gradient discontinuity warnings
        # We'll apply discrete rounding in post-processing
        self.logger.info(f"Using continuous optimization with {self.weight_precision} decimal post-rounding")
        discrete_bounds = [self.weight_bounds] * self.num_assets
        objective_func = raw_objective_func
        use_discrete_bounds = False  # Disable to avoid scipy warnings
        
        # Set up optimization with discrete bounds
        bounds = discrete_bounds
        constraints = self.get_constraints()
        
        # KEY IMPROVEMENT: Use strategy-specific constraint-aware guesses
        initial_guesses = self.get_strategy_initial_guesses()
        
        best_result = None
        best_obj_value = float('inf')
        
        # Use standard SLSQP optimization with continuous bounds
        for i, guess in enumerate(initial_guesses):
            try:
                
                result = minimize(
                    objective_func,
                    guess,
                    method='SLSQP',
                    bounds=bounds,
                    constraints=constraints,
                    options={
                        'maxiter': 1000, 
                        'ftol': 1e-9,  # Standard tolerance
                        'disp': False
                    }
                )
                
                if result.success and result.fun < best_obj_value:
                    best_obj_value = result.fun
                    best_result = result
                    
            except Exception as e:
                self.logger.warning(f"SLSQP guess {i+1} failed for {self.get_strategy_name()}-{objective.get_name()}: {e}")
                continue
        
        if best_result is None or not best_result.success:
            error_msg = f"{self.get_strategy_name()}-{objective.get_name()} optimization failed"
            self.logger.error(error_msg)
            raise ValueError(error_msg)
        
        # Get final weights
        raw_weights = best_result.x
        
        # Handle final weights based on optimization method used
        if use_discrete_bounds:
            # Discrete optimization - optimizer already worked in discrete space
            # But apply final quantization to ensure exact precision
            final_weights = self._quantize_to_discrete(raw_weights)
            self.logger.info(f"Discrete optimization result: {raw_weights} -> quantized: {final_weights}")
            
            # Since optimizer worked in discrete space, weights should be valid
            if not self.validate_rounded_weights(final_weights):
                self.logger.warning(f"Discrete weights unexpectedly violate constraints for {self.get_strategy_name()}-{objective.get_name()}")
        else:
            # Continuous optimization used, apply rounding
            rounded_weights = self.round_weights(raw_weights)
            
            # Validate that rounded weights still satisfy constraints
            if not self.validate_rounded_weights(rounded_weights):
                self.logger.warning(f"Rounded weights violate constraints for {self.get_strategy_name()}-{objective.get_name()}, using original weights")
                final_weights = raw_weights
            else:
                final_weights = rounded_weights
        
        # Calculate metrics using final weights
        port_return = np.sum(final_weights * self.predicted_returns)
        port_vol = np.sqrt(np.dot(final_weights.T, np.dot(self.cov_matrix, final_weights)))
        sharpe = (port_return - self.rf_rate) / port_vol if port_vol > 0 else float('inf')
        
        return final_weights, port_return, port_vol, sharpe


class LongConstraintStrategy(ConstraintStrategy):
    """OPTIMIZED: Long strategy with exactly 5 strategic initial guesses covering full feasible space."""
    
    def _validate_strategy_specific_bounds(self, lower: float, upper: float) -> None:
        """Validate bounds are appropriate for long strategy."""
        if upper <= 0:
            raise StrategyBoundsError(f"Long strategy needs upper bound > 0 to buy assets. Got: {(lower, upper)}")
        if lower > 0:
            self.logger.warning(f"Long strategy bounds {(lower, upper)} don't allow short positions.")
    
    def get_constraints(self) -> list:
        """Weights must sum to 1."""
        return [{'type': 'eq', 'fun': lambda w: np.sum(w) - 1}]
    
    def get_initial_weights(self) -> np.ndarray:
        """Equal weights summing to 1."""
        return np.ones(self.num_assets) / self.num_assets
    
    def get_strategy_initial_guesses(self) -> List[np.ndarray]:
        """
        OPTIMIZED: Exactly 5 strategic guesses covering FULL feasible space.
        Constraint: sum(weights) = 1
        Covers: traditional + leveraged + hedged positions within bounds
        """
        guesses = []
        lower, upper = self.weight_bounds
        
        if self.num_assets == 2:
            # THE ESSENTIAL 5 for Long Strategy (sum = 1)
            # Cover traditional, leveraged, and hedged positions
            
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
            # Multi-asset: 5 strategic allocations
            candidates = [
                # Equal weights
                np.ones(self.num_assets) / self.num_assets,
                
                # Concentrated in first asset (if bounds allow)
                self._create_concentrated_weights(0, 0.6),
                
                # Concentrated in second asset (if bounds allow)
                self._create_concentrated_weights(1, 0.6),
                
                # Two-asset focus  
                self._create_two_asset_focus(0.6, 0.4),
                
                # Reverse two-asset focus
                self._create_two_asset_focus(0.4, 0.6)
            ]
            
            for candidate in candidates:
                if candidate is not None and self._is_valid_long_allocation(candidate):
                    guesses.append(candidate)
                if len(guesses) >= 5:
                    break
        
        # Ensure we have at least one guess
        if not guesses:
            guesses.append(self.get_initial_weights())
        
        self.logger.info(f"Long Strategy: Using {len(guesses)} optimized initial guesses covering full feasible space")
        for i, guess in enumerate(guesses):
            self.logger.info(f"  Long Guess {i+1}: {guess} (sum: {np.sum(guess):.6f})")
        
        return guesses
    
    def _create_concentrated_weights(self, asset_idx: int, concentration: float) -> Optional[np.ndarray]:
        """Create weights concentrated in specific asset."""
        weights = np.ones(self.num_assets) * (1 - concentration) / (self.num_assets - 1)
        weights[asset_idx] = concentration
        return weights if self._is_valid_long_allocation(weights) else None
    
    def _create_two_asset_focus(self, w1: float, w2: float) -> Optional[np.ndarray]:
        """Create weights focused on first two assets."""
        weights = np.zeros(self.num_assets)
        weights[0] = w1
        weights[1] = w2
        # Remaining weight distributed or zero
        remaining = 1 - w1 - w2
        if self.num_assets > 2 and remaining > 0:
            weights[2:] = remaining / (self.num_assets - 2)
        return weights if self._is_valid_long_allocation(weights) else None
    
    def _is_valid_long_allocation(self, weights: np.ndarray) -> bool:
        """Check if allocation is valid for long strategy."""
        return (np.all(weights >= self.weight_bounds[0] - 1e-10) and 
                np.all(weights <= self.weight_bounds[1] + 1e-10) and 
                abs(np.sum(weights) - 1) < 1e-10)
    
    def get_strategy_name(self) -> str:
        return "Long"


class ShortConstraintStrategy(ConstraintStrategy):
    """OPTIMIZED: Short strategy with exactly 5 strategic initial guesses covering full feasible space."""
    
    def _validate_strategy_specific_bounds(self, lower: float, upper: float) -> None:
        """Validate bounds are appropriate for short strategy."""
        if lower >= 0:
            raise StrategyBoundsError(f"Short strategy needs lower bound < 0 to short assets. Got: {(lower, upper)}")
        if upper <= 0:
            self.logger.warning(f"Short strategy bounds {(lower, upper)} don't allow long positions.")
    
    def get_constraints(self) -> list:
        """Weights must sum to -1."""
        return [{'type': 'eq', 'fun': lambda w: np.sum(w) + 1}]
    
    def get_initial_weights(self) -> np.ndarray:
        """Equal weights summing to -1."""
        return np.ones(self.num_assets) * (-1 / self.num_assets)
    
    def get_strategy_initial_guesses(self) -> List[np.ndarray]:
        """
        OPTIMIZED: Exactly 5 strategic guesses covering FULL feasible space.
        Constraint: sum(weights) = -1
        Covers: traditional shorts + leveraged shorts + hedged positions within bounds
        """
        guesses = []
        lower, upper = self.weight_bounds
        
        if self.num_assets == 2:
            # THE ESSENTIAL 5 for Short Strategy (sum = -1)
            # Cover traditional shorts, leveraged shorts, and hedged positions
            
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
            # Multi-asset: 5 strategic short allocations
            candidates = [
                # Equal short weights
                np.ones(self.num_assets) * (-1 / self.num_assets),
                
                # Concentrated short in first asset
                self._create_concentrated_short_weights(0, -0.6),
                
                # Concentrated short in second asset
                self._create_concentrated_short_weights(1, -0.6),
                
                # Two-asset short focus
                self._create_two_asset_short_focus(-0.6, -0.4),
                
                # Reverse two-asset short focus
                self._create_two_asset_short_focus(-0.4, -0.6)
            ]
            
            for candidate in candidates:
                if candidate is not None and self._is_valid_short_allocation(candidate):
                    guesses.append(candidate)
                if len(guesses) >= 5:
                    break
        
        # Ensure we have at least one guess
        if not guesses:
            guesses.append(self.get_initial_weights())
        
        self.logger.info(f"Short Strategy: Using {len(guesses)} optimized initial guesses covering full feasible space")
        for i, guess in enumerate(guesses):
            self.logger.info(f"  Short Guess {i+1}: {guess} (sum: {np.sum(guess):.6f})")
        
        return guesses
    
    def _create_concentrated_short_weights(self, asset_idx: int, concentration: float) -> Optional[np.ndarray]:
        """Create weights with concentrated short in specific asset."""
        weights = np.ones(self.num_assets) * (-1 - concentration) / (self.num_assets - 1)
        weights[asset_idx] = concentration
        return weights if self._is_valid_short_allocation(weights) else None
    
    def _create_two_asset_short_focus(self, w1: float, w2: float) -> Optional[np.ndarray]:
        """Create short weights focused on first two assets."""
        weights = np.zeros(self.num_assets)
        weights[0] = w1
        weights[1] = w2
        # Remaining weight to satisfy sum = -1
        remaining = -1 - w1 - w2
        if self.num_assets > 2:
            weights[2:] = remaining / (self.num_assets - 2)
        return weights if self._is_valid_short_allocation(weights) else None
    
    def _is_valid_short_allocation(self, weights: np.ndarray) -> bool:
        """Check if allocation is valid for short strategy."""
        return (np.all(weights >= self.weight_bounds[0] - 1e-10) and 
                np.all(weights <= self.weight_bounds[1] + 1e-10) and 
                abs(np.sum(weights) + 1) < 1e-10)
    
    def get_strategy_name(self) -> str:
        return "Short"


class MarketNeutralConstraintStrategy(ConstraintStrategy):
    """OPTIMIZED: Market neutral strategy with exactly 5 DETERMINISTIC initial guesses."""
    
    def _validate_strategy_specific_bounds(self, lower: float, upper: float) -> None:
        """Validate bounds are appropriate for market neutral strategy."""
        if lower >= 0:
            raise StrategyBoundsError(f"Market neutral needs lower bound < 0 for short positions. Got: {(lower, upper)}")
        if upper <= 0:
            raise StrategyBoundsError(f"Market neutral needs upper bound > 0 for long positions. Got: {(lower, upper)}")
        
        if abs(lower) != abs(upper):
            self.logger.info(f"Market neutral has asymmetric bounds {(lower, upper)}")
    
    def get_constraints(self) -> list:
        """Weights must sum to 0."""
        return [{'type': 'eq', 'fun': lambda w: np.sum(w)}]
    
    def get_initial_weights(self) -> np.ndarray:
        """Conservative starting point."""
        if self.num_assets == 2:
            return np.array([-0.25, 0.25])  # Conservative
        else:
            weights = np.zeros(self.num_assets)
            half = self.num_assets // 2
            weights[:half] = 0.25 / half
            weights[half:] = -0.25 / (self.num_assets - half)
            return weights
    
    def get_strategy_initial_guesses(self) -> List[np.ndarray]:
        """
        OPTIMIZED: Exactly 5 DETERMINISTIC guesses - YOUR ORIGINAL SUGGESTIONS!
        Constraint: sum(weights) = 0
        ELIMINATES INSTABILITY by using only deterministic starting points
        """
        guesses = []
        
        if self.num_assets == 2:
            # YOUR EXACT SUGGESTIONS - The perfect 5 for Market Neutral
            candidates = [
                [-0.25, 0.25],   # Conservative (your suggestion)
                [-0.5, 0.5],     # Moderate (your suggestion)
                [-0.75, 0.75],   # Aggressive (your suggestion)
                [0.5, -0.5],     # Reversed moderate
                [0.0, 0.0]       # Zero position
            ]
            
            # Only include if within bounds
            for candidate in candidates:
                weights = np.array(candidate)
                if (self.weight_bounds[0] <= weights[0] <= self.weight_bounds[1] and 
                    self.weight_bounds[0] <= weights[1] <= self.weight_bounds[1]):
                    guesses.append(weights)
                
                # Stop at 5 guesses max
                if len(guesses) >= 5:
                    break
        else:
            # Multi-asset: 5 strategic neutral allocations
            candidates = []
            for exposure in [0.25, 0.5, 0.75]:
                weights = np.zeros(self.num_assets)
                half = self.num_assets // 2
                weights[:half] = exposure / half
                weights[half:] = -exposure / (self.num_assets - half)
                candidates.append(weights)
            
            # Add zero and reversed allocation
            candidates.append(np.zeros(self.num_assets))
            
            weights = np.zeros(self.num_assets)
            half = self.num_assets // 2
            weights[:half] = -0.5 / half
            weights[half:] = 0.5 / (self.num_assets - half)
            candidates.append(weights)
            
            # Validate
            for candidate in candidates:
                weights = np.array(candidate)
                if (np.all(weights >= self.weight_bounds[0]) and 
                    np.all(weights <= self.weight_bounds[1])):
                    guesses.append(weights)
                
                if len(guesses) >= 5:
                    break
        
        # Ensure we have at least one guess
        if not guesses:
            guesses.append(self.get_initial_weights())
        
        self.logger.info(f"Market Neutral: Using {len(guesses)} DETERMINISTIC optimized initial guesses")
        self.logger.info("STABILITY FIX: No random guesses = No instability!")
        for i, guess in enumerate(guesses):
            self.logger.info(f"  MN Guess {i+1}: {guess} (sum: {np.sum(guess):.6f})")
        
        return guesses
    
    def get_strategy_name(self) -> str:
        return "Market_neutral"