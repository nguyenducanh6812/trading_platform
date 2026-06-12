"""
Return Calculator for Expected Return Prediction
===============================================

Implements the calculation formulas for expected return prediction:

1. Prd_Diff_OC = Sigma(Ar.L(i)*Arima.Ar.L(i)) + Mean_Diff_OC
2. Prd_OC(T) = Prd_Diff_OC(T) + OC(T-1)
3. Prd_Return_Arima(T) = Prd_OC(T)/Open(T)
"""

import pandas as pd
import numpy as np
import logging
from typing import Dict, Any, Optional, List

logger = logging.getLogger(__name__)

class CalculationError(Exception):
    """Exception raised for calculation errors."""
    pass

class ReturnCalculator:
    """
    Calculates expected returns using ARIMA coefficients and OC analysis data.
    Implements the three-step calculation process.
    """
    
    def __init__(self):
        """Initialize the return calculator."""
        pass
    
    def calculate_predictions(self, data: pd.DataFrame, arima_coefficients: Dict[str, float], ar_lag_order: int, configured_mean_diff_oc: Optional[float] = None) -> Dict[str, Any]:
        """
        Calculate predicted returns using the full three-step process.
        Only calculates for rows with complete AR lag data.
        
        Args:
            data: DataFrame with AR.L columns and OC analysis data
            arima_coefficients: Dictionary of ARIMA coefficients from analysis
            ar_lag_order: Number of AR lags used
            configured_mean_diff_oc: Optional configured mean_diff_oc value from config.yaml
            
        Returns:
            Dictionary with enhanced data and calculation summary
            
        Raises:
            CalculationError: If calculation fails
        """
        try:
            logger.info("Starting expected return calculation process")
            
            # Create a copy of the data for calculations
            enhanced_data = data.copy()
            
            # Validate AR lag completeness before calculation
            ar_validation = self._validate_ar_lag_completeness(enhanced_data, ar_lag_order)
            logger.info(f"AR lag validation: {ar_validation['complete_rows']} of {ar_validation['total_rows']} rows have complete AR lag data")
            
            if ar_validation['complete_rows'] == 0:
                logger.warning("No rows with complete AR lag data found - skipping calculations")
                # Still return structure but with no valid predictions
                enhanced_data['Prd_Diff_OC'] = np.nan
                enhanced_data['Prd_OC'] = np.nan  
                enhanced_data['Prd_Return_Arima'] = np.nan
                
                return {
                    'enhanced_data': enhanced_data,
                    'summary': {'warning': 'No complete AR lag data available'},
                    'valid_predictions': 0,
                    'ar_lag_order': ar_lag_order
                }
            
            # Step 1: Calculate Prd_Diff_OC (only for rows with complete AR lags)
            logger.info("Step 1: Calculating Prd_Diff_OC...")
            enhanced_data = self._calculate_prd_diff_oc(enhanced_data, arima_coefficients, ar_lag_order, configured_mean_diff_oc)
            
            # Step 2: Calculate Prd_OC  
            logger.info("Step 2: Calculating Prd_OC...")
            enhanced_data = self._calculate_prd_oc(enhanced_data)
            
            # Step 3: Calculate Prd_Return_Arima
            logger.info("Step 3: Calculating Prd_Return_Arima...")
            enhanced_data = self._calculate_prd_return_arima(enhanced_data)
            
            # Generate summary statistics
            summary = self._generate_calculation_summary(enhanced_data)
            
            # Count valid predictions (only from rows with complete AR lags)
            valid_predictions = enhanced_data['Prd_Return_Arima'].notna().sum()
            
            logger.info(f"Calculation completed: {valid_predictions} valid predictions generated from {ar_validation['complete_rows']} rows with complete AR lag data")
            
            return {
                'enhanced_data': enhanced_data,
                'summary': summary,
                'valid_predictions': int(valid_predictions),
                'ar_lag_order': ar_lag_order,
                'ar_lag_validation': ar_validation
            }
            
        except Exception as e:
            if isinstance(e, CalculationError):
                raise
            else:
                raise CalculationError(f"Expected return calculation failed: {str(e)}")
    
    def _validate_ar_lag_completeness(self, data: pd.DataFrame, ar_lag_order: int) -> Dict[str, Any]:
        """
        Validate that we have complete AR lag data for calculation.
        
        Args:
            data: DataFrame with AR.L columns
            ar_lag_order: Expected number of AR lags
            
        Returns:
            Dictionary with validation results
        """
        try:
            # Check for AR.L columns
            ar_columns = [f"Ar.L{i}" for i in range(1, ar_lag_order + 1)]
            
            # Find which columns are present
            present_ar_columns = [col for col in ar_columns if col in data.columns]
            missing_ar_columns = [col for col in ar_columns if col not in data.columns]
            
            # Check for complete AR lag data in each row
            if present_ar_columns:
                # Check if we have the 'has_complete_ar_lags' column from data preparer
                if 'has_complete_ar_lags' in data.columns:
                    complete_rows = data['has_complete_ar_lags'].sum()
                else:
                    # Fallback: check manually for complete AR lag data
                    complete_mask = data[present_ar_columns].notna().all(axis=1)
                    complete_rows = complete_mask.sum()
                    # Add the validation column for future use
                    data['has_complete_ar_lags'] = complete_mask
            else:
                complete_rows = 0
                data['has_complete_ar_lags'] = False
            
            validation_results = {
                'total_rows': len(data),
                'ar_lag_order': ar_lag_order,
                'expected_ar_columns': ar_columns,
                'present_ar_columns': present_ar_columns,
                'missing_ar_columns': missing_ar_columns,
                'complete_rows': int(complete_rows),
                'incomplete_rows': len(data) - int(complete_rows),
                'completeness_ratio': float(complete_rows) / len(data) if len(data) > 0 else 0.0
            }
            
            logger.info(f"AR lag completeness validation:")
            logger.info(f"  Expected AR columns: {len(ar_columns)} (Ar.L1 to Ar.L{ar_lag_order})")
            logger.info(f"  Present AR columns: {len(present_ar_columns)}")
            logger.info(f"  Missing AR columns: {len(missing_ar_columns)}")
            if missing_ar_columns:
                logger.warning(f"  Missing AR columns: {missing_ar_columns}")
            logger.info(f"  Rows with complete AR lag data: {complete_rows}/{len(data)} ({validation_results['completeness_ratio']:.1%})")
            
            return validation_results
            
        except Exception as e:
            logger.error(f"AR lag validation failed: {str(e)}")
            return {
                'total_rows': len(data),
                'complete_rows': 0,
                'error': str(e)
            }
    
    def _calculate_prd_diff_oc(self, data: pd.DataFrame, arima_coefficients: Dict[str, float], ar_lag_order: int, configured_mean_diff_oc: Optional[float] = None) -> pd.DataFrame:
        """
        Calculate Prd_Diff_OC = Sigma(Ar.L(i)*Arima.Ar.L(i)) + Mean_Diff_OC
        
        Args:
            data: DataFrame with AR.L columns
            arima_coefficients: ARIMA coefficients dictionary
            ar_lag_order: Number of AR lags
            configured_mean_diff_oc: Optional configured mean_diff_oc value from config.yaml
            
        Returns:
            DataFrame with Prd_Diff_OC column added
            
        Raises:
            CalculationError: If calculation fails
        """
        try:
            # Extract AR coefficients from ARIMA results
            ar_coeffs = {}
            for i in range(1, ar_lag_order + 1):
                coeff_name = f"ar.L{i}"
                if coeff_name in arima_coefficients:
                    ar_coeffs[i] = arima_coefficients[coeff_name]
                else:
                    logger.warning(f"Missing ARIMA coefficient: {coeff_name}")
                    ar_coeffs[i] = 0.0  # Default to 0 if missing
            
            logger.info(f"Using AR coefficients: {ar_coeffs}")
            
            # Determine Mean_Diff_OC value - prioritize configured value
            if configured_mean_diff_oc is not None:
                # Use configured value from config.yaml
                mean_diff_oc = configured_mean_diff_oc
                logger.info(f"Using configured Mean_Diff_OC from config.yaml: {mean_diff_oc}")
            elif 'Mean_Diff_OC' in data.columns:
                # Use existing mean from master data
                mean_diff_oc = data['Mean_Diff_OC'].iloc[0] if data['Mean_Diff_OC'].notna().any() else 0.0
                logger.info(f"Using Mean_Diff_OC from master data: {mean_diff_oc}")
            elif 'Diff_OC' in data.columns:
                # Calculate mean from original Diff_OC column (correct approach)
                mean_diff_oc = data['Diff_OC'].mean()
                logger.info(f"Calculated Mean_Diff_OC from original Diff_OC data: {mean_diff_oc}")
            else:
                # Last resort: use 0.0 (but log a warning)
                mean_diff_oc = 0.0
                logger.warning(f"No Mean_Diff_OC source found - using 0.0 as fallback. This may produce incorrect results.")
            
            logger.info(f"Final Mean_Diff_OC value used in formula: {mean_diff_oc}")
            
            # Initialize Prd_Diff_OC column with NaN
            data['Prd_Diff_OC'] = np.nan
            
            # Only calculate for rows with complete AR lag data
            if 'has_complete_ar_lags' in data.columns:
                complete_mask = data['has_complete_ar_lags']
                complete_indices = data[complete_mask].index
                
                if len(complete_indices) > 0:
                    logger.info(f"Calculating Prd_Diff_OC for {len(complete_indices)} rows with complete AR lag data")
                    
                    # Calculate Sigma(Ar.L(i)*Arima.Ar.L(i)) for complete rows only
                    sigma_values = np.zeros(len(complete_indices))
                    
                    logger.info(f"Calculating Sigma(Ar.L(i) * AR_coefficient(i)) for each lag:")
                    for i in range(1, ar_lag_order + 1):
                        ar_column = f"Ar.L{i}"
                        
                        if ar_column in data.columns:
                            # Get AR lag values for complete rows only
                            ar_lag_values = data.loc[complete_indices, ar_column]
                            ar_coefficient = ar_coeffs[i]
                            
                            contribution = ar_lag_values * ar_coefficient
                            sigma_values += contribution.values
                            
                            logger.info(f"  {ar_column}: coefficient={ar_coefficient:.6f}, "
                                      f"mean_lag_value={ar_lag_values.mean():.6f}, "
                                      f"mean_contribution={contribution.mean():.6f}")
                        else:
                            logger.warning(f"Missing AR lag column: {ar_column}")
                    
                    # Log the sigma calculation result
                    logger.info(f"Sigma calculation complete: mean_sigma={sigma_values.mean():.6f}")
                    logger.info(f"Formula: Prd_Diff_OC = Sigma({sigma_values.mean():.6f}) + Mean_Diff_OC({mean_diff_oc:.6f})")
                    
                    # Calculate final Prd_Diff_OC = Sigma + Mean_Diff_OC for complete rows
                    data.loc[complete_indices, 'Prd_Diff_OC'] = sigma_values + mean_diff_oc
                    
                    # Log final result
                    final_mean = data.loc[complete_indices, 'Prd_Diff_OC'].mean()
                    logger.info(f"Final Prd_Diff_OC mean result: {final_mean:.6f}")
                else:
                    logger.warning("No rows with complete AR lag data found")
            else:
                logger.warning("Missing 'has_complete_ar_lags' column - cannot validate AR lag completeness")
            
            # Validate calculation
            valid_predictions = data['Prd_Diff_OC'].notna().sum()
            logger.info(f"Calculated Prd_Diff_OC for {valid_predictions} rows")
            logger.info(f"Prd_Diff_OC statistics: mean={data['Prd_Diff_OC'].mean():.6f}, "
                       f"std={data['Prd_Diff_OC'].std():.6f}")
            
            return data
            
        except Exception as e:
            raise CalculationError(f"Failed to calculate Prd_Diff_OC: {str(e)}")
    
    def _calculate_prd_oc(self, data: pd.DataFrame) -> pd.DataFrame:
        """
        Calculate Prd_OC(T) = Prd_Diff_OC(T) + OC(T-1)
        
        Args:
            data: DataFrame with Prd_Diff_OC column
            
        Returns:
            DataFrame with Prd_OC column added
            
        Raises:
            CalculationError: If calculation fails
        """
        try:
            # Validate required columns
            required_columns = ['Prd_Diff_OC', 'OC']
            missing_columns = [col for col in required_columns if col not in data.columns]
            if missing_columns:
                raise CalculationError(f"Missing required columns for Prd_OC calculation: {missing_columns}")
            
            # Initialize Prd_OC column with NaN
            data['Prd_OC'] = np.nan
            
            # Only calculate for rows with complete AR lag data and valid Prd_Diff_OC
            if 'has_complete_ar_lags' in data.columns:
                complete_mask = data['has_complete_ar_lags'] & data['Prd_Diff_OC'].notna()
                complete_indices = data[complete_mask].index
                
                if len(complete_indices) > 0:
                    logger.info(f"Calculating Prd_OC for {len(complete_indices)} rows with complete data")
                    
                    # Calculate OC(T-1) by shifting OC column by 1
                    oc_t_minus_1 = data['OC'].shift(1)
                    
                    # Calculate Prd_OC(T) = Prd_Diff_OC(T) + OC(T-1) for complete rows only
                    data.loc[complete_indices, 'Prd_OC'] = (
                        data.loc[complete_indices, 'Prd_Diff_OC'] + 
                        oc_t_minus_1.loc[complete_indices]
                    )
                else:
                    logger.warning("No rows with complete AR lag data and valid Prd_Diff_OC found")
            else:
                logger.warning("Missing 'has_complete_ar_lags' column - cannot validate completeness")
            
            # Validate calculation
            valid_predictions = data['Prd_OC'].notna().sum()
            logger.info(f"Calculated Prd_OC for {valid_predictions} rows")
            logger.info(f"Prd_OC statistics: mean={data['Prd_OC'].mean():.6f}, "
                       f"std={data['Prd_OC'].std():.6f}")
            
            return data
            
        except Exception as e:
            raise CalculationError(f"Failed to calculate Prd_OC: {str(e)}")
    
    def _calculate_prd_return_arima(self, data: pd.DataFrame) -> pd.DataFrame:
        """
        Calculate Prd_Return_Arima(T) = Prd_OC(T)/Open(T)
        
        Args:
            data: DataFrame with Prd_OC column
            
        Returns:
            DataFrame with Prd_Return_Arima column added
            
        Raises:
            CalculationError: If calculation fails
        """
        try:
            # Validate required columns
            required_columns = ['Prd_OC', 'Open_Price']
            missing_columns = [col for col in required_columns if col not in data.columns]
            if missing_columns:
                raise CalculationError(f"Missing required columns for Prd_Return_Arima calculation: {missing_columns}")
            
            # Initialize Prd_Return_Arima column with NaN
            data['Prd_Return_Arima'] = np.nan
            
            # Only calculate for rows with complete AR lag data and valid Prd_OC
            if 'has_complete_ar_lags' in data.columns:
                complete_mask = (data['has_complete_ar_lags'] & 
                               data['Prd_OC'].notna() & 
                               data['Open_Price'].notna() & 
                               (data['Open_Price'] != 0))
                complete_indices = data[complete_mask].index
                
                if len(complete_indices) > 0:
                    logger.info(f"Calculating Prd_Return_Arima for {len(complete_indices)} rows with complete data")
                    
                    # Calculate Prd_Return_Arima(T) = Prd_OC(T)/Open(T) for complete rows only
                    data.loc[complete_indices, 'Prd_Return_Arima'] = (
                        data.loc[complete_indices, 'Prd_OC'] / 
                        data.loc[complete_indices, 'Open_Price']
                    )
                    
                    # Check for any remaining issues
                    zero_open_count = ((data['Open_Price'] == 0) | data['Open_Price'].isna()).sum()
                    if zero_open_count > 0:
                        logger.warning(f"Found {zero_open_count} rows with zero or NaN Open_Price values (excluded from calculation)")
                    
                else:
                    logger.warning("No rows with complete AR lag data, valid Prd_OC, and valid Open_Price found")
            else:
                logger.warning("Missing 'has_complete_ar_lags' column - cannot validate completeness")
            
            # Validate calculation
            valid_predictions = data['Prd_Return_Arima'].notna().sum()
            infinite_predictions = np.isinf(data['Prd_Return_Arima']).sum()
            
            if infinite_predictions > 0:
                logger.warning(f"Found {infinite_predictions} infinite values in Prd_Return_Arima")
                # Replace infinite values with NaN
                data['Prd_Return_Arima'] = data['Prd_Return_Arima'].replace([np.inf, -np.inf], np.nan)
                valid_predictions = data['Prd_Return_Arima'].notna().sum()
            
            logger.info(f"Calculated Prd_Return_Arima for {valid_predictions} rows")
            
            if valid_predictions > 0:
                return_stats = data['Prd_Return_Arima'].dropna()
                logger.info(f"Prd_Return_Arima statistics: mean={return_stats.mean():.6f}, "
                           f"std={return_stats.std():.6f}, min={return_stats.min():.6f}, "
                           f"max={return_stats.max():.6f}")
            
            return data
            
        except Exception as e:
            raise CalculationError(f"Failed to calculate Prd_Return_Arima: {str(e)}")
    
    def _generate_calculation_summary(self, data: pd.DataFrame) -> Dict[str, Any]:
        """
        Generate summary statistics for the calculations.
        
        Args:
            data: DataFrame with all calculated columns
            
        Returns:
            Dictionary with calculation summary
        """
        try:
            summary = {
                'total_rows': len(data),
                'calculation_columns': []
            }
            
            # Summary for each calculated column
            calc_columns = ['Prd_Diff_OC', 'Prd_OC', 'Prd_Return_Arima']
            
            for col in calc_columns:
                if col in data.columns:
                    col_data = data[col].dropna()
                    if len(col_data) > 0:
                        col_summary = {
                            'column': col,
                            'valid_count': len(col_data),
                            'null_count': data[col].isnull().sum(),
                            'mean': float(col_data.mean()),
                            'std': float(col_data.std()),
                            'min': float(col_data.min()),
                            'max': float(col_data.max()),
                            'percentiles': {
                                '25th': float(col_data.quantile(0.25)),
                                '50th': float(col_data.quantile(0.50)),
                                '75th': float(col_data.quantile(0.75))
                            }
                        }
                        summary['calculation_columns'].append(col_summary)
                    else:
                        summary['calculation_columns'].append({
                            'column': col,
                            'valid_count': 0,
                            'null_count': len(data),
                            'error': 'No valid data'
                        })
                else:
                    summary['calculation_columns'].append({
                        'column': col,
                        'error': 'Column not found'
                    })
            
            return summary
            
        except Exception as e:
            logger.warning(f"Failed to generate calculation summary: {str(e)}")
            return {'error': str(e)}