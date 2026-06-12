"""
Simple Dynamic Backtester
=========================

Simple copy of existing backtester.py with minimal changes:
1. Dynamic asset loading instead of hardcoded BTC/ETH
2. Use predicted returns instead of mean returns for optimization
"""

from typing import Tuple, List

import numpy as np
import pandas as pd

from .backtester import Backtester
from ..config.config_manager import ConfigManager
from ..custom_logging.logger import CustomLogger
from ..data.data_processor import DataProcessor
from ..optimization.optimizer import Optimizer
from ..reporting.exporter import Exporter
from ..reporting.metrics import MetricsCalculator


class SimpleDynamicBacktester(Backtester):
    """Simple dynamic backtester - copy of original with minimal changes."""
    
    def __init__(self, logger: CustomLogger, data_processor: DataProcessor, 
                 optimizer: Optimizer, exporter: Exporter, 
                 metrics_calculator: MetricsCalculator, config_manager: ConfigManager,
                 asset_codes: List[str]):
        """Initialize with asset codes."""
        super().__init__(logger, data_processor, optimizer, exporter, metrics_calculator, config_manager)
        self.asset_codes = asset_codes
        # Optimization method configuration
        self.optimization_method = 'traditional'  # Default to traditional
        self.smart_grid_precision = 2  # Default precision for Smart Grid (keep at 2 for performance)
        self.weight_precision = None  # Override config weight_precision if set (None = use config default)
        self.logger.info(f"Simple dynamic backtester initialized for assets: {asset_codes}")

    def calculate_covariance_dynamic(self, returns_data: pd.DataFrame) -> Tuple[np.ndarray, np.ndarray, np.ndarray]:
        """Calculate covariance for dynamic assets (same logic as original)."""
        return_columns = [f'{asset}_Return' for asset in self.asset_codes]
        returns_matrix = returns_data[return_columns].values
        mean_returns = np.mean(returns_matrix, axis=0)
        
        # Same formula as original
        centered_returns = returns_matrix - mean_returns
        cov_matrix = np.dot(centered_returns.T, centered_returns) / len(centered_returns)
        
        asset_volatilities = np.sqrt(np.diag(cov_matrix))
        
        return mean_returns, cov_matrix, asset_volatilities

    def get_predicted_returns(self, data: pd.DataFrame, index: int) -> np.ndarray:
        """Get predicted returns for current day - STRICT MODE: Must have all predictions."""
        predicted_returns = []
        missing_assets = []
        
        for asset in self.asset_codes:
            pred_col = f'Prd_Return_Arima_{asset}'
            if pred_col not in data.columns:
                missing_assets.append(f"{asset} (column {pred_col} not found)")
            elif pd.isna(data.iloc[index][pred_col]):
                missing_assets.append(f"{asset} (prediction is NaN at index {index})")
            else:
                predicted_returns.append(data.iloc[index][pred_col])
        
        # FAIL FAST: If any predictions missing, raise error immediately
        if missing_assets:
            current_date = data.iloc[index]['Timestamp'].strftime('%Y-%m-%d') if 'Timestamp' in data.columns else f"index {index}"
            error_msg = f"STRICT MODE FAILURE at {current_date}: Missing ARIMA predictions for {missing_assets}. Model validation failed - cannot proceed without complete predictions."
            
            # Print to both logger and console for easy copying
            self.logger.error(error_msg)
            print(f"ERROR: {error_msg}")
            print(f"DETAILS: Missing predictions at index {index} for assets: {missing_assets}")
            
            raise ValueError(error_msg)
        
        return np.array(predicted_returns)

    def create_temp_optimizer_with_predictions(self, cov_matrix: np.ndarray,
                                             predicted_returns: np.ndarray, historical_mean_returns: np.ndarray) -> Optimizer:
        """Create temp optimizer using predicted returns - STRICT MODE: Always use predictions."""
        # STRICT MODE: Always use predicted returns (no fallback to mean returns)
        # BUT use consistent matrix dimensions: historical covariance with historical structure
        
        # Log the dimensional consistency check
        self.logger.info(f"Matrix dimensions - Cov: {cov_matrix.shape}, Predicted: {predicted_returns.shape}, Historical: {historical_mean_returns.shape}")
        
        # Use predicted returns for optimization (this is what we want for forward-looking decisions)
        temp_optimizer = Optimizer.create_with_reused_factory(
            logger=self.logger,
            predicted_returns=predicted_returns,  # STRICT MODE: Always use predicted returns for forward-looking optimization
            cov_matrix=cov_matrix,  # Historical covariance structure (computed from historical returns)
            rf_rate=self.optimizer.rf_rate,
            strategy_factory=self.strategy_factory
        )
        
        return temp_optimizer
    
    def create_temp_optimizer_with_weight_precision(self, cov_matrix: np.ndarray,
                                                   predicted_returns: np.ndarray, historical_mean_returns: np.ndarray) -> Optimizer:
        """Create temp optimizer with custom weight precision if specified."""
        # Log the dimensional consistency check
        self.logger.info(f"Matrix dimensions - Cov: {cov_matrix.shape}, Predicted: {predicted_returns.shape}, Historical: {historical_mean_returns.shape}")
        
        # Override weight precision if specified
        if self.weight_precision is not None:
            self.logger.info(f"Overriding config weight_precision with custom value: {self.weight_precision}")
            # Create a temporary config manager with custom weight precision
            from copy import deepcopy
            temp_config_manager = deepcopy(self.config_manager)
            temp_config_manager.get_config().strategy.weight_precision = self.weight_precision
            
            # Create optimizer with custom config
            temp_optimizer = Optimizer(
                logger=self.logger,
                predicted_returns=predicted_returns,
                cov_matrix=cov_matrix,
                rf_rate=self.optimizer.rf_rate,
                config_manager=temp_config_manager
            )
        else:
            # Use the original method with default config
            temp_optimizer = Optimizer.create_with_reused_factory(
                logger=self.logger,
                predicted_returns=predicted_returns,
                cov_matrix=cov_matrix,
                rf_rate=self.optimizer.rf_rate,
                strategy_factory=self.strategy_factory
            )
        
        return temp_optimizer

    def select_best_strategy_updated(self, phase1_results: dict, phase2_results: dict) -> str:
        """
        Updated strategy selection logic using Long/Short/Full Hedging comparison.
        """
        result = phase2_results['strategies']
        
        # Updated decision logic as provided
        if (result["Long"]["sharpe"] > 0 and 
            (result["Long"]["return"] - result["Short"]["return"] >
             result["Long"]["volatility"] - result["Short"]["volatility"])):
            decision = "Long"
        else:
            decision = "Short"
        
        # Log the decision for debugging
        long_sharpe = result["Long"]["sharpe"]
        long_return = result["Long"]["return"]
        long_volatility = result["Long"]["volatility"]
        mn_return = result["Short"]["return"]
        mn_volatility = result["Short"]["volatility"]
        
        excess_return = long_return - mn_return
        excess_volatility = long_volatility - mn_volatility
        
        self.logger.info(f"Updated Strategy Selection:")
        self.logger.info(f"  Long Sharpe: {long_sharpe:.6f}")
        self.logger.info(f"  Long excess return: {excess_return:.6f}")
        self.logger.info(f"  Long excess volatility: {excess_volatility:.6f}")
        self.logger.info(f"  Decision: {decision}")
        
        return decision

    def backtest_dynamic(self, data: pd.DataFrame, risk_profile: str, rebalance_frequency: int, 
                        lookback_period: int, export_excel: bool, excel_path: str) -> pd.DataFrame:
        """Run backtest with dynamic assets - copy of original logic with minimal changes."""
        
        # Validate input data
        if not isinstance(data, pd.DataFrame):
            try:
                data = pd.DataFrame(data)
            except Exception as e:
                raise TypeError(f"Input data must be a pandas DataFrame, got {type(data)}: {e}")
        
        # Verify required columns (DYNAMIC)
        required_columns = [f'{asset}_Return' for asset in self.asset_codes]
        missing_columns = [col for col in required_columns if col not in data.columns]
        if missing_columns:
            raise ValueError(f"Input DataFrame missing required columns: {missing_columns}")

        results = data.copy()
        
        # Initialize columns - DYNAMIC asset weights
        results['Portfolio_Value'] = 1.0
        
        for asset in self.asset_codes:
            results[f'{asset}_Weight'] = 0.0
        results['Risky_Weight'] = 0.0  # This was missing!
        
        results['Strategy'] = 'No Investment'
        results['Daily_Return'] = 0.0
        results['Decision_Log'] = 'Waiting for sufficient historical data'
        
        # Add tracking for predicted returns usage
        results['Used_Predicted_Returns'] = False
        
        # Add columns for mean returns - DYNAMIC
        for asset in self.asset_codes:
            results[f'{asset}_Mean_Return'] = np.nan
        
        results['Expected_Port_Return'] = np.nan
        results['Sharpe_Ratio'] = np.nan
        
        # Add max Sharpe ratio columns for each strategy
        results['Max_Sharpe_Long'] = np.nan
        results['Max_Sharpe_Short'] = np.nan
        results['Max_Sharpe_Market_Neutral'] = np.nan
        
        # Add volatility columns for each strategy
        results['Volatility_Long'] = np.nan
        results['Volatility_Short'] = np.nan
        results['Volatility_Market_Neutral'] = np.nan
        results['Portfolio_Volatility'] = np.nan
        
        # Add covariance matrix columns (flattened)
        cov_size = len(self.asset_codes)
        for i in range(cov_size):
            for j in range(cov_size):
                asset_i = self.asset_codes[i]
                asset_j = self.asset_codes[j]
                results[f'Cov_{asset_i}_{asset_j}'] = np.nan
        
        # STRICT MODE: Validate sufficient data for lookback period
        if len(results) < lookback_period:
            error_msg = f"STRICT MODE ERROR: Insufficient data for lookback period. Need {lookback_period} days, got {len(results)} days."
            self.logger.error(error_msg)
            print(f"ERROR: {error_msg}")
            raise ValueError(error_msg)
        
        # EXACT SAME LOOP LOGIC AS ORIGINAL
        for i in range(len(results)):
            if i >= lookback_period:
                # Get historical data for the lookback period
                historical_data = results.iloc[i-lookback_period:i]
                
                # Calculate covariance matrix - DYNAMIC
                mean_returns, cov_matrix, asset_volatilities = self.calculate_covariance_dynamic(historical_data)
                
                # Log matrix dimensions and values for debugging
                self.logger.info(f"Day {i}: Historical mean returns: {mean_returns}")
                self.logger.info(f"Day {i}: Covariance matrix shape: {cov_matrix.shape}")
                self.logger.info(f"Day {i}: Asset volatilities: {asset_volatilities}")
                
                # Store mean returns - DYNAMIC
                for j, asset in enumerate(self.asset_codes):
                    results.iloc[i, results.columns.get_loc(f'{asset}_Mean_Return')] = mean_returns[j]
                
                # On rebalancing days - EXACT SAME CONDITION AS ORIGINAL
                if i == lookback_period or (i - lookback_period) % rebalance_frequency == 0:
                    
                    # Get predicted returns for this day - STRICT MODE: Must have predictions
                    predicted_returns = self.get_predicted_returns(results, i)  # Will raise error if missing
                    
                    # Validate dimensional consistency
                    if len(predicted_returns) != len(mean_returns):
                        error_msg = f"DIMENSIONAL MISMATCH at day {i}: Predicted returns length {len(predicted_returns)} != Historical returns length {len(mean_returns)}"
                        self.logger.error(error_msg)
                        print(f"ERROR: {error_msg}")
                        raise ValueError(error_msg)
                    
                    # Log what we're using (always predictions in strict mode)
                    self.logger.info(f"Day {i}: Using ARIMA predicted returns for optimization: {predicted_returns}")
                    self.logger.info(f"Day {i}: Historical mean returns (for reference): {mean_returns}")
                    self.logger.info(f"Day {i}: Predicted vs Historical diff: {predicted_returns - mean_returns}")
                    
                    # Create optimizer with predicted returns and custom weight precision if specified
                    temp_optimizer = self.create_temp_optimizer_with_weight_precision(cov_matrix, predicted_returns, mean_returns)
                    
                    # Use selected optimization method
                    if self.optimization_method == 'traditional':
                        if self.weight_precision is not None:
                            self.logger.info(f"Day {i}: Using Traditional optimization with weight precision: {self.weight_precision} decimals")
                        else:
                            self.logger.info(f"Day {i}: Using Traditional optimization (config default precision)")
                        phase1_results = temp_optimizer.optimize_phase1()
                    elif self.optimization_method == 'smart_grid':
                        self.logger.info(f"Day {i}: Using Smart Grid optimization (precision: {self.smart_grid_precision})")
                        phase1_results = temp_optimizer.optimize_phase1_smart_grid('max_sharpe', self.smart_grid_precision)
                    elif self.optimization_method == 'compare':
                        self.logger.info(f"Day {i}: Using Comparison mode - Traditional vs Smart Grid")
                        comparison_results = temp_optimizer.compare_optimization_methods('max_sharpe')
                        phase1_results = comparison_results['smart_grid']  # Use Smart Grid results for decision
                        # Log comparison summary
                        for strategy, data in comparison_results['improvements'].items():
                            self.logger.info(f"  {strategy}: Traditional={data['traditional_sharpe']:.6f} -> "
                                           f"SmartGrid={data['smart_grid_sharpe']:.6f} "
                                           f"({data['improvement_percent']:+.2f}%)")
                    else:
                        self.logger.warning(f"Unknown optimization method: {self.optimization_method}, using traditional")
                        phase1_results = temp_optimizer.optimize_phase1()
                    
                    phase2_results = temp_optimizer.optimize_phase2(risk_profile)
                    
                    # UPDATED STRATEGY SELECTION LOGIC
                    best_strategy = self.select_best_strategy_updated(phase1_results, phase2_results)
                    
                    # Store max Sharpe ratios for each strategy - NEW FEATURE
                    results.iloc[i, results.columns.get_loc('Max_Sharpe_Long')] = phase1_results['Long']['sharpe']
                    results.iloc[i, results.columns.get_loc('Max_Sharpe_Short')] = phase1_results['Short']['sharpe']
                    results.iloc[i, results.columns.get_loc('Max_Sharpe_Market_Neutral')] = phase1_results['Market_neutral']['sharpe']
                    
                    # Store volatility for each strategy - NEW FEATURE
                    results.iloc[i, results.columns.get_loc('Volatility_Long')] = phase1_results['Long']['volatility']
                    results.iloc[i, results.columns.get_loc('Volatility_Short')] = phase1_results['Short']['volatility']
                    results.iloc[i, results.columns.get_loc('Volatility_Market_Neutral')] = phase1_results['Market_neutral']['volatility']
                    
                    # Store covariance matrix - NEW FEATURE
                    for idx_i in range(len(self.asset_codes)):
                        for idx_j in range(len(self.asset_codes)):
                            asset_i = self.asset_codes[idx_i]
                            asset_j = self.asset_codes[idx_j]
                            cov_col = f'Cov_{asset_i}_{asset_j}'
                            results.iloc[i, results.columns.get_loc(cov_col)] = cov_matrix[idx_i, idx_j]
                    
                    # Store weights - DYNAMIC
                    max_sharpe_weights = phase1_results[best_strategy]['weights']
                    risky_weight = phase2_results['strategies'][best_strategy]['risky_weight']
                    
                    for j, asset in enumerate(self.asset_codes):
                        results.iloc[i, results.columns.get_loc(f'{asset}_Weight')] = max_sharpe_weights[j]
                    
                    results.iloc[i, results.columns.get_loc('Risky_Weight')] = risky_weight
                    results.iloc[i, results.columns.get_loc('Strategy')] = best_strategy
                    results.iloc[i, results.columns.get_loc('Used_Predicted_Returns')] = True  # Always True in strict mode
                    
                    # Decision log with new strategy selection
                    decision_log = f"Updated {best_strategy}, w_risky={risky_weight:.6f}, returns=ARIMA_STRICT"
                    results.iloc[i, results.columns.get_loc('Decision_Log')] = decision_log
                    
                    # Calculate expected return using predicted returns (strict mode)
                    # This is the portfolio's expected return based on ARIMA predictions
                    returns_for_calculation = predicted_returns
                    expected_return = np.dot(max_sharpe_weights, returns_for_calculation)
                    results.iloc[i, results.columns.get_loc('Expected_Port_Return')] = expected_return
                    
                    # Calculate portfolio volatility using covariance matrix
                    portfolio_volatility = np.sqrt(np.dot(max_sharpe_weights.T, np.dot(cov_matrix, max_sharpe_weights)))
                    results.iloc[i, results.columns.get_loc('Portfolio_Volatility')] = portfolio_volatility
                    
                else:
                    # Carry forward weights - DYNAMIC
                    for asset in self.asset_codes:
                        results.iloc[i, results.columns.get_loc(f'{asset}_Weight')] = results.iloc[i-1][f'{asset}_Weight']
                    results.iloc[i, results.columns.get_loc('Risky_Weight')] = results.iloc[i-1]['Risky_Weight']
                    results.iloc[i, results.columns.get_loc('Strategy')] = results.iloc[i-1]['Strategy']
                    results.iloc[i, results.columns.get_loc('Used_Predicted_Returns')] = True  # Always True in strict mode
                    results.iloc[i, results.columns.get_loc('Decision_Log')] = f"Maintained previous weights"
                    
                    # Carry forward max Sharpe ratios - NEW FEATURE
                    results.iloc[i, results.columns.get_loc('Max_Sharpe_Long')] = results.iloc[i-1]['Max_Sharpe_Long']
                    results.iloc[i, results.columns.get_loc('Max_Sharpe_Short')] = results.iloc[i-1]['Max_Sharpe_Short']
                    results.iloc[i, results.columns.get_loc('Max_Sharpe_Market_Neutral')] = results.iloc[i-1]['Max_Sharpe_Market_Neutral']
                    
                    # Carry forward volatility data - NEW FEATURE
                    results.iloc[i, results.columns.get_loc('Volatility_Long')] = results.iloc[i-1]['Volatility_Long']
                    results.iloc[i, results.columns.get_loc('Volatility_Short')] = results.iloc[i-1]['Volatility_Short']
                    results.iloc[i, results.columns.get_loc('Volatility_Market_Neutral')] = results.iloc[i-1]['Volatility_Market_Neutral']
                    results.iloc[i, results.columns.get_loc('Portfolio_Volatility')] = results.iloc[i-1]['Portfolio_Volatility']
                    
                    # Carry forward covariance matrix - NEW FEATURE
                    for idx_i in range(len(self.asset_codes)):
                        for idx_j in range(len(self.asset_codes)):
                            asset_i = self.asset_codes[idx_i]
                            asset_j = self.asset_codes[idx_j]
                            cov_col = f'Cov_{asset_i}_{asset_j}'
                            results.iloc[i, results.columns.get_loc(cov_col)] = results.iloc[i-1][cov_col]
                    
                    # Calculate expected return - DYNAMIC
                    portfolio_weights = np.array([results.iloc[i][f'{asset}_Weight'] for asset in self.asset_codes])
                    expected_return = np.dot(portfolio_weights, mean_returns)
                    results.iloc[i, results.columns.get_loc('Expected_Port_Return')] = expected_return
                
                # EXACT SAME DAILY RETURN CALCULATION AS ORIGINAL - DYNAMIC
                if i > 0:
                    current_strategy = results.iloc[i]['Strategy']
                    
                    if current_strategy == 'No Investment':
                        portfolio_return = self.optimizer.rf_rate
                    else:
                        # Get weights and returns - DYNAMIC
                        current_weights = [results.iloc[i][f'{asset}_Weight'] for asset in self.asset_codes]
                        current_risky_weight = results.iloc[i]['Risky_Weight']
                        
                        # Current day's actual returns - DYNAMIC
                        actual_returns = [results.iloc[i][f'{asset}_Return'] for asset in self.asset_codes]
                        
                        # UPDATED MODEL FORMULA WITH NEW STRATEGY SELECTION
                        w_risky = current_risky_weight
                        w_max_sr = np.array(current_weights)
                        r_actual = np.array(actual_returns)
                        r_f = self.optimizer.rf_rate
                        
                        risky_portfolio_performance = np.dot(r_actual, w_max_sr)
                        risky_allocation_return = w_risky * risky_portfolio_performance
                        risk_free_allocation_return = (1 - w_risky) * r_f
                        portfolio_return = risky_allocation_return + risk_free_allocation_return
                    
                    results.iloc[i, results.columns.get_loc('Daily_Return')] = portfolio_return
                    
                    # Update portfolio value
                    results.iloc[i, results.columns.get_loc('Portfolio_Value')] = results.iloc[i-1]['Portfolio_Value'] * (1 + portfolio_return)

        self.logger.info(f"Simple dynamic backtest completed")
        
        if export_excel:
            self.exporter.export_to_excel(results, excel_path)
        
        return results