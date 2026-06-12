"""
Tests for Trading Simulation API
================================

Unit tests for trading simulation endpoints and services.
"""

import pytest
from fastapi.testclient import TestClient
from unittest.mock import Mock, patch
import pandas as pd
import numpy as np

from app.main import app
from app.services.trading_simulator import (
    TradingSimulator, TradingAccount, AssetPosition, RebalancingStrategy
)

client = TestClient(app)


class TestTradingAPI:
    """Test trading simulation API endpoints."""

    def test_example_request(self):
        """Test example request endpoint."""
        response = client.get("/api/v1/trading/example-request")
        assert response.status_code == 200
        data = response.json()
        assert "description" in data
        assert "form_data" in data
        assert "workflow" in data

    def test_configuration_templates(self):
        """Test configuration templates endpoint."""
        response = client.get("/api/v1/trading/configuration-templates")
        assert response.status_code == 200
        data = response.json()

        # Check all templates exist
        expected_templates = ["conservative", "moderate", "aggressive", "high_frequency"]
        for template in expected_templates:
            assert template in data
            assert "config" in data[template]
            assert "description" in data[template]

    @patch('app.services.data_processor.DataProcessor.validate_file')
    def test_validate_backtest_data(self, mock_validate):
        """Test backtest data validation."""
        mock_validate.return_value = {
            "status": "valid",
            "total_rows": 100,
            "columns": ["Timestamp", "BTC_Weight", "ETH_Weight", "Strategy"]
        }

        test_file = ("backtest_results.xlsx", b"test content", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")

        response = client.post(
            "/api/v1/trading/validate-backtest-data",
            files={"file": test_file}
        )

        assert response.status_code == 200
        data = response.json()
        assert data["is_valid"] is True


class TestTradingAccount:
    """Test trading account functionality."""

    def test_asset_position_creation(self):
        """Test asset position creation and rounding."""
        position = AssetPosition(
            quantity=1.23456789,
            asset_name="BTC",
            decimal_places=3
        )

        assert position.rounded_quantity == 1.235
        assert position.asset_name == "BTC"

    def test_asset_position_value_calculation(self):
        """Test asset position value calculation."""
        position = AssetPosition(
            quantity=0.5,
            asset_name="BTC",
            decimal_places=3
        )

        value = position.calculate_value(50000.0)
        assert value == 25000.0

    def test_trading_account_initialization(self):
        """Test trading account initialization."""
        from app.services.trading_simulator import (
            TradingAccountConfig, AssetConfig, TradingConfig
        )

        account_config = TradingAccountConfig(
            initial_trading_balance=500.0,
            initial_saving_balance=500.0,
            target_trading_balance=500.0
        )
        asset_config = AssetConfig()
        trading_config = TradingConfig()

        account = TradingAccount(account_config, asset_config, trading_config)
        state = account.current_state

        assert state.trading_balance == 500.0
        assert state.saving_balance == 500.0
        assert state.btc_position.quantity == 0.0
        assert state.eth_position.quantity == 0.0

    def test_asset_allocation_calculation(self):
        """Test asset allocation calculation."""
        from app.services.trading_simulator import (
            TradingAccountConfig, AssetConfig, TradingConfig
        )

        account_config = TradingAccountConfig(initial_trading_balance=1000.0)
        asset_config = AssetConfig()
        trading_config = TradingConfig(leverage_scale=2.0)

        account = TradingAccount(account_config, asset_config, trading_config)

        btc_qty, eth_qty = account.calculate_asset_allocation(
            risky_weight=0.8,
            btc_weight=0.6,
            eth_weight=0.4,
            btc_open_price=50000.0,
            eth_open_price=3000.0
        )

        # Expected: 1000 * 2.0 * 0.8 * 0.6 / 50000 = 0.0192 BTC
        # Expected: 1000 * 2.0 * 0.8 * 0.4 / 3000 = 0.213 ETH (rounded to 2 decimals)
        expected_btc = round(1000 * 2.0 * 0.8 * 0.6 / 50000, 3)
        expected_eth = round(1000 * 2.0 * 0.8 * 0.4 / 3000, 2)

        assert abs(btc_qty - expected_btc) < 1e-6
        assert abs(eth_qty - expected_eth) < 1e-6

    def test_execute_trade_with_fees(self):
        """Test trade execution with fees."""
        from app.services.trading_simulator import (
            TradingAccountConfig, AssetConfig, TradingConfig
        )

        account_config = TradingAccountConfig(initial_trading_balance=1000.0)
        asset_config = AssetConfig()
        trading_config = TradingConfig(trading_fee=0.001)  # 0.1% fee

        account = TradingAccount(account_config, asset_config, trading_config)

        initial_balance = account.current_state.trading_balance

        # Execute trade
        fees_paid = account.execute_trade(
            new_btc_quantity=0.02,
            new_eth_quantity=0.5,
            btc_price=50000.0,
            eth_price=3000.0,
            apply_fees=True
        )

        # Calculate expected fee: (0.02 * 50000 + 0.5 * 3000) * 0.001 = 2.5
        expected_fee = (0.02 * 50000 + 0.5 * 3000) * 0.001
        assert abs(fees_paid - expected_fee) < 1e-6

        # Check balance was reduced by fee
        new_balance = account.current_state.trading_balance
        assert abs(new_balance - (initial_balance - expected_fee)) < 1e-6

    def test_rebalance_accounts_transfer_to_savings(self):
        """Test account rebalancing - transfer to savings."""
        from app.services.trading_simulator import (
            TradingAccountConfig, AssetConfig, TradingConfig
        )

        account_config = TradingAccountConfig(
            initial_trading_balance=800.0,  # Above target
            initial_saving_balance=200.0,
            target_trading_balance=500.0
        )
        asset_config = AssetConfig()
        trading_config = TradingConfig()

        account = TradingAccount(account_config, asset_config, trading_config)

        rebalance_info = account.rebalance_accounts()

        assert rebalance_info["transfer_direction"] == "to_savings"
        assert rebalance_info["transfer_amount"] == 300.0  # 800 - 500
        assert account.current_state.trading_balance == 500.0
        assert account.current_state.saving_balance == 500.0  # 200 + 300

    def test_rebalance_accounts_transfer_from_savings(self):
        """Test account rebalancing - transfer from savings."""
        from app.services.trading_simulator import (
            TradingAccountConfig, AssetConfig, TradingConfig
        )

        account_config = TradingAccountConfig(
            initial_trading_balance=300.0,  # Below target
            initial_saving_balance=700.0,
            target_trading_balance=500.0
        )
        asset_config = AssetConfig()
        trading_config = TradingConfig()

        account = TradingAccount(account_config, asset_config, trading_config)

        rebalance_info = account.rebalance_accounts()

        assert rebalance_info["transfer_direction"] == "from_savings"
        assert rebalance_info["transfer_amount"] == 200.0  # 500 - 300
        assert account.current_state.trading_balance == 500.0
        assert account.current_state.saving_balance == 500.0  # 700 - 200


class TestRebalancingStrategy:
    """Test rebalancing strategy functionality."""

    def test_monthly_rebalancing_first_day(self):
        """Test monthly rebalancing on first day of month."""
        from app.services.trading_simulator import RebalancingConfig, RebalancingFrequency

        config = RebalancingConfig(frequency=RebalancingFrequency.MONTHLY)
        strategy = RebalancingStrategy(config)

        # Test first day of month
        test_date = pd.Timestamp('2023-03-01')
        assert strategy.should_rebalance(test_date) is True

        # Test middle of month
        test_date = pd.Timestamp('2023-03-15')
        assert strategy.should_rebalance(test_date) is False

    def test_custom_rebalancing(self):
        """Test custom rebalancing strategy."""
        from app.services.trading_simulator import RebalancingConfig, RebalancingFrequency

        config = RebalancingConfig(
            frequency=RebalancingFrequency.CUSTOM,
            custom_days=7
        )
        strategy = RebalancingStrategy(config)

        # First rebalance
        first_date = pd.Timestamp('2023-01-01')
        assert strategy.should_rebalance(first_date) is True
        strategy.record_rebalance(first_date)

        # Before 7 days
        test_date = pd.Timestamp('2023-01-05')
        assert strategy.should_rebalance(test_date) is False

        # After 7 days
        test_date = pd.Timestamp('2023-01-08')
        assert strategy.should_rebalance(test_date) is True


class TestTradingSimulator:
    """Test trading simulator service."""

    def test_extract_prices_standard_columns(self):
        """Test price extraction with standard column names."""
        simulator = TradingSimulator()

        day_data = pd.Series({
            'BTC_open_price': 50000.0,
            'BTC_close_price': 51000.0
        })

        open_price, close_price = simulator._extract_prices(day_data, 'BTC')
        assert open_price == 50000.0
        assert close_price == 51000.0

    def test_extract_prices_flexible_columns(self):
        """Test price extraction with flexible column naming."""
        simulator = TradingSimulator()

        day_data = pd.Series({
            'BTC_Open': 50000.0,
            'BTC_Close': 51000.0
        })

        open_price, close_price = simulator._extract_prices(day_data, 'BTC')
        assert open_price == 50000.0
        assert close_price == 51000.0

    def test_create_trading_config(self):
        """Test trading configuration creation."""
        simulator = TradingSimulator()

        config_input = {
            'total_capital': 2000.0,
            'trading_portion': 0.6,
            'trading_fee': 0.002,
            'leverage_scale': 2.0,
            'rebalancing_frequency': 'quarterly',
            'btc_decimal_places': 4,
            'eth_decimal_places': 3
        }

        trading_config = simulator._create_trading_config(config_input)

        assert trading_config['account'].initial_trading_balance == 1200.0  # 2000 * 0.6
        assert trading_config['account'].initial_saving_balance == 800.0    # 2000 * 0.4
        assert trading_config['trading'].trading_fee == 0.002
        assert trading_config['assets'].btc_decimal_places == 4


if __name__ == "__main__":
    pytest.main([__file__])