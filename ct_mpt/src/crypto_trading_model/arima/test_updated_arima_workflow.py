"""
Test Updated ARIMA Workflow
===========================

File: crypto_trading_model/arima/test_updated_arima_workflow.py

This script demonstrates the complete updated ARIMA workflow that leverages
your existing work and implements your requested monitoring format.

Workflow:
1. Use existing ARIMA analysis → Export master data JSON
2. Use ARIMA applicator → Apply forecasting using JSON  
3. Generate monitoring reports with your exact format

Usage:
    cd crypto_trading_model/arima
    python test_updated_arima_workflow.py
"""

import sys
from pathlib import Path
import pandas as pd
import numpy as np
import json

# Add project root to path
project_root = Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(project_root))

from crypto_trading_model.custom_logging.logger import CustomLogger
from crypto_trading_model.config.config_manager import ConfigManager
from crypto_trading_model.reporting.updated_arima_monitoring import UpdatedArimaMonitoring

def test_complete_updated_workflow():
    """Test the complete updated ARIMA workflow."""
    
    logger = CustomLogger('UpdatedArimaWorkflowTest')
    logger.info("🚀 UPDATED ARIMA WORKFLOW TEST")
    logger.info("=" * 60)
    logger.info("Testing the improved architecture that leverages your existing work:")
    logger.info("1. ✅ Existing ARIMA analysis (BacktestArimaAnalyzer)")
    logger.info("2. 💾 Master data export to JSON")
    logger.info("3. 🎯 ARIMA application using JSON")
    logger.info("4. 📊 Monitoring report generation")
    print()
    
    try:
        # Initialize configuration
        config_manager = None
        try:
            config_manager = ConfigManager(logger)
            logger.info("✅ Configuration manager loaded")
        except:
            logger.warning("⚠️ Could not load config manager - using defaults")
        
        # Initialize updated monitoring system
        monitor = UpdatedArimaMonitoring(logger, config_manager)
        
        # Look for your existing backtest results
        backtest_files = [
            "backtest_results_neutral.xlsx",
            "../backtest_results_neutral.xlsx", 
            "../../backtest_results_neutral.xlsx",
            "backtest_results/backtest_results_neutral.xlsx"
        ]
        
        backtest_file = None
        for file_path in backtest_files:
            if Path(file_path).exists():
                backtest_file = file_path
                logger.info(f"✅ Found backtest file: {backtest_file}")
                break
        
        if backtest_file:
            # Test with your actual data
            test_with_real_backtest_data(monitor, backtest_file, logger)
        else:
            # Test with synthetic data
            logger.info("📊 No existing backtest file found")
            logger.info("🧪 Creating synthetic data to demonstrate workflow...")
            test_with_synthetic_backtest_data(monitor, logger)
        
        logger.info("\n✅ UPDATED ARIMA WORKFLOW TEST COMPLETED!")
        
    except Exception as e:
        logger.error(f"❌ Test failed: {e}")
        import traceback
        traceback.print_exc()

def test_with_real_backtest_data(monitor, backtest_file: str, logger):
    """Test with your actual backtest data."""
    
    logger.info(f"\n📈 TESTING WITH YOUR ACTUAL DATA")
    logger.info(f"File: {backtest_file}")
    logger.info("-" * 50)
    
    # Test different risk profiles
    risk_profiles = ["neutral"]  # Start with one for testing
    
    for risk_profile in risk_profiles:
        try:
            logger.info(f"\n🎯 Processing {risk_profile.upper()} Risk Profile")
            logger.info("=" * 40)
            
            # Run complete workflow
            result = monitor.generate_complete_monitoring_report(
                backtest_file=backtest_file,
                risk_profile=risk_profile,
                output_path=f"updated_arima_monitoring_{risk_profile}.xlsx",
                forecast_steps=5,
                export_format='xlsx'
            )
            
            # Display results
            display_workflow_results(result, risk_profile, logger)
            
            # Test individual components
            if result['status'] == 'success':
                test_individual_components(result, logger)
            
        except Exception as e:
            logger.error(f"❌ Failed for {risk_profile} profile: {e}")

def test_with_synthetic_backtest_data(monitor, logger):
    """Test with synthetic backtest data to demonstrate workflow."""
    
    logger.info(f"\n🧪 CREATING SYNTHETIC BACKTEST DATA")
    logger.info("-" * 50)
    
    # Create realistic synthetic portfolio returns
    np.random.seed(42)
    n_periods = 150  # 5 months of daily data
    dates = pd.date_range(start='2023-01-01', periods=n_periods, freq='D')
    
    # Generate returns with ARIMA structure for realistic testing
    returns = []
    errors = [0.0, 0.0]  # Initialize error terms
    
    # Realistic parameters for crypto portfolio
    ar1, ar2 = 0.12, -0.05  # Autoregressive components
    ma1 = 0.20              # Moving average component
    mean_return = 0.0008    # 0.08% daily mean return (good performance)
    noise_std = 0.022       # 2.2% daily volatility (crypto-like)
    
    logger.info(f"📊 Synthetic ARIMA parameters:")
    logger.info(f"   AR components: [{ar1}, {ar2}]")
    logger.info(f"   MA component: [{ma1}]")
    logger.info(f"   Mean return: {mean_return:.4f} ({mean_return*252:.1%} annual)")
    logger.info(f"   Daily volatility: {noise_std:.1%}")
    
    for i in range(n_periods):
        # Generate random shock
        noise = np.random.normal(0, noise_std)
        
        # Calculate return using ARMA(2,1) structure
        if i == 0:
            ret = mean_return + noise
        elif i == 1:
            ret = mean_return + ar1 * (returns[i-1] - mean_return) + ma1 * errors[i-1] + noise
        else:
            ret = (mean_return + 
                   ar1 * (returns[i-1] - mean_return) + 
                   ar2 * (returns[i-2] - mean_return) + 
                   ma1 * errors[i-1] + 
                   noise)
        
        returns.append(ret)
        errors.append(noise)
    
    # Create complete synthetic backtest data
    synthetic_data = pd.DataFrame({
        'Timestamp': dates,
        'Daily_Return': returns,
        'Portfolio_Value': np.cumprod(1 + np.array(returns)),
        'Strategy': np.random.choice(['Long', 'Short', 'Market_neutral'], n_periods, p=[0.5, 0.3, 0.2]),
        'BTC_Weight': np.random.uniform(0.2, 0.8, n_periods),
        'ETH_Weight': 1 - np.random.uniform(0.2, 0.8, n_periods),
        'Sharpe_Ratio': np.random.uniform(0.5, 2.0, n_periods),
        'Expected_Port_Return': np.array(returns) + np.random.normal(0, 0.001, n_periods)  # Add prediction noise
    })
    
    synthetic_file = "synthetic_portfolio_updated_test.xlsx"
    synthetic_data.to_excel(synthetic_file, sheet_name='Backtest Results', index=False)
    
    logger.info(f"✅ Created synthetic backtest data: {synthetic_file}")
    logger.info(f"   Periods: {n_periods}")
    logger.info(f"   Actual mean return: {np.mean(returns):.6f}")
    logger.info(f"   Actual volatility: {np.std(returns):.6f}")
    logger.info(f"   Final portfolio value: {synthetic_data['Portfolio_Value'].iloc[-1]:.2f}")
    
    # Test workflow with synthetic data
    try:
        logger.info(f"\n🚀 RUNNING WORKFLOW ON SYNTHETIC DATA")
        logger.info("=" * 40)
        
        result = monitor.generate_complete_monitoring_report(
            backtest_file=synthetic_file,
            risk_profile="neutral",
            output_path="synthetic_updated_monitoring.xlsx",
            forecast_steps=7,
            export_format='xlsx'
        )
        
        display_workflow_results(result, "synthetic", logger)
        
        if result['status'] == 'success':
            test_individual_components(result, logger)
            demonstrate_json_master_data(result, logger)
        
        # Also test CSV export
        logger.info(f"\n📁 TESTING CSV EXPORT")
        logger.info("-" * 30)
        
        csv_result = monitor.generate_complete_monitoring_report(
            backtest_file=synthetic_file,
            risk_profile="neutral",
            output_path="synthetic_updated_monitoring.csv",
            forecast_steps=5,
            export_format='csv'
        )
        
        if csv_result['status'] == 'success':
            logger.info("✅ CSV export test successful")
        
    except Exception as e:
        logger.error(f"❌ Synthetic data test failed: {e}")
        raise

def display_workflow_results(result: dict, profile: str, logger):
    """Display comprehensive workflow results."""
    
    logger.info(f"\n📊 WORKFLOW RESULTS - {profile.upper()}")
    logger.info("=" * 50)
    
    status = result.get('status', 'unknown')
    logger.info(f"🏁 Status: {status}")
    logger.info(f"✅ Workflow Completed: {result.get('workflow_completed', False)}")
    
    if status == 'error':
        logger.error(f"❌ Error: {result.get('error', 'Unknown error')}")
        return
    
    # Display generated files
    if 'files_generated' in result:
        files = result['files_generated']
        logger.info(f"\n📁 Generated Files:")
        logger.info(f"   Master Data JSON: {files.get('master_data_json', 'N/A')}")
        logger.info(f"   Monitoring Report: {files.get('monitoring_report', 'N/A')}")
    
    # Display summary
    if 'summary' in result:
        summary = result['summary']
        logger.info(f"\n📈 Analysis Summary:")
        logger.info(f"   ARIMA Model: {summary.get('arima_model', 'N/A')}")
        logger.info(f"   Forecast Ready: {'✅ YES' if summary.get('forecast_ready', False) else '❌ NO'}")
        logger.info(f"   Forecast Steps: {summary.get('forecast_steps', 'N/A')}")
        logger.info(f"   Risk Profile: {summary.get('risk_profile', 'N/A')}")
    
    # Display monitoring data summary
    if 'monitoring_data' in result and 'summary_metrics' in result['monitoring_data']:
        metrics = result['monitoring_data']['summary_metrics']
        
        if 'data_summary' in metrics:
            data_sum = metrics['data_summary']
            logger.info(f"\n📊 Data Summary:")
            logger.info(f"   Total Periods: {data_sum.get('total_periods', 'N/A')}")
            logger.info(f"   Historical: {data_sum.get('historical_periods', 'N/A')}")
            logger.info(f"   Forecasts: {data_sum.get('forecast_periods', 'N/A')}")
        
        if 'model_summary' in metrics:
            model_sum = metrics['model_summary']
            logger.info(f"\n🔬 Model Summary:")
            logger.info(f"   Mean Value: {model_sum.get('mean_value', 'N/A'):.8f}")
            logger.info(f"   AR Coefficients: {model_sum.get('ar_coefficients_count', 'N/A')}")
            logger.info(f"   MA Coefficients: {model_sum.get('ma_coefficients_count', 'N/A')}")
        
        if 'forecast_performance' in metrics:
            perf = metrics['forecast_performance']
            logger.info(f"\n🎯 Forecast Performance:")
            logger.info(f"   Mean Absolute Error: {perf.get('mean_absolute_error', 'N/A'):.6f}")
            logger.info(f"   Accuracy: {perf.get('forecast_accuracy_pct', 'N/A'):.1f}%")

def test_individual_components(result: dict, logger):
    """Test individual components to show they work correctly."""
    
    logger.info(f"\n🔧 TESTING INDIVIDUAL COMPONENTS")
    logger.info("=" * 40)
    
    try:
        # Test 1: Master Data JSON
        if 'files_generated' in result and 'master_data_json' in result['files_generated']:
            master_data_path = result['files_generated']['master_data_json']
            
            if Path(master_data_path).exists():
                logger.info(f"✅ Master Data JSON exists: {master_data_path}")
                
                # Load and validate JSON
                with open(master_data_path, 'r') as f:
                    master_data = json.load(f)
                
                logger.info(f"   Export timestamp: {master_data['export_info']['export_timestamp']}")
                logger.info(f"   Model: {master_data['model_specification']['model_string']}")
                logger.info(f"   AR coefficients: {len(master_data['model_coefficients']['ar_coefficients'])}")
                logger.info(f"   MA coefficients: {len(master_data['model_coefficients']['ma_coefficients'])}")
            else:
                logger.error(f"❌ Master Data JSON not found: {master_data_path}")
        
        # Test 2: Time Series Data Format
        if 'monitoring_data' in result and 'time_series_data' in result['monitoring_data']:
            time_series_df = result['monitoring_data']['time_series_data']
            
            logger.info(f"\n✅ Time Series Data Generated:")
            logger.info(f"   Rows: {len(time_series_df)}")
            logger.info(f"   Columns: {list(time_series_df.columns)}")
            
            # Check your requested format
            required_columns = ['Date', 'Return', 'Demean', 'AR.1', 'AR.2', 'AR.3', 
                              'MA.1', 'MA.2', 'MA.3', 'MA.4', 'MA.5', 'E', 'Prd demean', 'Prd Return']
            
            missing_columns = [col for col in required_columns if col not in time_series_df.columns]
            if not missing_columns:
                logger.info(f"   ✅ All requested columns present!")
            else:
                logger.warning(f"   ⚠️ Missing columns: {missing_columns}")
            
            # Show sample data
            if len(time_series_df) > 0:
                logger.info(f"\n📋 Sample Time Series Data (first 3 rows):")
                sample_df = time_series_df.head(3)
                for i, (_, row) in enumerate(sample_df.iterrows()):
                    date_str = str(row['Date'])[:10]
                    return_val = f"{row['Return']:.6f}" if pd.notna(row['Return']) else 'FORECAST'
                    pred_return = f"{row['Prd Return']:.6f}" if pd.notna(row['Prd Return']) else 'N/A'
                    logger.info(f"   Row {i+1}: {date_str} | Return={return_val} | Predicted={pred_return}")
        
        logger.info(f"\n✅ Individual component tests completed")
        
    except Exception as e:
        logger.error(f"❌ Component testing failed: {e}")

def demonstrate_json_master_data(result: dict, logger):
    """Demonstrate how the JSON master data can be reused."""
    
    logger.info(f"\n🔄 DEMONSTRATING JSON MASTER DATA REUSE")
    logger.info("=" * 45)
    
    try:
        master_data_path = result['files_generated']['master_data_json']
        
        # Show how to reuse the master data JSON
        logger.info(f"📁 Master Data JSON: {master_data_path}")
        
        # Load the JSON
        with open(master_data_path, 'r') as f:
            master_data = json.load(f)
        
        # Show key components for reuse
        forecasting_params = master_data['forecasting_parameters']
        
        logger.info(f"\n🎯 Ready for Reuse:")
        logger.info(f"   Mean Value: {forecasting_params['mean_value']:.8f}")
        logger.info(f"   Is Differenced: {forecasting_params['is_differenced']}")
        logger.info(f"   Required History Length: {forecasting_params['required_history_length']}")
        
        logger.info(f"\n📜 Formula Components Available:")
        formula_components = forecasting_params['formula_components']
        for formula_name, formula_text in formula_components.items():
            logger.info(f"   {formula_name}: {formula_text}")
        
        # Show coefficient mapping
        ar_mapping = forecasting_params['ar_lag_mapping']
        ma_mapping = forecasting_params['ma_lag_mapping']
        
        if ar_mapping:
            logger.info(f"\n🔢 AR Coefficients Ready for Application:")
            for lag, coeff in ar_mapping.items():
                logger.info(f"   AR.{lag} = {coeff:.8f}")
        
        if ma_mapping:
            logger.info(f"\n🔢 MA Coefficients Ready for Application:")
            for lag, coeff in ma_mapping.items():
                logger.info(f"   MA.{lag} = {coeff:.8f}")
        
        logger.info(f"\n💡 Usage: Load this JSON with ArimaForecastingApplicator")
        logger.info(f"         to apply forecasting to new data without re-running analysis!")
        
    except Exception as e:
        logger.error(f"❌ JSON demonstration failed: {e}")

def demonstrate_file_outputs(logger):
    """Show what files are created and their contents."""
    
    logger.info(f"\n📁 FILE OUTPUTS DEMONSTRATION")
    logger.info("=" * 40)
    
    # Look for generated files
    generated_files = [
        "updated_arima_monitoring_neutral.xlsx",
        "synthetic_updated_monitoring.xlsx", 
        "arima_master_data_neutral.json",
        "synthetic_updated_monitoring_time_series.csv",
        "synthetic_updated_monitoring_master_data.csv"
    ]
    
    for file_path in generated_files:
        if Path(file_path).exists():
            file_size = Path(file_path).stat().st_size
            logger.info(f"✅ {file_path} ({file_size:,} bytes)")
            
            # Show preview for different file types
            if file_path.endswith('.json'):
                with open(file_path, 'r') as f:
                    data = json.load(f)
                logger.info(f"   📋 JSON preview: {list(data.keys())}")
            
            elif file_path.endswith('.xlsx'):
                try:
                    sheets = pd.ExcelFile(file_path).sheet_names
                    logger.info(f"   📋 Excel sheets: {sheets}")
                except:
                    logger.info(f"   📋 Excel file (could not read sheets)")
            
            elif file_path.endswith('.csv'):
                try:
                    df = pd.read_csv(file_path)
                    logger.info(f"   📋 CSV: {len(df)} rows, {len(df.columns)} columns")
                except:
                    logger.info(f"   📋 CSV file (could not read)")
        else:
            logger.info(f"❌ {file_path} (not found)")

def run_batch_demo(monitor, logger):
    """Demonstrate batch processing capabilities."""
    
    logger.info(f"\n🔄 BATCH PROCESSING DEMONSTRATION")
    logger.info("=" * 40)
    
    try:
        # Look for multiple files
        batch_files = []
        for pattern in ["*backtest*.xlsx", "*monitoring*.xlsx"]:
            batch_files.extend(Path(".").glob(pattern))
        
        if len(batch_files) >= 1:
            logger.info(f"Found {len(batch_files)} files for batch demo")
            
            # Run batch processing
            batch_result = monitor.run_batch_monitoring(
                backtest_files=[str(f) for f in batch_files[:2]],  # Limit for demo
                risk_profiles=["neutral"],
                output_dir="batch_demo_output",
                forecast_steps=3
            )
            
            logger.info(f"Batch Results:")
            logger.info(f"  Success Rate: {batch_result['summary']['success_rate']:.1%}")
            logger.info(f"  Output Directory: {batch_result['summary']['output_directory']}")
        else:
            logger.info("No multiple files found - skipping batch demo")
            
    except Exception as e:
        logger.error(f"Batch demo failed: {e}")

def main():
    """Main test function."""
    
    print("🚀 UPDATED ARIMA WORKFLOW - COMPLETE TEST")
    print("=" * 60)
    print("This demonstrates the improved architecture that:")
    print("✅ Leverages your existing ARIMA analysis work")
    print("💾 Exports master data to JSON for reuse")
    print("🎯 Applies forecasting using saved parameters")
    print("📊 Generates monitoring reports in your exact format")
    print()
    
    # Run main test
    test_complete_updated_workflow()
    
    # Demonstrate file outputs
    logger = CustomLogger('FileDemo')
    demonstrate_file_outputs(logger)
    
    print("\n" + "=" * 60)
    print("✅ UPDATED ARIMA WORKFLOW TEST COMPLETED!")
    print("\n📋 KEY IMPROVEMENTS:")
    print("1. ✅ Leverages your existing ARIMA analysis")
    print("2. 💾 Saves master data to JSON for reuse")
    print("3. 🎯 Separates analysis from application")
    print("4. 📊 Generates your exact monitoring format")
    print("5. 🔄 Enables efficient batch processing")
    print("\n📁 CHECK GENERATED FILES:")
    print("- *.xlsx files: Your monitoring reports")
    print("- *.json files: Reusable master data")  
    print("- *_time_series.csv: Time series data")
    print("- *_master_data.csv: Model parameters")

if __name__ == "__main__":
    main()