"""
Trading account management for actual trading backtest.

Implements account balance tracking, asset allocation, and rebalancing logic.
"""

from dataclasses import dataclass
from typing import Dict, Tuple
import numpy as np
from .config import TradingAccountConfig, AssetConfig, TradingConfig


@dataclass
class AssetPosition:
    """Represents a position in a specific asset."""
    
    quantity: float
    asset_name: str
    decimal_places: int
    
    def __post_init__(self):
        """Round quantity to appropriate decimal places."""
        self.quantity = round(self.quantity, self.decimal_places)
    
    @property
    def rounded_quantity(self) -> float:
        """Get quantity rounded to asset's decimal places."""
        return round(self.quantity, self.decimal_places)
    
    def calculate_value(self, price: float) -> float:
        """Calculate current value of position."""
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
        """Calculate total account balance."""
        return self.trading_balance + self.saving_balance
    
    def calculate_portfolio_value(self, btc_price: float, eth_price: float) -> float:
        """Calculate total portfolio value including positions."""
        btc_value = self.btc_position.calculate_value(btc_price)
        eth_value = self.eth_position.calculate_value(eth_price)
        return self.trading_balance + btc_value + eth_value


class TradingAccount:
    """
    Manages trading and saving accounts with realistic constraints.
    
    Follows Single Responsibility Principle - only handles account operations.
    """
    
    def __init__(
        self,
        account_config: TradingAccountConfig,
        asset_config: AssetConfig,
        trading_config: TradingConfig
    ):
        """
        Initialize trading account.
        
        Args:
            account_config: Account configuration
            asset_config: Asset configuration  
            trading_config: Trading configuration
        """
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
        """Get current account state."""
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
        """
        Calculate actual asset quantities based on portfolio weights.
        
        Args:
            risky_weight: Portfolio risky weight (leverage factor)
            btc_weight: BTC weight in risky portfolio
            eth_weight: ETH weight in risky portfolio
            btc_open_price: BTC opening price
            eth_open_price: ETH opening price
            
        Returns:
            Tuple of (btc_quantity, eth_quantity) rounded to appropriate decimals
        """
        # Calculate scaled allocation
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
        """
        Execute trade and update positions.
        
        Args:
            new_btc_quantity: Target BTC quantity
            new_eth_quantity: Target ETH quantity
            btc_price: Current BTC price
            eth_price: Current ETH price
            apply_fees: Whether to apply trading fees
            
        Returns:
            Total trading fee paid
        """
        # Check if position changes (triggers fees)
        btc_changed = abs(new_btc_quantity - self._btc_position.quantity) > 1e-8
        eth_changed = abs(new_eth_quantity - self._eth_position.quantity) > 1e-8
        
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
        """
        Rebalance trading and saving accounts to target allocation.
        
        Returns:
            Dictionary with rebalancing details
        """
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
    
    def get_account_summary(self, btc_price: float, eth_price: float) -> Dict[str, float]:
        """
        Get comprehensive account summary.
        
        Args:
            btc_price: Current BTC price
            eth_price: Current ETH price
            
        Returns:
            Dictionary with account details
        """
        state = self.current_state
        
        return {
            "trading_balance": state.trading_balance,
            "saving_balance": state.saving_balance,
            "total_cash": state.total_balance,
            "btc_quantity": state.btc_position.rounded_quantity,
            "eth_quantity": state.eth_position.rounded_quantity,
            "btc_value": state.btc_position.calculate_value(btc_price),
            "eth_value": state.eth_position.calculate_value(eth_price),
            "total_portfolio_value": state.calculate_portfolio_value(btc_price, eth_price)
        }