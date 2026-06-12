"""
Expected Return Prediction Module
=================================

This module implements expected return prediction based on OC analysis results
and ARIMA coefficients following a multi-step calculation process.

Components:
- DataPreparer: Prepares AR.L value allocation from OC analysis data
- ReturnCalculator: Calculates predicted returns using ARIMA coefficients
- ResultsWriter: Writes calculated values back to OC analysis files

Usage:
    from crypto_trading_model.expected_return_prediction import ExpectedReturnPredictor
    
    predictor = ExpectedReturnPredictor()
    results = predictor.predict_returns(
        oc_file_path="oc_analysis_results/BTC_oc_analysis.xlsx",
        arima_file_path="oc_analysis_results/BTC_arima_results.json"
    )
"""

from .expected_return_predictor import ExpectedReturnPredictor, PredictionConfig, PredictionResults
from .data_preparer import DataPreparer, ARLAllocationError
from .return_calculator import ReturnCalculator, CalculationError
from .results_writer import ResultsWriter, WriterError

__all__ = [
    'ExpectedReturnPredictor',
    'PredictionConfig', 
    'PredictionResults',
    'DataPreparer',
    'ARLAllocationError',
    'ReturnCalculator',
    'CalculationError',
    'ResultsWriter',
    'WriterError'
]

__version__ = "1.0.0"
__author__ = "Crypto Trading Model"
__description__ = "Expected return prediction based on OC analysis and ARIMA results"