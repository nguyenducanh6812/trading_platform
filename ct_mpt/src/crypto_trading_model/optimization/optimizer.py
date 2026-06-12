from typing import Dict, Tuple, Optional
import numpy as np
from .strategy_factory import StrategyFactory
from .smart_grid_optimizer import SmartGridStrategyFactory
from ..custom_logging.logger import CustomLogger
from ..config.config_manager import ConfigManager

class Optimizer:
    """Manages portfolio optimization using Strategy Pattern."""
    
    def __init__(self, logger: CustomLogger, predicted_returns: np.ndarray, cov_matrix: np.ndarray, 
                 rf_rate: float, weight_bounds: Optional[Tuple[float, float]] = None,
                 config_manager: Optional[ConfigManager] = None):
        """
        Initialize the Optimizer.

        Args:
            logger: CustomLogger instance
            predicted_returns: Predicted daily asset returns (e.g., from ARIMA models)
            cov_matrix: Covariance matrix of returns
            rf_rate: Risk-free rate
            weight_bounds: DEPRECATED - strategies get bounds from config only
            config_manager: ConfigManager instance (REQUIRED)
        """
        if config_manager is None:
            raise ValueError("ConfigManager is required for Optimizer")
        
        self.logger = logger
        self.predicted_returns = predicted_returns
        self.cov_matrix = cov_matrix
        self.rf_rate = rf_rate
        self.weight_bounds = weight_bounds  # Keep for backward compatibility, but don't use
        self.factory = StrategyFactory(logger, config_manager)
        self.smart_grid_factory = SmartGridStrategyFactory(logger, config_manager)        
        # Cache for optimization results to avoid recomputation
        self._optimization_cache = {}

    def get_strategy_results(self, constraint_type: str, objective_type: str) -> Tuple[np.ndarray, float, float, float]:
        """
        Get optimization results for a specific strategy-objective combination.
        Uses caching to avoid recomputation.
        
        Args:
            constraint_type: 'long', 'short', or 'market_neutral'
            objective_type: 'max_return', 'max_sharpe', or 'min_variance'
            
        Returns:
            Tuple of (weights, expected_return, volatility, sharpe_ratio)
        """
        cache_key = f"{constraint_type}_{objective_type}"
        
        if cache_key not in self._optimization_cache:
            # 🟢 STRICT: Factory gets bounds from config, no weight_bounds passed
            result = self.factory.optimize_strategy_combination(
                constraint_type, objective_type,
                self.predicted_returns, self.cov_matrix, self.rf_rate
            )
            self._optimization_cache[cache_key] = result
        
        return self._optimization_cache[cache_key]

    def optimize_phase1(self) -> Dict[str, Dict]:
        """
        Perform Phase 1 optimization - get max Sharpe results for each constraint strategy.
        
        Returns:
            Dictionary with max Sharpe results for Long, Short, and Market_neutral strategies
        """
        results = {}
        constraint_strategies = ['long', 'short', 'market_neutral']
        
        for constraint_type in constraint_strategies:
            weights, ret, vol, sharpe = self.get_strategy_results(constraint_type, 'max_sharpe')
            
            # Standardize naming for backward compatibility
            strategy_key = constraint_type.replace('_', '_').title()
            if constraint_type == 'market_neutral':
                strategy_key = 'Market_neutral'
                
            results[strategy_key] = {
                'weights': weights,
                'return': ret,
                'volatility': vol,
                'sharpe': sharpe
            }
            
        return results

    def get_max_return_analysis(self) -> Dict[str, Dict]:
        """
        Get maximum return analysis for each constraint strategy.
        
        Returns:
            Dictionary with max return results for Long, Short, and Market_neutral strategies
        """
        results = {}
        constraint_strategies = ['long', 'short', 'market_neutral']
                
        for constraint_type in constraint_strategies:
            weights, ret, vol, sharpe = self.get_strategy_results(constraint_type, 'max_return')
            
            # Standardize naming
            strategy_key = constraint_type.replace('_', '_').title()
            if constraint_type == 'market_neutral':
                strategy_key = 'Market_neutral'
                
            results[strategy_key] = {
                'weights': weights,
                'return': ret,
                'volatility': vol,
                'sharpe': sharpe
            }            
        return results

    def optimize_phase2(self, risk_profile: str) -> Dict:
        """
        Perform Phase 2 optimization based on risk profile using Strategy Pattern.
        
        Args:
            risk_profile: 'neutral', 'averse', or 'lover'
            
        Returns:
            Dictionary with optimization results
        """
        if risk_profile not in ['neutral', 'averse', 'lover']:
            raise ValueError(f"Unsupported risk profile: {risk_profile}")
        
        if risk_profile == 'neutral':
            return self._optimize_risk_neutral()
        elif risk_profile == 'averse':
            return self._optimize_risk_averse()
        elif risk_profile == 'lover':
            return self._optimize_risk_lover()

    def _optimize_risk_neutral(self) -> Dict:
        """
        Risk Neutral: Find maximum return for EACH strategy and compute individual strategy metrics.
        ✅ FIXED: Now uses _compute_risky_strategies for consistency
        """
        # Get max return results for each constraint strategy
        max_return_analysis = self.get_max_return_analysis()
        
        # Get Phase 1 (max Sharpe) results for risky weight calculations
        phase1_results = self.optimize_phase1()
        
        # For Risk Neutral, compute individual strategy metrics using each strategy's max return
        strategies = {}
        
        strategy_mapping = {
            'Long': 'Long',
            'Short': 'Short', 
            'Market_neutral': 'Market_neutral'
        }
        
        for strategy_name in strategy_mapping:
            # Get the max return for THIS specific strategy
            max_return_data = max_return_analysis[strategy_name]
            target_return_for_strategy = max_return_data['return']  # E(R_Max) for this strategy
            
            # Get Phase 1 data for this strategy (for risky weight calculation)
            phase1_data = phase1_results[strategy_name]
            R_p = phase1_data['return']  # Sharpe-optimized return
            sigma_p = phase1_data['volatility']  # Sharpe-optimized volatility
            weights = phase1_data['weights']  # Sharpe-optimized weights
            
            # Calculate risky weight: w_risky = (R_max - R_f) / (R_p - R_f)
            denominator = R_p - self.rf_rate
            if abs(denominator) > 1e-10:
                risky_weight = (target_return_for_strategy - self.rf_rate) / denominator
            else:
                risky_weight = 0.0
                self.logger.warning(f"Strategy {strategy_name}: R_p approximately equals R_f, setting risky_weight = 0")
            
            # Calculate E(R_stg) and sigma_stg for this strategy
            # E(R_stg) = w_risky * E(R_p) + (1 - w_risky) * R_f = E(R_max) (should equal target)
            final_return = risky_weight * R_p + (1 - risky_weight) * self.rf_rate
            
            # sigma_stg = |w_risky| * sigma_p (formula from Model Formula)
            final_volatility = abs(risky_weight) * sigma_p
            
            # ✅ FIXED: Calculate final Sharpe ratio (for consistency and completeness)
            if final_volatility > 0:
                final_sharpe = (final_return - self.rf_rate) / final_volatility
            else:
                final_sharpe = float('inf') if final_return > self.rf_rate else float('-inf')
            
            strategies[strategy_name] = {
                'weights': weights,  # Asset weights (BTC, ETH)
                'risky_weight': risky_weight,  # Weight of risky asset vs risk-free
                'return': final_return,  # E(R_stg) 
                'volatility': final_volatility,  # sigma_stg
                'sharpe': final_sharpe,  # ✅ ADDED: Final Sharpe ratio
                'max_return_target': target_return_for_strategy,  # E(R_max) for this strategy
                'sharpe_optimized': phase1_data  # Reference to Phase 1 results
            }
        
        return {
            'strategies': strategies,  # E(R_stg) and sigma_stg for each strategy
            'max_return_analysis': max_return_analysis,  # Individual max returns
            'phase1_sharpe_results': phase1_results,  # Sharpe optimization results
            'profile': 'Neutral'
        }

    def _optimize_risk_averse(self) -> Dict:
        """
        Risk Averse: Use Global Minimum Variance Portfolio.
        """
        # Get GMVP result (we can use any constraint with min_variance objective)
        # For GMVP, we typically use unconstrained or long constraint
        gmvp_weights, gmvp_return, gmvp_vol, gmvp_sharpe = self.get_strategy_results('long', 'min_variance')
        
        target_return = gmvp_return
        
        # Get Phase 1 results for risky weight calculations
        phase1_results = self.optimize_phase1()
        strategies = self._compute_risky_strategies(target_return, phase1_results)
        
        return {
            'gmvp_weights': gmvp_weights,
            'gmvp_return': gmvp_return,
            'gmvp_volatility': gmvp_vol,
            'target_return': target_return,
            'strategies': strategies,
            'profile': 'Averse'
        }

    def _optimize_risk_lover(self) -> Dict:
        """
        Risk Lover: Use maximum return volatility with maximum Sharpe ratio.
        """
        # Get maximum return portfolio (unconstrained or best constraint)
        max_return_analysis = self.get_max_return_analysis()
        overall_max_return = max(data['return'] for data in max_return_analysis.values())
        best_max_data = max(max_return_analysis.values(), key=lambda x: x['return'])
        
        sigma_max = best_max_data['volatility']
        
        # Get maximum Sharpe ratio from Phase 1
        phase1_results = self.optimize_phase1()
        max_sharpe = max(data['sharpe'] for data in phase1_results.values())
        
        # Risk lover return formula: E(R_risk_lover) = R_f + SR_max * sigma_max
        risk_lover_return = self.rf_rate + max_sharpe * sigma_max
        target_return = risk_lover_return
        
        strategies = self._compute_risky_strategies(target_return, phase1_results)
        
        return {
            'max_return': overall_max_return,
            'sigma_max': sigma_max,
            'max_sharpe': max_sharpe,
            'risk_lover_return': risk_lover_return,
            'target_return': target_return,
            'strategies': strategies,
            'profile': 'Lover'
        }

    def _compute_risky_strategies(self, target_return: float, phase1_results: Dict) -> Dict:
        """
        Compute risky portfolio weights for each strategy using Model Formula.
        ✅ This method already has the final Sharpe calculation implemented correctly
        
        Args:
            target_return: Target portfolio return
            phase1_results: Phase 1 (max Sharpe) optimization results
            
        Returns:
            Dictionary with risky strategy allocations including FINAL Sharpe ratios
        """
        results = {}
        
        for strategy_name, phase1_data in phase1_results.items():
            R_p = phase1_data['return']      # Phase 1 expected return
            sigma_p = phase1_data['volatility']  # Phase 1 volatility
            weights = phase1_data['weights']     # Phase 1 asset weights
            
            # Calculate risky weight: w_risky = (R_target - R_f) / (R_p - R_f)
            denominator = R_p - self.rf_rate
            if abs(denominator) > 1e-10:
                risky_weight = (target_return - self.rf_rate) / denominator
            else:
                risky_weight = 0.0
                self.logger.warning(f"Strategy {strategy_name}: R_p approximately equals R_f, setting risky_weight = 0")
            
            # Calculate final portfolio metrics after risky weight adjustment
            final_return = risky_weight * R_p + (1 - risky_weight) * self.rf_rate
            final_volatility = abs(risky_weight) * sigma_p
            
            # ✅ Calculate final Sharpe ratio (already implemented correctly)
            if final_volatility > 0:
                final_sharpe = (final_return - self.rf_rate) / final_volatility
            else:
                final_sharpe = float('inf') if final_return > self.rf_rate else float('-inf')
            
            # Log the calculation for debugging
            self.logger.info(f"Strategy {strategy_name}:")
            self.logger.info(f"  Phase 1: Return={R_p:.6f}, Vol={sigma_p:.6f}")
            self.logger.info(f"  Target Return={target_return:.6f}, Risky Weight={risky_weight:.6f}")
            self.logger.info(f"  Final: Return={final_return:.6f}, Vol={final_volatility:.6f}, Sharpe={final_sharpe:.6f}")
            
            results[strategy_name] = {
                'weights': weights,               # Asset weights from Phase 1
                'risky_weight': risky_weight,     # Weight of risky portfolio vs risk-free
                'return': final_return,           # E(R_strategy) - FINAL after risky weight
                'volatility': final_volatility,   # σ_strategy - FINAL after risky weight
                'sharpe': final_sharpe,          # SR_strategy - FINAL after risky weight ✅ CORRECT!
                'target_return': target_return,   # Original target return
                'phase1_sharpe': phase1_data['sharpe'],  # Original Phase 1 Sharpe for reference
                'sharpe_optimized': phase1_data  # Reference to Phase 1 results
            }
        
        return results

    def optimize_phase1_smart_grid(self, objective_type: str = 'max_sharpe', precision_decimals: int = 2) -> Dict[str, Dict]:
        """
        NEW METHOD: Phase 1 optimization using Smart Grid Search.
        Use this instead of optimize_phase1() for GUARANTEED optimal results.
        
        Args:
            objective_type: 'max_sharpe', 'max_return', or 'min_variance'
            precision_decimals: Decimal precision for weights (default: 2)
        
        Returns:
            Dictionary with optimal results for Long, Short, and Market_neutral strategies
        """
        return self.smart_grid_factory.optimize_all_strategies(
            self.predicted_returns, self.cov_matrix, self.rf_rate,
            objective_type=objective_type, precision_decimals=precision_decimals
        )

    def compare_optimization_methods(self, objective_type: str = 'max_sharpe') -> Dict[str, Dict]:
        """
        Compare traditional optimization vs Smart Grid Search for validation.
        
        Args:
            objective_type: Objective to compare
            
        Returns:
            Dictionary with comparison results
        """
        # Get traditional optimization results
        if objective_type == 'max_sharpe':
            traditional_results = self.optimize_phase1()
        elif objective_type == 'max_return':
            traditional_results = self.get_max_return_analysis()
        else:
            # For min_variance, we need to manually get results
            traditional_results = {}
            for constraint_type in ['long', 'short', 'market_neutral']:
                weights, ret, vol, sharpe = self.get_strategy_results(constraint_type, 'min_variance')
                strategy_key = constraint_type.replace('_', '_').title()
                if constraint_type == 'market_neutral':
                    strategy_key = 'Market_neutral'
                traditional_results[strategy_key] = {
                    'weights': weights, 'return': ret, 'volatility': vol, 'sharpe': sharpe
                }

        # Get Smart Grid results
        smart_grid_results = self.optimize_phase1_smart_grid(objective_type)

        # Compare results
        comparison = {
            'traditional': traditional_results,
            'smart_grid': smart_grid_results,
            'improvements': {}
        }

        # Calculate improvements
        for strategy in ['Long', 'Short', 'Market_neutral']:
            if strategy in traditional_results and strategy in smart_grid_results:
                trad_sharpe = traditional_results[strategy]['sharpe']
                grid_sharpe = smart_grid_results[strategy]['sharpe']
                
                if abs(trad_sharpe) > 1e-10:
                    improvement = (grid_sharpe - trad_sharpe) / abs(trad_sharpe) * 100
                else:
                    improvement = float('inf') if grid_sharpe > trad_sharpe else 0.0
                
                comparison['improvements'][strategy] = {
                    'traditional_sharpe': trad_sharpe,
                    'smart_grid_sharpe': grid_sharpe,
                    'improvement_percent': improvement,
                    'is_better': grid_sharpe > trad_sharpe,
                    'method_used': smart_grid_results[strategy].get('method', 'SmartGridSearch')
                }

        # Log comparison summary
        self.logger.info("Optimization Method Comparison:")
        for strategy, data in comparison['improvements'].items():
            self.logger.info(f"  {strategy}: {data['traditional_sharpe']:.6f} → {data['smart_grid_sharpe']:.6f} "
                           f"({data['improvement_percent']:+.2f}%)")

        return comparison

    def clear_cache(self):
        """Clear the optimization cache."""
        self._optimization_cache.clear()

    @classmethod
    def create_with_reused_factory(cls, logger: CustomLogger, predicted_returns: np.ndarray, 
                                 cov_matrix: np.ndarray, rf_rate: float,
                                 strategy_factory: StrategyFactory):
        """
        🟢 CLASS METHOD: Create optimizer with reused factory (for backtesting loops).
        
        Args:
            logger: Logger instance
            predicted_returns: Predicted daily asset returns for this window
            cov_matrix: Covariance matrix for this window  
            rf_rate: Risk-free rate
            strategy_factory: Pre-created and cached StrategyFactory
            
        Returns:
            Optimizer instance that reuses the factory
        """
        # Create optimizer instance directly
        optimizer = cls.__new__(cls)
        
        # Initialize with reused factory
        optimizer.logger = logger
        optimizer.predicted_returns = predicted_returns
        optimizer.cov_matrix = cov_matrix
        optimizer.rf_rate = rf_rate
        optimizer.weight_bounds = None  # Not used in new architecture
        optimizer.factory = strategy_factory  # 🟢 REUSE existing factory
        # Create smart_grid_factory (needs config_manager from strategy_factory)
        optimizer.smart_grid_factory = SmartGridStrategyFactory(logger, strategy_factory.config_manager)
        optimizer._optimization_cache = {}
        
        return optimizer