#!/usr/bin/env python3
"""
Test Script for Expected Return Prediction
=========================================

Simple test to verify the expected return prediction module works correctly.
"""

import pandas as pd
import numpy as np
import json
import tempfile
from pathlib import Path
import logging

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def create_test_oc_data():
    """Create test OC analysis data."""
    # Create sample data with 100 rows
    np.random.seed(42)  # For reproducible results
    
    dates = pd.date_range('2023-01-01', periods=100, freq='D')
    
    # Generate realistic price data
    base_price = 30000
    price_changes = np.random.normal(0, 0.02, 100)  # 2% daily volatility
    prices = [base_price]
    
    for change in price_changes[1:]:
        prices.append(prices[-1] * (1 + change))
    
    data = {
        'timestamp': dates,
        'Open_Price': prices,
        'Close_Price': [p * (1 + np.random.normal(0, 0.001)) for p in prices],  # Small open-close difference
    }
    
    # Calculate OC and related metrics
    df = pd.DataFrame(data)
    df['OC'] = (df['Close_Price'] - df['Open_Price']) / df['Open_Price']
    df['Diff_OC'] = df['OC'].diff()
    df['Mean_Diff_OC'] = df['Diff_OC'].mean()
    df['Demean_Diff_OC'] = df['Diff_OC'] - df['Mean_Diff_OC']
    
    return df

def create_test_arima_results():
    """Create test ARIMA results with coefficients."""
    # Create AR(3,0,0) model results
    arima_results = {
        'asset_code': 'TEST',
        'p': 3,
        'd': 0,
        'q': 0,
        'aic': 150.5,
        'bic': 160.2,
        'log_likelihood': -75.25,
        'coefficients': {
            'ar.L1': 0.3,
            'ar.L2': 0.2,
            'ar.L3': 0.1,
            'const': 0.001
        },
        'coefficient_pvalues': {
            'ar.L1': 0.001,
            'ar.L2': 0.05,
            'ar.L3': 0.1,
            'const': 0.5
        },
        'analysis_timestamp': '2023-12-01T10:00:00'
    }
    
    return arima_results

def test_expected_return_prediction():
    """Test the complete expected return prediction workflow."""
    try:
        from expected_return_predictor import ExpectedReturnPredictor, PredictionConfig
        
        logger.info("Starting expected return prediction test")
        
        # Create test data
        logger.info("Creating test data...")
        oc_data = create_test_oc_data()
        arima_results = create_test_arima_results()
        
        # Create temporary files
        with tempfile.TemporaryDirectory() as temp_dir:
            temp_path = Path(temp_dir)
            
            # Write test data to files
            oc_file = temp_path / "TEST_oc_analysis.xlsx"
            arima_file = temp_path / "TEST_arima_results.json"
            
            oc_data.to_excel(oc_file, index=False)
            
            with open(arima_file, 'w') as f:
                json.dump(arima_results, f, indent=2)
            
            logger.info(f"Test files created: {oc_file}, {arima_file}")
            
            # Create prediction configuration
            config = PredictionConfig(
                oc_file_path=str(oc_file),
                arima_file_path=str(arima_file),
                asset_code='TEST'
            )
            
            # Run prediction
            predictor = ExpectedReturnPredictor()
            results = predictor.predict_returns(config)
            
            # Validate results
            logger.info("Validating results...")
            assert results.asset_code == 'TEST'
            assert results.ar_lag_order == 3
            assert results.valid_predictions > 0
            assert Path(results.output_file_path).exists()
            
            # Read back the results file to verify
            result_data = pd.read_excel(results.output_file_path)
            
            # Check that prediction columns exist
            expected_columns = ['Ar.L1', 'Ar.L2', 'Ar.L3', 'Prd_Diff_OC', 'Prd_OC', 'Prd_Return_Arima']
            for col in expected_columns:
                assert col in result_data.columns, f"Missing column: {col}"
            
            # Check that predictions were calculated
            assert result_data['Prd_Return_Arima'].notna().sum() > 0, "No valid predictions calculated"
            
            logger.info("✓ All tests passed!")
            logger.info(f"Results summary:")
            logger.info(f"  Total data points: {results.total_data_points}")
            logger.info(f"  Valid predictions: {results.valid_predictions}")
            logger.info(f"  AR lag order: {results.ar_lag_order}")
            logger.info(f"  Output file: {results.output_file_path}")
            
            return True
            
    except Exception as e:
        logger.error(f"Test failed: {str(e)}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    success = test_expected_return_prediction()
    exit(0 if success else 1)