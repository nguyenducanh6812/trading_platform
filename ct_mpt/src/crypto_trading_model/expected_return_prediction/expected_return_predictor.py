"""
Expected Return Predictor
========================

Main orchestrator for expected return prediction based on OC analysis and ARIMA results.
Follows modular monolith architecture with clear separation of concerns.
"""

import pandas as pd
import numpy as np
import json
import yaml
import logging
from pathlib import Path
from typing import Dict, Any, Optional, List, Union
from dataclasses import dataclass
from datetime import datetime

from .data_preparer import DataPreparer, ARLAllocationError
from .return_calculator import ReturnCalculator, CalculationError  
from .results_writer import ResultsWriter, WriterError

logger = logging.getLogger(__name__)

class ExpectedReturnPredictorError(Exception):
    """Exception raised for expected return prediction errors."""
    pass

@dataclass
class PredictionConfig:
    """Configuration for expected return prediction."""
    oc_file_path: str
    arima_file_path: str
    asset_code: str
    output_file_path: Optional[str] = None  # If None, overwrites input file
    start_date: Optional[str] = None  # If specified, filter data from this date (YYYY-MM-DD format)
    end_date: Optional[str] = None  # If specified, filter data to this date (YYYY-MM-DD format)
    
    def __post_init__(self):
        """Validate configuration after initialization."""
        if not Path(self.oc_file_path).exists():
            raise ExpectedReturnPredictorError(f"OC analysis file not found: {self.oc_file_path}")
        if not Path(self.arima_file_path).exists():
            raise ExpectedReturnPredictorError(f"ARIMA results file not found: {self.arima_file_path}")

@dataclass 
class PredictionResults:
    """Results from expected return prediction."""
    asset_code: str
    ar_lag_order: int  # Number of AR lags used
    total_data_points: int
    valid_predictions: int
    prediction_summary: Dict[str, Any]
    output_file_path: str
    calculation_timestamp: str
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert results to dictionary for JSON serialization."""
        return {
            'asset_code': self.asset_code,
            'ar_lag_order': self.ar_lag_order,
            'total_data_points': self.total_data_points,
            'valid_predictions': self.valid_predictions,
            'prediction_summary': self.prediction_summary,
            'output_file_path': self.output_file_path,
            'calculation_timestamp': self.calculation_timestamp
        }

class ExpectedReturnPredictor:
    """
    Main expected return predictor.
    Orchestrates the full prediction workflow from data preparation to results writing.
    """
    
    def __init__(self):
        """Initialize the expected return predictor."""
        self.data_preparer = DataPreparer()
        self.return_calculator = ReturnCalculator()
        self.results_writer = ResultsWriter()
        
        # Setup logging
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        )
    
    def predict_returns(self, config: PredictionConfig) -> PredictionResults:
        """
        Perform complete expected return prediction workflow.
        
        Args:
            config: Prediction configuration
            
        Returns:
            PredictionResults containing analysis results and file paths
            
        Raises:
            ExpectedReturnPredictorError: If prediction workflow fails
        """
        try:
            logger.info("=" * 60)
            logger.info("Starting Expected Return Prediction")
            logger.info("=" * 60)
            logger.info(f"Asset: {config.asset_code}")
            logger.info(f"OC Analysis File: {config.oc_file_path}")
            logger.info(f"ARIMA Results File: {config.arima_file_path}")
            if config.start_date and config.end_date:
                logger.info(f"Date Range Mode: {config.start_date} to {config.end_date}")
            else:
                logger.info("Mode: Full dataset prediction")
            
            # Step 1: Load ARIMA coefficients
            logger.info("Step 1: Loading ARIMA coefficients...")
            arima_coefficients = self._load_arima_coefficients(config.arima_file_path)
            ar_lag_order = len([k for k in arima_coefficients.keys() if k.startswith('ar.L')])
            logger.info(f"Found {ar_lag_order} AR lag coefficients")
            
            # Step 2: Load OC analysis data
            logger.info("Step 2: Loading OC analysis data...")
            oc_data = self._load_oc_analysis_data(config.oc_file_path)
            logger.info(f"Loaded {len(oc_data)} data points from OC analysis")
            
            # Step 3: Prepare AR.L allocation data
            logger.info("Step 3: Preparing AR.L allocation data...")
            prepared_data = self.data_preparer.prepare_ar_lag_data(oc_data, ar_lag_order)
            logger.info(f"Prepared AR lag data with {len(prepared_data)} valid rows")
            
            # Step 3.5: Filter for date range if requested
            if config.start_date and config.end_date:
                logger.info(f"Step 3.5: Filtering data for date range: {config.start_date} to {config.end_date}")
                prepared_data = self._filter_for_date_range(prepared_data, config.start_date, config.end_date)
                logger.info(f"After date filtering: {len(prepared_data)} rows remain")
            
            # Step 3.7: Load configured mean_diff_oc value
            logger.info("Step 3.7: Loading configured mean_diff_oc value...")
            configured_mean_diff_oc = self._load_configured_mean_diff_oc(config.asset_code)
            if configured_mean_diff_oc is not None:
                logger.info(f"Using configured mean_diff_oc for {config.asset_code}: {configured_mean_diff_oc}")
            else:
                logger.info("No configured mean_diff_oc found - calculator will use fallback logic")

            # Step 4: Calculate predicted returns
            logger.info("Step 4: Calculating predicted returns...")
            calculation_results = self.return_calculator.calculate_predictions(
                data=prepared_data,
                arima_coefficients=arima_coefficients,
                ar_lag_order=ar_lag_order,
                configured_mean_diff_oc=configured_mean_diff_oc
            )
            logger.info(f"Calculated {calculation_results['valid_predictions']} predicted returns")
            
            # Step 5: Write results to file
            logger.info("Step 5: Writing results to file...")
            output_path = config.output_file_path or self._generate_prediction_output_path(
                config.oc_file_path, config.asset_code, config.start_date, config.end_date
            )
            written_file = self.results_writer.write_predictions_to_file(
                enhanced_data=calculation_results['enhanced_data'],
                output_path=output_path,
                asset_code=config.asset_code
            )
            logger.info(f"Results written to: {written_file}")
            
            # Step 6: Create summary results
            results = PredictionResults(
                asset_code=config.asset_code,
                ar_lag_order=ar_lag_order,
                total_data_points=len(oc_data),
                valid_predictions=calculation_results['valid_predictions'],
                prediction_summary=calculation_results['summary'],
                output_file_path=written_file,
                calculation_timestamp=datetime.now().isoformat()
            )
            
            logger.info("=" * 60)
            logger.info("Expected Return Prediction Completed Successfully")
            logger.info("=" * 60)
            
            return results
            
        except Exception as e:
            if isinstance(e, (ExpectedReturnPredictorError, ARLAllocationError, CalculationError, WriterError)):
                raise
            else:
                raise ExpectedReturnPredictorError(f"Expected return prediction failed: {str(e)}")
    
    def _load_arima_coefficients(self, arima_file_path: str) -> Dict[str, float]:
        """
        Load ARIMA coefficients from JSON results file.
        
        Args:
            arima_file_path: Path to ARIMA results JSON file
            
        Returns:
            Dictionary of coefficient names to values
            
        Raises:
            ExpectedReturnPredictorError: If loading fails
        """
        try:
            with open(arima_file_path, 'r') as f:
                arima_results = json.load(f)
            
            if 'coefficients' not in arima_results:
                raise ExpectedReturnPredictorError(f"No coefficients found in ARIMA results file")
            
            coefficients = arima_results['coefficients']
            
            # Validate that we have AR coefficients
            ar_coeffs = {k: v for k, v in coefficients.items() if k.startswith('ar.L')}
            if not ar_coeffs:
                raise ExpectedReturnPredictorError("No AR lag coefficients found in ARIMA results")
            
            logger.info(f"Loaded coefficients: {list(coefficients.keys())}")
            return coefficients
            
        except json.JSONDecodeError as e:
            raise ExpectedReturnPredictorError(f"Invalid JSON in ARIMA results file: {str(e)}")
        except Exception as e:
            raise ExpectedReturnPredictorError(f"Failed to load ARIMA coefficients: {str(e)}")
    
    def _load_oc_analysis_data(self, oc_file_path: str) -> pd.DataFrame:
        """
        Load OC analysis data from Excel file.
        
        Args:
            oc_file_path: Path to OC analysis Excel file
            
        Returns:
            DataFrame with OC analysis data
            
        Raises:
            ExpectedReturnPredictorError: If loading fails
        """
        try:
            # Try to read the main data sheet (usually the first sheet)
            df = pd.read_excel(oc_file_path, sheet_name=0)
            
            # Normalize column names to handle asset-specific naming
            df = self._normalize_column_names(df)
            
            # Validate required columns after normalization
            required_columns = ['Demean_Diff_OC', 'OC', 'Open_Price']
            missing_columns = [col for col in required_columns if col not in df.columns]
            
            if missing_columns:
                logger.error(f"Missing required columns: {missing_columns}")
                logger.error(f"Available columns: {list(df.columns)}")
                raise ExpectedReturnPredictorError(
                    f"Missing required columns in OC analysis file: {missing_columns}. "
                    f"Available columns: {list(df.columns)}"
                )
            
            logger.info(f"Successfully loaded OC data with columns: {list(df.columns)}")
            return df
            
        except Exception as e:
            logger.error(f"Failed to load OC analysis data: {str(e)}")
            if isinstance(e, ExpectedReturnPredictorError):
                raise
            else:
                raise ExpectedReturnPredictorError(f"Failed to load OC analysis data: {str(e)}")
    
    def _normalize_column_names(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Normalize column names to handle asset-specific naming conventions.
        
        Args:
            df: DataFrame with potentially asset-specific column names
            
        Returns:
            DataFrame with standardized column names
        """
        import re
        
        logger.info(f"Original columns: {list(df.columns)}")
        
        # Create a copy to avoid modifying the original
        normalized_df = df.copy()
        
        # Define column mapping patterns
        column_patterns = {
            'Open_Price': r'open_price_[a-zA-Z]+|Open_Price_[a-zA-Z]+|open_price|Open_Price',
            'OC': r'^OC$',
            'Demean_Diff_OC': r'Demean_Diff_OC',
            'Mean_Diff_OC': r'Mean_Diff_OC'
        }
        
        # Find and rename columns
        column_mapping = {}
        for standard_name, pattern in column_patterns.items():
            for col in df.columns:
                if re.match(pattern, str(col), re.IGNORECASE):
                    if standard_name not in column_mapping:  # Use first match
                        column_mapping[col] = standard_name
                        logger.info(f"Mapping column '{col}' -> '{standard_name}'")
                        break
        
        # Apply column renaming
        if column_mapping:
            normalized_df = normalized_df.rename(columns=column_mapping)
            logger.info(f"Normalized columns: {list(normalized_df.columns)}")
        else:
            logger.warning("No column mappings applied")
        
        return normalized_df
    
    def _filter_for_date_range(self, data: pd.DataFrame, start_date: str, end_date: str) -> pd.DataFrame:
        """
        Filter the data to only include rows within the specified date range.
        
        Args:
            data: DataFrame with timestamp data
            start_date: Start date in YYYY-MM-DD format (inclusive)
            end_date: End date in YYYY-MM-DD format (inclusive)
            
        Returns:
            DataFrame filtered for the date range
            
        Raises:
            ExpectedReturnPredictorError: If filtering fails or no data found
        """
        try:
            # Convert date strings to datetime for comparison
            start_datetime = datetime.strptime(start_date, "%Y-%m-%d").date()
            end_datetime = datetime.strptime(end_date, "%Y-%m-%d").date()
            logger.info(f"Filtering for date range: {start_datetime} to {end_datetime}")
            
            # Check if we have a Timestamp column
            if 'Timestamp' not in data.columns:
                raise ExpectedReturnPredictorError("No 'Timestamp' column found in data for date filtering")
            
            # Convert Timestamp column to datetime if it's not already
            if not pd.api.types.is_datetime64_any_dtype(data['Timestamp']):
                try:
                    data['Timestamp'] = pd.to_datetime(data['Timestamp'])
                except Exception as e:
                    raise ExpectedReturnPredictorError(f"Failed to convert Timestamp column to datetime: {str(e)}")
            
            # Extract date part from timestamp and filter for range
            data_dates = data['Timestamp'].dt.date
            date_mask = (data_dates >= start_datetime) & (data_dates <= end_datetime)
            filtered_data = data[date_mask].copy()
            
            # Log filtering results
            unique_dates = sorted(data_dates.unique())
            available_start = min(unique_dates) if unique_dates else None
            available_end = max(unique_dates) if unique_dates else None
            
            logger.info(f"Available dates in dataset: {len(unique_dates)} unique dates")
            if available_start and available_end:
                logger.info(f"Dataset date range: {available_start} to {available_end}")
            
            # Count dates within the requested range
            dates_in_range = [d for d in unique_dates if start_datetime <= d <= end_datetime]
            logger.info(f"Requested range {start_date} to {end_date} contains {len(dates_in_range)} unique dates")
            logger.info(f"Rows matching date range: {len(filtered_data)}")
            
            if len(filtered_data) == 0:
                # Show some available dates for reference
                recent_dates_str = ", ".join([str(d) for d in unique_dates[-5:]])  # Show last 5 dates
                raise ExpectedReturnPredictorError(
                    f"No data found for date range {start_date} to {end_date}. "
                    f"Recent available dates: {recent_dates_str}"
                )
            
            logger.info(f"Successfully filtered data to {len(filtered_data)} rows for date range {start_date} to {end_date}")
            return filtered_data
            
        except Exception as e:
            if isinstance(e, ExpectedReturnPredictorError):
                raise
            else:
                raise ExpectedReturnPredictorError(f"Failed to filter data for date range {start_date} to {end_date}: {str(e)}")
    
    def _load_configured_mean_diff_oc(self, asset_code: str) -> Optional[float]:
        """
        Load configured mean_diff_oc value for the specified asset from config.yaml.
        
        Args:
            asset_code: Asset code (e.g., 'BTC', 'ETH')
            
        Returns:
            Configured mean_diff_oc value if found, None otherwise
        """
        try:
            # Find config.yaml file (similar to ConfigManager logic)
            config_locations = [
                Path("config.yaml"),
                Path("../config.yaml"), 
                Path("../../config.yaml"),
            ]
            
            config_path = None
            for config_file in config_locations:
                if config_file.exists():
                    config_path = config_file
                    break
            
            if not config_path:
                logger.warning("config.yaml not found - cannot load configured mean_diff_oc values")
                return None
                
            # Load and parse config.yaml
            with open(config_path, 'r') as file:
                config_data = yaml.safe_load(file)
            
            # Extract OC analysis configuration
            oc_config = config_data.get('oc_analysis', {})
            if not oc_config:
                logger.warning("No oc_analysis section found in config.yaml")
                return None
            
            # Get configured mean_diff_oc values
            configured_means = oc_config.get('mean_diff_oc', {})
            default_mean = oc_config.get('default_mean_diff_oc', 0.0)
            
            # Look for asset-specific value
            if asset_code in configured_means:
                mean_diff_oc = configured_means[asset_code]
                logger.info(f"Found configured mean_diff_oc for {asset_code}: {mean_diff_oc}")
                return mean_diff_oc
            else:
                logger.info(f"No specific configuration for {asset_code}, using default: {default_mean}")
                return default_mean
                
        except Exception as e:
            logger.warning(f"Failed to load configured mean_diff_oc for {asset_code}: {str(e)}")
            return None
    
    def _generate_prediction_output_path(self, oc_file_path: str, asset_code: str, 
                                       start_date: Optional[str] = None, end_date: Optional[str] = None) -> str:
        """
        Generate a general output file path for prediction results that backtester can easily find.
        
        Args:
            oc_file_path: Original OC analysis file path
            asset_code: Asset code (e.g., 'BTC', 'ETH')
            start_date: Optional start date for range predictions
            end_date: Optional end date for range predictions
            
        Returns:
            Path for prediction results file
        """
        try:
            oc_path = Path(oc_file_path)
            
            # Use simple, general naming convention for backtester integration
            if start_date and end_date:
                # For date range predictions: BTC_with_predictions.xlsx (latest version)
                filename = f"{asset_code}_with_predictions.xlsx"
            else:
                # For full dataset predictions: BTC_with_predictions_full.xlsx
                filename = f"{asset_code}_with_predictions_full.xlsx"
            
            # Use same directory as original OC file but with prediction filename
            output_path = oc_path.parent / filename
            
            logger.info(f"Generated prediction output path: {output_path}")
            logger.info(f"Backtester can read predictions from: {output_path}")
            return str(output_path)
            
        except Exception as e:
            logger.warning(f"Failed to generate prediction output path: {str(e)}")
            # Fallback to simple naming
            return oc_file_path.replace('.xlsx', f'_with_predictions.xlsx')
    
    def validate_inputs(self, config: PredictionConfig) -> bool:
        """
        Validate prediction configuration inputs.
        
        Args:
            config: Configuration to validate
            
        Returns:
            True if valid, raises exception if invalid
            
        Raises:
            ExpectedReturnPredictorError: If validation fails
        """
        # File existence is already checked in PredictionConfig.__post_init__
        
        # Validate asset code
        if not config.asset_code or not config.asset_code.strip():
            raise ExpectedReturnPredictorError("Asset code cannot be empty")
        
        # Validate file extensions
        if not config.oc_file_path.endswith(('.xlsx', '.csv')):
            raise ExpectedReturnPredictorError("OC analysis file must be Excel (.xlsx) or CSV (.csv)")
        
        if not config.arima_file_path.endswith('.json'):
            raise ExpectedReturnPredictorError("ARIMA results file must be JSON (.json)")
        
        return True

def create_sample_config(asset_code: str = "BTC") -> PredictionConfig:
    """
    Create a sample configuration for testing.
    
    Args:
        asset_code: Asset code (default: "BTC")
        
    Returns:
        Sample PredictionConfig
    """
    return PredictionConfig(
        oc_file_path=f"oc_analysis_results/{asset_code}_oc_analysis.xlsx",
        arima_file_path=f"oc_analysis_results/{asset_code}_arima_results.json",
        asset_code=asset_code
    )