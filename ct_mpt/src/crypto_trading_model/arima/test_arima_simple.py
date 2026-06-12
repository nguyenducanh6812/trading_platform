"""
Simple ARIMA Test Script - CORRECTED VERSION
===========================================

File: crypto_trading_model/arima/test_arima_simple.py

Quick test to verify ARIMA implementation works with your backtest results.

Usage:
    cd crypto_trading_model/arima
    python test_arima_simple.py
"""

import sys
from pathlib import Path
import pandas as pd
import numpy as np

# Add project root to path
project_root = Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(project_root))

from crypto_trading_model.custom_logging.logger import CustomLogger
# ✅ CORRECTED: Import from the correct file
from crypto_trading_model.arima.backtest_arima_analyzer import BacktestArimaAnalyzer

def test_with_your_backtest_results():
    """Test ARIMA with your actual backtest results."""
    
    logger = CustomLogger('ArimaTest')
    logger.info("Testing ARIMA with your backtest results...")
    
    # Paths to check for your backtest results
    possible_files = [
        "backtest_results_neutral.xlsx",
        "../backtest_results_neutral.xlsx", 
        "../../backtest_results_neutral.xlsx",
        "backtest_results/backtest_results_neutral.xlsx",
        "../backtest_results/backtest_results_neutral.xlsx"
    ]
    
    analyzer = BacktestArimaAnalyzer(logger)
    
    # Try to find your actual backtest results
    for file_path in possible_files:
        if Path(file_path).exists():
            try:
                logger.info(f"Found backtest results: {file_path}")
                
                # Analyze the results
                results = analyzer.analyze_backtest_results(file_path, "neutral")
                
                # Print key findings
                print("\n" + "="*60)
                print("🎯 ARIMA ANALYSIS RESULTS")
                print("="*60)
                print(f"File: {file_path}")
                print(f"Forecast Ready: {'✅ YES' if results['forecast_ready'] else '❌ NO'}")
                
                if results['forecast_ready']:
                    arima_summary = results['arima_analysis']['analysis_summary']
                    print(f"ARIMA Model: {arima_summary['optimal_order']}")
                    print(f"AIC: {arima_summary['best_aic']:.4f}")
                    print(f"Significant Coefficients: {arima_summary['num_significant_coefficients']}")
                    print("\n🎯 YOUR PORTFOLIO RETURNS ARE PREDICTABLE!")
                    print("Next: Implement Model Formula Step 5 (forecasting)")
                else:
                    print("\n📊 Portfolio returns follow random walk")
                    print("This suggests your strategy is already market-efficient")
                
                # Portfolio stats
                stats = results['portfolio_statistics']
                print(f"\n📈 Portfolio Performance:")
                print(f"  Sharpe Ratio: {stats.get('sharpe_ratio', 0):.4f}")
                print(f"  Win Rate: {stats.get('win_rate', 0):.1%}")
                print(f"  Daily Return: {stats.get('mean_daily_return', 0):.6f}")
                print("="*60)
                
                return results
                
            except Exception as e:
                logger.error(f"Failed to analyze {file_path}: {e}")
                continue
    
    # If no actual file found, create sample data
    logger.info("📊 No backtest results found. Creating sample data for testing...")
    return test_with_sample_data(analyzer, logger)

def test_with_sample_data(analyzer, logger):
    """Test ARIMA with sample portfolio returns data."""
    
    # Create sample portfolio returns (with some autocorrelation for ARIMA)
    np.random.seed(42)
    n_days = 100
    dates = pd.date_range('2020-01-01', periods=n_days, freq='D')
    
    # Generate portfolio returns with autocorrelation
    returns = [0.001]  # Starting return
    for i in range(1, n_days):
        # Add autocorrelation + random component
        autocorr = 0.3 * returns[-1]  # 30% autocorrelation
        random_shock = np.random.normal(0.0005, 0.015)
        new_return = autocorr + random_shock
        returns.append(new_return)
    
    # Create sample backtest DataFrame
    sample_data = pd.DataFrame({
        'Timestamp': dates,
        'Daily_Return': returns,  # Phase 2 portfolio returns
        'Portfolio_Value': np.cumprod(1 + np.array(returns)),
        'Strategy': np.random.choice(['Long', 'Short', 'Market_neutral'], n_days),
        'BTC_Weight': np.random.uniform(0, 1, n_days),
        'ETH_Weight': np.random.uniform(0, 1, n_days)
    })
    
    # Save sample data
    sample_file = "sample_portfolio_returns.xlsx"
    sample_data.to_excel(sample_file, sheet_name='Backtest Results', index=False)
    logger.info(f"Created sample data: {sample_file}")
    
    # Analyze sample data
    try:
        results = analyzer.analyze_backtest_results(sample_file, "neutral")
        
        print("\n" + "="*60)
        print("🧪 SAMPLE DATA ARIMA TEST")
        print("="*60)
        print(f"Forecast Ready: {'✅ YES' if results['forecast_ready'] else '❌ NO'}")
        
        if results['forecast_ready']:
            arima_summary = results['arima_analysis']['analysis_summary']
            print(f"ARIMA Model: {arima_summary['optimal_order']}")
            print("✅ Sample data shows ARIMA implementation works!")
        else:
            print("📊 Sample data shows no strong patterns")
        
        print("="*60)
        return results
        
    except Exception as e:
        logger.error(f"Sample data test failed: {e}")
        raise

def main():
    """Main test function."""
    
    print("🚀 SIMPLE ARIMA TEST - CORRECTED VERSION")
    print("📦 Requirements: pip install statsmodels scipy")
    print("="*50)
    
    try:
        # Test with your actual data or sample data
        results = test_with_your_backtest_results()
        
        print("\n✅ ARIMA IMPLEMENTATION TEST COMPLETED!")
        print("\n📋 NEXT STEPS:")
        print("1. If forecast_ready=YES: Implement Model Formula Step 5")
        print("2. If forecast_ready=NO: Focus on strategy optimization")
        print("3. Run this test on all your risk profiles")
        print("4. Integrate ARIMA with your backtester.py")
        
    except Exception as e:
        print(f"\n❌ Test failed: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()