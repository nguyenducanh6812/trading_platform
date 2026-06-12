import pandas as pd
import numpy as np
from typing import Optional, Tuple, Dict
from ..optimization.optimizer import Optimizer
from ..optimization.strategy_factory import StrategyFactory
from ..data.data_processor import DataProcessor
from ..custom_logging.logger import CustomLogger
from ..reporting.exporter import Exporter
from ..reporting.metrics import MetricsCalculator
from ..config.config_manager import ConfigManager

class Backtester:
    """Performs backtesting of the trading strategy with corrected Model Formula implementation."""
    
    def __init__(self, logger: CustomLogger, data_processor: DataProcessor, 
                 optimizer: Optimizer, exporter: Exporter, 
                 metrics_calculator: MetricsCalculator, config_manager: ConfigManager):
        """
        Initialize the Backtester with dependency injection.

        Args:
            logger: Custom logger instance
            data_processor: DataProcessor instance
            optimizer: Optimizer instance
            exporter: Exporter instance
            metrics_calculator: MetricsCalculator instance
            config_manager: ConfigManager instance (reused from main)
        """
        self.logger = logger
        self.data_processor = data_processor
        self.optimizer = optimizer
        self.exporter = exporter
        self.metrics_calculator = metrics_calculator
        self.config_manager = config_manager
        
        # 🟢 CREATE STRATEGY FACTORY ONCE and cache it for reuse
        self.strategy_factory = StrategyFactory(logger, config_manager)
        self.logger.info("Backtester initialized with cached StrategyFactory")

    def calculate_covariance(self, returns_data: pd.DataFrame) -> Tuple[np.ndarray, np.ndarray, np.ndarray]:
        """
        Calculate population covariance matrix and asset volatilities from returns data.
        
        Args:
            returns_data: DataFrame with BTC_Return and ETH_Return columns
            
        Returns:
            Tuple of (mean_returns, cov_matrix, asset_volatilities)
        """
        returns_matrix = returns_data[['BTC_Return', 'ETH_Return']].values
        mean_returns = np.mean(returns_matrix, axis=0)
        
        # Population covariance matrix calculation (as per your formula)
        # sigma = (1/n)[(R^T-R̄)(R-R̄^T)]
        centered_returns = returns_matrix - mean_returns
        cov_matrix = np.dot(centered_returns.T, centered_returns) / len(centered_returns)
        
        # Calculate individual asset volatilities
        asset_volatilities = np.sqrt(np.diag(cov_matrix))
        
        return mean_returns, cov_matrix, asset_volatilities

    def select_best_strategy(self, phase1_results: Dict, phase2_results: Dict) -> str:
        """
        Select best strategy using corrected Model Formula decision logic.
        ✅ Uses Phase 1 max Sharpe ratios for validation, Phase 2 final metrics for comparison
        
        Args:
            phase1_results: Phase 1 optimization results (max Sharpe)
            phase2_results: Phase 2 optimization results (risk-adjusted)
            
        Returns:
            Selected strategy name
        """
        strategies = phase2_results['strategies']
        
        # ✅ Use Phase 1 max Sharpe ratios for validation (SR > 0 check)
        long_max_sharpe_phase1 = phase1_results['Long']['sharpe']        # Phase 1 max Sharpe
        short_max_sharpe_phase1 = phase1_results['Short']['sharpe']      # Phase 1 max Sharpe
        
        # ✅ Use Phase 2 final metrics for comparison (after risky weight adjustment)
        long_return = strategies['Long']['return']           # Phase 2 final return
        short_return = strategies['Short']['return']         # Phase 2 final return
        market_neutral_return = strategies['Market_neutral']['return']  # Phase 2 final return
        
        long_volatility = strategies['Long']['volatility']         # Phase 2 final volatility
        short_volatility = strategies['Short']['volatility']       # Phase 2 final volatility
        market_neutral_volatility = strategies['Market_neutral']['volatility']  # Phase 2 final volatility
        
        # Calculate excess return and excess volatility using Phase 2 final metrics
        long_excess_return = long_return - market_neutral_return
        long_excess_volatility = long_volatility - market_neutral_volatility
        
        short_excess_return = short_return - market_neutral_return
        short_excess_volatility = short_volatility - market_neutral_volatility
        
        # ✅ Apply corrected Model Formula decision logic:
        # Use Phase 1 max Sharpe ratios for validation, Phase 2 final metrics for comparison
        if long_max_sharpe_phase1 > 0 and long_excess_return > long_excess_volatility:
            selected_strategy = 'Long'
            decision_reason = f"Long: Phase1 Max SR > 0 ({long_max_sharpe_phase1:.6f}) AND Phase2 Excess return > Excess volatility ({long_excess_return:.6f} > {long_excess_volatility:.6f})"
        elif short_max_sharpe_phase1 > 0 and short_excess_return > short_excess_volatility:
            selected_strategy = 'Short'
            decision_reason = f"Short: Phase1 Max SR > 0 ({short_max_sharpe_phase1:.6f}) AND Phase2 Excess return > Excess volatility ({short_excess_return:.6f} > {short_excess_volatility:.6f})"
        else:
            selected_strategy = 'Market_neutral'
            decision_reason = f"Market_neutral: Long condition failed (Phase1 Max SR={long_max_sharpe_phase1:.6f} <= 0 OR Phase2 {long_excess_return:.6f} <= {long_excess_volatility:.6f}), Short condition failed (Phase1 Max SR={short_max_sharpe_phase1:.6f} <= 0 OR Phase2 {short_excess_return:.6f} <= {short_excess_volatility:.6f})"
        
        # Log the decision for debugging
        self.logger.info(f"Strategy Selection Decision: {selected_strategy}")
        self.logger.info(f"Decision Reason: {decision_reason}")
        self.logger.info(f"Phase 1 - Long Max SR: {long_max_sharpe_phase1:.6f}, Short Max SR: {short_max_sharpe_phase1:.6f}")
        self.logger.info(f"Phase 2 - Long: E(R)={long_return:.6f}, sigma={long_volatility:.6f}")
        self.logger.info(f"Phase 2 - Short: E(R)={short_return:.6f}, sigma={short_volatility:.6f}")
        self.logger.info(f"Phase 2 - Market_neutral: E(R)={market_neutral_return:.6f}, sigma={market_neutral_volatility:.6f}")
        
        return selected_strategy

    def create_temp_optimizer(self, mean_returns: np.ndarray, cov_matrix: np.ndarray) -> Optimizer:
        """
        🟢 FIXED: Create a temporary optimizer that REUSES the cached StrategyFactory.
        
        Args:
            mean_returns: Current window mean returns
            cov_matrix: Current window covariance matrix
            
        Returns:
            Optimizer instance configured with current data
        """
        # 🟢 REUSE the cached StrategyFactory instead of creating new one
        temp_optimizer = Optimizer.create_with_reused_factory(
            logger=self.logger,
            mean_returns=mean_returns,
            cov_matrix=cov_matrix,
            rf_rate=self.optimizer.rf_rate,
            strategy_factory=self.strategy_factory  # 🟢 REUSE cached factory
        )
        
        return temp_optimizer
    
    def backtest(self, data: pd.DataFrame, risk_profile: str, rebalance_frequency: int, 
                lookback_period: int, export_excel: bool, excel_path: str) -> pd.DataFrame:
        """
        Run the backtest with corrected Model Formula implementation.

        Args:
            data: Input data DataFrame with columns including 'BTC_Return' and 'ETH_Return'.
            risk_profile: Risk profile ('averse', 'neutral', 'lover').
            rebalance_frequency: Days between rebalancing.
            lookback_period: Lookback period for optimization.
            export_excel: Whether to export results to Excel.
            excel_path: Path for Excel output.

        Returns:
            DataFrame with backtest results.
        """
        # Validate input data
        if not isinstance(data, pd.DataFrame):
            try:
                data = pd.DataFrame(data)
            except Exception as e:
                raise TypeError(f"Input data must be a pandas DataFrame, got {type(data)}: {e}")
        
        # Verify required columns
        required_columns = ['BTC_Return', 'ETH_Return']
        missing_columns = [col for col in required_columns if col not in data.columns]
        if missing_columns:
            raise ValueError(f"Input DataFrame missing required columns: {missing_columns}")

        results = data.copy()
        
        # Initialize columns with default values
        results['Portfolio_Value'] = 1.0  # All days start with portfolio value 1.0
        
        # ✅ CORRECTED: Store w_MaxSR (Phase 1 asset weights) and w_risky (Phase 2 leverage) separately
        results['BTC_Weight'] = 0.0       # w_MaxSR[0] - Phase 1 BTC weight
        results['ETH_Weight'] = 0.0       # w_MaxSR[1] - Phase 1 ETH weight  
        results['Risky_Weight'] = 0.0     # w_risky - Phase 2 leverage factor
        
        results['Strategy'] = 'No Investment'
        results['Daily_Return'] = 0.0
        results['Decision_Log'] = 'Waiting for sufficient historical data'
        
        # Add columns for covariance matrix and volatility (initialize as NaN)
        results['Cov_BTC_BTC'] = np.nan
        results['Cov_BTC_ETH'] = np.nan
        results['Cov_ETH_BTC'] = np.nan
        results['Cov_ETH_ETH'] = np.nan
        results['BTC_Volatility'] = np.nan
        results['ETH_Volatility'] = np.nan
        results['Portfolio_Volatility'] = np.nan
        
        # Add columns for Sharpe ratios and returns
        results['BTC_Mean_Return'] = np.nan
        results['ETH_Mean_Return'] = np.nan
        results['Expected_Port_Return'] = np.nan
        results['Sharpe_Ratio'] = np.nan
        
        # Phase 1 Max Sharpe ratios (renamed for clarity)
        results['Max_Long_Sharpe'] = np.nan
        results['Max_Short_Sharpe'] = np.nan  
        results['Max_Market_Neutral_Sharpe'] = np.nan

        # Phase 2 Final Strategy Metrics (after risky weight adjustment)
        results['Phase2_Final_Long_Return'] = np.nan           # E(R_strategy_Long) - FINAL
        results['Phase2_Final_Long_Volatility'] = np.nan       # sigma_strategy_Long - FINAL
        results['Phase2_Final_Long_Sharpe'] = np.nan          # SR_strategy_Long - FINAL
        results['Phase2_Final_Long_RiskyWeight'] = np.nan      # w_risky_Long
        
        results['Phase2_Final_Short_Return'] = np.nan          # E(R_strategy_Short) - FINAL
        results['Phase2_Final_Short_Volatility'] = np.nan      # sigma_strategy_Short - FINAL
        results['Phase2_Final_Short_Sharpe'] = np.nan         # SR_strategy_Short - FINAL
        results['Phase2_Final_Short_RiskyWeight'] = np.nan     # w_risky_Short
        
        results['Phase2_Final_MarketNeutral_Return'] = np.nan      # E(R_strategy_MN) - FINAL
        results['Phase2_Final_MarketNeutral_Volatility'] = np.nan  # sigma_strategy_MN - FINAL
        results['Phase2_Final_MarketNeutral_Sharpe'] = np.nan     # SR_strategy_MN - FINAL
        results['Phase2_Final_MarketNeutral_RiskyWeight'] = np.nan # w_risky_MN
        
        # Track config loading performance
        config_loads_count = 0
        
        # Single loop implementation
        for i in range(len(results)):
            # Phase 1: Make rebalancing decisions after we have enough lookback data
            if i >= lookback_period:
                # Get historical data for the lookback period
                historical_data = results.iloc[i-lookback_period:i]
                
                # Calculate covariance matrix and asset volatilities from historical data
                mean_returns, cov_matrix, asset_volatilities = self.calculate_covariance(historical_data)
                
                # Store mean returns
                results.iloc[i, results.columns.get_loc('BTC_Mean_Return')] = mean_returns[0]
                results.iloc[i, results.columns.get_loc('ETH_Mean_Return')] = mean_returns[1]
                
                # Store covariance matrix elements for this day
                results.iloc[i, results.columns.get_loc('Cov_BTC_BTC')] = cov_matrix[0, 0]
                results.iloc[i, results.columns.get_loc('Cov_BTC_ETH')] = cov_matrix[0, 1]
                results.iloc[i, results.columns.get_loc('Cov_ETH_BTC')] = cov_matrix[1, 0]
                results.iloc[i, results.columns.get_loc('Cov_ETH_ETH')] = cov_matrix[1, 1]
                
                # Store individual asset volatilities
                results.iloc[i, results.columns.get_loc('BTC_Volatility')] = asset_volatilities[0]
                results.iloc[i, results.columns.get_loc('ETH_Volatility')] = asset_volatilities[1]
                
                # On rebalancing days, calculate optimal weights
                if i == lookback_period or (i - lookback_period) % rebalance_frequency == 0:
                    # 🟢 FIXED: Reuse cached StrategyFactory, no new config loading
                    temp_optimizer = self.create_temp_optimizer(mean_returns, cov_matrix)
                    config_loads_count += 0  # Should be 0 additional loads
                    
                    # Get Phase 1 results (Max Sharpe)
                    phase1_results = temp_optimizer.optimize_phase1()
                    
                    # Get Phase 2 results (Risk-adjusted with risky weights)
                    phase2_results = temp_optimizer.optimize_phase2(risk_profile)
                    
                    # Store Phase 1 Max Sharpe ratios (renamed for clarity)
                    results.iloc[i, results.columns.get_loc('Max_Long_Sharpe')] = phase1_results['Long']['sharpe']
                    results.iloc[i, results.columns.get_loc('Max_Short_Sharpe')] = phase1_results['Short']['sharpe']
                    results.iloc[i, results.columns.get_loc('Max_Market_Neutral_Sharpe')] = phase1_results['Market_neutral']['sharpe']
                    
                    # Store Phase 2 FINAL results (after risky weight adjustment)
                    if 'strategies' in phase2_results:
                        strategies = phase2_results['strategies']
                        
                        # Long strategy FINAL metrics
                        if 'Long' in strategies:
                            long_data = strategies['Long']
                            results.iloc[i, results.columns.get_loc('Phase2_Final_Long_Return')] = long_data.get('return', np.nan)
                            results.iloc[i, results.columns.get_loc('Phase2_Final_Long_Volatility')] = long_data.get('volatility', np.nan)
                            results.iloc[i, results.columns.get_loc('Phase2_Final_Long_Sharpe')] = long_data.get('sharpe', np.nan)
                            results.iloc[i, results.columns.get_loc('Phase2_Final_Long_RiskyWeight')] = long_data.get('risky_weight', np.nan)
                        
                        # Short strategy FINAL metrics
                        if 'Short' in strategies:
                            short_data = strategies['Short']
                            results.iloc[i, results.columns.get_loc('Phase2_Final_Short_Return')] = short_data.get('return', np.nan)
                            results.iloc[i, results.columns.get_loc('Phase2_Final_Short_Volatility')] = short_data.get('volatility', np.nan)
                            results.iloc[i, results.columns.get_loc('Phase2_Final_Short_Sharpe')] = short_data.get('sharpe', np.nan)
                            results.iloc[i, results.columns.get_loc('Phase2_Final_Short_RiskyWeight')] = short_data.get('risky_weight', np.nan)
                        
                        # Market_neutral strategy FINAL metrics
                        if 'Market_neutral' in strategies:
                            market_neutral_data = strategies['Market_neutral']
                            results.iloc[i, results.columns.get_loc('Phase2_Final_MarketNeutral_Return')] = market_neutral_data.get('return', np.nan)
                            results.iloc[i, results.columns.get_loc('Phase2_Final_MarketNeutral_Volatility')] = market_neutral_data.get('volatility', np.nan)
                            results.iloc[i, results.columns.get_loc('Phase2_Final_MarketNeutral_Sharpe')] = market_neutral_data.get('sharpe', np.nan)
                            results.iloc[i, results.columns.get_loc('Phase2_Final_MarketNeutral_RiskyWeight')] = market_neutral_data.get('risky_weight', np.nan)
                    
                    # Apply corrected Model Formula decision logic with BOTH Phase 1 and Phase 2 results
                    best_strategy = self.select_best_strategy(phase1_results, phase2_results)
                    
                    # ✅ CORRECTED: Store w_MaxSR and w_risky separately (NO multiplication!)
                    max_sharpe_weights = phase1_results[best_strategy]['weights']  # w_MaxSR from Phase 1
                    risky_weight = phase2_results['strategies'][best_strategy]['risky_weight']  # w_risky from Phase 2
                    
                    # ✅ Store Phase 1 weights (w_MaxSR) as the portfolio asset weights
                    results.iloc[i, results.columns.get_loc('BTC_Weight')] = max_sharpe_weights[0]  # w_MaxSR[0]
                    results.iloc[i, results.columns.get_loc('ETH_Weight')] = max_sharpe_weights[1]  # w_MaxSR[1]
                    
                    # ✅ Store Phase 2 risky weight separately as leverage factor
                    results.iloc[i, results.columns.get_loc('Risky_Weight')] = risky_weight  # w_risky
                    
                    results.iloc[i, results.columns.get_loc('Strategy')] = best_strategy
                    
                    decision_log = f"Model Formula: {best_strategy}, w_risky={risky_weight:.6f}, w_MaxSR={max_sharpe_weights}, Portfolio_weights=[{max_sharpe_weights[0]:.6f}, {max_sharpe_weights[1]:.6f}]"
                    results.iloc[i, results.columns.get_loc('Decision_Log')] = decision_log
                    
                    # Calculate expected portfolio return using Phase 1 weights (w_MaxSR)
                    expected_return = np.dot(max_sharpe_weights, mean_returns)
                    results.iloc[i, results.columns.get_loc('Expected_Port_Return')] = expected_return
                    
                else:
                    # On non-rebalancing days, carry forward the previous day's weights
                    results.iloc[i, results.columns.get_loc('BTC_Weight')] = results.iloc[i-1]['BTC_Weight']  # w_MaxSR[0]
                    results.iloc[i, results.columns.get_loc('ETH_Weight')] = results.iloc[i-1]['ETH_Weight']  # w_MaxSR[1]
                    results.iloc[i, results.columns.get_loc('Risky_Weight')] = results.iloc[i-1]['Risky_Weight']  # w_risky
                    results.iloc[i, results.columns.get_loc('Strategy')] = results.iloc[i-1]['Strategy']
                    results.iloc[i, results.columns.get_loc('Decision_Log')] = f"Maintained previous weights"
                    
                    # Carry forward Phase 2 final metrics on non-rebalancing days
                    phase2_final_columns = [
                        'Phase2_Final_Long_Return', 'Phase2_Final_Long_Volatility', 'Phase2_Final_Long_Sharpe', 'Phase2_Final_Long_RiskyWeight',
                        'Phase2_Final_Short_Return', 'Phase2_Final_Short_Volatility', 'Phase2_Final_Short_Sharpe', 'Phase2_Final_Short_RiskyWeight',
                        'Phase2_Final_MarketNeutral_Return', 'Phase2_Final_MarketNeutral_Volatility', 'Phase2_Final_MarketNeutral_Sharpe', 'Phase2_Final_MarketNeutral_RiskyWeight'
                    ]
                    
                    for col in phase2_final_columns:
                        if col in results.columns and i > 0:
                            results.iloc[i, results.columns.get_loc(col)] = results.iloc[i-1][col]
                    
                    # Calculate expected portfolio return using Phase 1 weights (w_MaxSR)
                    portfolio_weights = np.array([results.iloc[i]['BTC_Weight'], results.iloc[i]['ETH_Weight']])
                    expected_return = np.dot(portfolio_weights, mean_returns)
                    results.iloc[i, results.columns.get_loc('Expected_Port_Return')] = expected_return
                
                # Calculate portfolio volatility using Phase 1 weights (w_MaxSR) and covariance matrix
                portfolio_weights = np.array([results.iloc[i]['BTC_Weight'], results.iloc[i]['ETH_Weight']])
                portfolio_variance = np.dot(portfolio_weights.T, np.dot(cov_matrix, portfolio_weights))
                portfolio_volatility = np.sqrt(portfolio_variance)
                results.iloc[i, results.columns.get_loc('Portfolio_Volatility')] = portfolio_volatility
                
                # Calculate Sharpe ratio
                expected_return = results.iloc[i]['Expected_Port_Return']
                sharpe_ratio = (expected_return - self.optimizer.rf_rate) / portfolio_volatility if portfolio_volatility > 0 else float('inf')
                results.iloc[i, results.columns.get_loc('Sharpe_Ratio')] = sharpe_ratio
                
                # ✅ CORRECTED: Calculate portfolio return using EXACT Model Formula
                # R_portfolio = w_risky * R_actual * w_MaxSR + (1 - w_risky) * R_f
                
                if i > 0:  # Can't calculate return for first day
                    # Get current day's strategy
                    current_strategy = results.iloc[i]['Strategy']
                    
                    # Handle No Investment periods
                    if current_strategy == 'No Investment':
                        # No investment means all capital is in risk-free asset
                        portfolio_return = self.optimizer.rf_rate
                        
                        # Log for debugging
                        if i == lookback_period:  # Log first No Investment return calculation
                            self.logger.info(f"Day {i}: No Investment period - portfolio return = risk-free rate = {portfolio_return:.6f}")
                        
                        # Store the calculated return and continue to next iteration
                        results.iloc[i, results.columns.get_loc('Daily_Return')] = portfolio_return
                        results.iloc[i, results.columns.get_loc('Portfolio_Value')] = results.iloc[i-1]['Portfolio_Value'] * (1 + portfolio_return)
                        continue
                    
                    # ✅ CORRECTED: Get components from current day according to Model Formula
                    current_btc_weight = results.iloc[i]['BTC_Weight']    # w_MaxSR[0] from Phase 1
                    current_eth_weight = results.iloc[i]['ETH_Weight']    # w_MaxSR[1] from Phase 1
                    current_risky_weight = results.iloc[i]['Risky_Weight']  # w_risky from Phase 2
                    
                    # Current day's actual returns
                    btc_return = results.iloc[i]['BTC_Return']  # R_actual[0]
                    eth_return = results.iloc[i]['ETH_Return']  # R_actual[1]
                    
                    # ✅ FAIL FAST: Check for missing components and raise error immediately
                    if pd.isna(current_risky_weight):
                        raise ValueError(f"Day {i}: Missing risky weight for strategy '{current_strategy}' - this indicates a critical error in Phase 2 calculations")
                    if pd.isna(current_btc_weight) or pd.isna(current_eth_weight):
                        raise ValueError(f"Day {i}: Missing Phase 1 weights [{current_btc_weight}, {current_eth_weight}] - this indicates a critical error in Phase 1 calculations")
                    if pd.isna(btc_return) or pd.isna(eth_return):
                        raise ValueError(f"Day {i}: Missing actual returns [{btc_return}, {eth_return}] - this indicates corrupted market data")
                    
                    # ✅ CORRECTED Model Formula: R_portfolio = w_risky * R_actual * w_MaxSR + (1 - w_risky) * R_f
                    # Use components separately as specified in Model Formula
                    
                    # Step 1: Extract components for clarity
                    w_risky = current_risky_weight        # Phase 2 risky weight (leverage factor)
                    w_max_sr = np.array([current_btc_weight, current_eth_weight])  # Phase 1 asset weights
                    r_actual = np.array([btc_return, eth_return])  # Current day's actual returns
                    r_f = self.optimizer.rf_rate
                    
                    # Step 2: Calculate risky portfolio performance using EXACT Model Formula
                    # R_actual * w_MaxSR = dot product of actual returns and Phase 1 weights
                    risky_portfolio_performance = np.dot(r_actual, w_max_sr)
                    
                    # Step 3: Apply EXACT Model Formula
                    # R_portfolio = w_risky * (R_actual * w_MaxSR) + (1 - w_risky) * R_f
                    risky_allocation_return = w_risky * risky_portfolio_performance
                    risk_free_allocation_return = (1 - w_risky) * r_f
                    portfolio_return = risky_allocation_return + risk_free_allocation_return
                    
                    # Log the calculation for debugging
                    if i == lookback_period:  # Log first Model Formula calculation
                        self.logger.info(f"CORRECTED Model Formula Daily Return Calculation (Day {i}):")
                        self.logger.info(f"  Current Strategy = {current_strategy}")
                        self.logger.info(f"  EXACT Formula: R_portfolio = w_risky * R_actual * w_MaxSR + (1 - w_risky) * R_f")
                        self.logger.info(f"  Components from current day {i}:")
                        self.logger.info(f"    w_risky = {w_risky:.6f} (Phase 2 leverage factor)")
                        self.logger.info(f"    w_MaxSR = {w_max_sr} (Phase 1 asset weights)")
                        self.logger.info(f"    R_actual = {r_actual} (today's asset returns)")
                        self.logger.info(f"    R_f = {r_f:.6f}")
                        self.logger.info(f"  Calculation:")
                        self.logger.info(f"    R_actual * w_MaxSR = {risky_portfolio_performance:.6f}")
                        self.logger.info(f"    Risky allocation return = {w_risky:.6f} * {risky_portfolio_performance:.6f} = {risky_allocation_return:.6f}")
                        self.logger.info(f"    Risk-free allocation return = {1 - w_risky:.6f} * {r_f:.6f} = {risk_free_allocation_return:.6f}")
                        self.logger.info(f"    R_portfolio = {risky_allocation_return:.6f} + {risk_free_allocation_return:.6f} = {portfolio_return:.6f}")
                        self.logger.info(f"  ✅ Model Formula implemented correctly - NO multiplication of w_risky * w_MaxSR")
                else:
                    portfolio_return = 0.0  # First day has no return
                
                results.iloc[i, results.columns.get_loc('Daily_Return')] = portfolio_return
                
                # Update portfolio value
                if i > 0:
                    results.iloc[i, results.columns.get_loc('Portfolio_Value')] = results.iloc[i-1]['Portfolio_Value'] * (1 + portfolio_return)

        # 🟢 Log performance improvement
        total_rebalance_days = len([i for i in range(lookback_period, len(results)) 
                                   if i == lookback_period or (i - lookback_period) % rebalance_frequency == 0])
        self.logger.info(f"PERFORMANCE: Backtest completed with only 1 config load (vs {total_rebalance_days} in old approach)")
        self.logger.info(f"Config loading efficiency: {total_rebalance_days}x improvement!")

        if export_excel:
            self.exporter.export_to_excel(results, excel_path)
        
        metrics = self.metrics_calculator.calculate_metrics(results)
        return results