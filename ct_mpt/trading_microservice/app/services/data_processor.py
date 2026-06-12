"""
Data Processing Service
======================

Handles data loading, validation, and preprocessing for both portfolio optimization
and trading simulation.
"""

import pandas as pd
import numpy as np
from typing import Dict, List, Tuple, Optional
from pathlib import Path
import tempfile
import os

from app.core.exceptions import (
    DataProcessingError,
    ValidationError,
    FileProcessingError,
    InsufficientDataError
)
from app.utils.logger import get_logger
from app.core.config import settings

logger = get_logger(__name__)


class DataProcessor:
    """Data processing service for trading operations."""

    def __init__(self):
        self.temp_dir = Path(tempfile.mkdtemp())

    def validate_file(self, file_path: str, file_type: str = "prediction") -> Dict[str, any]:
        """
        Validate uploaded file for required columns and data quality.

        Args:
            file_path: Path to the file
            file_type: Type of file ('prediction' or 'backtest_results')

        Returns:
            Validation results dictionary
        """
        try:
            # Check file exists and size
            path = Path(file_path)
            if not path.exists():
                raise FileProcessingError(f"File not found: {file_path}")

            file_size = path.stat().st_size
            if file_size > settings.MAX_FILE_SIZE:
                raise FileProcessingError(
                    f"File too large: {file_size} bytes (max: {settings.MAX_FILE_SIZE})"
                )

            # Load file
            if file_path.endswith('.xlsx'):
                df = pd.read_excel(file_path)
            elif file_path.endswith('.csv'):
                df = pd.read_csv(file_path)
            else:
                raise FileProcessingError("Unsupported file format. Use .xlsx or .csv")

            # Check data size
            if len(df) > settings.MAX_DATA_ROWS:
                raise FileProcessingError(
                    f"Too many rows: {len(df)} (max: {settings.MAX_DATA_ROWS})"
                )

            # Validate based on file type
            if file_type == "prediction":
                return self._validate_prediction_file(df, file_path)
            elif file_type == "backtest_results":
                return self._validate_backtest_results(df, file_path)
            else:
                raise ValidationError(f"Unknown file type: {file_type}")

        except Exception as e:
            if isinstance(e, (DataProcessingError, ValidationError, FileProcessingError)):
                raise
            raise DataProcessingError(f"File validation failed: {str(e)}")

    def _validate_prediction_file(self, df: pd.DataFrame, file_path: str) -> Dict[str, any]:
        """Validate ARIMA prediction file."""
        # Extract asset code from filename
        filename = Path(file_path).stem
        if "_with_predictions" in filename.lower():
            asset_code = filename.split("_with_predictions")[0].upper()
        elif "_prediction" in filename.lower():
            asset_code = filename.split("_prediction")[0].upper()
        else:
            raise ValidationError("Cannot extract asset code from filename")

        # Required columns for prediction files
        required_cols = [
            'Timestamp',
            'Open_Price',
            f'Close_Price_{asset_code}',
            f'Prd_Return_Arima_{asset_code}'
        ]

        missing_cols = [col for col in required_cols if col not in df.columns]
        if missing_cols:
            raise ValidationError(
                f"Missing required columns for {asset_code}: {missing_cols}",
                details={"available_columns": list(df.columns)}
            )

        # Check prediction data
        pred_col = f'Prd_Return_Arima_{asset_code}'
        pred_count = df[pred_col].notna().sum()

        if pred_count == 0:
            raise InsufficientDataError(f"No ARIMA predictions found in {pred_col}")

        # Validate timestamp column
        try:
            df['Timestamp'] = pd.to_datetime(df['Timestamp'])
        except Exception:
            raise ValidationError("Invalid timestamp format in Timestamp column")

        return {
            "status": "valid",
            "asset_code": asset_code,
            "total_rows": int(len(df)),  # Convert to Python int
            "prediction_count": int(pred_count),  # Convert to Python int
            "prediction_coverage": float(pred_count / len(df)),  # Convert to Python float
            "date_range": {
                "start": df['Timestamp'].min().strftime('%Y-%m-%d'),
                "end": df['Timestamp'].max().strftime('%Y-%m-%d')
            },
            "columns": list(df.columns)
        }

    def _validate_backtest_results(self, df: pd.DataFrame, file_path: str) -> Dict[str, any]:
        """Validate backtest results file (from Step 1)."""
        required_cols = [
            'Timestamp', 'BTC_Weight', 'ETH_Weight', 'Risky_Weight',
            'Strategy', 'Daily_Return'
        ]

        missing_cols = [col for col in required_cols if col not in df.columns]
        if missing_cols:
            # Check for price columns
            price_columns_found = []
            for asset in ['BTC', 'ETH']:
                for col in df.columns:
                    if asset in col and ('open' in col.lower() or 'close' in col.lower()):
                        price_columns_found.append(col)

            if not price_columns_found:
                missing_cols.append("Price columns (BTC/ETH open/close)")

            if missing_cols:
                raise ValidationError(
                    f"Missing required columns: {missing_cols}",
                    details={"available_columns": list(df.columns)}
                )

        # Validate numeric columns
        numeric_cols = ['BTC_Weight', 'ETH_Weight', 'Risky_Weight', 'Daily_Return']
        for col in numeric_cols:
            if col in df.columns:
                if not pd.api.types.is_numeric_dtype(df[col]):
                    raise ValidationError(f"Column {col} must be numeric")

        # Validate strategy values
        if 'Strategy' in df.columns:
            valid_strategies = ['Long', 'Short', 'Market_neutral', 'No Investment']
            invalid_strategies = set(df['Strategy'].unique()) - set(valid_strategies)
            if invalid_strategies:
                logger.warning(f"Unknown strategies found: {invalid_strategies}")

        return {
            "status": "valid",
            "total_rows": int(len(df)),  # Convert to Python int
            "columns": list(df.columns),
            "strategy_distribution": {k: int(v) for k, v in df['Strategy'].value_counts().to_dict().items()} if 'Strategy' in df.columns else {},  # Convert values to Python int
            "date_range": {
                "start": df['Timestamp'].min().strftime('%Y-%m-%d') if 'Timestamp' in df.columns else None,
                "end": df['Timestamp'].max().strftime('%Y-%m-%d') if 'Timestamp' in df.columns else None
            }
        }

    def load_and_combine_prediction_data(
        self,
        asset_files: Dict[str, str],
        date_range: Optional[Tuple[str, str]] = None
    ) -> pd.DataFrame:
        """
        Load and combine multiple prediction files.

        Args:
            asset_files: Dictionary mapping asset codes to file paths
            date_range: Optional tuple of (start_date, end_date) strings

        Returns:
            Combined DataFrame with all assets
        """
        combined_data = None

        try:
            for asset_code, file_path in asset_files.items():
                logger.info(f"Loading {asset_code} from {Path(file_path).name}")

                # Load file
                if file_path.endswith('.xlsx'):
                    df = pd.read_excel(file_path)
                else:
                    df = pd.read_csv(file_path)

                # Convert timestamp
                df['Timestamp'] = pd.to_datetime(df['Timestamp'])

                # Select essential columns
                essential_cols = [
                    'Timestamp',
                    'Open_Price',
                    f'Close_Price_{asset_code}',
                    f'Prd_Return_Arima_{asset_code}'
                ]

                missing_cols = [col for col in essential_cols if col not in df.columns]
                if missing_cols:
                    raise DataProcessingError(
                        f"Missing columns in {asset_code}: {missing_cols}"
                    )

                # Create asset-specific Open_Price column
                df[f'Open_Price_{asset_code}'] = df['Open_Price']

                # Select final columns
                final_cols = [
                    'Timestamp',
                    f'Open_Price_{asset_code}',
                    f'Close_Price_{asset_code}',
                    f'Prd_Return_Arima_{asset_code}'
                ]
                df = df[final_cols]

                # Merge with combined data
                if combined_data is None:
                    combined_data = df
                else:
                    combined_data = pd.merge(combined_data, df, on='Timestamp', how='inner')

            # Sort by timestamp
            combined_data = combined_data.sort_values('Timestamp').reset_index(drop=True)

            # Calculate returns for all assets
            for asset_code in asset_files.keys():
                open_col = f'Open_Price_{asset_code}'
                close_col = f'Close_Price_{asset_code}'
                return_col = f'{asset_code}_Return'

                # Calculate intraday returns: (Close - Open) / Open
                combined_data[return_col] = (
                    combined_data[close_col] - combined_data[open_col]
                ) / combined_data[open_col]

            # Remove rows with NaN returns
            return_cols = [f'{asset}_Return' for asset in asset_files.keys()]
            combined_data = combined_data.dropna(subset=return_cols).reset_index(drop=True)

            # Apply date filter if specified
            if date_range:
                start_date, end_date = date_range
                start_date = pd.to_datetime(start_date)
                end_date = pd.to_datetime(end_date)

                date_mask = (
                    (combined_data['Timestamp'] >= start_date) &
                    (combined_data['Timestamp'] <= end_date)
                )
                combined_data = combined_data[date_mask].reset_index(drop=True)

                if len(combined_data) == 0:
                    raise InsufficientDataError(
                        f"No data found in date range {start_date.strftime('%Y-%m-%d')} "
                        f"to {end_date.strftime('%Y-%m-%d')}"
                    )

            logger.info(f"Combined data: {len(combined_data)} rows")
            return combined_data

        except Exception as e:
            if isinstance(e, (DataProcessingError, InsufficientDataError)):
                raise
            raise DataProcessingError(f"Failed to combine prediction data: {str(e)}")

    def find_first_prediction_index(self, data: pd.DataFrame, asset_codes: List[str]) -> int:
        """
        Find the first index where all assets have predictions.

        Args:
            data: Combined DataFrame
            asset_codes: List of asset codes

        Returns:
            Index of first complete prediction
        """
        first_pred_index = None

        for asset in asset_codes:
            pred_col = f'Prd_Return_Arima_{asset}'
            if pred_col not in data.columns:
                raise DataProcessingError(f"Prediction column not found: {pred_col}")

            valid_mask = data[pred_col].notna()
            if valid_mask.any():
                asset_first_index = valid_mask.idxmax()
                if first_pred_index is None or asset_first_index > first_pred_index:
                    first_pred_index = asset_first_index

        if first_pred_index is None:
            raise InsufficientDataError("No valid predictions found for any asset")

        return first_pred_index

    def save_temp_file(self, content: bytes, filename: str) -> str:
        """Save uploaded file content to temporary location."""
        temp_path = self.temp_dir / filename
        with open(temp_path, 'wb') as f:
            f.write(content)
        return str(temp_path)

    def cleanup_temp_files(self):
        """Clean up temporary files."""
        try:
            for file_path in self.temp_dir.glob("*"):
                file_path.unlink()
            self.temp_dir.rmdir()
        except Exception as e:
            logger.warning(f"Failed to cleanup temp files: {str(e)}")