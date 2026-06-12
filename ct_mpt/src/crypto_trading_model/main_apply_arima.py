#!/usr/bin/env python
"""
Main ARIMA Forecasting Application
==================================

File: main_apply_arima.py

This script applies ARIMA forecasting using the master data JSON files created by
main_find_arima_parameter.py to generate portfolio return predictions and monitoring reports.

Workflow:
1. Load ARIMA master data JSON files
2. Apply forecasting to portfolio returns
3. Generate Excel monitoring reports with your exact format
4. Create forecast-enhanced backtests (optional)

Usage:
    python main_apply_arima.py
    
    # Or with specific parameters:
    python main_apply_arima.py --risk-profile neutral --forecast-steps 7 --format xlsx
    
Output:
    - arima_monitoring_{risk_profile}.xlsx (monitoring reports)
    - arima_forecast_data_{risk_profile}.csv (raw forecast data)
    - enhanced_backtest_results_{risk_profile}.xlsx (if enabled)
"""

import sys
import argparse
from pathlib import Path
import pandas as pd
import numpy as np
from datetime import datetime
from typing import Dict, List, Optional, Any

import sys
import argparse
from pathlib import Path
import pandas as pd
import numpy as np
from datetime import datetime
from typing import Dict, List, Optional, Any

# Add the src directory to Python path so we can import crypto_trading_model
if __name__ == "__main__":
    # Get current file location: .../src/crypto_trading_model/main_apply_arima.py
    current_file = Path(__file__).resolve()
    
    # Go up to src directory: .../src/
    src_dir = current_file.parent.parent
    
    # Add src to Python path
    sys.path.insert(0, str(src_dir))

# Now use absolute imports that work with the package structure
from crypto_trading_model.custom_logging.logger import CustomLogger
from crypto_trading_model.config.config_manager import ConfigManager
from crypto_trading_model.arima.arima_forecasting_applicator import ArimaForecastingApplicator
from crypto_trading_model.reporting.updated_arima_monitoring import UpdatedArimaMonitoring

def apply_arima_forecasting_for_profile(risk_profile: str, forecast_steps: int = 5,
                                      output_format: str = 'xlsx', 
                                      backtest_file: Optional[str] = None,
                                      logger: CustomLogger = None) -> Dict[str, Any]:
    """
    Apply ARIMA forecasting for a specific risk profile.
    
    Args:
        risk_profile: Risk profile to analyze ('neutral', 'averse', 'lover')
        forecast_steps: Number of future steps to forecast
        output_format: Output format ('xlsx' or 'csv')
        backtest_file: Optional specific backtest file path
        logger: Logger instance
        
    Returns:
        Dictionary with forecasting results and file paths
    """
    if logger is None:
        logger = CustomLogger(f'ArimaForecaster_{risk_profile}')
    
    logger.info(f"🎯 APPLYING ARIMA FORECASTING FOR {risk_profile.upper()} PROFILE")
    logger.info("=" * 60)
    
    try:
        # Step 1: Check for ARIMA master data
        master_data_path = f"arima_master_data_{risk_profile}.json"
        
        if not Path(master_data_path).exists():
            error_msg = f"❌ ARIMA master data not found: {master_data_path}"
            logger.error(error_msg)
            logger.info(f"💡 Run main_find_arima_parameter.py first for {risk_profile} profile")
            return {'status': 'error', 'error': error_msg}
        
        logger.info(f"✅ Found ARIMA master data: {master_data_path}")
        
        # Step 2: Determine backtest file
        if backtest_file is None:
            possible_files = [
                f"backtest_results_{risk_profile}.xlsx",
                f"../backtest_results_{risk_profile}.xlsx",
                f"../../backtest_results_{risk_profile}.xlsx",
                f"backtest_results/backtest_results_{risk_profile}.xlsx"
            ]
            
            backtest_file = None
            for file_path in possible_files:
                if Path(file_path).exists():
                    backtest_file = file_path
                    logger.info(f"✅ Found backtest file: {backtest_file}")
                    break
            
            if backtest_file is None:
                error_msg = f"❌ No backtest results found for {risk_profile} profile"
                logger.error(error_msg)
                logger.info("💡 Run main.py first to generate backtest results")
                return {'status': 'error', 'error': error_msg}
        
        # Step 3: Initialize ARIMA applicator
        logger.info(f"\n📊 INITIALIZING ARIMA FORECASTING")
        logger.info("-" * 40)
        
        applicator = ArimaForecastingApplicator(logger)
        master_data = applicator.load_master_data(master_data_path)
        
        # Display model information
        model_spec = master_data["model_specification"]
        forecasting_params = master_data["forecasting_parameters"]
        
        logger.info(f"✅ ARIMA Model Loaded:")
        logger.info(f"   Model: {model_spec['model_string']}")
        logger.info(f"   AIC: {model_spec['aic']:.4f}")
        logger.info(f"   Mean Value: {forecasting_params['mean_value']:.8f}")
        logger.info(f"   AR Coefficients: {len(forecasting_params['ar_lag_mapping'])}")
        logger.info(f"   MA Coefficients: {len(forecasting_params['ma_lag_mapping'])}")
        
        # Step 4: Load portfolio returns from backtest file
        logger.info(f"\n📈 LOADING PORTFOLIO RETURNS")
        logger.info("-" * 40)
        
        portfolio_returns = load_portfolio_returns_from_backtest(backtest_file, logger)
        
        logger.info(f"✅ Loaded {len(portfolio_returns)} portfolio returns")
        logger.info(f"   Date range: {portfolio_returns.index[0]} to {portfolio_returns.index[-1]}")
        logger.info(f"   Mean return: {portfolio_returns.mean():.6f}")
        logger.info(f"   Volatility: {portfolio_returns.std():.6f}")
        
        # Step 5: Generate forecasting data
        logger.info(f"\n🔮 GENERATING FORECASTS")
        logger.info("-" * 40)
        
        # Generate time series data in Excel format
        time_series_df = applicator.generate_time_series_data(
            historical_returns=portfolio_returns,
            include_forecasts=True,
            forecast_steps=forecast_steps
        )
        
        logger.info(f"✅ Generated forecasting data:")
        logger.info(f"   Total periods: {len(time_series_df)}")
        logger.info(f"   Historical: {len(portfolio_returns)}")
        logger.info(f"   Forecasts: {forecast_steps}")
        logger.info(f"   Columns: {list(time_series_df.columns)}")
        
        # Step 6: Calculate forecast accuracy for historical data
        historical_data = time_series_df.dropna(subset=['Return'])
        if len(historical_data) > 5:
            actual_returns = historical_data['Return']
            predicted_returns = historical_data['Prd Return']
            forecast_errors = actual_returns - predicted_returns
            
            mae = np.mean(np.abs(forecast_errors))
            rmse = np.sqrt(np.mean(forecast_errors ** 2))
            
            logger.info(f"\n📊 FORECAST ACCURACY (Historical):")
            logger.info(f"   Mean Absolute Error: {mae:.6f}")
            logger.info(f"   Root Mean Square Error: {rmse:.6f}")
            logger.info(f"   Max Error: {np.max(np.abs(forecast_errors)):.6f}")
        
        # Step 7: Display future forecasts
        forecast_data = time_series_df[time_series_df['Return'].isna()]
        if len(forecast_data) > 0:
            logger.info(f"\n🔮 FUTURE FORECASTS:")
            logger.info("-" * 30)
            for i, (_, row) in enumerate(forecast_data.iterrows()):
                date_str = row['Date'].strftime('%Y-%m-%d')
                pred_return = row['Prd Return']
                logger.info(f"   Day {i+1} ({date_str}): {pred_return:.6f} ({pred_return*100:.2f}%)")
        
        # Step 8: Export results
        logger.info(f"\n📁 EXPORTING RESULTS")
        logger.info("-" * 40)
        
        # Create master data DataFrame for monitoring report
        master_data_df = create_master_data_dataframe(master_data, logger)
        
        # Export based on format
        output_files = {}
        
        if output_format.lower() == 'xlsx':
            # Excel format with multiple sheets
            excel_path = f"arima_monitoring_{risk_profile}.xlsx"
            export_to_excel(time_series_df, master_data_df, excel_path, logger)
            output_files['monitoring_report'] = excel_path
            
        else:
            # CSV format (separate files)
            time_series_path = f"arima_forecast_data_{risk_profile}.csv"
            master_data_path_csv = f"arima_master_data_{risk_profile}.csv"
            
            time_series_df.to_csv(time_series_path, index=False, float_format='%.8f')
            master_data_df.to_csv(master_data_path_csv, index=False)
            
            logger.info(f"✅ Exported CSV files:")
            logger.info(f"   📊 Time series: {time_series_path}")
            logger.info(f"   📋 Master data: {master_data_path_csv}")
            
            output_files['time_series_csv'] = time_series_path
            output_files['master_data_csv'] = master_data_path_csv
        
        # Step 9: Create summary
        forecast_summary = {
            'next_day_forecast': forecast_data.iloc[0]['Prd Return'] if len(forecast_data) > 0 else None,
            'next_week_avg_forecast': forecast_data['Prd Return'].head(7).mean() if len(forecast_data) >= 7 else None,
            'forecast_trend': 'positive' if len(forecast_data) > 0 and forecast_data['Prd Return'].mean() > 0 else 'negative'
        }
        
        result = {
            'status': 'success',
            'risk_profile': risk_profile,
            'model_info': {
                'arima_order': model_spec['arima_order'],
                'aic': model_spec['aic'],
                'forecast_ready': model_spec['forecast_ready']
            },
            'data_info': {
                'historical_periods': len(portfolio_returns),
                'forecast_periods': forecast_steps,
                'total_periods': len(time_series_df)
            },
            'forecast_summary': forecast_summary,
            'output_files': output_files,
            'backtest_file': backtest_file,
            'master_data_file': master_data_path
        }
        
        logger.info(f"\n✅ ARIMA FORECASTING COMPLETED SUCCESSFULLY!")
        logger.info(f"🎯 Next day forecast: {forecast_summary['next_day_forecast']:.6f}")
        logger.info(f"📈 Forecast trend: {forecast_summary['forecast_trend']}")
        
        return result
        
    except Exception as e:
        error_msg = f"ARIMA forecasting failed for {risk_profile}: {e}"
        logger.error(error_msg)
        import traceback
        traceback.print_exc()
        return {'status': 'error', 'error': error_msg}

def load_portfolio_returns_from_backtest(backtest_file: str, logger: CustomLogger) -> pd.Series:
    """Load portfolio returns from backtest results file."""
    
    try:
        logger.info(f"Loading portfolio returns from: {backtest_file}")
        
        # Load backtest data
        if backtest_file.endswith('.xlsx'):
            data = pd.read_excel(backtest_file, sheet_name='Backtest Results')
        else:
            data = pd.read_csv(backtest_file)
        
        # Validate required columns
        if 'Daily_Return' not in data.columns or 'Timestamp' not in data.columns:
            raise ValueError("Required columns 'Daily_Return' and 'Timestamp' not found")
        
        # Create portfolio returns series
        data['Timestamp'] = pd.to_datetime(data['Timestamp'])
        portfolio_returns = pd.Series(
            data['Daily_Return'].values,
            index=data['Timestamp'],
            name='Portfolio_Returns'
        )
        
        # Clean data
        portfolio_returns = portfolio_returns.dropna()
        portfolio_returns = portfolio_returns[np.isfinite(portfolio_returns)]
        
        # Remove zero returns (typically No Investment periods)
        non_zero_returns = portfolio_returns[portfolio_returns != 0]
        
        if len(non_zero_returns) < len(portfolio_returns) * 0.5:
            logger.warning(f"Many zero returns found ({len(portfolio_returns) - len(non_zero_returns)}/{len(portfolio_returns)})")
            logger.info("Using all returns including zeros for ARIMA analysis")
        else:
            portfolio_returns = non_zero_returns
            logger.info(f"Using {len(portfolio_returns)} non-zero returns for ARIMA analysis")
        
        return portfolio_returns
        
    except Exception as e:
        raise Exception(f"Failed to load portfolio returns: {e}")

def create_master_data_dataframe(master_data: Dict[str, Any], logger: CustomLogger) -> pd.DataFrame:
    """Create master data DataFrame for monitoring report."""
    
    try:
        master_data_rows = []
        
        # Basic model information
        model_spec = master_data["model_specification"]
        data_prep = master_data["data_preparation"]
        forecasting_params = master_data["forecasting_parameters"]
        
        master_data_rows.extend([
            ["Parameter", "Value", "Description"],
            ["=== MODEL SPECIFICATION ===", "", ""],
            ["ARIMA Order", f"({model_spec['arima_order']['p']},{model_spec['arima_order']['d']},{model_spec['arima_order']['q']})", "Model specification"],
            ["AIC", model_spec["aic"], "Akaike Information Criterion"],
            ["Forecast Ready", model_spec["forecast_ready"], "Model ready for forecasting"],
            ["", "", ""],
            ["=== DATA INFORMATION ===", "", ""],
            ["Total Observations", data_prep["original_data"]["total_observations"], "Number of historical observations"],
            ["Mean Value", forecasting_params["mean_value"], "Mean removed during preparation"],
            ["Is Differenced", forecasting_params["is_differenced"], "Whether data was differenced"],
            ["Data Start Date", data_prep["original_data"]["data_start_date"], "First observation date"],
            ["Data End Date", data_prep["original_data"]["data_end_date"], "Last observation date"],
            ["", "", ""]
        ])
        
        # AR coefficients
        ar_coeffs = forecasting_params["ar_lag_mapping"]
        master_data_rows.append(["=== AR COEFFICIENTS ===", "", ""])
        if ar_coeffs:
            for lag, coeff in sorted(ar_coeffs.items()):
                master_data_rows.append([f"AR.{lag}", coeff, f"Autoregressive coefficient lag {lag}"])
        else:
            master_data_rows.append(["AR Coefficients", "None significant", "No significant AR terms"])
        
        master_data_rows.append(["", "", ""])
        
        # MA coefficients
        ma_coeffs = forecasting_params["ma_lag_mapping"]
        master_data_rows.append(["=== MA COEFFICIENTS ===", "", ""])
        if ma_coeffs:
            for lag, coeff in sorted(ma_coeffs.items()):
                master_data_rows.append([f"MA.{lag}", coeff, f"Moving average coefficient lag {lag}"])
        else:
            master_data_rows.append(["MA Coefficients", "None significant", "No significant MA terms"])
        
        # Convert to DataFrame
        df_data = []
        for row in master_data_rows[1:]:  # Skip header row
            df_data.append({
                'Parameter': row[0],
                'Value': row[1],
                'Description': row[2]
            })
        
        return pd.DataFrame(df_data)
        
    except Exception as e:
        logger.error(f"Failed to create master data DataFrame: {e}")
        return pd.DataFrame()

def export_to_excel(time_series_df: pd.DataFrame, master_data_df: pd.DataFrame, 
                   output_path: str, logger: CustomLogger):
    """Export data to Excel with proper formatting."""
    
    try:
        with pd.ExcelWriter(output_path, engine='openpyxl') as writer:
            # Time series data sheet
            time_series_df.to_excel(writer, sheet_name='Time Series Data', index=False)
            
            # Master data sheet
            master_data_df.to_excel(writer, sheet_name='Master Data', index=False)
            
            # Apply basic formatting
            try:
                from openpyxl.styles import Font, PatternFill
                
                workbook = writer.book
                
                # Format time series sheet
                ts_sheet = writer.sheets['Time Series Data']
                header_font = Font(bold=True, color="FFFFFF")
                header_fill = PatternFill(start_color="366092", end_color="366092", fill_type="solid")
                
                for cell in ts_sheet[1]:  # Header row
                    cell.font = header_font
                    cell.fill = header_fill
                
                # Format master data sheet
                master_sheet = writer.sheets['Master Data']
                for cell in master_sheet[1]:  # Header row
                    cell.font = header_font
                    cell.fill = header_fill
                
                logger.info("✅ Applied Excel formatting")
                
            except ImportError:
                logger.warning("⚠️ openpyxl styling not available - exported without formatting")
        
        logger.info(f"✅ Exported Excel monitoring report: {output_path}")
        
    except ImportError:
        logger.warning("openpyxl not available, falling back to CSV export")
        # Fallback to CSV
        base_name = Path(output_path).stem
        time_series_df.to_csv(f"{base_name}_time_series.csv", index=False, float_format='%.8f')
        master_data_df.to_csv(f"{base_name}_master_data.csv", index=False)
        logger.info(f"✅ Exported CSV files: {base_name}_time_series.csv, {base_name}_master_data.csv")
        
    except Exception as e:
        logger.error(f"Excel export failed: {e}")
        raise

def apply_arima_forecasting_all_profiles(config_manager: ConfigManager, forecast_steps: int,
                                       output_format: str, logger: CustomLogger) -> Dict[str, Any]:
    """Apply ARIMA forecasting for all viable risk profiles."""
    
    logger.info("🎯 APPLYING ARIMA FORECASTING FOR ALL VIABLE PROFILES")
    logger.info("=" * 60)
    
    try:
        # Find available master data files
        master_data_files = list(Path(".").glob("arima_master_data_*.json"))
        
        if not master_data_files:
            error_msg = "❌ No ARIMA master data files found"
            logger.error(error_msg)
            logger.info("💡 Run main_find_arima_parameter.py first")
            return {'status': 'error', 'error': error_msg}
        
        logger.info(f"✅ Found {len(master_data_files)} master data files:")
        
        viable_profiles = []
        for file in master_data_files:
            # Extract profile from filename: arima_master_data_{profile}.json
            profile = file.stem.replace('arima_master_data_', '')
            viable_profiles.append(profile)
            logger.info(f"   📁 {file} -> {profile} profile")
        
        results = {
            'timestamp': datetime.now().isoformat(),
            'profiles_processed': len(viable_profiles),
            'forecast_steps': forecast_steps,
            'output_format': output_format,
            'profiles': {}
        }
        
        successful_profiles = []
        failed_profiles = []
        
        for profile in viable_profiles:
            logger.info(f"\n{'='*20} {profile.upper()} PROFILE {'='*20}")
            
            profile_result = apply_arima_forecasting_for_profile(
                risk_profile=profile,
                forecast_steps=forecast_steps,
                output_format=output_format,
                logger=logger
            )
            
            results['profiles'][profile] = profile_result
            
            if profile_result['status'] == 'success':
                successful_profiles.append(profile)
                logger.info(f"✅ {profile}: SUCCESS")
                
                # Log forecast summary
                forecast_summary = profile_result['forecast_summary']
                next_day = forecast_summary['next_day_forecast']
                trend = forecast_summary['forecast_trend']
                logger.info(f"   🎯 Next day forecast: {next_day:.6f} ({trend})")
                
            else:
                failed_profiles.append(profile)
                logger.error(f"❌ {profile}: FAILED - {profile_result.get('error', 'Unknown error')}")
        
        # Summary
        results['summary'] = {
            'successful_profiles': successful_profiles,
            'failed_profiles': failed_profiles,
            'success_count': len(successful_profiles),
            'failed_count': len(failed_profiles),
            'success_rate': len(successful_profiles) / len(viable_profiles) if viable_profiles else 0
        }
        
        logger.info(f"\n📊 FINAL SUMMARY")
        logger.info("=" * 40)
        logger.info(f"Profiles processed: {len(viable_profiles)}")
        logger.info(f"✅ Successful: {len(successful_profiles)} - {successful_profiles}")
        logger.info(f"❌ Failed: {len(failed_profiles)} - {failed_profiles}")
        
        if successful_profiles:
            logger.info(f"\n📁 Generated Files:")
            for profile in successful_profiles:
                profile_result = results['profiles'][profile]
                output_files = profile_result.get('output_files', {})
                for file_type, file_path in output_files.items():
                    logger.info(f"   📊 {profile} - {file_type}: {file_path}")
        
        return results
        
    except Exception as e:
        error_msg = f"Failed to process all profiles: {e}"
        logger.error(error_msg)
        return {'status': 'error', 'error': error_msg}

def validate_master_data_availability(logger: CustomLogger) -> List[str]:
    """Check which risk profiles have ARIMA master data available."""
    
    logger.info("🔍 CHECKING MASTER DATA AVAILABILITY")
    logger.info("-" * 40)
    
    available_profiles = []
    
    # Check for master data files
    master_data_files = list(Path(".").glob("arima_master_data_*.json"))
    
    if master_data_files:
        logger.info(f"✅ Found {len(master_data_files)} master data files:")
        
        for file in master_data_files:
            # Extract profile name
            profile = file.stem.replace('arima_master_data_', '')
            
            try:
                # Validate the file is readable
                import json
                with open(file, 'r') as f:
                    data = json.load(f)
                
                # Check if it has required components
                required_keys = ['model_specification', 'forecasting_parameters']
                if all(key in data for key in required_keys):
                    available_profiles.append(profile)
                    model_info = data['model_specification']
                    logger.info(f"   📁 {profile}: {model_info.get('model_string', 'N/A')} (AIC: {model_info.get('aic', 'N/A'):.4f})")
                else:
                    logger.warning(f"   ⚠️ {profile}: Invalid master data format")
                    
            except Exception as e:
                logger.error(f"   ❌ {profile}: Cannot read file - {e}")
    else:
        logger.warning("❌ No master data files found")
        logger.info("💡 Run main_find_arima_parameter.py first")
    
    return available_profiles

def show_forecast_summary(results: Dict[str, Any], logger: CustomLogger):
    """Display a summary of forecasting results."""
    
    if 'profiles' not in results:
        return
    
    logger.info(f"\n📊 FORECASTING SUMMARY")
    logger.info("=" * 50)
    
    for profile, result in results['profiles'].items():
        if result['status'] == 'success':
            forecast_summary = result['forecast_summary']
            next_day = forecast_summary['next_day_forecast']
            trend = forecast_summary['forecast_trend']
            
            logger.info(f"🎯 {profile.upper()} Profile:")
            logger.info(f"   Next day forecast: {next_day:.6f} ({next_day*100:.2f}%)")
            logger.info(f"   Trend: {trend}")
            
            if forecast_summary['next_week_avg_forecast']:
                week_avg = forecast_summary['next_week_avg_forecast']
                logger.info(f"   Next week average: {week_avg:.6f} ({week_avg*100:.2f}%)")
            
            logger.info(f"   Model: ARIMA{result['model_info']['arima_order']}")
            logger.info("")

def main():
    """Main function for ARIMA forecasting application."""
    
    # Parse command line arguments
    parser = argparse.ArgumentParser(description='Apply ARIMA forecasting to crypto trading strategy')
    parser.add_argument('--risk-profile', type=str, choices=['neutral', 'averse', 'lover'],
                       help='Specific risk profile to forecast')
    parser.add_argument('--forecast-steps', type=int, default=5,
                       help='Number of future steps to forecast (default: 5)')
    parser.add_argument('--format', type=str, choices=['xlsx', 'csv'], default='xlsx',
                       help='Output format (default: xlsx)')
    parser.add_argument('--backtest-file', type=str,
                       help='Specific backtest file to use')
    parser.add_argument('--check-only', action='store_true',
                       help='Only check master data availability without running forecasts')
    
    args = parser.parse_args()
    
    # Initialize logger
    logger = CustomLogger('ArimaForecaster')
    
    print("🎯 ARIMA FORECASTING APPLICATION")
    print("=" * 50)
    print("This script applies ARIMA forecasting using master data JSON files")
    print("to generate portfolio return predictions and monitoring reports.")
    print()
    
    # Check master data availability
    available_profiles = validate_master_data_availability(logger)
    
    if not available_profiles:
        print("❌ No master data available. Run main_find_arima_parameter.py first.")
        return 1
    
    if args.check_only:
        print(f"✅ Master data available for: {available_profiles}")
        return 0
    
    try:
        # Initialize configuration
        config_manager = ConfigManager(logger)
        
        if args.risk_profile:
            # Forecast for specific profile
            if args.risk_profile not in available_profiles:
                print(f"❌ Master data not available for {args.risk_profile} profile")
                print(f"✅ Available profiles: {available_profiles}")
                return 1
            
            logger.info(f"Forecasting for specific risk profile: {args.risk_profile}")
            
            result = apply_arima_forecasting_for_profile(
                risk_profile=args.risk_profile,
                forecast_steps=args.forecast_steps,
                output_format=args.format,
                backtest_file=args.backtest_file,
                logger=logger
            )
            
            if result['status'] == 'success':
                print(f"\n✅ SUCCESS: ARIMA forecasting completed for {args.risk_profile}")
                print(f"📁 Output files: {result['output_files']}")
                
                forecast_summary = result['forecast_summary']
                next_day = forecast_summary['next_day_forecast']
                print(f"🎯 Next day forecast: {next_day:.6f} ({next_day*100:.2f}%)")
                return 0
            else:
                print(f"\n❌ FAILED: {result.get('error', 'Unknown error')}")
                return 1
                
        else:
            # Forecast for all available profiles
            logger.info("Forecasting for all available profiles")
            
            results = apply_arima_forecasting_all_profiles(
                config_manager=config_manager,
                forecast_steps=args.forecast_steps,
                output_format=args.format,
                logger=logger
            )
            
            if 'summary' in results:
                summary = results['summary']
                
                if summary['success_count'] > 0:
                    print(f"\n✅ SUCCESS: Forecasting completed for {summary['success_count']} profiles")
                    print(f"📁 Profiles: {summary['successful_profiles']}")
                    
                    # Show forecast summary
                    show_forecast_summary(results, logger)
                    return 0
                else:
                    print(f"\n❌ FAILED: No profiles processed successfully")
                    return 1
            else:
                print(f"\n❌ PROCESSING FAILED: {results.get('error', 'Unknown error')}")
                return 1
                
    except Exception as e:
        logger.error(f"Main execution failed: {e}")
        import traceback
        traceback.print_exc()
        return 1

if __name__ == "__main__":
    exit(main())