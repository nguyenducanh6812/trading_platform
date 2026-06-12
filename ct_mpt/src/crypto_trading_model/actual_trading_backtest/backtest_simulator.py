"""
Main backtest simulator for actual trading with fees and rebalancing.

Implements the core simulation logic following the requirements specification.
"""

import pandas as pd
import numpy as np
from typing import Dict, Any, Optional
from pathlib import Path

from .config import ActualTradingConfig
from .trading_account import TradingAccount
from .rebalancing import create_rebalancing_strategy, RebalancingStrategy
from .results_exporter import ActualTradingResultsExporter
from ..custom_logging.logger import CustomLogger


class ActualTradingBacktest:
    """
    Main backtest simulator implementing realistic trading behavior.
    
    Follows Single Responsibility Principle and Dependency Inversion Principle.
    """
    
    def __init__(
        self,
        config: ActualTradingConfig,
        logger: Optional[CustomLogger] = None
    ):
        """
        Initialize the backtest simulator.
        
        Args:
            config: Configuration for the simulation
            logger: Optional logger instance
        """
        self.config = config
        self.logger = logger
        
        # Initialize components
        self.trading_account = TradingAccount(
            config.account,
            config.assets,
            config.trading
        )
        
        self.rebalancing_strategy = create_rebalancing_strategy(config.rebalancing)
        self.results_exporter = ActualTradingResultsExporter(logger)
        
        # Simulation state
        self.results_data = []
        self.current_day_index = 0
        
        # Weight tracking for fee calculation
        self.previous_weights = {
            'btc_weight': None,
            'eth_weight': None,
            'risky_weight': None
        }
        
        if self.logger:
            self.logger.info("ActualTradingBacktest initialized")
    
    def run_backtest(
        self,
        backtest_data: pd.DataFrame,
        output_path: Optional[str] = None
    ) -> pd.DataFrame:
        """
        Run the complete backtest simulation.
        
        Args:
            backtest_data: DataFrame with backtest results from simple_backtest
            output_path: Optional path to export results
            
        Returns:
            DataFrame with actual trading simulation results
        """
        if self.logger:
            self.logger.info(f"Starting actual trading backtest with {len(backtest_data)} days")
        
        print("BACKTEST: About to validate input data...")
        # Validate input data
        self._validate_input_data(backtest_data)
        print("BACKTEST: Input data validation completed")
        
        print("BACKTEST: About to reset simulation...")
        # Reset simulation state
        self._reset_simulation()
        print("BACKTEST: Simulation reset completed")
        
        # Process each day
        print("BACKTEST: About to start processing daily trading...")
        for index, row in backtest_data.iterrows():
            if index == 0:
                print(f"BACKTEST: Processing first day (index {index})...")
            elif index == 1:
                print(f"BACKTEST: Processing second day (index {index})...")
            elif index % 50 == 0:
                print(f"BACKTEST: Processing day {index + 1}/{len(backtest_data)}...")
            
            self.current_day_index = index
            daily_result = self._process_daily_trading(row, index)
            self.results_data.append(daily_result)
            
            # Log progress periodically
            if self.logger and (index + 1) % 100 == 0:
                self.logger.info(f"Processed {index + 1}/{len(backtest_data)} days")
        
        print("BACKTEST: Daily processing completed, creating results DataFrame...")
        
        # Create results DataFrame
        results_df = pd.DataFrame(self.results_data)
        
        # Export results if path provided
        if output_path:
            self.results_exporter.export_to_excel(results_df, output_path)
        
        if self.logger:
            self.logger.info(f"Backtest completed. Final portfolio value: {results_df['trading_account_after'].iloc[-1]:.2f}")
        
        return results_df
    
    def _validate_input_data(self, data: pd.DataFrame) -> None:
        """Validate input backtest data."""
        print("VALIDATE: Starting input data validation...")
        print(f"VALIDATE: Data shape: {data.shape}")
        print(f"VALIDATE: Data columns: {list(data.columns)}")
        
        required_columns = [
            'BTC_Weight', 'ETH_Weight', 'Risky_Weight', 'Strategy',
            'Daily_Return'
        ]
        print(f"VALIDATE: Required columns: {required_columns}")
        
        # Check for price columns (flexible naming)
        price_columns = []
        print("VALIDATE: Looking for price columns...")
        for asset in ['BTC', 'ETH']:
            print(f"VALIDATE: Searching for {asset} price columns...")
            open_col = None
            close_col = None
            
            # Look for various naming patterns
            for col in data.columns:
                if asset in col and ('open' in col.lower() or 'Open' in col):
                    open_col = col
                    print(f"VALIDATE: Found {asset} open column: {open_col}")
                elif asset in col and ('close' in col.lower() or 'Close' in col):
                    close_col = col
                    print(f"VALIDATE: Found {asset} close column: {close_col}")
            
            if open_col and close_col:
                price_columns.extend([open_col, close_col])
                print(f"VALIDATE: {asset} price columns found successfully")
            else:
                print(f"VALIDATE: ERROR - Missing {asset} price columns")
                raise ValueError(f"Missing {asset} price columns in input data")
        
        print("VALIDATE: Checking for missing required columns...")
        missing_columns = [col for col in required_columns if col not in data.columns]
        if missing_columns:
            print(f"VALIDATE: ERROR - Missing required columns: {missing_columns}")
            raise ValueError(f"Missing required columns: {missing_columns}")
        
        print("VALIDATE: All validations passed!")
        if self.logger:
            self.logger.info(f"Input data validation passed. Found price columns: {price_columns}")
    
    def _reset_simulation(self) -> None:
        """Reset simulation state for new run."""
        print("RESET: Starting simulation reset...")
        print("RESET: Clearing results data...")
        self.results_data = []
        self.current_day_index = 0
        
        # Reset weight tracking
        self.previous_weights = {
            'btc_weight': None,
            'eth_weight': None,
            'risky_weight': None
        }
        
        print("RESET: Recreating trading account...")
        # Reset trading account
        self.trading_account = TradingAccount(
            self.config.account,
            self.config.assets,
            self.config.trading
        )
        print("RESET: Trading account recreated")
        
        print("RESET: Recreating rebalancing strategy...")
        # Reset rebalancing strategy
        self.rebalancing_strategy = create_rebalancing_strategy(self.config.rebalancing)
        print("RESET: Simulation reset completed successfully")
    
    def _process_daily_trading(self, day_data: pd.Series, day_index: int) -> Dict[str, Any]:
        """
        Process trading for a single day.
        
        Args:
            day_data: Series with day's data
            day_index: Day index in the simulation
            
        Returns:
            Dictionary with day's results
        """
        # Get timestamp
        timestamp = day_data.get('Timestamp', day_data.name if hasattr(day_data, 'name') else day_index)
        
        # Extract price data
        btc_open_price, btc_close_price = self._extract_prices(day_data, 'BTC')
        eth_open_price, eth_close_price = self._extract_prices(day_data, 'ETH')
        
        # Get portfolio weights and strategy
        btc_weight = day_data['BTC_Weight']
        eth_weight = day_data['ETH_Weight']
        risky_weight = day_data['Risky_Weight']
        strategy = day_data['Strategy']
        
        # Check if weights changed from previous day
        weights_changed = False
        is_first_day = self.previous_weights['btc_weight'] is None
        
        if not is_first_day:
            if (abs(btc_weight - self.previous_weights['btc_weight']) > 1e-8 or
                abs(eth_weight - self.previous_weights['eth_weight']) > 1e-8 or
                abs(risky_weight - self.previous_weights['risky_weight']) > 1e-8):
                weights_changed = True
        # First day - no weight change, no decision made
        
        # Check if rebalancing should occur FIRST (before any trading activity)
        rebalancing_occurred = False
        rebalancing_info = {}
        
        if isinstance(timestamp, pd.Timestamp):
            if self.rebalancing_strategy.should_rebalance(timestamp):
                rebalancing_info = self.trading_account.rebalance_accounts()
                self.rebalancing_strategy.record_rebalance(timestamp)
                rebalancing_occurred = True
        
        # Record trading account state AFTER rebalancing (this becomes the "before trading" state)
        account_before = self.trading_account.current_state.trading_balance
        
        # Calculate asset allocation
        btc_quantity, eth_quantity = self.trading_account.calculate_asset_allocation(
            risky_weight=risky_weight,
            btc_weight=btc_weight,
            eth_weight=eth_weight,
            btc_open_price=btc_open_price,
            eth_open_price=eth_open_price
        )
        
        # Execute trades (apply fees only if weights changed)
        trading_fees = self.trading_account.execute_trade(
            new_btc_quantity=btc_quantity,
            new_eth_quantity=eth_quantity,
            btc_price=btc_open_price,
            eth_price=eth_open_price,
            apply_fees=weights_changed
        )
        
        # Calculate actual fee according to requirement: actual_fee(T) = 0.0015 if weight changed, else 0
        # The requirement specifies this should be the trading_fee rate (0.0015), not the USD amount
        actual_fee_rate = self.config.trading.trading_fee if weights_changed else 0.0
        
        # Get Daily_Portfolio_Return from input data
        daily_portfolio_return = day_data.get('Daily_Return', 0.0)
        
        # Calculate Final_Portfolio_Return based on decision-making and weight changes
        # Per requirement: 
        # - If decision made and weights same as previous: Final_Portfolio_Return = Daily_Return (no fee)
        # - If decision made and weights changed: Final_Portfolio_Return = Daily_Return - fee
        # - If no decision made: Final_Portfolio_Return = None
        
        # Decision logic: First day = no decision (no investment), other days = decision made
        decision_made = not is_first_day
        
        if decision_made:
            if weights_changed:
                # Decision made and weights changed: apply fee
                final_portfolio_return = daily_portfolio_return - actual_fee_rate
            else:
                # Decision made but weights same: no fee, just daily return
                final_portfolio_return = daily_portfolio_return
        else:
            # No decision made, no Final_Portfolio_Return calculation
            final_portfolio_return = None
        
        # Update trading account balance using new formula: trading_account(T) = trading_account(T-1) * (1 + Final_Portfolio_Return)
        # But only if we have a valid Final_Portfolio_Return
        if final_portfolio_return is not None:
            # Apply the new trading account update formula
            self.trading_account._trading_balance = account_before * (1 + final_portfolio_return)
        
        # Handle rebalancing reset (as per requirement)
        if rebalancing_occurred:
            # On rebalancing days, we might need special handling
            pass
        
        # Update previous weights for next day comparison
        self.previous_weights = {
            'btc_weight': btc_weight,
            'eth_weight': eth_weight,
            'risky_weight': risky_weight
        }
        
        # Record trading account state after all updates
        account_after = self.trading_account.current_state.trading_balance
        
        # Calculate final weights (after leverage application)
        final_btc_weight = btc_weight * risky_weight
        final_eth_weight = eth_weight * risky_weight
        
        # Calculate profit
        profit_usd = account_after - account_before
        
        # Get current account balances
        current_state = self.trading_account.current_state
        
        # Calculate withdrawal/transfer information
        withdraw_amount = 0.0
        withdraw_direction = "none"
        trading_account_change = account_after - account_before
        savings_account_change = 0.0
        
        if rebalancing_occurred and rebalancing_info:
            withdraw_amount = rebalancing_info.get('transfer_amount', 0.0)
            withdraw_direction = rebalancing_info.get('transfer_direction', 'none')
            
            # Calculate savings account change
            pre_rebalance_saving = rebalancing_info.get('pre_rebalance_saving', current_state.saving_balance)
            savings_account_change = current_state.saving_balance - pre_rebalance_saving
        
        # Create result record
        result = {
            'Timestamp': timestamp,
            'trading_account_before': account_before,
            'trading_account_after': account_after,
            'trading_account_change': trading_account_change,
            'savings_account_balance': current_state.saving_balance,
            'savings_account_change': savings_account_change,
            'total_account_balance': current_state.total_balance,
            'withdraw_amount': withdraw_amount,
            'withdraw_direction': withdraw_direction,
            'BTC_open_price': btc_open_price,
            'BTC_close_price': btc_close_price,
            'ETH_open_price': eth_open_price,
            'ETH_close_price': eth_close_price,
            'BTC_weight': btc_weight,
            'ETH_weight': eth_weight,
            'Risky_Weight': risky_weight,
            'Final_Decision': strategy,
            'Daily_Portfolio_Return': daily_portfolio_return,
            'Final_BTC_Weight': final_btc_weight,
            'Final_ETH_Weight': final_eth_weight,
            'Actual_BTC': current_state.btc_position.rounded_quantity,
            'Actual_ETH': current_state.eth_position.rounded_quantity,
            'Final_Portfolio_Return': final_portfolio_return,
            'Profit_USD': profit_usd,
            'trading_fees_paid': trading_fees,
            'rebalancing_occurred': rebalancing_occurred
        }
        
        # Add rebalancing info if it occurred
        if rebalancing_occurred:
            result.update({
                'transfer_amount': rebalancing_info.get('transfer_amount', 0.0),
                'transfer_direction': rebalancing_info.get('transfer_direction', 'none')
            })
        else:
            result.update({
                'transfer_amount': 0.0,
                'transfer_direction': 'none'
            })
        
        return result
    
    def _extract_prices(self, day_data: pd.Series, asset: str) -> tuple:
        """
        Extract open and close prices for an asset.
        
        Args:
            day_data: Series with day's data
            asset: Asset name ('BTC' or 'ETH')
            
        Returns:
            Tuple of (open_price, close_price)
        """
        # Look for price columns with flexible naming
        open_price = None
        close_price = None
        
        for col in day_data.index:
            if asset in col:
                if 'open' in col.lower() or 'Open' in col:
                    open_price = day_data[col]
                elif 'close' in col.lower() or 'Close' in col:
                    close_price = day_data[col]
        
        # Fallback to standard naming
        if open_price is None:
            open_price_col = f'{asset}_open_price'
            open_price = day_data.get(open_price_col, day_data.get(f'Open Price_{asset}', 0.0))
        
        if close_price is None:
            close_price_col = f'{asset}_close_price'
            close_price = day_data.get(close_price_col, day_data.get(f'Close Price_{asset}', 0.0))
        
        if open_price is None or close_price is None:
            raise ValueError(f"Could not find {asset} price data in day_data")
        
        return float(open_price), float(close_price)
    
    def get_simulation_summary(self) -> Dict[str, Any]:
        """
        Get summary of simulation results.
        
        Returns:
            Dictionary with simulation summary
        """
        if not self.results_data:
            return {"error": "No simulation data available"}
        
        results_df = pd.DataFrame(self.results_data)
        
        # Calculate summary statistics
        initial_balance = results_df['trading_account_before'].iloc[0]
        final_balance = results_df['trading_account_after'].iloc[-1]
        total_fees = results_df['trading_fees_paid'].sum()
        total_rebalances = results_df['rebalancing_occurred'].sum()
        
        returns = results_df['Final_Portfolio_Return']
        
        summary = {
            'initial_trading_balance': initial_balance,
            'final_trading_balance': final_balance,
            'total_profit_loss': final_balance - initial_balance,
            'total_return_pct': ((final_balance - initial_balance) / initial_balance * 100) if initial_balance > 0 else 0,
            'total_fees_paid': total_fees,
            'total_rebalancing_events': int(total_rebalances),
            'average_daily_return': returns.mean(),
            'volatility': returns.std(),
            'max_daily_return': returns.max(),
            'min_daily_return': returns.min(),
            'total_trading_days': len(results_df)
        }
        
        return summary
    
    def export_results(
        self,
        output_path: str,
        include_summary: bool = True
    ) -> None:
        """
        Export simulation results to Excel.
        
        Args:
            output_path: Path for output file
            include_summary: Whether to include summary statistics
        """
        if not self.results_data:
            raise ValueError("No simulation data to export")
        
        results_df = pd.DataFrame(self.results_data)
        self.results_exporter.export_to_excel(
            results_df,
            output_path,
            include_summary=include_summary
        )