"""
Trading Simulation Service
==========================

Trading simulation service (Step 2) - Extracted from ActualTradingBacktest.
Applies realistic trading constraints including fees, account management, and rebalancing.
"""

import pandas as pd
import numpy as np
from typing import Dict, List, Tuple, Optional, Any
from datetime import datetime
import tempfile
from pathlib import Path
from dataclasses import dataclass
from enum import Enum

from app.core.exceptions import (
    TradingSimulationError,
    ValidationError,
    ConfigurationError,
    InsufficientDataError
)
from app.utils.logger import get_logger
from app.services.data_processor import DataProcessor

logger = get_logger(__name__)


class RebalancingFrequency(Enum):
    """Rebalancing frequency options."""
    MONTHLY = "monthly"
    QUARTERLY = "quarterly"
    YEARLY = "yearly"
    CUSTOM = "custom"


@dataclass
class TradingAccountConfig:
    """Configuration for trading account setup."""
    initial_trading_balance: float = 500.0
    initial_saving_balance: float = 500.0
    target_trading_balance: float = 500.0

    @property
    def total_capital(self) -> float:
        return self.initial_trading_balance + self.initial_saving_balance


@dataclass
class AssetConfig:
    """Configuration for asset-specific parameters."""
    btc_decimal_places: int = 3
    eth_decimal_places: int = 2


@dataclass
class TradingConfig:
    """Configuration for trading parameters."""
    trading_fee: float = 0.0015
    leverage_scale: float = 1.5


@dataclass
class RebalancingConfig:
    """Configuration for account rebalancing strategy."""
    frequency: RebalancingFrequency = RebalancingFrequency.MONTHLY
    custom_days: Optional[int] = None


@dataclass
class AssetPosition:
    """Represents a position in a specific asset."""
    quantity: float
    asset_name: str
    decimal_places: int

    def __post_init__(self):
        self.quantity = round(self.quantity, self.decimal_places)

    @property
    def rounded_quantity(self) -> float:
        return round(self.quantity, self.decimal_places)

    def calculate_value(self, price: float) -> float:
        return self.rounded_quantity * price


@dataclass
class AccountState:
    """Represents the current state of trading accounts."""
    trading_balance: float
    saving_balance: float
    btc_position: AssetPosition
    eth_position: AssetPosition

    @property
    def total_balance(self) -> float:
        return self.trading_balance + self.saving_balance

    def calculate_portfolio_value(self, btc_price: float, eth_price: float) -> float:
        btc_value = self.btc_position.calculate_value(btc_price)
        eth_value = self.eth_position.calculate_value(eth_price)
        return self.trading_balance + btc_value + eth_value


class TradingAccount:
    """Manages trading and saving accounts with realistic constraints."""

    def __init__(self, account_config: TradingAccountConfig, asset_config: AssetConfig, trading_config: TradingConfig):
        self.account_config = account_config
        self.asset_config = asset_config
        self.trading_config = trading_config

        # Initialize account state
        self._trading_balance = account_config.initial_trading_balance
        self._saving_balance = account_config.initial_saving_balance

        # Initialize positions
        self._btc_position = AssetPosition(
            quantity=0.0,
            asset_name="BTC",
            decimal_places=asset_config.btc_decimal_places
        )
        self._eth_position = AssetPosition(
            quantity=0.0,
            asset_name="ETH",
            decimal_places=asset_config.eth_decimal_places
        )

    @property
    def current_state(self) -> AccountState:
        return AccountState(
            trading_balance=self._trading_balance,
            saving_balance=self._saving_balance,
            btc_position=self._btc_position,
            eth_position=self._eth_position
        )

    def calculate_asset_allocation(
        self,
        risky_weight: float,
        btc_weight: float,
        eth_weight: float,
        btc_open_price: float,
        eth_open_price: float
    ) -> Tuple[float, float]:
        """Calculate actual asset quantities based on portfolio weights."""
        scale = self.trading_config.leverage_scale

        # Calculate volume allocation for each asset
        btc_volume = self._trading_balance * scale * risky_weight * btc_weight
        eth_volume = self._trading_balance * scale * risky_weight * eth_weight

        # Calculate quantities and round
        btc_quantity = round(btc_volume / btc_open_price, self.asset_config.btc_decimal_places)
        eth_quantity = round(eth_volume / eth_open_price, self.asset_config.eth_decimal_places)

        return btc_quantity, eth_quantity

    def execute_trade(
        self,
        new_btc_quantity: float,
        new_eth_quantity: float,
        btc_price: float,
        eth_price: float,
        apply_fees: bool = True
    ) -> float:
        """Execute trade and update positions."""
        total_fee = 0.0

        if apply_fees:
            # Calculate fee based on new position values
            new_btc_value = abs(new_btc_quantity * btc_price)
            new_eth_value = abs(new_eth_quantity * eth_price)
            total_position_value = new_btc_value + new_eth_value

            total_fee = total_position_value * self.trading_config.trading_fee
            self._trading_balance -= total_fee

        # Update positions
        self._btc_position = AssetPosition(
            quantity=new_btc_quantity,
            asset_name="BTC",
            decimal_places=self.asset_config.btc_decimal_places
        )
        self._eth_position = AssetPosition(
            quantity=new_eth_quantity,
            asset_name="ETH",
            decimal_places=self.asset_config.eth_decimal_places
        )

        return total_fee

    def apply_daily_return(
        self,
        btc_return: float,
        eth_return: float,
        btc_price: float,
        eth_price: float
    ) -> float:
        """
        Apply daily returns to trading account.

        Args:
            btc_return: BTC daily return
            eth_return: ETH daily return
            btc_price: Current BTC price
            eth_price: Current ETH price

        Returns:
            Net account return after applying position returns
        """
        # Calculate position values before return
        initial_btc_value = self._btc_position.calculate_value(btc_price / (1 + btc_return))
        initial_eth_value = self._eth_position.calculate_value(eth_price / (1 + eth_return))
        initial_total = self._trading_balance + initial_btc_value + initial_eth_value

        # Calculate new position values after return
        new_btc_value = self._btc_position.calculate_value(btc_price)
        new_eth_value = self._eth_position.calculate_value(eth_price)
        new_total = self._trading_balance + new_btc_value + new_eth_value

        # Calculate account return
        if initial_total > 0:
            account_return = (new_total - initial_total) / initial_total
        else:
            account_return = 0.0

        # Update trading balance to reflect new total
        self._trading_balance = new_total - new_btc_value - new_eth_value

        return account_return

    def rebalance_accounts(self) -> Dict[str, float]:
        """Rebalance trading and saving accounts to target allocation."""
        target_trading = self.account_config.target_trading_balance
        current_trading = self._trading_balance

        rebalance_info = {
            "pre_rebalance_trading": current_trading,
            "pre_rebalance_saving": self._saving_balance,
            "target_trading": target_trading,
            "transfer_amount": 0.0,
            "transfer_direction": "none"
        }

        if current_trading > target_trading:
            # Transfer excess to savings
            transfer_amount = current_trading - target_trading
            self._trading_balance = target_trading
            self._saving_balance += transfer_amount

            rebalance_info["transfer_amount"] = transfer_amount
            rebalance_info["transfer_direction"] = "to_savings"

        elif current_trading < target_trading:
            # Transfer from savings to restore trading balance
            needed_amount = target_trading - current_trading

            if self._saving_balance >= needed_amount:
                self._trading_balance = target_trading
                self._saving_balance -= needed_amount

                rebalance_info["transfer_amount"] = needed_amount
                rebalance_info["transfer_direction"] = "from_savings"
            else:
                # Transfer all available savings
                self._trading_balance += self._saving_balance
                transfer_amount = self._saving_balance
                self._saving_balance = 0.0

                rebalance_info["transfer_amount"] = transfer_amount
                rebalance_info["transfer_direction"] = "from_savings_partial"

        rebalance_info["post_rebalance_trading"] = self._trading_balance
        rebalance_info["post_rebalance_saving"] = self._saving_balance

        return rebalance_info


class RebalancingStrategy:
    """Handles rebalancing logic."""

    def __init__(self, config: RebalancingConfig):
        self.config = config
        self._last_rebalance_date: Optional[pd.Timestamp] = None

    def should_rebalance(self, current_date: pd.Timestamp) -> bool:
        """Determine if rebalancing should occur on the given date."""
        if self._last_rebalance_date is None:
            return current_date.day == 1  # First rebalance on first day of month

        if self.config.frequency == RebalancingFrequency.MONTHLY:
            return (current_date.day == 1 and
                    current_date.month != self._last_rebalance_date.month)
        elif self.config.frequency == RebalancingFrequency.QUARTERLY:
            current_quarter = ((current_date.month - 1) // 3) + 1
            last_quarter = ((self._last_rebalance_date.month - 1) // 3) + 1
            return current_quarter != last_quarter
        elif self.config.frequency == RebalancingFrequency.YEARLY:
            return current_date.year != self._last_rebalance_date.year
        elif self.config.frequency == RebalancingFrequency.CUSTOM:
            days_since = (current_date - self._last_rebalance_date).days
            return days_since >= self.config.custom_days

        return False

    def record_rebalance(self, date: pd.Timestamp) -> None:
        """Record that rebalancing occurred on the given date."""
        self._last_rebalance_date = date


class TradingSimulator:
    """Main trading simulation service."""

    def __init__(self):
        self.data_processor = DataProcessor()

    async def simulate_trading(
        self,
        backtest_results_file: str,
        config: Dict[str, any]
    ) -> Dict[str, any]:
        """
        Main trading simulation method.

        Args:
            backtest_results_file: Path to backtest results from Step 1
            config: Trading simulation configuration

        Returns:
            Trading simulation results
        """
        trading_config = None  # Initialize to avoid NameError
        try:
            logger.info("Starting trading simulation")

            # Validate and load backtest results
            validation_result = self.data_processor.validate_file(
                backtest_results_file, "backtest_results"
            )
            logger.info(f"Input validation: {validation_result['status']}")

            # Load backtest data
            if backtest_results_file.endswith('.xlsx'):
                backtest_data = pd.read_excel(backtest_results_file)
            else:
                backtest_data = pd.read_csv(backtest_results_file)

            # Create trading configuration
            trading_config = self._create_trading_config(config)

            # Run trading simulation
            results = self._run_trading_simulation(backtest_data, trading_config)

            # Calculate summary
            summary = self._calculate_trading_summary(results)

            # Export results
            output_path = self._export_trading_results(results, config.get('include_summary', True))

            return {
                "status": "success",
                "results": results.to_dict('records'),
                "summary": summary,
                "output_file": str(output_path),
                "metadata": {
                    "input_rows": len(backtest_data),
                    "output_rows": len(results),
                    "trading_config": self._config_to_dict(trading_config) if trading_config else {}
                }
            }

        except Exception as e:
            if isinstance(e, (TradingSimulationError, ValidationError, ConfigurationError)):
                raise
            raise TradingSimulationError(f"Trading simulation failed: {str(e)}")

    def _create_trading_config(self, config: Dict[str, any]) -> Dict[str, any]:
        """Create trading configuration from input parameters."""

        # Extract configuration parameters
        total_capital = config.get('total_capital', 1000.0)
        trading_portion = config.get('trading_portion', 0.5)

        # Calculate account balances
        trading_balance = total_capital * trading_portion
        saving_balance = total_capital * (1 - trading_portion)

        # Create configuration objects
        account_config = TradingAccountConfig(
            initial_trading_balance=trading_balance,
            initial_saving_balance=saving_balance,
            target_trading_balance=trading_balance
        )

        asset_config = AssetConfig(
            btc_decimal_places=config.get('btc_decimal_places', 3),
            eth_decimal_places=config.get('eth_decimal_places', 2)
        )

        trading_config = TradingConfig(
            trading_fee=config.get('trading_fee', 0.0015),
            leverage_scale=config.get('leverage_scale', 1.5)
        )

        # Rebalancing configuration
        rebalancing_frequency = config.get('rebalancing_frequency', 'monthly')
        frequency_map = {
            'monthly': RebalancingFrequency.MONTHLY,
            'quarterly': RebalancingFrequency.QUARTERLY,
            'yearly': RebalancingFrequency.YEARLY,
            'custom': RebalancingFrequency.CUSTOM
        }

        rebalancing_config = RebalancingConfig(
            frequency=frequency_map.get(rebalancing_frequency, RebalancingFrequency.MONTHLY),
            custom_days=config.get('custom_rebalancing_days') if rebalancing_frequency == 'custom' else None
        )

        return {
            'account': account_config,
            'assets': asset_config,
            'trading': trading_config,
            'rebalancing': rebalancing_config
        }

    def _run_trading_simulation(
        self,
        backtest_data: pd.DataFrame,
        trading_config: Dict[str, any]
    ) -> pd.DataFrame:
        """Run the main trading simulation."""

        # Initialize trading account and rebalancing strategy
        trading_account = TradingAccount(
            trading_config['account'],
            trading_config['assets'],
            trading_config['trading']
        )

        rebalancing_strategy = RebalancingStrategy(trading_config['rebalancing'])

        # Validate input data
        self._validate_backtest_data(backtest_data)

        results_data = []

        # Track previous weights for fee calculation
        previous_weights = {
            'btc_weight': None,
            'eth_weight': None,
            'risky_weight': None
        }

        # Process each day
        for index, row in backtest_data.iterrows():
            daily_result = self._process_daily_trading(
                row, index, trading_account, rebalancing_strategy, previous_weights
            )
            results_data.append(daily_result)

        return pd.DataFrame(results_data)

    def _validate_backtest_data(self, data: pd.DataFrame) -> None:
        """Validate input backtest data."""
        required_columns = [
            'BTC_Weight', 'ETH_Weight', 'Risky_Weight', 'Strategy', 'Daily_Return'
        ]

        missing_columns = [col for col in required_columns if col not in data.columns]
        if missing_columns:
            # Check for price columns
            price_columns = []
            for asset in ['BTC', 'ETH']:
                for col in data.columns:
                    if asset in col and ('open' in col.lower() or 'close' in col.lower()):
                        price_columns.append(col)

            if not price_columns:
                missing_columns.append("Price columns (BTC/ETH open/close)")

            if missing_columns:
                raise ValidationError(f"Missing required columns: {missing_columns}")

    def _process_daily_trading(
        self,
        day_data: pd.Series,
        day_index: int,
        trading_account: TradingAccount,
        rebalancing_strategy: RebalancingStrategy,
        previous_weights: Dict[str, float]
    ) -> Dict[str, Any]:
        """Process trading for a single day."""

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
        is_first_day = previous_weights['btc_weight'] is None

        if not is_first_day:
            if (abs(btc_weight - previous_weights['btc_weight']) > 1e-8 or
                abs(eth_weight - previous_weights['eth_weight']) > 1e-8 or
                abs(risky_weight - previous_weights['risky_weight']) > 1e-8):
                weights_changed = True

        # Check if rebalancing should occur
        rebalancing_occurred = False
        rebalancing_info = {}

        if isinstance(timestamp, pd.Timestamp):
            if rebalancing_strategy.should_rebalance(timestamp):
                rebalancing_info = trading_account.rebalance_accounts()
                rebalancing_strategy.record_rebalance(timestamp)
                rebalancing_occurred = True

        # Record trading account state before trading
        account_before = trading_account.current_state.trading_balance

        # Calculate asset allocation
        btc_quantity, eth_quantity = trading_account.calculate_asset_allocation(
            risky_weight=risky_weight,
            btc_weight=btc_weight,
            eth_weight=eth_weight,
            btc_open_price=btc_open_price,
            eth_open_price=eth_open_price
        )

        # Execute trades (apply fees only if weights changed)
        trading_fees = trading_account.execute_trade(
            new_btc_quantity=btc_quantity,
            new_eth_quantity=eth_quantity,
            btc_price=btc_open_price,
            eth_price=eth_open_price,
            apply_fees=weights_changed
        )

        # Calculate fee rate
        actual_fee_rate = trading_account.trading_config.trading_fee if weights_changed else 0.0

        # Get Daily_Portfolio_Return from input data
        daily_portfolio_return = day_data.get('Daily_Return', 0.0)

        # Calculate Final_Portfolio_Return
        decision_made = not is_first_day

        if decision_made:
            if weights_changed:
                final_portfolio_return = daily_portfolio_return - actual_fee_rate
            else:
                final_portfolio_return = daily_portfolio_return
        else:
            final_portfolio_return = None

        # Update trading account balance
        if final_portfolio_return is not None:
            trading_account._trading_balance = account_before * (1 + final_portfolio_return)

        # Update previous weights
        previous_weights.update({
            'btc_weight': btc_weight,
            'eth_weight': eth_weight,
            'risky_weight': risky_weight
        })

        # Record final state
        account_after = trading_account.current_state.trading_balance
        current_state = trading_account.current_state

        # Calculate final weights
        final_btc_weight = btc_weight * risky_weight
        final_eth_weight = eth_weight * risky_weight

        # Create result record
        result = {
            'Timestamp': timestamp,
            'trading_account_before': account_before,
            'trading_account_after': account_after,
            'trading_account_change': account_after - account_before,
            'savings_account_balance': current_state.saving_balance,
            'savings_account_change': 0.0,  # Calculate based on rebalancing
            'total_account_balance': current_state.total_balance,
            'withdraw_amount': rebalancing_info.get('transfer_amount', 0.0),
            'withdraw_direction': rebalancing_info.get('transfer_direction', 'none'),
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
            'Profit_USD': account_after - account_before,
            'trading_fees_paid': trading_fees,
            'rebalancing_occurred': rebalancing_occurred
        }

        return result

    def _extract_prices(self, day_data: pd.Series, asset: str) -> Tuple[float, float]:
        """Extract open and close prices for an asset."""
        open_price = None
        close_price = None

        for col in day_data.index:
            if asset in col:
                if 'open' in col.lower() or 'Open' in col:
                    open_price = day_data[col]
                elif 'close' in col.lower() or 'Close' in col:
                    close_price = day_data[col]

        if open_price is None or close_price is None:
            # Fallback to standard naming
            open_price_col = f'{asset}_open_price'
            close_price_col = f'{asset}_close_price'
            open_price = day_data.get(open_price_col, day_data.get(f'Open Price_{asset}', 0.0))
            close_price = day_data.get(close_price_col, day_data.get(f'Close Price_{asset}', 0.0))

        if open_price is None or close_price is None:
            raise TradingSimulationError(f"Could not find {asset} price data")

        return float(open_price), float(close_price)

    def _calculate_trading_summary(self, results: pd.DataFrame) -> Dict[str, Any]:
        """Calculate trading simulation summary."""
        if len(results) == 0:
            return {"error": "No simulation data available"}

        initial_balance = results['trading_account_before'].iloc[0]
        final_balance = results['trading_account_after'].iloc[-1]
        total_fees = results['trading_fees_paid'].sum()
        total_rebalances = results['rebalancing_occurred'].sum()

        returns = results['Final_Portfolio_Return'].dropna()

        summary = {
            'initial_trading_balance': round(initial_balance, 2),
            'final_trading_balance': round(final_balance, 2),
            'total_profit_loss': round(final_balance - initial_balance, 2),
            'total_return_pct': round((final_balance - initial_balance) / initial_balance * 100, 2) if initial_balance > 0 else 0,
            'total_fees_paid': round(total_fees, 2),
            'total_rebalancing_events': int(total_rebalances),
            'average_daily_return': round(returns.mean(), 6) if len(returns) > 0 else 0,
            'volatility': round(returns.std(), 6) if len(returns) > 0 else 0,
            'max_daily_return': round(returns.max(), 6) if len(returns) > 0 else 0,
            'min_daily_return': round(returns.min(), 6) if len(returns) > 0 else 0,
            'total_trading_days': len(results)
        }

        return summary

    def _export_trading_results(
        self,
        results: pd.DataFrame,
        include_summary: bool = True
    ) -> Path:
        """Export trading simulation results."""

        output_dir = Path(tempfile.mkdtemp())
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        output_path = output_dir / f"trading_simulation_results_{timestamp}.xlsx"

        with pd.ExcelWriter(output_path, engine='openpyxl') as writer:
            # Main results
            results.to_excel(writer, sheet_name='Trading_Results', index=False)

            if include_summary:
                # Summary statistics
                summary = self._calculate_trading_summary(results)
                summary_df = pd.DataFrame(list(summary.items()), columns=['Metric', 'Value'])
                summary_df.to_excel(writer, sheet_name='Summary', index=False)

                # Strategy breakdown
                if 'Final_Decision' in results.columns:
                    strategy_counts = results['Final_Decision'].value_counts()
                    strategy_df = pd.DataFrame({
                        'Strategy': strategy_counts.index,
                        'Days': strategy_counts.values,
                        'Percentage': (strategy_counts.values / len(results) * 100).round(1)
                    })
                    strategy_df.to_excel(writer, sheet_name='Strategy_Breakdown', index=False)

                # Rebalancing events
                rebalancing_events = results[results['rebalancing_occurred'] == True]
                if len(rebalancing_events) > 0:
                    rebalancing_df = rebalancing_events[[
                        'Timestamp', 'trading_account_before', 'trading_account_after',
                        'savings_account_balance', 'withdraw_amount', 'withdraw_direction'
                    ]]
                    rebalancing_df.to_excel(writer, sheet_name='Rebalancing_Events', index=False)

        logger.info(f"Trading results exported to: {output_path}")
        return output_path

    def _config_to_dict(self, trading_config: Dict[str, any]) -> Dict[str, any]:
        """Convert configuration objects to dictionary for serialization."""
        return {
            'account': {
                'initial_trading_balance': trading_config['account'].initial_trading_balance,
                'initial_saving_balance': trading_config['account'].initial_saving_balance,
                'target_trading_balance': trading_config['account'].target_trading_balance
            },
            'assets': {
                'btc_decimal_places': trading_config['assets'].btc_decimal_places,
                'eth_decimal_places': trading_config['assets'].eth_decimal_places
            },
            'trading': {
                'trading_fee': trading_config['trading'].trading_fee,
                'leverage_scale': trading_config['trading'].leverage_scale
            },
            'rebalancing': {
                'frequency': trading_config['rebalancing'].frequency.value,
                'custom_days': trading_config['rebalancing'].custom_days
            }
        }


    async def simulate_trading_job(self, job, backtest_file_path: str, config: Dict[str, any]) -> str:
        """
        Job-based trading simulation that saves results to file.

        Args:
            job: Job instance for progress tracking
            backtest_file_path: Path to backtest results file
            config: Trading simulation configuration

        Returns:
            Path to the generated results file
        """
        from app.core.job_manager import job_manager

        try:
            logger.info(f"Starting job-based trading simulation for file: {backtest_file_path}")
            print(f"======= TRADING SIMULATION JOB STARTED =======", flush=True)
            print(f"Job ID: {job.job_id}", flush=True)
            print(f"Backtest file: {backtest_file_path}", flush=True)
            print(f"Config: {config}", flush=True)

            # Update progress
            job_manager.update_job_progress(job.job_id, 10, "Loading backtest results")

            # Run the simulation using the existing method
            results = await self.simulate_trading(backtest_file_path, config)

            # Update progress
            job_manager.update_job_progress(job.job_id, 90, "Saving results to Excel file")

            # Use the existing Excel export method but save to results directory
            import pandas as pd
            from datetime import datetime

            # Convert results to DataFrame for export
            results_df = pd.DataFrame(results['results'])

            # Export using existing method
            temp_excel_path = self._export_trading_results(results_df, include_summary=True)

            # Move to permanent results directory
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            results_filename = f"trading_simulation_{timestamp}.xlsx"
            results_dir = Path("D:/work/ct_mpt/trading_microservice/results")
            results_dir.mkdir(exist_ok=True)
            results_file_path = results_dir / results_filename

            # Copy from temp to results directory
            import shutil
            shutil.move(str(temp_excel_path), str(results_file_path))

            job_manager.update_job_progress(job.job_id, 100, "Trading simulation completed")

            print(f"======= TRADING SIMULATION JOB COMPLETED =======", flush=True)
            print(f"Results saved to: {results_file_path}", flush=True)

            return str(results_file_path)

        except Exception as e:
            error_msg = f"Trading simulation job failed: {str(e)}"
            logger.error(error_msg)
            print(f"======= TRADING SIMULATION JOB FAILED =======", flush=True)
            print(f"Error: {error_msg}", flush=True)
            raise