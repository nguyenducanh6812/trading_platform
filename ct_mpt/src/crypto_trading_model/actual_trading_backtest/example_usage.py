"""
Example usage of the Actual Trading Backtest module.

Demonstrates how to use the module with sample data and various configurations.
"""

import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from pathlib import Path

from . import (
    ActualTradingBacktest,
    ActualTradingConfig,
    RebalancingFrequency,
    DataValidator,
    ConfigValidator
)


def create_sample_backtest_data(num_days: int = 100) -> pd.DataFrame:
    """
    Create sample backtest data for testing.
    
    Args:
        num_days: Number of days to generate
        
    Returns:
        DataFrame with sample backtest data
    """
    # Create date range
    start_date = datetime(2025, 1, 1)
    dates = [start_date + timedelta(days=i) for i in range(num_days)]
    
    # Generate sample price data
    np.random.seed(42)  # For reproducible results
    
    # BTC prices (starting around $65,000)
    btc_base_price = 65000
    btc_returns = np.random.normal(0.001, 0.03, num_days)  # 0.1% daily return, 3% volatility
    btc_prices = [btc_base_price]
    
    for ret in btc_returns[1:]:
        btc_prices.append(btc_prices[-1] * (1 + ret))
    
    # ETH prices (starting around $3,500) 
    eth_base_price = 3500
    eth_returns = np.random.normal(0.0015, 0.035, num_days)  # 0.15% daily return, 3.5% volatility
    eth_prices = [eth_base_price]
    
    for ret in eth_returns[1:]:
        eth_prices.append(eth_prices[-1] * (1 + ret))
    
    # Generate portfolio weights and strategies
    strategies = np.random.choice(['Long', 'Short', 'Market_neutral'], num_days, p=[0.4, 0.3, 0.3])
    
    # Generate weights based on strategy
    btc_weights = []
    eth_weights = []
    risky_weights = []
    
    for strategy in strategies:
        if strategy == 'Long':
            btc_weight = np.random.uniform(-0.5, 1.5)
            eth_weight = 1.0 - btc_weight
            risky_weight = np.random.uniform(0.8, 1.5)
        elif strategy == 'Short':
            btc_weight = np.random.uniform(-1.5, 0.5)
            eth_weight = -1.0 - btc_weight
            risky_weight = np.random.uniform(0.8, 1.5)
        else:  # Market_neutral
            btc_weight = 0.0
            eth_weight = 0.0
            risky_weight = 0.0
        
        btc_weights.append(btc_weight)
        eth_weights.append(eth_weight)
        risky_weights.append(risky_weight)
    
    # Calculate daily returns
    btc_daily_returns = [(btc_prices[i] - btc_prices[i-1]) / btc_prices[i-1] if i > 0 else 0.0 
                        for i in range(len(btc_prices))]
    eth_daily_returns = [(eth_prices[i] - eth_prices[i-1]) / eth_prices[i-1] if i > 0 else 0.0
                        for i in range(len(eth_prices))]
    
    # Calculate portfolio daily returns (simplified)
    portfolio_returns = []
    for i in range(num_days):
        if strategies[i] == 'Market_neutral':
            portfolio_return = 0.001  # Risk-free rate
        else:
            portfolio_return = (btc_weights[i] * btc_daily_returns[i] + 
                              eth_weights[i] * eth_daily_returns[i]) * risky_weights[i]
        portfolio_returns.append(portfolio_return)
    
    # Create DataFrame
    data = pd.DataFrame({
        'Timestamp': dates,
        'Open Price_BTC': btc_prices,
        'Close Price_BTC': [p * (1 + btc_daily_returns[i]) for i, p in enumerate(btc_prices)],
        'Open Price_ETH': eth_prices,
        'Close Price_ETH': [p * (1 + eth_daily_returns[i]) for i, p in enumerate(eth_prices)],
        'BTC_Weight': btc_weights,
        'ETH_Weight': eth_weights,
        'Risky_Weight': risky_weights,
        'Strategy': strategies,
        'Daily_Return': portfolio_returns,
        'BTC_Return': btc_daily_returns,
        'ETH_Return': eth_daily_returns
    })
    
    return data


def example_basic_usage():
    """Demonstrate basic usage of the ActualTradingBacktest."""
    print("=== Basic Usage Example ===")
    
    # Create sample data
    sample_data = create_sample_backtest_data(30)  # 30 days
    print(f"Created sample data with {len(sample_data)} days")
    
    # Create default configuration
    config = ActualTradingConfig.create_default()
    print(f"Created default configuration: {config.account.total_capital} total capital")
    
    # Validate configuration
    try:
        ConfigValidator.validate_trading_config(config)
        print("✓ Configuration validation passed")
    except Exception as e:
        print(f"✗ Configuration validation failed: {e}")
        return
    
    # Validate input data
    try:
        validation_result = DataValidator.validate_backtest_data(sample_data)
        print(f"✓ Data validation passed: {validation_result['row_count']} rows, {validation_result['column_count']} columns")
        if validation_result['warnings']:
            print(f"  Warnings: {validation_result['warnings']}")
    except Exception as e:
        print(f"✗ Data validation failed: {e}")
        return
    
    # Run backtest
    backtest = ActualTradingBacktest(config)
    
    try:
        results = backtest.run_backtest(sample_data)
        print(f"✓ Backtest completed successfully with {len(results)} result rows")
        
        # Get summary
        summary = backtest.get_simulation_summary()
        print(f"  Initial balance: ${summary['initial_trading_balance']:.2f}")
        print(f"  Final balance: ${summary['final_trading_balance']:.2f}")
        print(f"  Total P&L: ${summary['total_profit_loss']:.2f}")
        print(f"  Total return: {summary['total_return_pct']:.2f}%")
        print(f"  Total fees: ${summary['total_fees_paid']:.2f}")
        print(f"  Rebalancing events: {summary['total_rebalancing_events']}")
        
    except Exception as e:
        print(f"✗ Backtest failed: {e}")


def example_custom_configuration():
    """Demonstrate usage with custom configuration."""
    print("\n=== Custom Configuration Example ===")
    
    # Create custom configuration
    config = ActualTradingConfig.create_custom(
        total_capital=2000.0,
        trading_portion=0.6,  # 60% for trading, 40% for savings
        rebalancing_frequency=RebalancingFrequency.QUARTERLY,
        trading_fee=0.002,  # 0.2% trading fee
        leverage_scale=2.0   # 2x leverage
    )
    
    print(f"Custom config - Trading: ${config.account.initial_trading_balance:.2f}, "
          f"Savings: ${config.account.initial_saving_balance:.2f}")
    print(f"Rebalancing: {config.rebalancing.frequency.value}")
    print(f"Trading fee: {config.trading.trading_fee:.3f}, Leverage: {config.trading.leverage_scale}x")
    
    # Create longer sample data
    sample_data = create_sample_backtest_data(180)  # 6 months
    
    # Run backtest with custom config
    backtest = ActualTradingBacktest(config)
    
    try:
        results = backtest.run_backtest(sample_data)
        summary = backtest.get_simulation_summary()
        
        print(f"✓ Custom backtest completed")
        print(f"  Final balance: ${summary['final_trading_balance']:.2f}")
        print(f"  Total return: {summary['total_return_pct']:.2f}%")
        print(f"  Total fees: ${summary['total_fees_paid']:.2f}")
        print(f"  Rebalancing events: {summary['total_rebalancing_events']}")
        
    except Exception as e:
        print(f"✗ Custom backtest failed: {e}")


def example_export_results():
    """Demonstrate exporting results to Excel."""
    print("\n=== Export Results Example ===")
    
    # Create configuration and data
    config = ActualTradingConfig.create_default()
    sample_data = create_sample_backtest_data(50)
    
    # Run backtest
    backtest = ActualTradingBacktest(config)
    results = backtest.run_backtest(sample_data)
    
    # Export results
    output_path = Path("actual_trading_backtest_example.xlsx")
    
    try:
        backtest.export_results(str(output_path), include_summary=True)
        print(f"✓ Results exported to {output_path}")
        print(f"  File size: {output_path.stat().st_size} bytes")
        
    except Exception as e:
        print(f"✗ Export failed: {e}")


def example_validation_failures():
    """Demonstrate validation error handling."""
    print("\n=== Validation Error Handling Example ===")
    
    # Test invalid configuration
    try:
        invalid_config = ActualTradingConfig.create_custom(
            total_capital=-1000,  # Invalid negative capital
            trading_portion=1.5,   # Invalid portion > 1
            trading_fee=2.0        # Invalid fee > 1
        )
        print("✗ Should have failed validation")
    except Exception as e:
        print(f"✓ Correctly caught configuration error: {e}")
    
    # Test invalid data
    try:
        invalid_data = pd.DataFrame({
            'BTC_Weight': [1, 2, 3],
            'Strategy': ['Invalid', 'Also Invalid', 'Bad']
            # Missing required columns
        })
        
        DataValidator.validate_backtest_data(invalid_data)
        print("✗ Should have failed data validation")
    except Exception as e:
        print(f"✓ Correctly caught data validation error: {e}")


def main():
    """Run all examples."""
    print("Actual Trading Backtest Module - Example Usage")
    print("=" * 50)
    
    example_basic_usage()
    example_custom_configuration()
    example_export_results()
    example_validation_failures()
    
    print("\n" + "=" * 50)
    print("All examples completed!")


if __name__ == "__main__":
    main()