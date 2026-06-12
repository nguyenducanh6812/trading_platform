from pathlib import Path
import pandas as pd
import numpy as np
from typing import Optional
from ..custom_logging.logger import CustomLogger

class DataValidationError(Exception):
    """Exception raised for data validation errors."""
    pass

class DataLoader:
    """Loads and preprocesses price data from CSV files."""
    
    def __init__(self, logger: CustomLogger):
        """
        Initialize the DataLoader.

        Args:
            logger: Custom logger instance.
        """
        self.logger = logger

    def load_data(self, file_path: str) -> pd.DataFrame:
        """
        Load and preprocess data from a CSV file.

        Args:
            file_path: Path to the CSV file.

        Returns:
            Preprocessed pandas DataFrame.

        Raises:
            DataValidationError: If the data is invalid or cannot be loaded.
        """
        self.logger.info(f"Loading data from {file_path}")
        try:
            df = pd.read_csv(file_path)
        except Exception as e:
            self.logger.error(f"Failed to load CSV: {e}")
            raise DataValidationError(f"Cannot load CSV file: {e}")

        # Validate required columns
        required_columns = ['Timestamp', 'Open Price_BTC', 'Close Price_BTC', 'Open Price_ETH', 'Close Price_ETH']
        if not all(col in df.columns for col in required_columns):
            missing = [col for col in required_columns if col not in df.columns]
            self.logger.error(f"Missing required columns: {missing}")
            raise DataValidationError(f"Missing columns: {missing}")

        # Clean price columns with high precision
        price_columns = [col for col in df.columns if 'Price' in col]
        self.logger.info(f"Cleaning price columns: {price_columns}")
        for col in price_columns:
            # Convert to string first to handle any comma formatting, then convert to float64
            df[col] = df[col].astype(str).str.replace(',', '').astype(np.float64)

        # Convert timestamp
        df['Timestamp'] = pd.to_datetime(df['Timestamp'])
        df = df.sort_values('Timestamp')

        self.logger.info(f"Loaded {len(df)} rows from {df['Timestamp'].min()} to {df['Timestamp'].max()}")
        self.logger.info(f"DataFrame columns: {df.columns.tolist()}")
        return df