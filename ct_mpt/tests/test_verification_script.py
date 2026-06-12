#!/usr/bin/env python
"""
Test script to verify Risk Neutral calculation
Create this as test_risk_neutral.py in your project root
"""

import sys
from pathlib import Path
import pandas as pd
import numpy as np

# Add src to Python path
src_path = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(src_path))

from crypto_trading_model.config.config_manager import ConfigManager
from crypto_trading_model.data.data_loader import DataLoader
from crypto_trading_model.data.data_processor import DataProcessor
from crypto_trading_model.optimization.optimizer import Optimizer
from crypto_trading_model.custom_logging.logger import CustomLogger

def test_risk_neutral_verification():
    """Test the risk neutral verification with real data."""
    
    # Setup
    logger = CustomLogger('RiskNeutralTest')
    config_manager = ConfigManager(logger)
    config = config_manager.load_config()
    
    # Load data
    data_loader = DataLoader(logger)
    market_data = data_loader.load_data(config.data_file_path)
    
    # Process data
    data_processor = DataProcessor(logger, config.strategy.lookback_period)
    market_data = data_processor.calculate_returns(market_data)
    
    # Get data for a specific date (e.g., day 8 - first day with enough lookback)
    lookback_period = 7
    test_day_index = lookback_period  # Day 8 (0-indexed = 7)
    
    if len(market_data) <= test_day_index:
        logger.error(f"Not enough data. Need at least {test_day_index + 1} days, got {len(market_data)}")
        return
    
    # Get historical data for the lookback period
    historical_data = market_data.iloc[test_day_index-lookback_period:test_day_index]
    
    # Calculate returns and covariance
    returns_matrix = historical_data[['BTC_Return', 'ETH_Return']].values
    mean_returns = np.mean(returns_matrix, axis=0)
    centered_returns = returns_matrix - mean_returns
    cov_matrix = np.dot(centered_returns.T, centered_returns) / len(centered_returns)
    
    # Setup optimizer
    rf_rate = 0.0001075  # Same as your debug value
    optimizer = Optimizer(logger, mean_returns, cov_matrix, rf_rate)
    
    # Run verification
    logger.info(f"Testing with data from day {test_day_index + 1}")
    logger.info(f"Date: {market_data.iloc[test_day_index]['Timestamp']}")
    logger.info(f"Historical period: {historical_data['Timestamp'].iloc[0]} to {historical_data['Timestamp'].iloc[-1]}")
    
    optimizer.verify_risk_neutral_calculation('neutral')

if __name__ == "__main__":
    test_risk_neutral_verification()