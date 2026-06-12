"""
Input Validation Utilities
==========================

Common validation functions for API inputs.
"""

import pandas as pd
import numpy as np
from typing import List, Dict, Any, Optional
from datetime import datetime
import re

from app.core.exceptions import ValidationError


class InputValidator:
    """Input validation utilities."""

    @staticmethod
    def validate_asset_codes(asset_codes: List[str]) -> None:
        """Validate asset code format."""
        if not asset_codes:
            raise ValidationError("At least one asset code is required")

        if len(asset_codes) > 10:
            raise ValidationError("Maximum 10 asset codes allowed")

        for code in asset_codes:
            if not isinstance(code, str):
                raise ValidationError(f"Asset code must be string, got {type(code)}")

            if not re.match(r'^[A-Z0-9]{2,10}$', code):
                raise ValidationError(
                    f"Invalid asset code format: {code}. "
                    "Must be 2-10 uppercase alphanumeric characters"
                )

    @staticmethod
    def validate_date_range(date_range: Dict[str, str]) -> None:
        """Validate date range format."""
        if not isinstance(date_range, dict):
            raise ValidationError("Date range must be a dictionary")

        required_keys = ['start', 'end']
        for key in required_keys:
            if key not in date_range:
                raise ValidationError(f"Missing required key: {key}")

        try:
            start_date = pd.to_datetime(date_range['start'])
            end_date = pd.to_datetime(date_range['end'])
        except Exception as e:
            raise ValidationError(f"Invalid date format: {str(e)}")

        if start_date >= end_date:
            raise ValidationError("Start date must be before end date")

        # Check reasonable date range (not more than 10 years)
        if (end_date - start_date).days > 3650:
            raise ValidationError("Date range cannot exceed 10 years")

    @staticmethod
    def validate_file_content(content: bytes, filename: str) -> None:
        """Validate uploaded file content."""
        if len(content) == 0:
            raise ValidationError(f"File {filename} is empty")

        # Check if content looks like valid file format
        if filename.endswith('.xlsx'):
            # Check for Excel magic bytes
            if not content[:8] == b'PK\x03\x04\x14\x00\x06\x00':
                raise ValidationError(f"File {filename} does not appear to be valid Excel format")

        elif filename.endswith('.csv'):
            # Basic CSV validation
            try:
                content_str = content.decode('utf-8')
                if len(content_str.strip()) == 0:
                    raise ValidationError(f"CSV file {filename} is empty")
            except UnicodeDecodeError:
                raise ValidationError(f"CSV file {filename} has invalid encoding")

    @staticmethod
    def validate_trading_parameters(
        total_capital: float,
        trading_portion: float,
        trading_fee: float,
        leverage_scale: float
    ) -> None:
        """Validate trading simulation parameters."""
        if total_capital <= 0:
            raise ValidationError("Total capital must be positive")

        if not (0.1 <= trading_portion <= 0.9):
            raise ValidationError("Trading portion must be between 0.1 and 0.9")

        if not (0 <= trading_fee <= 0.01):
            raise ValidationError("Trading fee must be between 0% and 1%")

        if not (0.1 <= leverage_scale <= 10.0):
            raise ValidationError("Leverage scale must be between 0.1 and 10.0")

        # Check minimum trading balance
        trading_balance = total_capital * trading_portion
        if trading_balance < 100:
            raise ValidationError("Minimum trading balance is $100")

    @staticmethod
    def validate_optimization_parameters(
        lookback_period: int,
        rebalance_frequency: int,
        smart_grid_precision: Optional[int] = None
    ) -> None:
        """Validate portfolio optimization parameters."""
        if not (5 <= lookback_period <= 30):
            raise ValidationError("Lookback period must be between 5 and 30 days")

        if not (1 <= rebalance_frequency <= 30):
            raise ValidationError("Rebalance frequency must be between 1 and 30 days")

        if smart_grid_precision is not None:
            if not (1 <= smart_grid_precision <= 4):
                raise ValidationError("Smart grid precision must be between 1 and 4")

        # Check logical consistency
        if rebalance_frequency > lookback_period:
            raise ValidationError(
                "Rebalance frequency cannot be greater than lookback period"
            )


class DataFrameValidator:
    """DataFrame validation utilities."""

    @staticmethod
    def validate_prediction_data(df: pd.DataFrame, asset_code: str) -> Dict[str, Any]:
        """Validate ARIMA prediction data format."""
        required_columns = [
            'Timestamp',
            'Open_Price',
            f'Close_Price_{asset_code}',
            f'Prd_Return_Arima_{asset_code}'
        ]

        validation_result = {
            'is_valid': True,
            'errors': [],
            'warnings': [],
            'asset_code': asset_code,
            'total_rows': len(df),
            'columns': list(df.columns)
        }

        # Check required columns
        missing_cols = [col for col in required_columns if col not in df.columns]
        if missing_cols:
            validation_result['errors'].append(f"Missing columns: {missing_cols}")
            validation_result['is_valid'] = False

        # Validate timestamp column
        if 'Timestamp' in df.columns:
            try:
                pd.to_datetime(df['Timestamp'])
            except Exception:
                validation_result['errors'].append("Invalid timestamp format")
                validation_result['is_valid'] = False

        # Check prediction data availability
        pred_col = f'Prd_Return_Arima_{asset_code}'
        if pred_col in df.columns:
            pred_count = df[pred_col].notna().sum()
            validation_result['prediction_count'] = pred_count
            validation_result['prediction_coverage'] = pred_count / len(df)

            if pred_count == 0:
                validation_result['errors'].append("No ARIMA predictions found")
                validation_result['is_valid'] = False
            elif pred_count < len(df) * 0.5:
                validation_result['warnings'].append(
                    f"Low prediction coverage: {pred_count}/{len(df)} ({pred_count/len(df)*100:.1f}%)"
                )

        # Validate numeric columns
        numeric_cols = ['Open_Price', f'Close_Price_{asset_code}', f'Prd_Return_Arima_{asset_code}']
        for col in numeric_cols:
            if col in df.columns:
                if not pd.api.types.is_numeric_dtype(df[col]):
                    validation_result['errors'].append(f"Column {col} must be numeric")
                    validation_result['is_valid'] = False

                # Check for extreme values
                if col.endswith('_Price'):
                    if (df[col] <= 0).any():
                        validation_result['errors'].append(f"Price column {col} contains non-positive values")
                        validation_result['is_valid'] = False

        return validation_result

    @staticmethod
    def validate_backtest_results(df: pd.DataFrame) -> Dict[str, Any]:
        """Validate backtest results data format."""
        required_columns = [
            'Timestamp', 'BTC_Weight', 'ETH_Weight', 'Risky_Weight',
            'Strategy', 'Daily_Return'
        ]

        validation_result = {
            'is_valid': True,
            'errors': [],
            'warnings': [],
            'total_rows': len(df),
            'columns': list(df.columns)
        }

        # Check required columns
        missing_cols = [col for col in required_columns if col not in df.columns]
        if missing_cols:
            validation_result['errors'].append(f"Missing columns: {missing_cols}")
            validation_result['is_valid'] = False

        # Validate numeric columns
        numeric_cols = ['BTC_Weight', 'ETH_Weight', 'Risky_Weight', 'Daily_Return']
        for col in numeric_cols:
            if col in df.columns:
                if not pd.api.types.is_numeric_dtype(df[col]):
                    validation_result['errors'].append(f"Column {col} must be numeric")
                    validation_result['is_valid'] = False

                # Check for NaN values
                if df[col].isna().sum() > 0:
                    validation_result['warnings'].append(f"Column {col} contains NaN values")

        # Check strategy values
        if 'Strategy' in df.columns:
            valid_strategies = ['Long', 'Short', 'Market_neutral', 'No Investment']
            invalid_strategies = set(df['Strategy'].unique()) - set(valid_strategies)
            if invalid_strategies:
                validation_result['warnings'].append(
                    f"Unknown strategies found: {invalid_strategies}"
                )

        # Check for price columns
        price_columns = []
        for asset in ['BTC', 'ETH']:
            for col in df.columns:
                if asset in col and ('open' in col.lower() or 'close' in col.lower()):
                    price_columns.append(col)

        if not price_columns:
            validation_result['errors'].append("No price columns found (BTC/ETH open/close)")
            validation_result['is_valid'] = False
        else:
            validation_result['price_columns'] = price_columns

        return validation_result