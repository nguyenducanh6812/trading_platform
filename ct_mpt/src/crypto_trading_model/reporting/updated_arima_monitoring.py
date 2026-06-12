"""
Updated ARIMA Monitoring System
===============================

File: crypto_trading_model/reporting/updated_arima_monitoring.py

This module integrates the master data export and forecasting application
to create a comprehensive monitoring system that leverages your existing ARIMA work.

Architecture:
1. Use existing ARIMA analysis → Export master data to JSON
2. Use ARIMA applicator → Apply forecasting using JSON data  
3. Generate monitoring reports in your requested format

Usage:
    monitor = UpdatedArimaMonitoring(logger)
    monitor.generate_complete_monitoring_report(
        backtest_file="backtest_results_neutral.xlsx",
        output_path="arima_monitoring.xlsx"
    )
"""

import pandas as pd
import numpy as np
from pathlib import Path
from typing import Dict, List, Optional, Any, Tuple
import json

from ..custom_logging.logger import CustomLogger
from ..config.config_manager import ConfigManager
from ..arima.backtest_arima_analyzer import BacktestArimaAnalyzer
from ..arima.arima_master_data_exporter import ArimaMasterDataExporter
from ..arima.arima_forecasting_applicator import ArimaForecastingApplicator
from .arima_forecast_reporter import ArimaForecastReporter

class UpdatedArimaMonitoring:
    """
    Complete ARIMA monitoring system that leverages your existing work.
    
    This class orchestrates:
    1. Your existing ARIMA analysis (BacktestArimaAnalyzer)
    2. Master data export (ArimaMasterDataExporter) 
    3. Forecasting application (ArimaForecastingApplicator)
    4. Monitoring report generation (ArimaForecastReporter)
    """
    
    def __init__(self, logger: CustomLogger, config_manager: Optional[ConfigManager] = None):
        """
        Initialize the updated monitoring system.
        
        Args:
            logger: Custom logger instance
            config_manager: Optional configuration manager
        """
        self.logger = logger
        self.config_manager = config_manager
        
        # Initialize components
        self.arima_analyzer = BacktestArimaAnalyzer(logger)
        self.master_data_exporter = ArimaMasterDataExporter(logger)
        self.applicator = ArimaForecastingApplicator(logger)
        self.reporter = ArimaForecastReporter(logger)
        
        self.logger.info("UpdatedArimaMonitoring initialized")

    def generate_complete_monitoring_report(
        self,
        backtest_file: str,
        risk_profile: str = "neutral",
        output_path: str = "arima_monitoring.xlsx",
        forecast_steps: int = 5,
        export_format: str = 'xlsx'
    ) -> Dict[str, Any]:
        """
        Generate complete monitoring report using the 3-step process.
        
        Step 1: Run your existing ARIMA analysis
        Step 2: Export master data to JSON  
        Step 3: Apply forecasting and generate monitoring report
        
        Args:
            backtest_file: Path to backtest results file
            risk_profile: Risk profile to analyze
            output_path: Path for monitoring report
            forecast_steps: Number of forecast steps
            export_format: 'xlsx' or 'csv'
            
        Returns:
            Dictionary with complete results and file paths
        """
        try:
            self.logger.info("=== COMPLETE ARIMA MONITORING WORKFLOW ===")
            self.logger.info(f"Input: {backtest_file}")
            self.logger.info(f"Risk Profile: {risk_profile}")
            self.logger.info(f"Output: {output_path}")
            
            # Step 1: Run existing ARIMA analysis
            self.logger.info("\n🔬 STEP 1: Running existing ARIMA analysis...")
            arima_analysis_result = self.arima_analyzer.analyze_backtest_results(
                backtest_file, risk_profile
            )
            
            if not arima_analysis_result['forecast_ready']:
                return self._handle_no_forecast_case(arima_analysis_result, output_path)
            
            # Step 2: Export master data to JSON
            self.logger.info("\n💾 STEP 2: Exporting master data to JSON...")
            master_data_path = self._export_master_data(arima_analysis_result, risk_profile)
            
            # Step 3: Apply forecasting and generate monitoring report
            self.logger.info("\n📊 STEP 3: Applying forecasting and generating monitoring report...")
            monitoring_result = self._apply_forecasting_and_monitor(
                master_data_path, backtest_file, output_path, forecast_steps, export_format
            )
            
            # Step 4: Create comprehensive result
            complete_result = {
                'status': 'success',
                'workflow_completed': True,
                'files_generated': {
                    'master_data_json': master_data_path,
                    'monitoring_report': output_path
                },
                'arima_analysis': arima_analysis_result,
                'monitoring_data': monitoring_result,
                'summary': {
                    'arima_model': arima_analysis_result['arima_analysis']['analysis_summary']['optimal_order'],
                    'forecast_ready': True,
                    'forecast_steps': forecast_steps,
                    'risk_profile': risk_profile
                }
            }
            
            self.logger.info("\n✅ COMPLETE WORKFLOW FINISHED SUCCESSFULLY!")
            self.logger.info(f"📁 Master Data: {master_data_path}")
            self.logger.info(f"📊 Monitoring Report: {output_path}")
            
            return complete_result
            
        except Exception as e:
            error_msg = f"Complete monitoring workflow failed: {e}"
            self.logger.error(error_msg)
            return {
                'status': 'error',
                'workflow_completed': False,
                'error': error_msg
            }

    def _export_master_data(self, arima_analysis_result: Dict[str, Any], 
                           risk_profile: str) -> str:
        """Export master data from ARIMA analysis result."""
        
        try:
            # Extract components from analysis result
            arima_analysis = arima_analysis_result['arima_analysis']
            arima_result = arima_analysis['arima_result']
            prepared_data = arima_analysis['prepared_data']
            
            # Extract portfolio returns from backtest file (you may need to adjust this)
            portfolio_returns = self._extract_portfolio_returns_from_analysis(arima_analysis_result)
            
            # Create metadata
            metadata = {
                "source": "UpdatedArimaMonitoring",
                "risk_profile": risk_profile,
                "backtest_file": arima_analysis_result.get('backtest_file', 'unknown'),
                "analysis_timestamp": pd.Timestamp.now().isoformat(),
                "workflow_version": "2.0"
            }
            
            # Export master data
            master_data_path = f"arima_master_data_{risk_profile}.json"
            self.master_data_exporter.export_arima_master_data(
                arima_result=arima_result,
                prepared_data=prepared_data,
                portfolio_returns=portfolio_returns,
                analysis_metadata=metadata,
                output_path=master_data_path
            )
            
            self.logger.info(f"Master data exported to: {master_data_path}")
            return master_data_path
            
        except Exception as e:
            error_msg = f"Master data export failed: {e}"
            self.logger.error(error_msg)
            raise Exception(error_msg)

    def _extract_portfolio_returns_from_analysis(self, analysis_result: Dict[str, Any]) -> pd.Series:
        """Extract portfolio returns from analysis result."""
        
        try:
            # Try to get from analysis result first
            if 'portfolio_returns_series' in analysis_result:
                return analysis_result['portfolio_returns_series']
            
            # Fallback: extract from backtest file
            backtest_file = analysis_result.get('backtest_file', None)
            if backtest_file and Path(backtest_file).exists():
                return self._load_portfolio_returns_from_file(backtest_file)
            
            # Last resort: create from portfolio statistics
            portfolio_stats = analysis_result.get('portfolio_statistics', {})
            self.logger.warning("Creating synthetic portfolio returns from statistics")
            
            n_periods = 100
            mean_return = portfolio_stats.get('mean_daily_return', 0.001)
            std_return = portfolio_stats.get('daily_volatility', 0.02)
            
            returns = np.random.normal(mean_return, std_return, n_periods)
            dates = pd.date_range(end=pd.Timestamp.now(), periods=n_periods, freq='D')
            
            return pd.Series(returns, index=dates, name='Portfolio_Returns')
            
        except Exception as e:
            self.logger.error(f"Failed to extract portfolio returns: {e}")
            raise

    def _load_portfolio_returns_from_file(self, backtest_file: str) -> pd.Series:
        """Load portfolio returns directly from backtest file."""
        
        try:
            # Load backtest data
            if backtest_file.endswith('.xlsx'):
                data = pd.read_excel(backtest_file, sheet_name='Backtest Results')
            else:
                data = pd.read_csv(backtest_file)
            
            # Extract portfolio returns
            if 'Daily_Return' in data.columns and 'Timestamp' in data.columns:
                data['Timestamp'] = pd.to_datetime(data['Timestamp'])
                portfolio_returns = pd.Series(
                    data['Daily_Return'].values,
                    index=data['Timestamp'],
                    name='Portfolio_Returns'
                )
                
                # Clean data
                portfolio_returns = portfolio_returns.dropna()
                portfolio_returns = portfolio_returns[portfolio_returns != 0]  # Remove zero returns
                
                self.logger.info(f"Loaded {len(portfolio_returns)} portfolio returns from file")
                return portfolio_returns
            else:
                raise ValueError("Required columns not found in backtest file")
                
        except Exception as e:
            self.logger.error(f"Failed to load portfolio returns from file: {e}")
            raise

    def _apply_forecasting_and_monitor(
        self,
        master_data_path: str,
        backtest_file: str,
        output_path: str,
        forecast_steps: int,
        export_format: str
    ) -> Dict[str, Any]:
        """Apply forecasting using master data and generate monitoring report."""
        
        try:
            # Step 3a: Load master data into applicator
            self.applicator.load_master_data(master_data_path)
            
            # Step 3b: Load historical portfolio returns
            portfolio_returns = self._load_portfolio_returns_from_file(backtest_file)
            
            # Step 3c: Generate complete time series data
            time_series_df = self.applicator.generate_time_series_data(
                historical_returns=portfolio_returns,
                include_forecasts=True,
                forecast_steps=forecast_steps
            )
            
            # Step 3d: Load master data for reporter
            with open(master_data_path, 'r') as f:
                master_data = json.load(f)
            
            # Step 3e: Create master data DataFrame for reporter
            master_data_df = self._convert_master_data_to_dataframe(master_data)
            
            # Step 3f: Export using reporter
            if export_format.lower() == 'xlsx':
                self._export_to_excel(time_series_df, master_data_df, output_path)
            else:
                self._export_to_csv(time_series_df, master_data_df, output_path)
            
            # Step 3g: Generate summary metrics
            summary_metrics = self._calculate_monitoring_summary(time_series_df, master_data)
            
            return {
                'time_series_data': time_series_df,
                'master_data': master_data,
                'summary_metrics': summary_metrics,
                'export_path': output_path,
                'export_format': export_format
            }
            
        except Exception as e:
            error_msg = f"Forecasting and monitoring failed: {e}"
            self.logger.error(error_msg)
            raise Exception(error_msg)

    def _convert_master_data_to_dataframe(self, master_data: Dict[str, Any]) -> pd.DataFrame:
        """Convert master data JSON to DataFrame for reporting."""
        
        master_data_rows = []
        
        # Basic model information
        model_spec = master_data["model_specification"]
        master_data_rows.extend([
            ["Parameter", "Value", "Description"],
            ["=== MODEL SPECIFICATION ===", "", ""],
            ["ARIMA Order", f"({model_spec['arima_order']['p']},{model_spec['arima_order']['d']},{model_spec['arima_order']['q']})", "Model specification"],
            ["AIC", model_spec["aic"], "Akaike Information Criterion"],
            ["Forecast Ready", model_spec["forecast_ready"], "Model ready for forecasting"],
            ["", "", ""]
        ])
        
        # Data preparation info
        data_prep = master_data["data_preparation"]
        original_data = data_prep["original_data"]
        preparation_applied = data_prep["preparation_applied"]
        
        master_data_rows.extend([
            ["=== DATA INFORMATION ===", "", ""],
            ["Total Observations", original_data["total_observations"], "Number of historical observations"],
            ["Mean Value", preparation_applied["mean_value_removed"], "Mean removed during preparation"],
            ["Is Differenced", preparation_applied["is_differenced"], "Whether data was differenced"],
            ["Data Start Date", original_data["data_start_date"], "First observation date"],
            ["Data End Date", original_data["data_end_date"], "Last observation date"],
            ["", "", ""]
        ])
        
        # Model coefficients
        coefficients = master_data["model_coefficients"]
        ar_coeffs = coefficients["ar_coefficients"]
        ma_coeffs = coefficients["ma_coefficients"]
        
        master_data_rows.append(["=== AR COEFFICIENTS ===", "", ""])
        if ar_coeffs:
            for ar_name, ar_value in ar_coeffs.items():
                master_data_rows.append([ar_name, ar_value, f"Autoregressive coefficient {ar_name}"])
        else:
            master_data_rows.append(["AR Coefficients", "None significant", "No significant AR terms"])
        
        master_data_rows.append(["", "", ""])
        master_data_rows.append(["=== MA COEFFICIENTS ===", "", ""])
        if ma_coeffs:
            for ma_name, ma_value in ma_coeffs.items():
                master_data_rows.append([ma_name, ma_value, f"Moving average coefficient {ma_name}"])
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

    def _export_to_excel(self, time_series_df: pd.DataFrame, master_data_df: pd.DataFrame, output_path: str):
        """Export to Excel with both time series and master data sheets."""
        
        try:
            with pd.ExcelWriter(output_path, engine='openpyxl') as writer:
                # Time series data sheet
                time_series_df.to_excel(writer, sheet_name='Time Series Data', index=False)
                
                # Master data sheet
                master_data_df.to_excel(writer, sheet_name='Master Data', index=False)
            
            self.logger.info(f"Monitoring report exported to Excel: {output_path}")
            
        except ImportError:
            self.logger.warning("openpyxl not available, falling back to CSV")
            self._export_to_csv(time_series_df, master_data_df, output_path)
        except Exception as e:
            self.logger.error(f"Excel export failed: {e}")
            raise

    def _export_to_csv(self, time_series_df: pd.DataFrame, master_data_df: pd.DataFrame, base_path: str):
        """Export to separate CSV files."""
        
        base_path = Path(base_path)
        base_name = base_path.stem
        base_dir = base_path.parent
        
        # Export time series data
        time_series_path = base_dir / f"{base_name}_time_series.csv"
        time_series_df.to_csv(time_series_path, index=False, float_format='%.8f')
        
        # Export master data
        master_data_path = base_dir / f"{base_name}_master_data.csv"
        master_data_df.to_csv(master_data_path, index=False)
        
        self.logger.info(f"Monitoring report exported to CSV:")
        self.logger.info(f"  Time Series: {time_series_path}")
        self.logger.info(f"  Master Data: {master_data_path}")

    def _calculate_monitoring_summary(self, time_series_df: pd.DataFrame, 
                                    master_data: Dict[str, Any]) -> Dict[str, Any]:
        """Calculate summary metrics for monitoring."""
        
        # Separate historical and forecast data
        historical_data = time_series_df.dropna(subset=['Return'])
        forecast_data = time_series_df[time_series_df['Return'].isna()]
        
        summary = {
            'data_summary': {
                'total_periods': len(time_series_df),
                'historical_periods': len(historical_data),
                'forecast_periods': len(forecast_data),
                'data_start_date': time_series_df['Date'].iloc[0].isoformat() if len(time_series_df) > 0 else None,
                'data_end_date': time_series_df['Date'].iloc[-1].isoformat() if len(time_series_df) > 0 else None
            },
            'model_summary': {
                'arima_order': master_data['model_specification']['arima_order'],
                'aic': master_data['model_specification']['aic'],
                'mean_value': master_data['forecasting_parameters']['mean_value'],
                'ar_coefficients_count': len(master_data['model_coefficients']['ar_coefficients']),
                'ma_coefficients_count': len(master_data['model_coefficients']['ma_coefficients'])
            }
        }
        
        # Calculate forecast accuracy if historical data available
        if len(historical_data) > 1:
            actual_returns = historical_data['Return']
            predicted_returns = historical_data['Prd Return']
            
            errors = np.abs(actual_returns - predicted_returns)
            summary['forecast_performance'] = {
                'mean_absolute_error': errors.mean(),
                'root_mean_square_error': np.sqrt(np.mean(errors ** 2)),
                'max_error': errors.max(),
                'forecast_accuracy_pct': max(0, (1 - errors.mean() / actual_returns.std()) * 100)
            }
        
        return summary

    def _handle_no_forecast_case(self, arima_analysis_result: Dict[str, Any], 
                                output_path: str) -> Dict[str, Any]:
        """Handle case when forecasting is not viable."""
        
        self.logger.warning("ARIMA analysis indicates forecasting is not viable")
        
        # Create summary-only report
        analysis_summary = arima_analysis_result['analysis_summary']
        recommendations = arima_analysis_result.get('trading_insights', {}).get('integration_recommendations', [])
        
        summary_data = [
            ['Parameter', 'Value', 'Description'],
            ['=== ANALYSIS RESULTS ===', '', ''],
            ['Forecast Ready', False, 'Model cannot generate reliable forecasts'],
            ['ARIMA Order', str(analysis_summary['optimal_order']), 'Optimal model found'],
            ['AIC', analysis_summary['best_aic'], 'Model quality metric'],
            ['Significant Coefficients', analysis_summary['num_significant_coefficients'], 'Number of significant parameters'],
            ['', '', ''],
            ['=== RECOMMENDATIONS ===', '', '']
        ]
        
        for i, rec in enumerate(recommendations[:5], 1):  # Limit to 5 recommendations
            summary_data.append([f'Recommendation {i}', rec, 'Analysis recommendation'])
        
        # Export summary
        summary_df = pd.DataFrame(summary_data[1:], columns=summary_data[0])
        
        if output_path.endswith('.xlsx'):
            summary_df.to_excel(output_path, sheet_name='Analysis Summary', index=False)
        else:
            summary_df.to_csv(output_path, index=False)
        
        return {
            'status': 'no_forecast',
            'workflow_completed': True,
            'forecast_ready': False,
            'analysis_summary': analysis_summary,
            'recommendations': recommendations,
            'output_path': output_path
        }

    def run_batch_monitoring(
        self,
        backtest_files: List[str],
        risk_profiles: List[str] = ["neutral", "averse", "lover"],
        output_dir: str = "batch_arima_monitoring",
        forecast_steps: int = 5
    ) -> Dict[str, Any]:
        """
        Run batch monitoring for multiple files and risk profiles.
        
        Args:
            backtest_files: List of backtest result files
            risk_profiles: List of risk profiles to analyze
            output_dir: Directory for batch output
            forecast_steps: Number of forecast steps
            
        Returns:
            Batch processing results
        """
        
        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)
        
        batch_results = {
            'successful': [],
            'failed': [],
            'summary': {}
        }
        
        self.logger.info(f"Starting batch monitoring: {len(backtest_files)} files × {len(risk_profiles)} profiles")
        
        for backtest_file in backtest_files:
            file_name = Path(backtest_file).stem
            
            for risk_profile in risk_profiles:
                try:
                    output_file = output_path / f"{file_name}_{risk_profile}_monitoring.xlsx"
                    
                    result = self.generate_complete_monitoring_report(
                        backtest_file=backtest_file,
                        risk_profile=risk_profile,
                        output_path=str(output_file),
                        forecast_steps=forecast_steps
                    )
                    
                    if result['status'] in ['success', 'no_forecast']:
                        batch_results['successful'].append({
                            'file': backtest_file,
                            'profile': risk_profile,
                            'output': str(output_file),
                            'status': result['status']
                        })
                    else:
                        batch_results['failed'].append({
                            'file': backtest_file,
                            'profile': risk_profile,
                            'error': result.get('error', 'Unknown error')
                        })
                        
                except Exception as e:
                    batch_results['failed'].append({
                        'file': backtest_file,
                        'profile': risk_profile,
                        'error': str(e)
                    })
        
        # Create summary
        total_combinations = len(backtest_files) * len(risk_profiles)
        batch_results['summary'] = {
            'total_combinations': total_combinations,
            'successful_count': len(batch_results['successful']),
            'failed_count': len(batch_results['failed']),
            'success_rate': len(batch_results['successful']) / total_combinations if total_combinations > 0 else 0,
            'output_directory': str(output_path)
        }
        
        self.logger.info(f"Batch monitoring completed: {batch_results['summary']['successful_count']}/{total_combinations} successful")
        
        return batch_results

# Example usage and testing
def test_updated_monitoring_system():
    """Test the updated monitoring system."""
    
    logger = CustomLogger('UpdatedMonitoringTest')
    
    try:
        # Initialize system
        monitor = UpdatedArimaMonitoring(logger)
        
        # Test with sample file
        sample_files = [
            "backtest_results_neutral.xlsx",
            "../backtest_results_neutral.xlsx",
            "../../backtest_results_neutral.xlsx"
        ]
        
        backtest_file = None
        for file_path in sample_files:
            if Path(file_path).exists():
                backtest_file = file_path
                break
        
        if backtest_file:
            logger.info(f"Testing with: {backtest_file}")
            
            result = monitor.generate_complete_monitoring_report(
                backtest_file=backtest_file,
                risk_profile="neutral",
                output_path="test_updated_monitoring.xlsx",
                forecast_steps=5
            )
            
            logger.info(f"Test result: {result['status']}")
            if result['status'] == 'success':
                logger.info("✅ Updated monitoring system test successful!")
                logger.info(f"📁 Files: {result['files_generated']}")
            
        else:
            logger.info("No backtest file found - would need actual data for testing")
            
    except Exception as e:
        logger.error(f"Test failed: {e}")

if __name__ == "__main__":
    test_updated_monitoring_system()