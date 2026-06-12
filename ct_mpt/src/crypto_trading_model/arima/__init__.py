"""
ARIMA Module for Crypto Trading Model
=====================================

File: crypto_trading_model/arima/__init__.py

This module implements ARIMA (AutoRegressive Integrated Moving Average) forecasting
for time series analysis and prediction in the crypto trading strategy.

Components:
- DataStationarity: Tests for stationarity and prepares data for ARIMA
- ArimaOptimizer: Finds optimal ARIMA(p,d,q) parameters using AIC minimization
- BacktestArimaAnalyzer: Orchestrates ARIMA analysis of backtest portfolio returns

Usage:
    from crypto_trading_model.arima import BacktestArimaAnalyzer
    
    # Analyze your backtest results for ARIMA patterns
    analyzer = BacktestArimaAnalyzer(logger)
    results = analyzer.analyze_backtest_results("backtest_results_neutral.xlsx", "neutral")
    
    if results['forecast_ready']:
        print("Portfolio returns show predictable patterns!")
        # Implement Model Formula Step 5 (forecasting)
    else:
        print("Portfolio follows random walk (market efficient)")

Direct Service Usage:
    from crypto_trading_model.arima import DataStationarity, ArimaOptimizer
    
    # Use services directly for custom analysis
    stationarity_tester = DataStationarity(logger)
    arima_optimizer = ArimaOptimizer(logger)
    
    prepared_data = stationarity_tester.prepare_data(time_series)
    arima_result = arima_optimizer.find_optimal_arima(prepared_data)
"""

# Import existing modules with graceful handling for missing dependencies
try:
    from .data_stationarity import DataStationarity, StationarityTestResult, PreparedData
    from .arima_optimizer import ArimaOptimizer, ArimaResult
    from .backtest_arima_analyzer import BacktestArimaAnalyzer
    LEGACY_ARIMA_AVAILABLE = True
except ImportError as e:
    # Legacy ARIMA modules require statsmodels
    LEGACY_ARIMA_AVAILABLE = False
    print(f"Legacy ARIMA modules not available: {e}")

# Import new OC ARIMA modules (these have optional statsmodels dependency)
from .oc_data_reader import DataReaderFactory, OCAnalysisDataReader

# Try to import OC ARIMA main (requires statsmodels for full functionality)
try:
    from .oc_arima_main import OCArimaMain, ArimaJobConfig
    OC_ARIMA_AVAILABLE = True
except ImportError as e:
    OC_ARIMA_AVAILABLE = False
    print(f"OC ARIMA analysis not available: {e}")
    print("Install statsmodels to enable ARIMA analysis: pip install statsmodels")

__all__ = [
    # New OC ARIMA modules (always available)
    'DataReaderFactory',
    'OCAnalysisDataReader'
]

# Add OC ARIMA main if available
if OC_ARIMA_AVAILABLE:
    __all__.extend(['OCArimaMain', 'ArimaJobConfig'])

# Add legacy modules to __all__ if available
if LEGACY_ARIMA_AVAILABLE:
    __all__.extend([
        # Low-level services
        'DataStationarity',
        'StationarityTestResult', 
        'PreparedData',
        'ArimaOptimizer',
        'ArimaResult',
        
        # High-level orchestrator (main interface)
        'BacktestArimaAnalyzer'
    ])

__version__ = "1.0.0"
__author__ = "Crypto Trading Model"
__description__ = "ARIMA analysis for portfolio return forecasting in crypto trading strategies"