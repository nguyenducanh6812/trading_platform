"""
Calculator module for OC (Open-Close) analysis calculations.
Follows SOLID principles with single responsibility for calculations.
"""

from abc import ABC, abstractmethod
from typing import Dict, Tuple
import pandas as pd
import numpy as np


class CalculatorInterface(ABC):
    """Interface for calculators following the Interface Segregation Principle."""
    
    @abstractmethod
    def calculate_oc(self, open_prices: pd.Series, close_prices: pd.Series) -> pd.Series:
        """Calculate OC (Close - Open) values."""
        pass
    
    @abstractmethod
    def calculate_diff_oc(self, oc_values: pd.Series) -> pd.Series:
        """Calculate Diff_OC (OC(T) - OC(T-1)) values."""
        pass
    
    @abstractmethod
    def calculate_mean_diff_oc(self, diff_oc_values: pd.Series) -> float:
        """Calculate Mean_Diff_OC value."""
        pass
    
    @abstractmethod
    def calculate_demean_diff_oc(self, diff_oc_values: pd.Series, mean_diff_oc: float) -> pd.Series:
        """Calculate Demean_Diff_OC (Diff_OC - Mean_Diff_OC) values."""
        pass


class OCCalculator(CalculatorInterface):
    """
    Concrete implementation for OC analysis calculations.
    Follows Single Responsibility Principle - only responsible for calculations.
    """
    
    def calculate_oc(self, open_prices: pd.Series, close_prices: pd.Series) -> pd.Series:
        """
        Calculate OC (Close - Open) values.
        
        Args:
            open_prices: Series of opening prices
            close_prices: Series of closing prices
            
        Returns:
            Series of OC values (Close - Open)
            
        Raises:
            ValueError: If series lengths don't match or contain invalid data
        """
        if len(open_prices) != len(close_prices):
            raise ValueError("Open and close price series must have the same length")
        
        if open_prices.isna().any() or close_prices.isna().any():
            raise ValueError("Price series cannot contain NaN values")
        
        return close_prices - open_prices
    
    def calculate_diff_oc(self, oc_values: pd.Series) -> pd.Series:
        """
        Calculate Diff_OC (OC(T) - OC(T-1)) values.
        
        Args:
            oc_values: Series of OC values
            
        Returns:
            Series of Diff_OC values with NaN for the first row
            
        Raises:
            ValueError: If OC values contain invalid data
        """
        if oc_values.isna().any():
            raise ValueError("OC values cannot contain NaN values")
        
        if len(oc_values) < 2:
            raise ValueError("Need at least 2 OC values to calculate differences")
        
        # Calculate differences, first value will be NaN
        diff_oc = oc_values.diff()
        return diff_oc
    
    def calculate_mean_diff_oc(self, diff_oc_values: pd.Series) -> float:
        """
        Calculate Mean_Diff_OC value.
        
        Args:
            diff_oc_values: Series of Diff_OC values
            
        Returns:
            Mean of Diff_OC values (excluding NaN)
            
        Raises:
            ValueError: If no valid Diff_OC values exist
        """
        # Remove NaN values (first row is always NaN)
        valid_diff_oc = diff_oc_values.dropna()
        
        if len(valid_diff_oc) == 0:
            raise ValueError("No valid Diff_OC values to calculate mean")
        
        return float(valid_diff_oc.mean())
    
    def calculate_demean_diff_oc(self, diff_oc_values: pd.Series, mean_diff_oc: float) -> pd.Series:
        """
        Calculate Demean_Diff_OC (Diff_OC - Mean_Diff_OC) values.
        
        Args:
            diff_oc_values: Series of Diff_OC values
            mean_diff_oc: Mean of Diff_OC values
            
        Returns:
            Series of Demean_Diff_OC values
        """
        return diff_oc_values - mean_diff_oc


class AssetOCAnalyzer:
    """
    High-level analyzer for performing complete OC analysis on a single asset.
    Follows Single Responsibility Principle and Dependency Inversion Principle.
    """
    
    def __init__(self, calculator: CalculatorInterface):
        """
        Initialize with a calculator implementation.
        
        Args:
            calculator: Calculator implementation following CalculatorInterface
        """
        self.calculator = calculator
    
    def analyze_asset(self, asset_data: pd.DataFrame, asset_name: str) -> Tuple[pd.DataFrame, float]:
        """
        Perform complete OC analysis for a single asset.
        
        Args:
            asset_data: DataFrame with Timestamp, Open Price, and Close Price columns
            asset_name: Name of the asset (e.g., 'BTC', 'ETH')
            
        Returns:
            Tuple of (analysis_df, mean_diff_oc) where:
                - analysis_df: DataFrame with Timestamp, Open_Price, Close_Price, OC, Diff_OC, Demean_Diff_OC
                - mean_diff_oc: Mean of Diff_OC values
                
        Raises:
            ValueError: If required columns are missing or data is invalid
        """
        # Validate input data
        required_columns = ['Timestamp', f'Open Price_{asset_name}', f'Close Price_{asset_name}']
        missing_columns = [col for col in required_columns if col not in asset_data.columns]
        if missing_columns:
            raise ValueError(f"Missing required columns for {asset_name}: {missing_columns}")
        
        # Extract price data
        open_prices = asset_data[f'Open Price_{asset_name}']
        close_prices = asset_data[f'Close Price_{asset_name}']
        
        # Perform calculations
        oc_values = self.calculator.calculate_oc(open_prices, close_prices)
        diff_oc_values = self.calculator.calculate_diff_oc(oc_values)
        mean_diff_oc = self.calculator.calculate_mean_diff_oc(diff_oc_values)
        demean_diff_oc_values = self.calculator.calculate_demean_diff_oc(diff_oc_values, mean_diff_oc)
        
        # Create result DataFrame including open and close prices
        result_df = pd.DataFrame({
            'Timestamp': asset_data['Timestamp'],
            f'Open_Price_{asset_name}': open_prices,
            f'Close_Price_{asset_name}': close_prices,
            'OC': oc_values,
            'Diff_OC': diff_oc_values,
            'Demean_Diff_OC': demean_diff_oc_values
        })
        
        return result_df, mean_diff_oc


class MultiAssetOCAnalyzer:
    """
    Analyzer for performing OC analysis on multiple assets.
    Follows Open/Closed Principle - can be extended for new asset types.
    """
    
    def __init__(self, calculator: CalculatorInterface):
        """
        Initialize with a calculator implementation.
        
        Args:
            calculator: Calculator implementation following CalculatorInterface
        """
        self.asset_analyzer = AssetOCAnalyzer(calculator)
    
    def analyze_all_assets(self, data: pd.DataFrame, asset_names: list) -> Tuple[Dict[str, pd.DataFrame], Dict[str, float]]:
        """
        Perform OC analysis for multiple assets.
        
        Args:
            data: DataFrame with price data for all assets
            asset_names: List of asset names to analyze (e.g., ['BTC', 'ETH'])
            
        Returns:
            Tuple of (asset_analyses, mean_diff_ocs) where:
                - asset_analyses: Dict mapping asset names to their analysis DataFrames
                - mean_diff_ocs: Dict mapping asset names to their Mean_Diff_OC values
        """
        asset_analyses = {}
        mean_diff_ocs = {}
        
        for asset_name in asset_names:
            try:
                analysis_df, mean_diff_oc = self.asset_analyzer.analyze_asset(data, asset_name)
                asset_analyses[asset_name] = analysis_df
                mean_diff_ocs[asset_name] = mean_diff_oc
            except Exception as e:
                raise RuntimeError(f"Failed to analyze asset {asset_name}: {str(e)}")
        
        return asset_analyses, mean_diff_ocs
    
    def analyze_all_assets_with_configured_means(self, data: pd.DataFrame, asset_names: list, 
                                               configured_mean_diff_ocs: Dict[str, float]) -> Tuple[Dict[str, pd.DataFrame], Dict[str, float]]:
        """
        Perform OC analysis for multiple assets using pre-configured mean_diff_oc values.
        
        Args:
            data: DataFrame with price data for all assets
            asset_names: List of asset names to analyze (e.g., ['BTC', 'ETH'])
            configured_mean_diff_ocs: Dict mapping asset names to their configured mean_diff_oc values
            
        Returns:
            Tuple of (asset_analyses, mean_diff_ocs) where:
                - asset_analyses: Dict mapping asset names to their analysis DataFrames
                - mean_diff_ocs: Dict with the configured mean_diff_oc values used
        """
        asset_analyses = {}
        used_mean_diff_ocs = {}
        
        for asset_name in asset_names:
            try:
                # Get configured mean value for this asset
                configured_mean = configured_mean_diff_ocs.get(asset_name, 0.0)
                
                # Perform analysis with configured mean value
                analysis_df = self._analyze_asset_with_configured_mean(data, asset_name, configured_mean)
                asset_analyses[asset_name] = analysis_df
                used_mean_diff_ocs[asset_name] = configured_mean
                
            except Exception as e:
                raise RuntimeError(f"Failed to analyze asset {asset_name} with configured mean {configured_mean}: {str(e)}")
        
        return asset_analyses, used_mean_diff_ocs
    
    def _analyze_asset_with_configured_mean(self, data: pd.DataFrame, asset_name: str, 
                                          configured_mean_diff_oc: float) -> pd.DataFrame:
        """
        Analyze a single asset using a pre-configured mean_diff_oc value.
        
        Args:
            data: DataFrame with price data
            asset_name: Name of the asset (e.g., 'BTC', 'ETH')
            configured_mean_diff_oc: Pre-configured mean_diff_oc value to use for demeaning
            
        Returns:
            DataFrame with analysis results including Demean_Diff_OC column
        """
        print(f"  Analyzing {asset_name} with configured mean_diff_oc: {configured_mean_diff_oc:.6f}")
        
        # Extract columns for this asset (use same naming convention as original analyzer)
        timestamp_col = 'Timestamp'
        open_col = f'Open Price_{asset_name}'  # Note: uses space, not underscore
        close_col = f'Close Price_{asset_name}'  # Note: uses space, not underscore
        
        # Validate required columns exist
        required_cols = [timestamp_col, open_col, close_col]
        missing_cols = [col for col in required_cols if col not in data.columns]
        if missing_cols:
            raise ValueError(f"Missing required columns for {asset_name}: {missing_cols}")
        
        # Extract relevant data
        asset_data = data[[timestamp_col, open_col, close_col]].copy()
        asset_data = asset_data.dropna()
        
        # Calculate OC values first, then Diff_OC (consistent with original analyzer)
        oc_values = self.asset_analyzer.calculator.calculate_oc(
            asset_data[open_col], asset_data[close_col]
        )
        diff_oc_values = self.asset_analyzer.calculator.calculate_diff_oc(oc_values)
        
        # Use configured mean for demeaning instead of calculating it
        demean_diff_oc_values = self.asset_analyzer.calculator.calculate_demean_diff_oc(
            diff_oc_values, configured_mean_diff_oc
        )
        
        # Create result DataFrame (consistent with original analyzer output format)
        result_df = pd.DataFrame({
            'Timestamp': asset_data[timestamp_col],
            f'Open_Price_{asset_name}': asset_data[open_col],
            f'Close_Price_{asset_name}': asset_data[close_col],
            'OC': oc_values,
            'Diff_OC': diff_oc_values,
            'Demean_Diff_OC': demean_diff_oc_values
        })
        
        return result_df