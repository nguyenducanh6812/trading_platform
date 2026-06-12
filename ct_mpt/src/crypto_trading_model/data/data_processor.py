import numpy as np
import pandas as pd
from typing import Tuple
from ..custom_logging.logger import CustomLogger

class DataProcessor:
    """Processes data to compute returns and covariance matrices."""
    
    def __init__(self, logger: CustomLogger, lookback_period: int):
        """
        Initialize the DataProcessor.

        Args:
            logger: Custom logger instance.
            lookback_period: Number of days for lookback calculations.
        """
        self.logger = logger
        self.lookback_period = lookback_period

    def calculate_returns(self, data: pd.DataFrame) -> pd.DataFrame:
        """
        Calculate daily returns for BTC and ETH.

        Args:
            data: Input DataFrame with price data.

        Returns:
            DataFrame with added return columns.
        """
        data_copy = data.copy()
        data_copy['BTC_Return'] = np.round((data_copy['Close Price_BTC'] - data_copy['Open Price_BTC']) / data_copy['Open Price_BTC'], decimals=18)
        data_copy['ETH_Return'] = np.round((data_copy['Close Price_ETH'] - data_copy['Open Price_ETH']) / data_copy['Open Price_ETH'], decimals=18)
        return data_copy

    def compute_metrics(self, data: pd.DataFrame) -> Tuple[np.ndarray, np.ndarray, np.ndarray]:
        """
        Compute returns matrix, mean returns, and population covariance matrix.

        Args:
            data: DataFrame with return columns.

        Returns:
            Tuple of (returns_matrix, mean_returns, cov_matrix).
        """
        if len(data) < self.lookback_period:
            lookback_data = data
        else:
            lookback_data = data.iloc[-self.lookback_period:]
        
        # Extract returns matrix
        returns_matrix = lookback_data[['BTC_Return', 'ETH_Return']].values
        mean_returns = np.round(np.mean(returns_matrix, axis=0), decimals=18)
        
        # Calculate population covariance matrix using the formula:
        # Σ = (1/n)[(R^T-R̄)(R-R̄^T)]
        centered_returns = returns_matrix - mean_returns
        cov_matrix = np.round(np.dot(centered_returns.T, centered_returns) / len(centered_returns), decimals=18)

        # Only debug logging for specific date
        target_date = pd.Timestamp('2020-08-12')
        if not data.empty and target_date in data.index:
            self.logger.info(f"\n=== Debug information for {target_date} ===")
            self.logger.info(f"Data available for this date:")
            self.logger.info(f"BTC Return: {data.loc[target_date, 'BTC_Return']}")
            self.logger.info(f"ETH Return: {data.loc[target_date, 'ETH_Return']}")

            target_idx = data.index.get_loc(target_date)
            lookback_start = data.index[max(0, target_idx - self.lookback_period + 1)]
            lookback_end = data.index[target_idx]
            self.logger.info(f"\nLookback period for {target_date}:")
            self.logger.info(f"Start: {lookback_start}")
            self.logger.info(f"End: {lookback_end}")

            self.logger.info("\nReturns matrix shape for lookback period:")
            self.logger.info(f"Number of days: {returns_matrix.shape[0]}")
            self.logger.info(f"Number of assets: {returns_matrix.shape[1]}")
            self.logger.info("\nFirst 5 rows of returns matrix:")
            for i in range(min(5, len(returns_matrix))):
                self.logger.info(f"Day {i+1}: BTC={returns_matrix[i,0]:.8f}, ETH={returns_matrix[i,1]:.8f}")

            self.logger.info("\nDetailed mean returns:")
            self.logger.info(f"BTC mean return (raw): {np.mean(returns_matrix[:,0])}")
            self.logger.info(f"ETH mean return (raw): {np.mean(returns_matrix[:,1])}")
            self.logger.info(f"BTC mean return (rounded): {mean_returns[0]}")
            self.logger.info(f"ETH mean return (rounded): {mean_returns[1]}")

            self.logger.info("\nCentered returns statistics:")
            self.logger.info(f"Mean of centered returns (should be ~0):")
            self.logger.info(f"BTC: {np.mean(centered_returns[:,0]):.10f}")
            self.logger.info(f"ETH: {np.mean(centered_returns[:,1]):.10f}")

        return returns_matrix, mean_returns, cov_matrix