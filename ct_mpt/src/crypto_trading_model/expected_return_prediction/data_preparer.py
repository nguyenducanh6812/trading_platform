"""
Data Preparer for AR.L Allocation
=================================

Prepares data for expected return prediction by allocating AR.L values
from Demean_Diff_OC data according to ARIMA lag requirements.

Ar.L1 (T) = Demean Diff OC (T-1)
Ar.L2 (T) = Demean Diff OC (T-2)
...
Ar.L30 (T) = Demean Diff OC (T-30)
"""

import pandas as pd
import numpy as np
import logging
from typing import Dict, Any, Optional, List

logger = logging.getLogger(__name__)

class ARLAllocationError(Exception):
    """Exception raised for AR.L allocation errors."""
    pass

class DataPreparer:
    """
    Prepares AR.L allocation data for expected return prediction.
    Handles the lag allocation process from Demean_Diff_OC to AR.L columns.
    """
    
    def __init__(self):
        """Initialize the data preparer."""
        pass
    
    def prepare_ar_lag_data(self, oc_data: pd.DataFrame, ar_lag_order: int) -> pd.DataFrame:
        """
        Prepare AR lag data by allocating Demean_Diff_OC values to AR.L columns.
        
        Args:
            oc_data: DataFrame with OC analysis data
            ar_lag_order: Number of AR lags to create (e.g., 30 for AR(30,0,0))
            
        Returns:
            DataFrame with original data plus AR.L columns
            
        Raises:
            ARLAllocationError: If data preparation fails
        """
        try:
            logger.info(f"Preparing AR lag data with {ar_lag_order} lags")
            
            # Create a copy of the original data
            prepared_data = oc_data.copy()
            
            # Validate required columns
            required_columns = ['Demean_Diff_OC']
            missing_columns = [col for col in required_columns if col not in prepared_data.columns]
            if missing_columns:
                raise ARLAllocationError(f"Missing required columns: {missing_columns}")
            
            # Sort data by timestamp/index to ensure proper lag calculation
            if 'timestamp' in prepared_data.columns:
                prepared_data = prepared_data.sort_values('timestamp').reset_index(drop=True)
            elif 'date' in prepared_data.columns:
                prepared_data = prepared_data.sort_values('date').reset_index(drop=True)
            else:
                # Assume data is already in chronological order
                prepared_data = prepared_data.reset_index(drop=True)
            
            # Create AR.L columns with lag allocation
            logger.info(f"Creating AR.L columns for lags 1 to {ar_lag_order}")
            
            total_rows = len(prepared_data)
            ar_columns_created = 0
            
            for lag in range(1, ar_lag_order + 1):
                col_name = f"Ar.L{lag}"
                
                # Ar.L1 (T) = Demean_Diff_OC (T-1)
                # Ar.L2 (T) = Demean_Diff_OC (T-2)
                # etc.
                lagged_values = prepared_data['Demean_Diff_OC'].shift(lag)
                prepared_data[col_name] = lagged_values
                
                ar_columns_created += 1
                
                # Log progress every 10 lags or at the end
                if lag % 10 == 0 or lag == ar_lag_order:
                    logger.info(f"  Created AR.L columns 1-{lag} ({ar_columns_created}/{ar_lag_order})")
            
            # Count valid rows (rows without NaN in AR lag columns)
            # The first ar_lag_order rows will have NaN values due to lagging
            valid_rows_before = total_rows
            ar_columns = [f"Ar.L{i}" for i in range(1, ar_lag_order + 1)]
            valid_mask = prepared_data[ar_columns].notna().all(axis=1)
            valid_rows_after = valid_mask.sum()
            
            logger.info(f"AR lag allocation summary:")
            logger.info(f"  Total rows: {total_rows}")
            logger.info(f"  Valid rows after lagging: {valid_rows_after}")
            logger.info(f"  Rows lost to lagging: {total_rows - valid_rows_after}")
            logger.info(f"  AR.L columns created: {ar_columns_created}")
            
            # Add metadata column to track which rows have complete AR lag data
            prepared_data['has_complete_ar_lags'] = valid_mask
            
            return prepared_data
            
        except Exception as e:
            if isinstance(e, ARLAllocationError):
                raise
            else:
                raise ARLAllocationError(f"Failed to prepare AR lag data: {str(e)}")
    
    def validate_ar_lag_data(self, data: pd.DataFrame, ar_lag_order: int) -> Dict[str, Any]:
        """
        Validate the prepared AR lag data.
        
        Args:
            data: Prepared data with AR.L columns
            ar_lag_order: Expected number of AR lags
            
        Returns:
            Dictionary with validation results
            
        Raises:
            ARLAllocationError: If validation fails
        """
        try:
            validation_results = {
                'total_rows': len(data),
                'ar_lag_order': ar_lag_order,
                'ar_columns_present': [],
                'ar_columns_missing': [],
                'valid_ar_rows': 0,
                'invalid_ar_rows': 0,
                'validation_passed': False
            }
            
            # Check for AR.L columns
            expected_columns = [f"Ar.L{i}" for i in range(1, ar_lag_order + 1)]
            
            for col in expected_columns:
                if col in data.columns:
                    validation_results['ar_columns_present'].append(col)
                else:
                    validation_results['ar_columns_missing'].append(col)
            
            # Count valid/invalid rows
            if validation_results['ar_columns_present']:
                present_ar_columns = validation_results['ar_columns_present']
                valid_mask = data[present_ar_columns].notna().all(axis=1)
                validation_results['valid_ar_rows'] = valid_mask.sum()
                validation_results['invalid_ar_rows'] = len(data) - validation_results['valid_ar_rows']
            
            # Determine if validation passed
            validation_results['validation_passed'] = (
                len(validation_results['ar_columns_missing']) == 0 and
                validation_results['valid_ar_rows'] > 0
            )
            
            logger.info(f"AR lag data validation:")
            logger.info(f"  Expected AR columns: {len(expected_columns)}")
            logger.info(f"  Present AR columns: {len(validation_results['ar_columns_present'])}")
            logger.info(f"  Missing AR columns: {len(validation_results['ar_columns_missing'])}")
            logger.info(f"  Valid rows: {validation_results['valid_ar_rows']}")
            logger.info(f"  Validation passed: {validation_results['validation_passed']}")
            
            if not validation_results['validation_passed']:
                if validation_results['ar_columns_missing']:
                    raise ARLAllocationError(f"Missing AR.L columns: {validation_results['ar_columns_missing']}")
                if validation_results['valid_ar_rows'] == 0:
                    raise ARLAllocationError("No valid rows with complete AR lag data")
            
            return validation_results
            
        except Exception as e:
            if isinstance(e, ARLAllocationError):
                raise
            else:
                raise ARLAllocationError(f"AR lag data validation failed: {str(e)}")
    
    def get_data_summary(self, data: pd.DataFrame) -> Dict[str, Any]:
        """
        Get summary information about the prepared data.
        
        Args:
            data: Prepared data with AR.L columns
            
        Returns:
            Dictionary with data summary
        """
        try:
            # Find AR.L columns
            ar_columns = [col for col in data.columns if col.startswith('Ar.L')]
            ar_columns_sorted = sorted(ar_columns, key=lambda x: int(x.split('L')[1]))
            
            # Basic statistics
            summary = {
                'total_rows': len(data),
                'total_columns': len(data.columns),
                'ar_lag_columns': len(ar_columns),
                'ar_column_names': ar_columns_sorted,
                'original_columns': [col for col in data.columns if not col.startswith('Ar.L') and col != 'has_complete_ar_lags'],
                'data_types': data.dtypes.to_dict()
            }
            
            # Check for complete AR lag data
            if ar_columns:
                complete_ar_mask = data[ar_columns].notna().all(axis=1)
                summary['rows_with_complete_ar_lags'] = complete_ar_mask.sum()
                summary['rows_with_incomplete_ar_lags'] = len(data) - summary['rows_with_complete_ar_lags']
            else:
                summary['rows_with_complete_ar_lags'] = 0
                summary['rows_with_incomplete_ar_lags'] = len(data)
            
            # Statistical summary for key columns
            key_columns = ['Demean_Diff_OC', 'OC', 'Open_Price']
            summary['column_statistics'] = {}
            
            for col in key_columns:
                if col in data.columns:
                    col_data = data[col].dropna()
                    if len(col_data) > 0:
                        summary['column_statistics'][col] = {
                            'count': len(col_data),
                            'mean': float(col_data.mean()),
                            'std': float(col_data.std()),
                            'min': float(col_data.min()),
                            'max': float(col_data.max()),
                            'null_count': data[col].isnull().sum()
                        }
            
            return summary
            
        except Exception as e:
            logger.warning(f"Failed to generate data summary: {str(e)}")
            return {'error': str(e)}