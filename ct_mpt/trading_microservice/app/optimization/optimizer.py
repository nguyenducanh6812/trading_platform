"""
Portfolio Optimizer - Microservice Version
==========================================

Simplified version of the original optimizer following the same two-phase approach.
"""

from typing import Dict, Tuple
import numpy as np

from app.optimization.objectives import MaxReturnObjective, MaxSharpeObjective, MinVarianceObjective
from app.optimization.constraint_strategies import LongConstraintStrategy, ShortConstraintStrategy, MarketNeutralConstraintStrategy
from app.utils.logger import get_logger

logger = get_logger(__name__)

class Optimizer:
    """Portfolio optimizer using two-phase optimization approach."""

    def __init__(self, predicted_returns: np.ndarray, cov_matrix: np.ndarray, rf_rate: float,
                 weight_bounds: Tuple[float, float] = (-2.0, 2.0)):
        self.predicted_returns = predicted_returns
        self.cov_matrix = cov_matrix
        self.rf_rate = rf_rate
        self.weight_bounds = weight_bounds

        # Create constraint strategies
        self.long_strategy = LongConstraintStrategy(predicted_returns, cov_matrix, rf_rate, weight_bounds)
        self.short_strategy = ShortConstraintStrategy(predicted_returns, cov_matrix, rf_rate, weight_bounds)
        self.market_neutral_strategy = MarketNeutralConstraintStrategy(predicted_returns, cov_matrix, rf_rate, weight_bounds)

        # Create objectives
        self.max_return_obj = MaxReturnObjective()
        self.max_sharpe_obj = MaxSharpeObjective()
        self.min_variance_obj = MinVarianceObjective()


    def optimize_phase1(self) -> Dict[str, Dict]:
        """
        Phase 1: Get max Sharpe results for each constraint strategy.
        """
        logger.info("Starting Phase 1 optimization (Max Sharpe for each strategy)")
        results = {}

        strategies = [
            ('Long', self.long_strategy),
            ('Short', self.short_strategy),
            ('Market_neutral', self.market_neutral_strategy)
        ]

        for strategy_name, strategy in strategies:
            try:
                weights, ret, vol, sharpe = strategy.optimize_with_objective(self.max_sharpe_obj)
                # Keep original weights with full precision (like original implementation)
                results[strategy_name] = {
                    'weights': weights,
                    'return': ret,
                    'volatility': vol,
                    'sharpe': sharpe
                }
                logger.info(f"Phase 1 {strategy_name}: Sharpe={sharpe:.6f}, Return={ret:.6f}, Vol={vol:.6f}")
            except Exception as e:
                logger.error(f"Phase 1 failed for {strategy_name}: {e}")
                # Use fallback values with original weights
                results[strategy_name] = {
                    'weights': strategy.get_initial_weights(),
                    'return': 0.0,
                    'volatility': 0.1,
                    'sharpe': 0.0
                }

        return results

    def get_max_return_analysis(self) -> Dict[str, Dict]:
        """Get maximum return analysis for each constraint strategy."""
        logger.info("Getting max return analysis for each strategy")
        results = {}

        strategies = [
            ('Long', self.long_strategy),
            ('Short', self.short_strategy),
            ('Market_neutral', self.market_neutral_strategy)
        ]

        for strategy_name, strategy in strategies:
            try:
                weights, ret, vol, sharpe = strategy.optimize_with_objective(self.max_return_obj)
                results[strategy_name] = {
                    'weights': weights,
                    'return': ret,
                    'volatility': vol,
                    'sharpe': sharpe
                }
                logger.info(f"Max Return {strategy_name}: Return={ret:.6f}, Sharpe={sharpe:.6f}, Vol={vol:.6f}")
            except Exception as e:
                logger.error(f"Max return analysis failed for {strategy_name}: {e}")
                results[strategy_name] = {
                    'weights': strategy.get_initial_weights(),
                    'return': 0.0,
                    'volatility': 0.1,
                    'sharpe': 0.0
                }

        return results

    def optimize_phase2(self, risk_profile: str) -> Dict:
        """Phase 2: Optimize based on risk profile."""
        logger.info(f"Starting Phase 2 optimization for risk profile: {risk_profile}")

        if risk_profile not in ['neutral', 'averse', 'lover']:
            raise ValueError(f"Unsupported risk profile: {risk_profile}")

        if risk_profile == 'neutral':
            return self._optimize_risk_neutral()
        elif risk_profile == 'averse':
            return self._optimize_risk_averse()
        elif risk_profile == 'lover':
            return self._optimize_risk_lover()

    def _optimize_risk_neutral(self) -> Dict:
        """Risk Neutral: Use individual max returns for each strategy."""
        max_return_analysis = self.get_max_return_analysis()
        phase1_results = self.optimize_phase1()

        strategies = {}

        for strategy_name in ['Long', 'Short', 'Market_neutral']:
            max_return_data = max_return_analysis[strategy_name]
            target_return = max_return_data['return']

            phase1_data = phase1_results[strategy_name]
            R_p = phase1_data['return']
            sigma_p = phase1_data['volatility']
            weights = phase1_data['weights']

            # Calculate risky weight: w_risky = (R_target - R_f) / (R_p - R_f)
            denominator = R_p - self.rf_rate
            if abs(denominator) > 1e-10:
                risky_weight = (target_return - self.rf_rate) / denominator
            else:
                risky_weight = 0.0

            final_return = risky_weight * R_p + (1 - risky_weight) * self.rf_rate
            final_volatility = abs(risky_weight) * sigma_p

            if final_volatility > 0:
                final_sharpe = (final_return - self.rf_rate) / final_volatility
            else:
                final_sharpe = float('inf') if final_return > self.rf_rate else float('-inf')

            strategies[strategy_name] = {
                'weights': weights,
                'risky_weight': risky_weight,
                'return': final_return,
                'volatility': final_volatility,
                'sharpe': final_sharpe,
            }

        return {
            'strategies': strategies,
            'max_return_analysis': max_return_analysis,
            'phase1_sharpe_results': phase1_results,
            'profile': 'Neutral'
        }

    def _optimize_risk_averse(self) -> Dict:
        """Risk Averse: Use Global Minimum Variance Portfolio."""
        # Use long strategy with min variance objective
        try:
            gmvp_weights, gmvp_return, gmvp_vol, gmvp_sharpe = self.long_strategy.optimize_with_objective(self.min_variance_obj)
            # Keep original weights with full precision (like original implementation)
        except:
            # Fallback to equal weights
            gmvp_weights = self.long_strategy.get_initial_weights()
            gmvp_return = np.sum(gmvp_weights * self.predicted_returns)
            gmvp_vol = np.sqrt(np.dot(gmvp_weights.T, np.dot(self.cov_matrix, gmvp_weights)))
            gmvp_sharpe = (gmvp_return - self.rf_rate) / gmvp_vol if gmvp_vol > 0 else 0

        target_return = gmvp_return
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
        """Risk Lover: Use maximum return volatility with maximum Sharpe ratio."""
        max_return_analysis = self.get_max_return_analysis()
        overall_max_return = max(data['return'] for data in max_return_analysis.values())
        best_max_data = max(max_return_analysis.values(), key=lambda x: x['return'])

        sigma_max = best_max_data['volatility']

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
        """Compute risky portfolio weights for each strategy."""
        results = {}

        for strategy_name, phase1_data in phase1_results.items():
            R_p = phase1_data['return']
            sigma_p = phase1_data['volatility']
            weights = phase1_data['weights']

            # Calculate risky weight
            denominator = R_p - self.rf_rate
            if abs(denominator) > 1e-10:
                risky_weight = (target_return - self.rf_rate) / denominator
            else:
                risky_weight = 0.0

            final_return = risky_weight * R_p + (1 - risky_weight) * self.rf_rate
            final_volatility = abs(risky_weight) * sigma_p

            if final_volatility > 0:
                final_sharpe = (final_return - self.rf_rate) / final_volatility
            else:
                final_sharpe = float('inf') if final_return > self.rf_rate else float('-inf')

            results[strategy_name] = {
                'weights': weights,
                'risky_weight': risky_weight,
                'return': final_return,
                'volatility': final_volatility,
                'sharpe': final_sharpe,
            }

        return results