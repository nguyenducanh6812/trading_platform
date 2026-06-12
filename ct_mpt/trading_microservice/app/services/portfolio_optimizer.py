"""
Portfolio Optimization Service
==============================

Portfolio optimization service (Step 1) - Extracted from SimpleDynamicBacktester.
Generates optimal portfolio weights using ARIMA predictions.
"""

import pandas as pd
import numpy as np
from typing import Dict, List, Tuple, Optional
from datetime import datetime
import tempfile
from pathlib import Path
import asyncio
import sys

from app.core.exceptions import OptimizationError, InsufficientDataError, ConfigurationError
from app.utils.logger import get_logger
from app.services.data_processor import DataProcessor
from app.core.job_manager import job_manager
from app.optimization.optimizer import Optimizer

logger = get_logger(__name__)


def safe_print(message: str):
    """Safe print function that handles encoding issues on Windows."""
    try:
        # Force ASCII encoding to avoid any Unicode issues
        ascii_message = message.encode('ascii', 'replace').decode('ascii')
        print(ascii_message, flush=True)
        # Also log to logger as backup
        logger.info(f"CONSOLE: {ascii_message}")
    except Exception as e:
        # Ultimate fallback
        fallback_msg = f"[LOG] {str(e)[:50]}"
        print(fallback_msg, flush=True)
        logger.error(f"Print failed: {str(e)}")


class PortfolioOptimizer:
    """Portfolio optimization service for generating optimal weights."""

    def __init__(self):
        self.data_processor = DataProcessor()

    async def optimize_portfolio(
        self,
        asset_files: Dict[str, str],
        config: Dict[str, any]
    ) -> Dict[str, any]:
        """
        Main portfolio optimization method.

        Args:
            asset_files: Dictionary mapping asset codes to file paths
            config: Optimization configuration

        Returns:
            Optimization results with portfolio weights over time
        """
        try:
            logger.info(f"Starting portfolio optimization for assets: {list(asset_files.keys())}")

            # Load and combine data
            combined_data = self.data_processor.load_and_combine_prediction_data(
                asset_files=asset_files,
                date_range=config.get('date_range')
            )

            # Extract configuration
            asset_codes = list(asset_files.keys())
            risk_profile = config.get('risk_profile', 'neutral')
            rebalance_frequency = config.get('rebalance_frequency', 1)
            lookback_period = config.get('lookback_period', 7)
            optimization_method = config.get('optimization_method', 'traditional')

            # Find first prediction and decision start
            first_pred_index = self.data_processor.find_first_prediction_index(
                combined_data, asset_codes
            )
            decision_start_index = first_pred_index + lookback_period

            if decision_start_index >= len(combined_data):
                raise InsufficientDataError(
                    f"Insufficient data for optimization. Need at least "
                    f"{decision_start_index + 1} rows, got {len(combined_data)}"
                )

            logger.info(f"First prediction index: {first_pred_index}")
            logger.info(f"Decision starts at index: {decision_start_index}")

            # Run optimization
            results = await self._run_optimization(
                data=combined_data,
                asset_codes=asset_codes,
                risk_profile=risk_profile,
                rebalance_frequency=rebalance_frequency,
                lookback_period=lookback_period,
                optimization_method=optimization_method,
                config=config
            )

            # Calculate summary statistics
            summary = self._calculate_summary(
                results, decision_start_index, asset_codes
            )

            # Export results to temporary file
            output_path = self._export_results(results, asset_codes, save_to_results_dir=False)

            return {
                "status": "success",
                "results": results.to_dict('records'),
                "summary": summary,
                "output_file": str(output_path),
                "metadata": {
                    "asset_codes": asset_codes,
                    "total_days": len(results),
                    "decision_days": len(results) - decision_start_index,
                    "first_prediction_date": results.iloc[first_pred_index]['Timestamp'].strftime('%Y-%m-%d'),
                    "decision_start_date": results.iloc[decision_start_index]['Timestamp'].strftime('%Y-%m-%d'),
                    "optimization_method": optimization_method,
                    "risk_profile": risk_profile
                }
            }

        except Exception as e:
            if isinstance(e, (OptimizationError, InsufficientDataError, ConfigurationError)):
                raise
            raise OptimizationError(f"Portfolio optimization failed: {str(e)}")

    async def optimize_portfolio_job(self, job, asset_files: Dict[str, str], config: Dict[str, any]) -> str:
        """
        Job-based portfolio optimization that saves results to file.

        Args:
            job: Job instance for progress tracking
            asset_files: Dictionary mapping asset codes to file paths
            config: Optimization configuration

        Returns:
            Path to the generated results file
        """
        try:
            # Add job_id to config for progress tracking
            config['job_id'] = job.job_id

            logger.info(f"Starting job-based portfolio optimization for assets: {list(asset_files.keys())}")
            # Multiple ways to ensure console output is visible
            print(f"======= JOB STARTED =======", flush=True)
            print(f"Job ID: {job.job_id}", flush=True)
            print(f"Assets: {list(asset_files.keys())}", flush=True)
            print(f"==============================", flush=True)
            safe_print(f">> Starting portfolio optimization job {job.job_id}")

            # Load and combine data
            job_manager.update_job_progress(job.job_id, 10, "Loading and validating data")
            combined_data = self.data_processor.load_and_combine_prediction_data(
                asset_files=asset_files,
                date_range=config.get('date_range')
            )

            asset_codes = list(asset_files.keys())
            risk_profile = config.get('risk_profile', 'neutral')
            rebalance_frequency = config.get('rebalance_frequency', 1)
            lookback_period = config.get('lookback_period', 7)
            optimization_method = config.get('optimization_method', 'traditional')

            # Find first prediction and decision start
            job_manager.update_job_progress(job.job_id, 20, "Finding prediction start points")
            first_pred_index = self.data_processor.find_first_prediction_index(
                combined_data, asset_codes
            )
            decision_start_index = first_pred_index + lookback_period

            if decision_start_index >= len(combined_data):
                raise InsufficientDataError(
                    f"Insufficient data for optimization. Need at least "
                    f"{decision_start_index + 1} rows, got {len(combined_data)}"
                )

            logger.info(f"First prediction index: {first_pred_index}")
            logger.info(f"Decision starts at index: {decision_start_index}")

            # Run optimization
            job_manager.update_job_progress(job.job_id, 30, "Starting portfolio optimization")
            results = await self._run_optimization(
                data=combined_data,
                asset_codes=asset_codes,
                risk_profile=risk_profile,
                rebalance_frequency=rebalance_frequency,
                lookback_period=lookback_period,
                optimization_method=optimization_method,
                config=config
            )

            # Calculate summary statistics
            job_manager.update_job_progress(job.job_id, 90, "Calculating summary statistics")
            summary = self._calculate_summary(
                results, decision_start_index, asset_codes
            )

            # Export results to results directory
            job_manager.update_job_progress(job.job_id, 95, "Saving results to file")
            try:
                output_path = self._export_results(results, asset_codes, save_to_results_dir=True)
                logger.info("Job-based optimization completed successfully")
                return str(output_path)
            except UnicodeEncodeError as e:
                # Handle any Unicode issues during file operations
                logger.error(f"Unicode error during file save: {str(e)}")
                from app.core.config import settings
                fallback_dir = Path(settings.RESULTS_DIR)
                fallback_path = fallback_dir / f"portfolio_results_{datetime.now().strftime('%Y%m%d_%H%M%S')}.xlsx"
                # Simple fallback save
                results.to_excel(fallback_path, index=False)
                print("Portfolio optimization completed - results saved", flush=True)
                return str(fallback_path)

        except Exception as e:
            logger.error(f"Job-based optimization failed: {str(e)}")
            if isinstance(e, (OptimizationError, InsufficientDataError, ConfigurationError)):
                raise
            raise OptimizationError(f"Job-based optimization failed: {str(e)}")

    async def _run_optimization(
        self,
        data: pd.DataFrame,
        asset_codes: List[str],
        risk_profile: str,
        rebalance_frequency: int,
        lookback_period: int,
        optimization_method: str,
        config: Dict[str, any]
    ) -> pd.DataFrame:
        """Run the main optimization loop."""

        results = data.copy()

        # Initialize result columns
        self._initialize_result_columns(results, asset_codes)

        # Validate sufficient data
        if len(results) < lookback_period:
            raise InsufficientDataError(
                f"Insufficient data for lookback period. Need {lookback_period} days, "
                f"got {len(results)} days."
            )

        # Main optimization loop
        total_days = len(results)
        logger.info(f"Starting optimization for {total_days} days with lookback period {lookback_period}")

        for i in range(len(results)):
            # Progress logging and async yield
            if i % 50 == 0:  # Log every 50 days
                progress = (i / total_days) * 100
                current_date = results.iloc[i]['Timestamp'].strftime('%Y-%m-%d')
                logger.info(f"Processing day {i+1}/{total_days} ({progress:.1f}%) - Date: {current_date}")

                # Update job progress if we have a job_id in config
                if hasattr(config, 'get') and config.get('job_id'):
                    job_manager.update_job_progress(
                        config['job_id'],
                        progress,
                        f"Processing day {i+1}/{total_days}",
                        f"Date: {current_date}"
                    )

                # Yield control to allow other coroutines to run
                await asyncio.sleep(0)

            if i >= lookback_period:
                # Get historical data for lookback period
                historical_data = results.iloc[i-lookback_period:i]

                # Calculate covariance matrix
                mean_returns, cov_matrix, asset_volatilities = self._calculate_covariance(
                    historical_data, asset_codes
                )

                # Store mean returns
                for j, asset in enumerate(asset_codes):
                    results.iloc[i, results.columns.get_loc(f'{asset}_Mean_Return')] = mean_returns[j]

                # On rebalancing days
                if i == lookback_period or (i - lookback_period) % rebalance_frequency == 0:
                    rebalance_date = results.iloc[i]['Timestamp'].strftime('%Y-%m-%d')
                    logger.info(f"[REBALANCE] DAY {i+1} - Date: {rebalance_date}")

                    # Console logging for rebalancing (as requested)
                    safe_print(f"\n[REBALANCE] DAY {i+1} - {rebalance_date}")

                    # Get predicted returns
                    predicted_returns = self._get_predicted_returns(results, i, asset_codes)
                    logger.info(f"  Predicted returns: {dict(zip(asset_codes, predicted_returns))}")
                    safe_print(f"  > Predicted returns: {dict(zip(asset_codes, predicted_returns))}")

                    # Run optimization
                    optimization_results = self._optimize_strategies(
                        predicted_returns=predicted_returns,
                        cov_matrix=cov_matrix,
                        risk_profile=risk_profile,
                        optimization_method=optimization_method,
                        config=config
                    )

                    # Select best strategy
                    best_strategy = self._select_best_strategy(optimization_results)
                    logger.info(f"  Selected strategy: {best_strategy}")
                    safe_print(f"  > Selected strategy: {best_strategy}")

                    # Log portfolio weights
                    if 'weights' in optimization_results.get(best_strategy, {}):
                        weights = optimization_results[best_strategy]['weights']
                        weights_dict = dict(zip(asset_codes, weights))
                        logger.info(f"  Portfolio weights: {weights_dict}")
                        safe_print(f"  > Portfolio weights: {weights_dict}")

                    # Update job progress for rebalancing
                    if hasattr(config, 'get') and config.get('job_id'):
                        job_manager.update_job_progress(
                            config['job_id'],
                            (i / total_days) * 100,
                            f"Rebalanced on {rebalance_date}",
                            f"Strategy: {best_strategy}"
                        )

                    # Store results
                    self._store_optimization_results(
                        results, i, asset_codes, optimization_results,
                        best_strategy, predicted_returns, cov_matrix
                    )

                else:
                    # Carry forward previous weights
                    self._carry_forward_weights(results, i, asset_codes)

                # Calculate daily return and portfolio value
                if i > 0:
                    self._calculate_daily_return(results, i, asset_codes)

        return results

    def _initialize_result_columns(self, results: pd.DataFrame, asset_codes: List[str]):
        """Initialize result DataFrame columns."""
        results['Portfolio_Value'] = 1.0

        # Asset weights
        for asset in asset_codes:
            results[f'{asset}_Weight'] = 0.0
        results['Risky_Weight'] = 0.0

        results['Strategy'] = 'No Investment'
        results['Daily_Return'] = 0.0
        results['Decision_Log'] = 'Waiting for sufficient historical data'
        results['Used_Predicted_Returns'] = False

        # Mean returns
        for asset in asset_codes:
            results[f'{asset}_Mean_Return'] = np.nan

        results['Expected_Port_Return'] = np.nan
        results['Sharpe_Ratio'] = np.nan

        # Strategy performance metrics
        results['Max_Sharpe_Long'] = np.nan
        results['Max_Sharpe_Short'] = np.nan
        results['Max_Sharpe_Market_Neutral'] = np.nan

        # Volatility columns
        results['Volatility_Long'] = np.nan
        results['Volatility_Short'] = np.nan
        results['Volatility_Market_Neutral'] = np.nan
        results['Portfolio_Volatility'] = np.nan

        # Covariance matrix columns
        for i, asset_i in enumerate(asset_codes):
            for j, asset_j in enumerate(asset_codes):
                results[f'Cov_{asset_i}_{asset_j}'] = np.nan

    def _calculate_covariance(
        self,
        returns_data: pd.DataFrame,
        asset_codes: List[str]
    ) -> Tuple[np.ndarray, np.ndarray, np.ndarray]:
        """Calculate covariance matrix from historical returns."""
        return_columns = [f'{asset}_Return' for asset in asset_codes]
        returns_matrix = returns_data[return_columns].values
        mean_returns = np.mean(returns_matrix, axis=0)

        # Calculate covariance matrix
        centered_returns = returns_matrix - mean_returns
        cov_matrix = np.dot(centered_returns.T, centered_returns) / len(centered_returns)

        asset_volatilities = np.sqrt(np.diag(cov_matrix))

        return mean_returns, cov_matrix, asset_volatilities

    def _get_predicted_returns(
        self,
        data: pd.DataFrame,
        index: int,
        asset_codes: List[str]
    ) -> np.ndarray:
        """Get predicted returns for current day - STRICT MODE."""
        predicted_returns = []
        missing_assets = []

        for asset in asset_codes:
            pred_col = f'Prd_Return_Arima_{asset}'
            if pred_col not in data.columns:
                missing_assets.append(f"{asset} (column {pred_col} not found)")
            elif pd.isna(data.iloc[index][pred_col]):
                missing_assets.append(f"{asset} (prediction is NaN at index {index})")
            else:
                predicted_returns.append(data.iloc[index][pred_col])

        # Fail fast if any predictions missing
        if missing_assets:
            current_date = (
                data.iloc[index]['Timestamp'].strftime('%Y-%m-%d')
                if 'Timestamp' in data.columns
                else f"index {index}"
            )
            error_msg = (
                f"STRICT MODE FAILURE at {current_date}: Missing ARIMA predictions "
                f"for {missing_assets}. Model validation failed - cannot proceed "
                f"without complete predictions."
            )
            logger.error(error_msg)
            raise OptimizationError(error_msg)

        return np.array(predicted_returns)

    def _optimize_strategies(
        self,
        predicted_returns: np.ndarray,
        cov_matrix: np.ndarray,
        risk_profile: str,
        optimization_method: str,
        config: Dict[str, any]
    ) -> Dict[str, any]:
        """
        Run proper MPT portfolio optimization matching the original implementation.
        """
        # Risk-free rate using original formula from simple_dynamic_ui.py line 687
        rf_rate = ((1 + 0.04) ** (1 / 365)) - 1  # Daily rate from 4% annual

        safe_print(f"  > Using proper MPT optimization with predicted returns: {predicted_returns}")
        safe_print(f"  > Covariance matrix shape: {cov_matrix.shape}")
        safe_print(f"  > Risk profile: {risk_profile}")

        try:
            # Create optimizer with predicted returns - EXACTLY like original
            # Note: Removed weight_precision parameter to preserve full precision like original
            optimizer = Optimizer(
                predicted_returns=predicted_returns,
                cov_matrix=cov_matrix,
                rf_rate=rf_rate,
                weight_bounds=(-2.0, 2.0)  # Allow leverage and short positions like original
            )

            safe_print(f"  > Created optimizer with bounds (-2.0, 2.0)")

            # Phase 1: Get max Sharpe for each strategy - EXACTLY like original (line 279)
            safe_print(f"  > Running Phase 1 optimization (Max Sharpe)")
            phase1_results = optimizer.optimize_phase1()

            # Log Phase 1 results
            for strategy_name, results in phase1_results.items():
                safe_print(f"    - {strategy_name}: Sharpe={results['sharpe']:.6f}, Return={results['return']:.6f}, Vol={results['volatility']:.6f}")

            # Phase 2: Risk profile optimization - EXACTLY like original (line 296)
            safe_print(f"  > Running Phase 2 optimization (Risk profile: {risk_profile})")
            phase2_results = optimizer.optimize_phase2(risk_profile)

            safe_print(f"  > MPT optimization completed successfully")

            # Return combined results matching original structure
            return {
                'phase1_results': phase1_results,
                'phase2_results': phase2_results,
                'Long': phase1_results['Long'],
                'Short': phase1_results['Short'],
                'Market_neutral': phase1_results['Market_neutral']
            }

        except Exception as e:
            safe_print(f"  > MPT optimization failed: {str(e)}")
            logger.error(f"MPT optimization failed: {str(e)}")

            # Fallback to equal weights (only if MPT fails)
            n_assets = len(predicted_returns)
            fallback_weights = np.ones(n_assets) / n_assets
            fallback_return = np.dot(fallback_weights, predicted_returns)
            fallback_vol = np.sqrt(np.dot(fallback_weights.T, np.dot(cov_matrix, fallback_weights)))
            fallback_sharpe = (fallback_return - rf_rate) / fallback_vol if fallback_vol > 0 else 0

            safe_print(f"  > Using fallback equal weights: {fallback_weights}")

            return {
                'Long': {
                    'weights': fallback_weights,
                    'return': fallback_return,
                    'volatility': fallback_vol,
                    'sharpe': fallback_sharpe
                },
                'Short': {
                    'weights': -fallback_weights,
                    'return': -fallback_return,
                    'volatility': fallback_vol,
                    'sharpe': (-fallback_return - rf_rate) / fallback_vol if fallback_vol > 0 else 0
                },
                'Market_neutral': {
                    'weights': np.array([0.5, -0.5]) if n_assets == 2 else np.zeros(n_assets),
                    'return': 0.0,
                    'volatility': 0.1,
                    'sharpe': 0.0
                }
            }

    def _select_best_strategy(self, optimization_results: Dict[str, any]) -> str:
        """Select best strategy using original logic from simple_dynamic_backtester.py line 136."""

        # Handle different result structures
        if 'phase2_results' in optimization_results:
            # New structure with phase results
            phase2_results = optimization_results['phase2_results']
            result = phase2_results['strategies']
        else:
            # Direct structure for backwards compatibility
            result = optimization_results

        # Original strategy selection logic (lines 142-148)
        if (result["Long"]["sharpe"] > 0 and
            (result["Long"]["return"] - result["Short"]["return"] >
             result["Long"]["volatility"] - result["Short"]["volatility"])):
            decision = "Long"
        else:
            decision = "Short"

        # Log the decision for debugging (matching original lines 150-165)
        long_sharpe = result["Long"]["sharpe"]
        long_return = result["Long"]["return"]
        long_volatility = result["Long"]["volatility"]
        short_return = result["Short"]["return"]
        short_volatility = result["Short"]["volatility"]

        excess_return = long_return - short_return
        excess_volatility = long_volatility - short_volatility

        safe_print(f"  > Strategy Selection:")
        safe_print(f"    - Long Sharpe: {long_sharpe:.6f}")
        safe_print(f"    - Long excess return: {excess_return:.6f}")
        safe_print(f"    - Long excess volatility: {excess_volatility:.6f}")
        safe_print(f"    - Decision: {decision}")

        return decision

    def _store_optimization_results(
        self,
        results: pd.DataFrame,
        index: int,
        asset_codes: List[str],
        optimization_results: Dict[str, any],
        best_strategy: str,
        predicted_returns: np.ndarray,
        cov_matrix: np.ndarray
    ):
        """Store optimization results in DataFrame."""

        # Handle new optimization result structure
        if 'phase1_results' in optimization_results:
            # New structure with phase1_results
            strategies = optimization_results  # Direct access to Long, Short, Market_neutral
            phase2_results = optimization_results.get('phase2_results', {})
        else:
            # Legacy structure
            strategies = optimization_results.get('strategies', optimization_results)

        # Store strategy performance (Phase 1 Max Sharpe results)
        results.iloc[index, results.columns.get_loc('Max_Sharpe_Long')] = strategies['Long']['sharpe']
        results.iloc[index, results.columns.get_loc('Max_Sharpe_Short')] = strategies['Short']['sharpe']
        results.iloc[index, results.columns.get_loc('Max_Sharpe_Market_Neutral')] = strategies['Market_neutral']['sharpe']

        # Store volatilities
        results.iloc[index, results.columns.get_loc('Volatility_Long')] = strategies['Long']['volatility']
        results.iloc[index, results.columns.get_loc('Volatility_Short')] = strategies['Short']['volatility']
        results.iloc[index, results.columns.get_loc('Volatility_Market_Neutral')] = strategies['Market_neutral']['volatility']

        # Store covariance matrix
        for i, asset_i in enumerate(asset_codes):
            for j, asset_j in enumerate(asset_codes):
                cov_col = f'Cov_{asset_i}_{asset_j}'
                results.iloc[index, results.columns.get_loc(cov_col)] = cov_matrix[i, j]

        # Store best strategy weights and risky weight
        best_weights = strategies[best_strategy]['weights']

        # Get risky weight from phase2 results if available
        if 'phase2_results' in optimization_results:
            phase2_strategies = optimization_results['phase2_results']['strategies']
            risky_weight = phase2_strategies[best_strategy]['risky_weight']
        else:
            # Fallback to a default risky weight
            risky_weight = 1.0

        for j, asset in enumerate(asset_codes):
            results.iloc[index, results.columns.get_loc(f'{asset}_Weight')] = best_weights[j]

        results.iloc[index, results.columns.get_loc('Risky_Weight')] = risky_weight
        results.iloc[index, results.columns.get_loc('Strategy')] = best_strategy
        results.iloc[index, results.columns.get_loc('Used_Predicted_Returns')] = True

        # Decision log
        decision_log = f"Updated {best_strategy}, w_risky={risky_weight:.6f}, returns=ARIMA_STRICT"
        results.iloc[index, results.columns.get_loc('Decision_Log')] = decision_log

        safe_print(f"  > Stored results: Strategy={best_strategy}, Risky_Weight={risky_weight:.6f}")
        safe_print(f"  > Portfolio weights: {dict(zip(asset_codes, best_weights))}")

        # Calculate expected return
        expected_return = np.dot(best_weights, predicted_returns)
        results.iloc[index, results.columns.get_loc('Expected_Port_Return')] = expected_return

        # Calculate portfolio volatility
        portfolio_volatility = np.sqrt(
            np.dot(best_weights.T, np.dot(cov_matrix, best_weights))
        )
        results.iloc[index, results.columns.get_loc('Portfolio_Volatility')] = portfolio_volatility

    def _carry_forward_weights(
        self,
        results: pd.DataFrame,
        index: int,
        asset_codes: List[str]
    ):
        """Carry forward weights from previous day."""

        # Carry forward asset weights
        for asset in asset_codes:
            results.iloc[index, results.columns.get_loc(f'{asset}_Weight')] = (
                results.iloc[index-1][f'{asset}_Weight']
            )

        results.iloc[index, results.columns.get_loc('Risky_Weight')] = (
            results.iloc[index-1]['Risky_Weight']
        )
        results.iloc[index, results.columns.get_loc('Strategy')] = (
            results.iloc[index-1]['Strategy']
        )
        results.iloc[index, results.columns.get_loc('Used_Predicted_Returns')] = True
        results.iloc[index, results.columns.get_loc('Decision_Log')] = "Maintained previous weights"

        # Carry forward performance metrics
        performance_cols = [
            'Max_Sharpe_Long', 'Max_Sharpe_Short', 'Max_Sharpe_Market_Neutral',
            'Volatility_Long', 'Volatility_Short', 'Volatility_Market_Neutral',
            'Portfolio_Volatility'
        ]

        for col in performance_cols:
            results.iloc[index, results.columns.get_loc(col)] = results.iloc[index-1][col]

        # Carry forward covariance matrix
        for asset_i in asset_codes:
            for asset_j in asset_codes:
                cov_col = f'Cov_{asset_i}_{asset_j}'
                results.iloc[index, results.columns.get_loc(cov_col)] = (
                    results.iloc[index-1][cov_col]
                )

    def _calculate_daily_return(
        self,
        results: pd.DataFrame,
        index: int,
        asset_codes: List[str]
    ):
        """Calculate daily portfolio return."""

        current_strategy = results.iloc[index]['Strategy']
        rf_rate = 0.0001075  # Should come from config

        if current_strategy == 'No Investment':
            portfolio_return = rf_rate
        else:
            # Get current weights and returns
            current_weights = [results.iloc[index][f'{asset}_Weight'] for asset in asset_codes]
            current_risky_weight = results.iloc[index]['Risky_Weight']
            actual_returns = [results.iloc[index][f'{asset}_Return'] for asset in asset_codes]

            # Calculate portfolio return
            w_risky = current_risky_weight
            w_max_sr = np.array(current_weights)
            r_actual = np.array(actual_returns)

            risky_portfolio_performance = np.dot(r_actual, w_max_sr)
            risky_allocation_return = w_risky * risky_portfolio_performance
            risk_free_allocation_return = (1 - w_risky) * rf_rate
            portfolio_return = risky_allocation_return + risk_free_allocation_return

        results.iloc[index, results.columns.get_loc('Daily_Return')] = portfolio_return

        # Update portfolio value
        results.iloc[index, results.columns.get_loc('Portfolio_Value')] = (
            results.iloc[index-1]['Portfolio_Value'] * (1 + portfolio_return)
        )

    def _calculate_summary(
        self,
        results: pd.DataFrame,
        decision_start_index: int,
        asset_codes: List[str]
    ) -> Dict[str, any]:
        """Calculate summary statistics."""

        decision_data = results.iloc[decision_start_index:]

        if len(decision_data) == 0:
            return {"error": "No decision data available"}

        final_value = decision_data['Portfolio_Value'].iloc[-1]
        total_return = final_value - 1.0
        prediction_usage = decision_data['Used_Predicted_Returns'].sum()

        # Calculate annualized metrics
        days_in_year = 252
        annualized_return = (
            (final_value ** (days_in_year / len(decision_data)) - 1) * 100
            if len(decision_data) > 0 else 0.0
        )

        # Volatility metrics
        portfolio_vol_data = decision_data['Portfolio_Volatility'].dropna()
        avg_portfolio_vol = portfolio_vol_data.mean() if len(portfolio_vol_data) > 0 else 0.0
        annualized_volatility = avg_portfolio_vol * np.sqrt(days_in_year) * 100

        sharpe_ratio = (
            annualized_return / annualized_volatility
            if annualized_volatility > 0 else 0.0
        )

        return {
            "total_days": len(results),
            "decision_days": len(decision_data),
            "final_portfolio_value": round(final_value, 4),
            "total_return_pct": round(total_return * 100, 2),
            "annualized_return_pct": round(annualized_return, 2),
            "annualized_volatility_pct": round(annualized_volatility, 2),
            "sharpe_ratio": round(sharpe_ratio, 4),
            "prediction_usage_days": f"{prediction_usage}/{len(decision_data)}",
            "asset_codes": asset_codes
        }

    def _export_results(self, results: pd.DataFrame, asset_codes: List[str], save_to_results_dir: bool = False) -> Path:
        """Export results to Excel file."""

        if save_to_results_dir:
            # Save to results directory for job-based processing
            from app.core.config import settings
            output_dir = Path(settings.RESULTS_DIR)
        else:
            # Save to temp directory for immediate response
            output_dir = Path(tempfile.mkdtemp())

        asset_str = "_".join(asset_codes)
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        output_path = output_dir / f"portfolio_optimization_{asset_str}_{timestamp}.xlsx"

        # Clean data of any potential Unicode issues before saving
        try:
            # Create ASCII-safe copy of results
            clean_results = results.copy()

            # Convert any object columns to ASCII-safe strings
            for col in clean_results.columns:
                if clean_results[col].dtype == 'object':
                    clean_results[col] = clean_results[col].astype(str).str.encode('ascii', 'replace').str.decode('ascii')

            with pd.ExcelWriter(output_path, engine='openpyxl') as writer:
                # Main results
                clean_results.to_excel(writer, sheet_name='Results', index=False)

                # Strategy summary
                strategy_summary = clean_results['Strategy'].value_counts()
                pd.DataFrame({
                    'Strategy': strategy_summary.index,
                    'Days': strategy_summary.values,
                    'Percentage': (strategy_summary.values / len(clean_results) * 100).round(1)
                }).to_excel(writer, sheet_name='Strategy_Summary', index=False)

        except Exception as excel_error:
            # Fallback: save as CSV if Excel fails
            logger.warning(f"Excel save failed, using CSV fallback: {str(excel_error)}")
            csv_path = output_path.with_suffix('.csv')
            results.to_csv(csv_path, index=False, encoding='ascii', errors='replace')
            output_path = csv_path

        logger.info(f"Results exported to: {output_path}")
        return output_path