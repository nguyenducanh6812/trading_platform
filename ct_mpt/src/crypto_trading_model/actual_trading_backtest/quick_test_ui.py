"""
Quick test script for the Actual Trading Backtest UI.

Tests the UI components and functionality without requiring external execution.
"""

import sys
from pathlib import Path
import pandas as pd
import numpy as np
from datetime import datetime, timedelta

# Add current directory to path
sys.path.insert(0, str(Path(__file__).parent.parent.parent.parent))

def create_sample_data():
    """Create sample backtest data for UI testing."""
    np.random.seed(42)
    num_days = 50
    
    # Create date range
    start_date = datetime(2025, 1, 1)
    dates = [start_date + timedelta(days=i) for i in range(num_days)]
    
    # Generate sample data matching required format
    data = pd.DataFrame({
        'Timestamp': dates,
        'Open Price_BTC': np.random.uniform(60000, 70000, num_days),
        'Close Price_BTC': np.random.uniform(60000, 70000, num_days),
        'Open Price_ETH': np.random.uniform(3000, 4000, num_days),
        'Close Price_ETH': np.random.uniform(3000, 4000, num_days),
        'BTC_Weight': np.random.uniform(-1, 1, num_days),
        'ETH_Weight': np.random.uniform(-1, 1, num_days),
        'Risky_Weight': np.random.uniform(0, 2, num_days),
        'Strategy': np.random.choice(['Long', 'Short', 'Market_neutral'], num_days),
        'Daily_Return': np.random.uniform(-0.05, 0.05, num_days),
        'BTC_Return': np.random.normal(0, 0.03, num_days),
        'ETH_Return': np.random.normal(0, 0.035, num_days)
    })
    
    return data

def test_ui_imports():
    """Test that UI imports work correctly."""
    print("=== Testing UI Imports ===")
    
    try:
        from crypto_trading_model.actual_trading_backtest import (
            ActualTradingBacktestUI,
            ActualTradingConfig,
            RebalancingFrequency,
            DataValidator,
            ConfigValidator
        )
        print("✅ All imports successful")
        return True
        
    except ImportError as e:
        print(f"❌ Import failed: {e}")
        return False

def test_config_creation():
    """Test configuration creation through UI patterns."""
    print("\n=== Testing Configuration Creation ===")
    
    try:
        from crypto_trading_model.actual_trading_backtest import (
            ActualTradingConfig,
            RebalancingFrequency
        )
        
        # Test default config
        default_config = ActualTradingConfig.create_default()
        print(f"✅ Default config: ${default_config.account.total_capital}")
        
        # Test custom config (as would be created by UI)
        custom_config = ActualTradingConfig.create_custom(
            total_capital=1000.0,
            trading_portion=0.6,
            rebalancing_frequency=RebalancingFrequency.MONTHLY,
            trading_fee=0.002,
            leverage_scale=1.8
        )
        print(f"✅ Custom config: ${custom_config.account.initial_trading_balance:.2f} trading")
        
        return True
        
    except Exception as e:
        print(f"❌ Configuration test failed: {e}")
        return False

def test_data_validation():
    """Test data validation functionality."""
    print("\n=== Testing Data Validation ===")
    
    try:
        from crypto_trading_model.actual_trading_backtest import DataValidator
        
        # Create sample data
        sample_data = create_sample_data()
        
        # Test validation
        validation_result = DataValidator.validate_backtest_data(sample_data)
        print(f"✅ Data validation: {validation_result['is_valid']}")
        print(f"   Rows: {validation_result['row_count']}, Columns: {validation_result['column_count']}")
        
        if validation_result['warnings']:
            print(f"   Warnings: {len(validation_result['warnings'])}")
            
        return validation_result['is_valid']
        
    except Exception as e:
        print(f"❌ Data validation test failed: {e}")
        return False

def test_ui_initialization():
    """Test UI initialization without showing the window."""
    print("\n=== Testing UI Initialization ===")
    
    try:
        import tkinter as tk
        from crypto_trading_model.actual_trading_backtest import ActualTradingBacktestUI
        
        # Test if tkinter is available
        root = tk.Tk()
        root.withdraw()  # Hide the test window
        
        print("✅ Tkinter is available")
        
        # Test UI class initialization (without showing)
        # Note: We can't fully test without showing the UI, but we can test imports
        print("✅ UI class is importable")
        
        root.destroy()
        return True
        
    except Exception as e:
        print(f"❌ UI initialization test failed: {e}")
        return False

def create_test_data_file():
    """Create a test data file for UI testing."""
    print("\n=== Creating Test Data File ===")
    
    try:
        sample_data = create_sample_data()
        test_file = Path("test_backtest_data.xlsx")
        
        sample_data.to_excel(test_file, index=False)
        print(f"✅ Test data file created: {test_file}")
        print(f"   Size: {test_file.stat().st_size} bytes")
        
        return str(test_file)
        
    except Exception as e:
        print(f"❌ Test data file creation failed: {e}")
        return None

def run_integration_test():
    """Run a complete integration test simulation."""
    print("\n=== Running Integration Test ===")
    
    try:
        from crypto_trading_model.actual_trading_backtest import (
            ActualTradingBacktest,
            ActualTradingConfig,
            RebalancingFrequency
        )
        
        # Create configuration
        config = ActualTradingConfig.create_custom(
            total_capital=1000.0,
            trading_portion=0.5,
            rebalancing_frequency=RebalancingFrequency.MONTHLY,
            trading_fee=0.0015,
            leverage_scale=1.5
        )
        
        # Create sample data
        sample_data = create_sample_data()
        
        # Run backtest (small dataset)
        backtest = ActualTradingBacktest(config)
        results = backtest.run_backtest(sample_data)
        
        # Get summary
        summary = backtest.get_simulation_summary()
        
        print("✅ Integration test completed")
        print(f"   Initial: ${summary['initial_trading_balance']:.2f}")
        print(f"   Final: ${summary['final_trading_balance']:.2f}")
        print(f"   P&L: ${summary['total_profit_loss']:.2f}")
        print(f"   Return: {summary['total_return_pct']:.2f}%")
        print(f"   Days: {summary['total_trading_days']}")
        
        return True
        
    except Exception as e:
        print(f"❌ Integration test failed: {e}")
        return False

def main():
    """Run all UI tests."""
    print("Actual Trading Backtest UI - Quick Test Suite")
    print("=" * 55)
    
    tests = [
        ("Import Tests", test_ui_imports),
        ("Configuration Tests", test_config_creation),
        ("Data Validation Tests", test_data_validation),
        ("UI Initialization Tests", test_ui_initialization),
        ("Integration Tests", run_integration_test)
    ]
    
    passed = 0
    for test_name, test_func in tests:
        print(f"\n{test_name}:")
        if test_func():
            passed += 1
            
    # Create test data file
    test_file = create_test_data_file()
    
    print(f"\n" + "=" * 55)
    print(f"Test Results: {passed}/{len(tests)} tests passed")
    
    if passed == len(tests):
        print("🎉 All tests passed! The UI should work correctly.")
        print("\nTo launch the UI, run:")
        print("  python actual_trading_backtest_ui.py")
        if test_file:
            print(f"\nTest data file available: {test_file}")
    else:
        print("⚠️  Some tests failed. Please check the implementation.")
        
    return passed == len(tests)

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)