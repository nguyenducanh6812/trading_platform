"""
Data reader module for OC (Open-Close) analysis.
Follows SOLID principles with single responsibility for data reading.
"""

from abc import ABC, abstractmethod
from typing import Dict, Any
import pandas as pd


class DataReaderInterface(ABC):
    """Interface for data readers following the Interface Segregation Principle."""
    
    @abstractmethod
    def read_data(self, data_source: str) -> pd.DataFrame:
        """Read data from a data source and return a DataFrame."""
        pass


class CryptoDataReader(DataReaderInterface):
    """
    Concrete implementation for reading cryptocurrency data from sync_data_output folder.
    Follows Single Responsibility Principle - only responsible for reading data.
    """
    
    def read_data(self, sync_data_folder: str) -> pd.DataFrame:
        """
        Read cryptocurrency data from sync_data_output folder containing separate asset files.
        
        Args:
            sync_data_folder: Path to folder containing BTC.csv and ETH.csv files
            
        Returns:
            DataFrame with columns: Timestamp, Open Price_BTC, Close Price_BTC, 
                                   Open Price_ETH, Close Price_ETH
            
        Raises:
            FileNotFoundError: If the folder or required files don't exist
            pd.errors.EmptyDataError: If files are empty
            ValueError: If required columns are missing
        """
        import os
        
        try:
            # Construct file paths
            btc_file = os.path.join(sync_data_folder, 'BTC.csv')
            eth_file = os.path.join(sync_data_folder, 'ETH.csv')
            
            # Check if files exist
            if not os.path.exists(btc_file):
                raise FileNotFoundError(f"BTC data file not found: {btc_file}")
            if not os.path.exists(eth_file):
                raise FileNotFoundError(f"ETH data file not found: {eth_file}")
            
            # Read BTC data
            btc_df = pd.read_csv(btc_file)
            required_btc_columns = ['TIMESTAMP', 'OPEN_PRICE_BTC', 'CLOSE_PRICE_BTC']
            missing_btc_columns = [col for col in required_btc_columns if col not in btc_df.columns]
            if missing_btc_columns:
                raise ValueError(f"Missing required BTC columns: {missing_btc_columns}")
            
            # Read ETH data
            eth_df = pd.read_csv(eth_file)
            required_eth_columns = ['TIMESTAMP', 'OPEN_PRICE_ETH', 'CLOSE_PRICE_ETH']
            missing_eth_columns = [col for col in required_eth_columns if col not in eth_df.columns]
            if missing_eth_columns:
                raise ValueError(f"Missing required ETH columns: {missing_eth_columns}")
            
            # Merge on timestamp
            merged_df = pd.merge(
                btc_df[['TIMESTAMP', 'OPEN_PRICE_BTC', 'CLOSE_PRICE_BTC']], 
                eth_df[['TIMESTAMP', 'OPEN_PRICE_ETH', 'CLOSE_PRICE_ETH']], 
                on='TIMESTAMP', 
                how='inner'
            )
            
            # Rename columns to match expected format
            merged_df = merged_df.rename(columns={
                'TIMESTAMP': 'Timestamp',
                'OPEN_PRICE_BTC': 'Open Price_BTC',
                'CLOSE_PRICE_BTC': 'Close Price_BTC',
                'OPEN_PRICE_ETH': 'Open Price_ETH',
                'CLOSE_PRICE_ETH': 'Close Price_ETH'
            })
            
            return merged_df
            
        except FileNotFoundError:
            raise
        except pd.errors.EmptyDataError as e:
            raise pd.errors.EmptyDataError(f"One of the data files is empty: {str(e)}")
        except Exception as e:
            raise RuntimeError(f"Error reading data from {sync_data_folder}: {str(e)}")


class DataReaderFactory:
    """
    Factory class for creating data readers.
    Follows Open/Closed Principle - open for extension, closed for modification.
    """
    
    @staticmethod
    def create_reader(reader_type: str) -> DataReaderInterface:
        """
        Create a data reader based on the specified type.
        
        Args:
            reader_type: Type of reader to create ('crypto')
            
        Returns:
            DataReaderInterface implementation
            
        Raises:
            ValueError: If reader_type is not supported
        """
        readers = {
            'crypto': CryptoDataReader
        }
        
        if reader_type not in readers:
            raise ValueError(f"Unsupported reader type: {reader_type}. "
                           f"Available types: {list(readers.keys())}")
        
        return readers[reader_type]()