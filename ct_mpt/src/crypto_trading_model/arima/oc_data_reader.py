"""
OC Analysis Data Reader for ARIMA Module
========================================

Data reader module for reading OC analysis results to perform ARIMA modeling.
Follows modular monolith architecture with single responsibility principle.
"""

import pandas as pd
import numpy as np
from pathlib import Path
from typing import Optional, Union, List
import logging
from abc import ABC, abstractmethod

logger = logging.getLogger(__name__)

class DataReaderError(Exception):
    """Exception raised for data reading errors."""
    pass

class DataReaderInterface(ABC):
    """Abstract interface for data readers."""
    
    @abstractmethod
    def read_data(self, file_path: str, column_name: str) -> pd.Series:
        """Read data from file and return specified column as Series."""
        pass
    
    @abstractmethod
    def validate_data(self, data: pd.Series) -> bool:
        """Validate that data is suitable for ARIMA modeling."""
        pass

class OCAnalysisDataReader(DataReaderInterface):
    """
    Data reader for OC analysis results.
    Reads CSV or Excel files from oc_analysis_results folder.
    """
    
    def __init__(self):
        """Initialize the OC analysis data reader."""
        self.supported_formats = ['.csv', '.xlsx', '.xls']
        
    def read_data(self, file_path: str, column_name: str = "Demean_Diff_OC") -> pd.Series:
        """
        Read data from OC analysis file.
        
        Args:
            file_path: Path to the OC analysis file
            column_name: Name of the column to read (default: "Demean_Diff_OC")
            
        Returns:
            pandas Series containing the time series data
            
        Raises:
            DataReaderError: If file cannot be read or column doesn't exist
        """
        try:
            file_path = Path(file_path)
            
            # Validate file exists
            if not file_path.exists():
                raise DataReaderError(f"File not found: {file_path}")
            
            # Check file format
            if file_path.suffix.lower() not in self.supported_formats:
                raise DataReaderError(f"Unsupported file format: {file_path.suffix}. "
                                    f"Supported formats: {self.supported_formats}")
            
            # Read file based on extension
            if file_path.suffix.lower() == '.csv':
                df = pd.read_csv(file_path)
            else:  # Excel files
                # Try to read first sheet
                df = pd.read_excel(file_path, sheet_name=0)
            
            # Validate column exists
            if column_name not in df.columns:
                available_columns = list(df.columns)
                raise DataReaderError(f"Column '{column_name}' not found in file. "
                                    f"Available columns: {available_columns}")
            
            # Extract the series
            data_series = df[column_name].copy()
            
            # Log basic info
            logger.info(f"Successfully read {len(data_series)} records from {file_path}")
            logger.info(f"Column: {column_name}")
            logger.info(f"Data range: {data_series.min():.6f} to {data_series.max():.6f}")
            
            # Validate data
            if not self.validate_data(data_series):
                raise DataReaderError("Data validation failed")
            
            return data_series
            
        except Exception as e:
            if isinstance(e, DataReaderError):
                raise
            else:
                raise DataReaderError(f"Error reading file {file_path}: {str(e)}")
    
    def validate_data(self, data: pd.Series) -> bool:
        """
        Validate that data is suitable for ARIMA modeling.
        
        Args:
            data: Time series data to validate
            
        Returns:
            True if data is valid, False otherwise
            
        Raises:
            DataReaderError: If data validation fails
        """
        try:
            # Check for empty data
            if len(data) == 0:
                raise DataReaderError("Data series is empty")
            
            # Check minimum length for ARIMA
            if len(data) < 10:
                raise DataReaderError(f"Insufficient data points: {len(data)}. "
                                    "Need at least 10 points for ARIMA modeling")
            
            # Check for all NaN values
            if data.isna().all():
                raise DataReaderError("All data points are NaN")
            
            # Check percentage of NaN values
            nan_percentage = (data.isna().sum() / len(data)) * 100
            if nan_percentage > 50:
                raise DataReaderError(f"Too many NaN values: {nan_percentage:.1f}%")
            
            # Check for infinite values
            if np.isinf(data).any():
                raise DataReaderError("Data contains infinite values")
            
            # Check for constant values (no variation)
            non_nan_data = data.dropna()
            if len(non_nan_data.unique()) == 1:
                raise DataReaderError("Data has no variation (all values are the same)")
            
            # Log validation results
            logger.info(f"Data validation passed:")
            logger.info(f"  Total points: {len(data)}")
            logger.info(f"  Valid points: {len(non_nan_data)}")
            logger.info(f"  NaN points: {data.isna().sum()} ({nan_percentage:.1f}%)")
            logger.info(f"  Standard deviation: {non_nan_data.std():.6f}")
            
            return True
            
        except DataReaderError:
            raise
        except Exception as e:
            raise DataReaderError(f"Error during data validation: {str(e)}")
    
    def get_data_info(self, data: pd.Series) -> dict:
        """
        Get detailed information about the data series.
        
        Args:
            data: Time series data
            
        Returns:
            Dictionary with data information
        """
        non_nan_data = data.dropna()
        
        info = {
            'total_points': len(data),
            'valid_points': len(non_nan_data),
            'nan_points': data.isna().sum(),
            'nan_percentage': (data.isna().sum() / len(data)) * 100,
            'mean': float(non_nan_data.mean()),
            'std': float(non_nan_data.std()),
            'min': float(non_nan_data.min()),
            'max': float(non_nan_data.max()),
            'skewness': float(non_nan_data.skew()),
            'kurtosis': float(non_nan_data.kurtosis())
        }
        
        return info

class DataReaderFactory:
    """Factory for creating data readers."""
    
    @staticmethod
    def create_reader(reader_type: str = "oc_analysis") -> DataReaderInterface:
        """
        Create a data reader based on type.
        
        Args:
            reader_type: Type of reader ("oc_analysis")
            
        Returns:
            DataReaderInterface implementation
            
        Raises:
            ValueError: If reader type is not supported
        """
        readers = {
            "oc_analysis": OCAnalysisDataReader
        }
        
        if reader_type not in readers:
            raise ValueError(f"Unsupported reader type: {reader_type}. "
                           f"Available types: {list(readers.keys())}")
        
        return readers[reader_type]()