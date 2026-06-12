"""
OC ARIMA Analyzer Module
========================

ARIMA analyzer specifically designed for OC analysis results.
Implements AR(p) models with configurable parameters following modular monolith architecture.
"""

import pandas as pd
import numpy as np
import json
from datetime import datetime
from pathlib import Path
from typing import Dict, Any, Optional, Tuple, List
from dataclasses import dataclass, asdict
import logging

try:
    from statsmodels.tsa.arima.model import ARIMA
    from statsmodels.stats.diagnostic import acorr_ljungbox
    from statsmodels.tsa.stattools import adfuller
    import warnings
    warnings.filterwarnings('ignore')
    STATSMODELS_AVAILABLE = True
except ImportError:
    STATSMODELS_AVAILABLE = False

from .oc_data_reader import DataReaderFactory, DataReaderError

logger = logging.getLogger(__name__)

class ArimaAnalyzerError(Exception):
    """Exception raised for ARIMA analysis errors."""
    pass

@dataclass
class ArimaConfig:
    """Configuration for ARIMA analysis."""
    asset_code: str
    p: int  # AR parameter
    d: int  # Differencing parameter (should be 0 for this requirement)
    q: int  # MA parameter (should be 0 for this requirement)
    file_path: str
    column_name: str = "Demean_Diff_OC"
    optimize_parameters: bool = False  # If True, will search for optimal p
    max_p: int = 10  # Maximum p value to test when optimizing
    
    def __post_init__(self):
        """Validate configuration after initialization."""
        if self.d != 0:
            logger.warning(f"d parameter is {self.d}, but requirement specifies AR(p,0,0) model")
        if self.q != 0:
            logger.warning(f"q parameter is {self.q}, but requirement specifies AR(p,0,0) model")
        if self.optimize_parameters:
            logger.info(f"Parameter optimization enabled. Will search for optimal p in range 1-{self.max_p}")

@dataclass
class ArimaResults:
    """Results from ARIMA analysis."""
    asset_code: str
    p: int
    d: int
    q: int
    aic: float
    bic: float
    log_likelihood: float
    coefficients: Dict[str, float]
    coefficient_pvalues: Dict[str, float]
    residuals_summary: Dict[str, Any]
    model_summary: str
    data_info: Dict[str, Any]
    stationarity_test: Dict[str, Any]
    ljung_box_test: Dict[str, Any]
    analysis_timestamp: str
    file_path: str
    column_name: str
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert results to dictionary for JSON serialization."""
        return asdict(self)

class OCArimaAnalyzer:
    """
    ARIMA analyzer for OC analysis results.
    Specifically designed for AR(p,0,0) models on Demean_Diff_OC data.
    """
    
    def __init__(self):
        """Initialize the ARIMA analyzer."""
        if not STATSMODELS_AVAILABLE:
            raise ArimaAnalyzerError("statsmodels package is required but not available. "
                                   "Please install it with: pip install statsmodels")
        
        self.data_reader = DataReaderFactory.create_reader("oc_analysis")
        
    def analyze(self, config: ArimaConfig) -> ArimaResults:
        """
        Perform ARIMA analysis on OC data.
        
        Args:
            config: ARIMA configuration parameters
            
        Returns:
            ArimaResults containing analysis results
            
        Raises:
            ArimaAnalyzerError: If analysis fails
        """
        try:
            logger.info(f"Starting ARIMA analysis for {config.asset_code}")
            logger.info(f"Model: ARIMA({config.p}, {config.d}, {config.q})")
            logger.info(f"File: {config.file_path}")
            logger.info(f"Column: {config.column_name}")
            
            # Step 1: Read data
            data_series = self.data_reader.read_data(config.file_path, config.column_name)
            
            # Step 2: Get data information
            data_info = self.data_reader.get_data_info(data_series)
            
            # Step 3: Clean data (remove NaN values)
            clean_data = data_series.dropna()
            logger.info(f"Using {len(clean_data)} clean data points for analysis")
            
            # Step 4: Stationarity test
            stationarity_test = self._perform_stationarity_test(clean_data)
            
            # Step 5: Fit ARIMA model (with optional parameter optimization)
            if config.optimize_parameters:
                logger.info(f"Starting parameter optimization for AR model (p,{config.d},{config.q})")
                optimal_p, model_results = self._optimize_parameters(clean_data, config)
                logger.info(f"Optimal AR parameter found: p={optimal_p}")
            else:
                logger.info(f"Fitting single ARIMA({config.p},{config.d},{config.q}) model")
                optimal_p = config.p
                model_results = self._fit_arima_model(clean_data, config.p, config.d, config.q)
            
            # Step 6: Model diagnostics
            ljung_box_test = self._perform_ljung_box_test(model_results['residuals'])
            
            # Step 7: Create results
            results = ArimaResults(
                asset_code=config.asset_code,
                p=optimal_p,
                d=config.d,
                q=config.q,
                aic=model_results['aic'],
                bic=model_results['bic'],
                log_likelihood=model_results['log_likelihood'],
                coefficients=model_results['coefficients'],
                coefficient_pvalues=model_results['coefficient_pvalues'],
                residuals_summary=model_results['residuals_summary'],
                model_summary=model_results['model_summary'],
                data_info=data_info,
                stationarity_test=stationarity_test,
                ljung_box_test=ljung_box_test,
                analysis_timestamp=datetime.now().isoformat(),
                file_path=config.file_path,
                column_name=config.column_name
            )
            
            logger.info(f"ARIMA analysis completed successfully")
            logger.info(f"AIC: {results.aic:.4f}, BIC: {results.bic:.4f}")
            
            return results
            
        except Exception as e:
            if isinstance(e, (DataReaderError, ArimaAnalyzerError)):
                raise
            else:
                raise ArimaAnalyzerError(f"Error during ARIMA analysis: {str(e)}")
    
    def _fit_arima_model(self, data: pd.Series, p: int, d: int, q: int) -> Dict[str, Any]:
        """
        Fit ARIMA model to data.
        
        Args:
            data: Clean time series data
            p, d, q: ARIMA parameters
            
        Returns:
            Dictionary with model results
            
        Raises:
            ArimaAnalyzerError: If model fitting fails
        """
        try:
            logger.info(f"Fitting ARIMA({p}, {d}, {q}) model...")
            
            # Fit ARIMA model
            model = ARIMA(data, order=(p, d, q))
            fitted_model = model.fit()
            
            # Extract coefficients
            coefficients = {}
            coefficient_pvalues = {}
            
            if hasattr(fitted_model, 'params') and hasattr(fitted_model, 'pvalues'):
                for param_name, coef_value in fitted_model.params.items():
                    coefficients[param_name] = float(coef_value)
                
                for param_name, pvalue in fitted_model.pvalues.items():
                    coefficient_pvalues[param_name] = float(pvalue)
            
            # Get residuals
            residuals = fitted_model.resid
            
            # Residuals summary
            residuals_summary = {
                'mean': float(residuals.mean()),
                'std': float(residuals.std()),
                'min': float(residuals.min()),
                'max': float(residuals.max()),
                'skewness': float(residuals.skew()),
                'kurtosis': float(residuals.kurtosis())
            }
            
            # Model summary as string
            model_summary = str(fitted_model.summary())
            
            results = {
                'aic': float(fitted_model.aic),
                'bic': float(fitted_model.bic),
                'log_likelihood': float(fitted_model.llf),
                'coefficients': coefficients,
                'coefficient_pvalues': coefficient_pvalues,
                'residuals': residuals,
                'residuals_summary': residuals_summary,
                'model_summary': model_summary,
                'fitted_model': fitted_model
            }
            
            logger.info(f"Model fitted successfully. AIC: {results['aic']:.4f}")
            
            return results
            
        except Exception as e:
            raise ArimaAnalyzerError(f"Failed to fit ARIMA({p}, {d}, {q}) model: {str(e)}")
    
    def _perform_stationarity_test(self, data: pd.Series) -> Dict[str, Any]:
        """
        Perform Augmented Dickey-Fuller test for stationarity.
        
        Args:
            data: Time series data
            
        Returns:
            Dictionary with test results
        """
        try:
            logger.info("Performing Augmented Dickey-Fuller stationarity test...")
            
            adf_result = adfuller(data)
            
            test_result = {
                'test_statistic': float(adf_result[0]),
                'p_value': float(adf_result[1]),
                'critical_values': {k: float(v) for k, v in adf_result[4].items()},
                'is_stationary': adf_result[1] < 0.05,
                'interpretation': 'Stationary' if adf_result[1] < 0.05 else 'Non-stationary'
            }
            
            logger.info(f"ADF test: p-value = {test_result['p_value']:.6f}, "
                       f"Result: {test_result['interpretation']}")
            
            return test_result
            
        except Exception as e:
            logger.warning(f"Stationarity test failed: {str(e)}")
            return {
                'test_statistic': None,
                'p_value': None,
                'critical_values': {},
                'is_stationary': None,
                'interpretation': 'Test failed',
                'error': str(e)
            }
    
    def _perform_ljung_box_test(self, residuals: pd.Series, lags: int = 10) -> Dict[str, Any]:
        """
        Perform Ljung-Box test on residuals.
        
        Args:
            residuals: Model residuals
            lags: Number of lags to test
            
        Returns:
            Dictionary with test results
        """
        try:
            logger.info(f"Performing Ljung-Box test with {lags} lags...")
            
            lb_result = acorr_ljungbox(residuals, lags=lags, return_df=True)
            
            # Get overall test result (if any p-value < 0.05, residuals are not white noise)
            min_pvalue = lb_result['lb_pvalue'].min()
            
            test_result = {
                'lags_tested': lags,
                'min_p_value': float(min_pvalue),
                'white_noise': min_pvalue >= 0.05,
                'interpretation': 'Residuals are white noise' if min_pvalue >= 0.05 else 'Residuals show autocorrelation',
                'detailed_results': {
                    'statistics': lb_result['lb_stat'].to_dict(),
                    'p_values': lb_result['lb_pvalue'].to_dict()
                }
            }
            
            logger.info(f"Ljung-Box test: min p-value = {test_result['min_p_value']:.6f}, "
                       f"Result: {test_result['interpretation']}")
            
            return test_result
            
        except Exception as e:
            logger.warning(f"Ljung-Box test failed: {str(e)}")
            return {
                'lags_tested': lags,
                'min_p_value': None,
                'white_noise': None,
                'interpretation': 'Test failed',
                'error': str(e)
            }
    
    def _optimize_parameters(self, data: pd.Series, config: ArimaConfig) -> Tuple[int, Dict[str, Any]]:
        """
        Find optimal AR parameter p using grid search with progress logging.
        
        Args:
            data: Clean time series data
            config: ARIMA configuration
            
        Returns:
            Tuple of (optimal_p, model_results)
            
        Raises:
            ArimaAnalyzerError: If optimization fails
        """
        try:
            logger.info(f"Grid search for optimal AR parameter:")
            logger.info(f"  Testing p values from 1 to {config.max_p}")
            logger.info(f"  Fixed d = {config.d}, q = {config.q}")
            
            best_aic = float('inf')
            best_p = None
            best_results = None
            valid_models = 0
            failed_models = 0
            
            # Search from high to low to match user expectation (30,0,0, 29,0,0, etc.)
            p_values = list(range(config.max_p, 0, -1))  # e.g., [10, 9, 8, ..., 1]
            
            for i, p in enumerate(p_values):
                try:
                    logger.info(f"  Testing ARIMA({p},{config.d},{config.q}) [{i+1}/{len(p_values)}]...")
                    
                    model_results = self._fit_arima_model(data, p, config.d, config.q)
                    
                    aic_value = model_results['aic']
                    logger.info(f"    AIC: {aic_value:.4f}")
                    
                    if aic_value < best_aic:
                        best_aic = aic_value
                        best_p = p
                        best_results = model_results
                        logger.info(f"    New best model: ARIMA({p},{config.d},{config.q}) with AIC: {aic_value:.4f}")
                    
                    valid_models += 1
                    
                except Exception as e:
                    failed_models += 1
                    logger.warning(f"    Failed to fit ARIMA({p},{config.d},{config.q}): {str(e)}")
                    continue
            
            if best_p is None:
                raise ArimaAnalyzerError(f"No valid ARIMA models found during optimization. "
                                       f"Failed: {failed_models}, Valid: {valid_models}")
            
            logger.info(f"Parameter optimization completed:")
            logger.info(f"  Best model: ARIMA({best_p},{config.d},{config.q})")
            logger.info(f"  Best AIC: {best_aic:.4f}")
            logger.info(f"  Valid models tested: {valid_models}")
            logger.info(f"  Failed models: {failed_models}")
            
            return best_p, best_results
            
        except Exception as e:
            if isinstance(e, ArimaAnalyzerError):
                raise
            else:
                raise ArimaAnalyzerError(f"Parameter optimization failed: {str(e)}")

class ArimaResultsExporter:
    """Exporter for ARIMA results to JSON format."""
    
    @staticmethod
    def export_to_json(results: ArimaResults, output_path: str) -> str:
        """
        Export ARIMA results to JSON file.
        
        Args:
            results: ARIMA analysis results
            output_path: Path for output JSON file
            
        Returns:
            Path to the exported JSON file
            
        Raises:
            ArimaAnalyzerError: If export fails
        """
        try:
            output_path = Path(output_path)
            output_path.parent.mkdir(parents=True, exist_ok=True)
            
            # Convert results to dictionary
            results_dict = results.to_dict()
            
            # Write to JSON file
            with open(output_path, 'w') as f:
                json.dump(results_dict, f, indent=2, default=str)
            
            logger.info(f"ARIMA results exported to: {output_path}")
            
            return str(output_path)
            
        except Exception as e:
            raise ArimaAnalyzerError(f"Failed to export results to JSON: {str(e)}")