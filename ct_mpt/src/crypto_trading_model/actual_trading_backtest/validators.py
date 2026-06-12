"""
Validation utilities for actual trading backtest module.

Provides comprehensive validation for inputs and configurations.
"""

import pandas as pd
import numpy as np
from typing import List, Dict, Any, Optional, Union
from .exceptions import DataValidationError, ConfigurationError


class DataValidator:
    """
    Validates input data for actual trading backtest.
    
    Follows Single Responsibility Principle.
    """
    
    @staticmethod
    def validate_backtest_data(data: pd.DataFrame) -> Dict[str, Any]:
        """
        Validate backtest input data.
        
        Args:
            data: Input DataFrame to validate
            
        Returns:
            Dictionary with validation results
            
        Raises:
            DataValidationError: If validation fails
        """
        validation_result = {
            'is_valid': True,
            'errors': [],
            'warnings': [],
            'row_count': len(data),
            'column_count': len(data.columns)
        }
        
        # Check if DataFrame is empty
        if data.empty:
            raise DataValidationError("Input DataFrame is empty")
        
        # Required columns
        required_columns = [
            'BTC_Weight', 'ETH_Weight', 'Risky_Weight', 'Strategy', 'Daily_Return'
        ]
        
        missing_columns = [col for col in required_columns if col not in data.columns]
        if missing_columns:
            raise DataValidationError(missing_columns)
        
        # Check for price columns
        price_validation = DataValidator._validate_price_columns(data)
        validation_result.update(price_validation)
        
        # Validate data types and ranges
        numeric_validations = DataValidator._validate_numeric_columns(data)
        validation_result['errors'].extend(numeric_validations['errors'])
        validation_result['warnings'].extend(numeric_validations['warnings'])
        
        # Check for missing values in critical columns
        missing_value_check = DataValidator._check_missing_values(data, required_columns)
        validation_result['errors'].extend(missing_value_check['errors'])
        validation_result['warnings'].extend(missing_value_check['warnings'])
        
        # Validate strategy values
        strategy_validation = DataValidator._validate_strategy_column(data)
        validation_result['errors'].extend(strategy_validation['errors'])
        validation_result['warnings'].extend(strategy_validation['warnings'])
        
        # Set overall validation status
        validation_result['is_valid'] = len(validation_result['errors']) == 0
        
        return validation_result
    
    @staticmethod
    def _validate_price_columns(data: pd.DataFrame) -> Dict[str, Any]:
        """Validate price columns are present."""
        result = {'price_columns_found': [], 'errors': [], 'warnings': []}
        
        required_assets = ['BTC', 'ETH']
        for asset in required_assets:
            open_found = False
            close_found = False
            
            for col in data.columns:
                if asset in col:
                    if 'open' in col.lower() or 'Open' in col:
                        open_found = True
                        result['price_columns_found'].append(col)
                    elif 'close' in col.lower() or 'Close' in col:
                        close_found = True
                        result['price_columns_found'].append(col)
            
            if not open_found:
                result['errors'].append(f"Missing {asset} open price column")
            if not close_found:
                result['errors'].append(f"Missing {asset} close price column")
        
        return result
    
    @staticmethod
    def _validate_numeric_columns(data: pd.DataFrame) -> Dict[str, Any]:
        """Validate numeric columns have appropriate values."""
        result = {'errors': [], 'warnings': []}
        
        numeric_columns = {
            'BTC_Weight': {'min': -10, 'max': 10},
            'ETH_Weight': {'min': -10, 'max': 10},
            'Risky_Weight': {'min': 0, 'max': 10},
            'Daily_Return': {'min': -1, 'max': 1}
        }
        
        for col, bounds in numeric_columns.items():
            if col in data.columns:
                # Check data type
                if not pd.api.types.is_numeric_dtype(data[col]):
                    result['errors'].append(f"Column {col} must be numeric")
                    continue
                
                # Check for infinite values
                if np.isinf(data[col]).any():
                    result['errors'].append(f"Column {col} contains infinite values")
                
                # Check bounds
                min_val = data[col].min()
                max_val = data[col].max()
                
                if min_val < bounds['min']:
                    result['warnings'].append(
                        f"Column {col} has values below expected range ({min_val:.6f} < {bounds['min']})"
                    )
                
                if max_val > bounds['max']:
                    result['warnings'].append(
                        f"Column {col} has values above expected range ({max_val:.6f} > {bounds['max']})"
                    )
        
        return result
    
    @staticmethod
    def _check_missing_values(data: pd.DataFrame, required_columns: List[str]) -> Dict[str, Any]:
        """Check for missing values in required columns."""
        result = {'errors': [], 'warnings': []}
        
        for col in required_columns:
            if col in data.columns:
                missing_count = data[col].isnull().sum()
                if missing_count > 0:
                    missing_pct = (missing_count / len(data)) * 100
                    if missing_pct > 10:  # More than 10% missing
                        result['errors'].append(
                            f"Column {col} has {missing_count} missing values ({missing_pct:.1f}%)"
                        )
                    else:
                        result['warnings'].append(
                            f"Column {col} has {missing_count} missing values ({missing_pct:.1f}%)"
                        )
        
        return result
    
    @staticmethod
    def _validate_strategy_column(data: pd.DataFrame) -> Dict[str, Any]:
        """Validate strategy column values."""
        result = {'errors': [], 'warnings': []}
        
        if 'Strategy' not in data.columns:
            return result
        
        valid_strategies = ['Long', 'Short', 'Market_neutral', 'No Investment']
        unique_strategies = data['Strategy'].unique()
        
        invalid_strategies = [s for s in unique_strategies if s not in valid_strategies]
        if invalid_strategies:
            result['warnings'].append(
                f"Unknown strategy values found: {invalid_strategies}"
            )
        
        return result


class ConfigValidator:
    """
    Validates configuration objects.
    
    Follows Single Responsibility Principle.
    """
    
    @staticmethod
    def validate_trading_config(config) -> Dict[str, Any]:
        """
        Validate trading configuration.
        
        Args:
            config: ActualTradingConfig object to validate
            
        Returns:
            Dictionary with validation results
            
        Raises:
            ConfigurationError: If validation fails
        """
        from .exceptions import ConfigurationError
        
        validation_result = {
            'is_valid': True,
            'errors': [],
            'warnings': []
        }
        
        # Validate account configuration
        try:
            if config.account.initial_trading_balance <= 0:
                validation_result['errors'].append("Initial trading balance must be positive")
            
            if config.account.initial_saving_balance < 0:
                validation_result['errors'].append("Initial saving balance cannot be negative")
            
            if config.account.target_trading_balance <= 0:
                validation_result['errors'].append("Target trading balance must be positive")
            
            # Warning if target differs significantly from initial
            balance_diff_pct = abs(
                config.account.target_trading_balance - config.account.initial_trading_balance
            ) / config.account.initial_trading_balance * 100
            
            if balance_diff_pct > 50:
                validation_result['warnings'].append(
                    f"Target trading balance differs significantly from initial ({balance_diff_pct:.1f}%)"
                )
        
        except AttributeError as e:
            validation_result['errors'].append(f"Invalid account configuration: {str(e)}")
        
        # Validate trading configuration
        try:
            if not (0 <= config.trading.trading_fee <= 1):
                validation_result['errors'].append("Trading fee must be between 0 and 1")
            
            if config.trading.leverage_scale <= 0:
                validation_result['errors'].append("Leverage scale must be positive")
            
            if config.trading.leverage_scale > 5:
                validation_result['warnings'].append(
                    f"High leverage scale ({config.trading.leverage_scale}) may be risky"
                )
        
        except AttributeError as e:
            validation_result['errors'].append(f"Invalid trading configuration: {str(e)}")
        
        # Validate asset configuration
        try:
            if not (0 <= config.assets.btc_decimal_places <= 8):
                validation_result['errors'].append("BTC decimal places must be between 0 and 8")
            
            if not (0 <= config.assets.eth_decimal_places <= 8):
                validation_result['errors'].append("ETH decimal places must be between 0 and 8")
        
        except AttributeError as e:
            validation_result['errors'].append(f"Invalid asset configuration: {str(e)}")
        
        # Set overall validation status
        validation_result['is_valid'] = len(validation_result['errors']) == 0
        
        if not validation_result['is_valid']:
            raise ConfigurationError(f"Configuration validation failed: {validation_result['errors']}")
        
        return validation_result


class FinancialValidator:
    """
    Validates financial calculations and constraints.
    
    Follows Single Responsibility Principle.
    """
    
    @staticmethod
    def validate_portfolio_weights(
        btc_weight: float,
        eth_weight: float,
        tolerance: float = 1e-6
    ) -> bool:
        """
        Validate that portfolio weights sum approximately to 1 (or -1 for short).
        
        Args:
            btc_weight: BTC weight
            eth_weight: ETH weight
            tolerance: Tolerance for weight sum validation
            
        Returns:
            True if weights are valid
        """
        weight_sum = abs(btc_weight) + abs(eth_weight)
        return abs(weight_sum - 1.0) <= tolerance
    
    @staticmethod
    def validate_price_data(
        open_price: float,
        close_price: float
    ) -> bool:
        """
        Validate price data is reasonable.
        
        Args:
            open_price: Opening price
            close_price: Closing price
            
        Returns:
            True if prices are valid
        """
        if open_price <= 0 or close_price <= 0:
            return False
        
        # Check for extreme price movements (>50% in one day)
        daily_return = abs((close_price - open_price) / open_price)
        return daily_return <= 0.5
    
    @staticmethod
    def validate_return_value(return_value: float) -> bool:
        """
        Validate that return value is reasonable.
        
        Args:
            return_value: Return value to validate
            
        Returns:
            True if return is valid
        """
        # Check for reasonable bounds (between -100% and +1000%)
        return -1.0 <= return_value <= 10.0