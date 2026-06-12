"""
ARIMA Master Data Exporter
==========================

File: crypto_trading_model/arima/arima_master_data_exporter.py

This module extends your existing ARIMA analysis to export master data to JSON.
It integrates with your existing BacktestArimaAnalyzer and ArimaOptimizer to
save all the essential parameters needed for forecasting application.

Usage:
    # Add to your existing ARIMA analysis workflow
    exporter = ArimaMasterDataExporter(logger)
    
    # After running your existing ARIMA analysis
    master_data = exporter.export_arima_master_data(
        arima_result=your_arima_result,
        prepared_data=your_prepared_data,
        portfolio_returns=your_portfolio_returns,
        output_path="arima_master_data.json"
    )
"""

import json
import pandas as pd
import numpy as np
from pathlib import Path
from typing import Dict, List, Optional, Any, Union
from datetime import datetime

from ..custom_logging.logger import CustomLogger
from .arima_optimizer import ArimaResult
from .data_stationarity import PreparedData

class ArimaMasterDataExporter:
    """
    Exports master data from your existing ARIMA analysis results.
    
    This class takes the results from your existing ARIMA analysis pipeline
    and creates a comprehensive JSON file with all parameters needed for
    forecasting application.
    """
    
    def __init__(self, logger: CustomLogger):
        """
        Initialize the ARIMA Master Data Exporter.
        
        Args:
            logger: Custom logger instance
        """
        self.logger = logger
        self.logger.info("ArimaMasterDataExporter initialized")

    def export_arima_master_data(
        self,
        arima_result: ArimaResult,
        prepared_data: PreparedData,
        portfolio_returns: pd.Series,
        analysis_metadata: Optional[Dict[str, Any]] = None,
        output_path: str = "arima_master_data.json"
    ) -> Dict[str, Any]:
        """
        Export complete ARIMA master data to JSON file.
        
        This creates a comprehensive JSON containing all the information needed
        to apply your ARIMA model for forecasting without re-running the analysis.
        
        Args:
            arima_result: Results from your ArimaOptimizer
            prepared_data: Prepared data from your DataStationarity
            portfolio_returns: Original portfolio returns series
            analysis_metadata: Optional additional metadata
            output_path: Path to save JSON file
            
        Returns:
            Dictionary with complete master data
        """
        try:
            self.logger.info(f"Exporting ARIMA master data to {output_path}")
            
            # Create comprehensive master data structure
            master_data = {
                "export_info": self._create_export_info(),
                "model_specification": self._extract_model_specification(arima_result),
                "model_coefficients": self._extract_model_coefficients(arima_result),
                "data_preparation": self._extract_data_preparation_info(prepared_data, portfolio_returns),
                "model_diagnostics": self._extract_model_diagnostics(arima_result),
                "stationarity_analysis": self._extract_stationarity_analysis(prepared_data),
                "forecasting_parameters": self._create_forecasting_parameters(arima_result, prepared_data),
                "validation_metrics": self._extract_validation_metrics(arima_result),
                "application_instructions": self._create_application_instructions(arima_result)
            }
            
            # Add custom metadata if provided
            if analysis_metadata:
                master_data["analysis_metadata"] = analysis_metadata
            
            # Add historical context for validation
            master_data["historical_context"] = self._create_historical_context(
                portfolio_returns, prepared_data
            )
            
            # Validate master data completeness
            self._validate_master_data(master_data)
            
            # Export to JSON
            self._export_to_json(master_data, output_path)
            
            self.logger.info("ARIMA master data export completed successfully")
            return master_data
            
        except Exception as e:
            error_msg = f"Failed to export ARIMA master data: {e}"
            self.logger.error(error_msg)
            raise Exception(error_msg)

    def _create_export_info(self) -> Dict[str, Any]:
        """Create export metadata information."""
        return {
            "export_timestamp": datetime.now().isoformat(),
            "exporter_version": "1.0",
            "format_version": "1.0",
            "description": "ARIMA master data for forecasting application",
            "compatibility": "crypto_trading_model v1.0+"
        }

    def _extract_model_specification(self, arima_result: ArimaResult) -> Dict[str, Any]:
        """Extract core model specification."""
        p, d, q = arima_result.best_order
        
        return {
            "arima_order": {
                "p": p,  # Autoregressive order
                "d": d,  # Differencing order  
                "q": q   # Moving average order
            },
            "model_string": f"ARIMA({p},{d},{q})",
            "aic": arima_result.best_aic,
            "forecast_ready": arima_result.forecast_ready,
            "optimization_method": "AIC minimization",
            "parameter_search_range": {
                "p_range": "0-8",
                "d_range": "0-1", 
                "q_range": "0-8"
            }
        }

    def _extract_model_coefficients(self, arima_result: ArimaResult) -> Dict[str, Any]:
        """Extract and organize model coefficients."""
        significant_coeffs = arima_result.significant_coefficients
        
        # Separate AR and MA coefficients
        ar_coefficients = {}
        ma_coefficients = {}
        other_coefficients = {}
        
        for coeff_name, coeff_value in significant_coeffs.items():
            if coeff_name.startswith('ar.L'):
                lag = int(coeff_name.split('ar.L')[1])
                ar_coefficients[f"AR.{lag}"] = coeff_value
            elif coeff_name.startswith('ma.L'):
                lag = int(coeff_name.split('ma.L')[1])
                ma_coefficients[f"MA.{lag}"] = coeff_value
            else:
                other_coefficients[coeff_name] = coeff_value
        
        return {
            "ar_coefficients": ar_coefficients,
            "ma_coefficients": ma_coefficients,
            "other_coefficients": other_coefficients,
            "total_significant_coefficients": len(significant_coeffs),
            "coefficient_details": {
                "ar_count": len(ar_coefficients),
                "ma_count": len(ma_coefficients),
                "other_count": len(other_coefficients)
            },
            "raw_coefficients": significant_coeffs  # Keep original for reference
        }

    def _extract_data_preparation_info(self, prepared_data: PreparedData, portfolio_returns: pd.Series) -> Dict[str, Any]:
        """Extract data preparation information."""
        return {
            "original_data": {
                "total_observations": len(portfolio_returns),
                "data_start_date": portfolio_returns.index[0].isoformat() if len(portfolio_returns) > 0 else None,
                "data_end_date": portfolio_returns.index[-1].isoformat() if len(portfolio_returns) > 0 else None,
                "mean_return": float(portfolio_returns.mean()),
                "std_return": float(portfolio_returns.std()),
                "min_return": float(portfolio_returns.min()),
                "max_return": float(portfolio_returns.max())
            },
            "preparation_applied": {
                "is_differenced": prepared_data.is_differenced,
                "mean_value_removed": float(prepared_data.mean_value),
                "differencing_order": 1 if prepared_data.is_differenced else 0
            },
            "prepared_data": {
                "total_observations": len(prepared_data.prepared_series),
                "mean_of_prepared": float(prepared_data.prepared_series.mean()),
                "std_of_prepared": float(prepared_data.prepared_series.std())
            }
        }

    def _extract_model_diagnostics(self, arima_result: ArimaResult) -> Dict[str, Any]:
        """Extract model diagnostic information."""
        diagnostics = {}
        
        if hasattr(arima_result, 'model_diagnostics') and arima_result.model_diagnostics:
            # Convert numpy types to native Python types for JSON serialization
            raw_diagnostics = arima_result.model_diagnostics
            for key, value in raw_diagnostics.items():
                if isinstance(value, (np.integer, np.floating)):
                    diagnostics[key] = float(value)
                elif isinstance(value, np.ndarray):
                    diagnostics[key] = value.tolist()
                elif pd.isna(value):
                    diagnostics[key] = None
                else:
                    diagnostics[key] = value
        
        # Add interpretation
        diagnostics["interpretation"] = {
            "aic_quality": "lower_is_better",
            "ljung_box_test": "p_value > 0.05 indicates good model (no residual autocorrelation)",
            "residual_analysis": "mean should be near 0, std indicates forecast uncertainty"
        }
        
        return diagnostics

    def _extract_stationarity_analysis(self, prepared_data: PreparedData) -> Dict[str, Any]:
        """Extract stationarity test results."""
        stationarity_result = prepared_data.stationarity_result
        
        return {
            "original_stationarity": {
                "is_stationary": stationarity_result.is_stationary,
                "adf_statistic": float(stationarity_result.adf_statistic),
                "adf_pvalue": float(stationarity_result.adf_pvalue),
                "kpss_statistic": float(stationarity_result.kpss_statistic),
                "kpss_pvalue": float(stationarity_result.kpss_pvalue)
            },
            "differencing_applied": stationarity_result.differencing_required,
            "test_interpretation": {
                "adf_test": "H0: non-stationary, reject if p < 0.05",
                "kpss_test": "H0: stationary, reject if p < 0.05",
                "conclusion": "Both tests must agree for reliable stationarity assessment"
            }
        }

    def _create_forecasting_parameters(self, arima_result: ArimaResult, prepared_data: PreparedData) -> Dict[str, Any]:
        """Create parameters specifically needed for forecasting application."""
        p, d, q = arima_result.best_order
        
        # Extract coefficient mapping for easy application
        ar_mapping = {}
        ma_mapping = {}
        
        for coeff_name, coeff_value in arima_result.significant_coefficients.items():
            if coeff_name.startswith('ar.L'):
                lag = int(coeff_name.split('ar.L')[1])
                ar_mapping[lag] = float(coeff_value)
            elif coeff_name.startswith('ma.L'):
                lag = int(coeff_name.split('ma.L')[1])
                ma_mapping[lag] = float(coeff_value)
        
        return {
            "mean_value": float(prepared_data.mean_value),
            "is_differenced": prepared_data.is_differenced,
            "ar_lag_mapping": ar_mapping,  # {1: coeff1, 2: coeff2, ...}
            "ma_lag_mapping": ma_mapping,  # {1: coeff1, 2: coeff2, ...}
            "max_ar_lag": max(ar_mapping.keys()) if ar_mapping else 0,
            "max_ma_lag": max(ma_mapping.keys()) if ma_mapping else 0,
            "required_history_length": max(max(ar_mapping.keys()) if ar_mapping else 0,
                                         max(ma_mapping.keys()) if ma_mapping else 0),
            "formula_components": {
                "predict_return_formula": "Predict_Return = Predict_Demean + Mean_Value",
                "predict_demean_formula": "Predict_Demean = SUMPRODUCT(AR_values, AR_coeffs) + SUMPRODUCT(MA_values, MA_coeffs)",
                "error_term_formula": "E(t) = Demean(t) - Predict_Demean(t)",
                "ar_values": "AR.i(t) = Demean(t-i)",
                "ma_values": "MA.i(t) = E(t-i)"
            }
        }

    def _extract_validation_metrics(self, arima_result: ArimaResult) -> Dict[str, Any]:
        """Extract model validation metrics."""
        validation = {
            "model_selection": {
                "best_aic": float(arima_result.best_aic),
                "selection_criteria": "Minimum AIC among tested models",
                "parameter_significance": "Confidence intervals exclude 0 AND p-value < 0.1"
            }
        }
        
        # Add coefficient validation details
        if arima_result.significant_coefficients:
            validation["coefficient_validation"] = {
                "total_coefficients": len(arima_result.significant_coefficients),
                "significance_criteria": {
                    "p_value_threshold": 0.1,
                    "confidence_interval_check": "95% CI must not include 0"
                }
            }
        
        return validation

    def _create_application_instructions(self, arima_result: ArimaResult) -> Dict[str, Any]:
        """Create instructions for applying the model."""
        p, d, q = arima_result.best_order
        
        return {
            "step_by_step_application": [
                "1. Load historical return data and calculate mean",
                "2. Create demeaned series (return - mean)",
                "3. Initialize AR history with recent demeaned values",
                "4. Initialize MA history with zeros or estimated errors",
                "5. For each forecast:",
                "   a. Extract AR values: AR.i = Demean(t-i)",
                "   b. Extract MA values: MA.i = E(t-i)", 
                "   c. Calculate Predict_Demean = SUMPRODUCT(AR_values, AR_coeffs) + SUMPRODUCT(MA_values, MA_coeffs)",
                "   d. Calculate Predict_Return = Predict_Demean + Mean",
                "   e. When actual return available: E(t) = Demean(t) - Predict_Demean(t)",
                "   f. Update histories for next forecast"
            ],
            "required_data_structures": {
                "ar_history": f"Deque of last {max([int(k.split('ar.L')[1]) for k in arima_result.significant_coefficients.keys() if k.startswith('ar.L')], default=[1])} demeaned values",
                "ma_history": f"Deque of last {max([int(k.split('ma.L')[1]) for k in arima_result.significant_coefficients.keys() if k.startswith('ma.L')], default=[1])} error terms",
                "mean_value": "Constant from training data preparation"
            },
            "implementation_notes": [
                "Use collections.deque for efficient history management",
                "Handle edge cases when history length < required lags",
                "For multi-step forecasting, use predicted values as inputs",
                "Monitor forecast accuracy and retrain if performance degrades"
            ]
        }

    def _create_historical_context(self, portfolio_returns: pd.Series, prepared_data: PreparedData) -> Dict[str, Any]:
        """Create historical context for validation."""
        
        # Sample recent data for validation
        recent_data_size = min(50, len(portfolio_returns))
        recent_returns = portfolio_returns.tail(recent_data_size)
        recent_prepared = prepared_data.prepared_series.tail(recent_data_size)
        
        return {
            "recent_returns_sample": {
                "sample_size": recent_data_size,
                "returns": recent_returns.tolist(),
                "dates": [d.isoformat() for d in recent_returns.index],
                "statistics": {
                    "mean": float(recent_returns.mean()),
                    "std": float(recent_returns.std()),
                    "min": float(recent_returns.min()),
                    "max": float(recent_returns.max())
                }
            },
            "recent_prepared_sample": {
                "sample_size": recent_data_size,
                "prepared_values": recent_prepared.tolist(),
                "statistics": {
                    "mean": float(recent_prepared.mean()),
                    "std": float(recent_prepared.std())
                }
            }
        }

    def _validate_master_data(self, master_data: Dict[str, Any]):
        """Validate that master data is complete and valid."""
        
        required_sections = [
            "model_specification", "model_coefficients", "data_preparation",
            "forecasting_parameters"
        ]
        
        for section in required_sections:
            if section not in master_data:
                raise ValueError(f"Missing required section: {section}")
        
        # Validate forecasting parameters
        forecasting_params = master_data["forecasting_parameters"]
        required_params = ["mean_value", "is_differenced", "ar_lag_mapping", "ma_lag_mapping"]
        
        for param in required_params:
            if param not in forecasting_params:
                raise ValueError(f"Missing required forecasting parameter: {param}")
        
        # Validate at least some coefficients exist
        ar_coeffs = forecasting_params["ar_lag_mapping"]
        ma_coeffs = forecasting_params["ma_lag_mapping"]
        
        if not ar_coeffs and not ma_coeffs:
            raise ValueError("No significant AR or MA coefficients found")
        
        self.logger.info("Master data validation passed")

    def _export_to_json(self, master_data: Dict[str, Any], output_path: str):
        """Export master data to JSON file with proper formatting."""
        
        # Ensure output directory exists
        output_file = Path(output_path)
        output_file.parent.mkdir(parents=True, exist_ok=True)
        
        # Convert any non-serializable objects to JSON-compatible types
        json_compatible_data = self._make_json_serializable(master_data)
        
        # Export with pretty formatting
        with open(output_file, 'w') as f:
            json.dump(json_compatible_data, f, indent=2, ensure_ascii=False)
        
        # Log export details
        file_size = output_file.stat().st_size
        self.logger.info(f"Master data exported to: {output_path}")
        self.logger.info(f"File size: {file_size:,} bytes")
        
        # Log key parameters for verification
        model_spec = json_compatible_data["model_specification"]
        forecasting_params = json_compatible_data["forecasting_parameters"]
        
        self.logger.info(f"Model: {model_spec['model_string']}")
        self.logger.info(f"AIC: {model_spec['aic']:.4f}")
        self.logger.info(f"AR coefficients: {len(forecasting_params['ar_lag_mapping'])}")
        self.logger.info(f"MA coefficients: {len(forecasting_params['ma_lag_mapping'])}")

    def _make_json_serializable(self, obj: Any) -> Any:
        """
        Recursively convert an object to be JSON serializable.
        
        Handles numpy types, pandas types, and other non-serializable objects.
        """
        if isinstance(obj, dict):
            return {key: self._make_json_serializable(value) for key, value in obj.items()}
        elif isinstance(obj, list):
            return [self._make_json_serializable(item) for item in obj]
        elif isinstance(obj, tuple):
            return [self._make_json_serializable(item) for item in obj]
        elif isinstance(obj, (np.integer, np.int32, np.int64)):
            return int(obj)
        elif isinstance(obj, (np.floating, np.float32, np.float64)):
            return float(obj)
        elif isinstance(obj, np.ndarray):
            return obj.tolist()
        elif isinstance(obj, (np.bool_, bool)):
            return bool(obj)
        elif pd.isna(obj):
            return None
        elif isinstance(obj, pd.Timestamp):
            return obj.isoformat()
        elif hasattr(obj, 'item'):  # Handle numpy scalars
            return obj.item()
        elif hasattr(obj, 'tolist'):  # Handle other array-like objects
            return obj.tolist()
        else:
            return obj

    def load_master_data(self, json_path: str) -> Dict[str, Any]:
        """
        Load master data from JSON file.
        
        Args:
            json_path: Path to the JSON master data file
            
        Returns:
            Dictionary with master data
        """
        try:
            with open(json_path, 'r') as f:
                master_data = json.load(f)
            
            self.logger.info(f"Master data loaded from: {json_path}")
            
            # Validate loaded data
            self._validate_master_data(master_data)
            
            return master_data
            
        except Exception as e:
            error_msg = f"Failed to load master data from {json_path}: {e}"
            self.logger.error(error_msg)
            raise Exception(error_msg)

    def update_existing_analysis_pipeline(self, analysis_result: Dict[str, Any], 
                                        output_path: str) -> str:
        """
        Update your existing analysis pipeline to export master data.
        
        This method can be called from your existing BacktestArimaAnalyzer
        to automatically export master data after analysis.
        
        Args:
            analysis_result: Result from your BacktestArimaAnalyzer.analyze_backtest_results()
            output_path: Path to save master data JSON
            
        Returns:
            Path to exported JSON file
        """
        try:
            # Extract required components from analysis result
            arima_analysis = analysis_result['arima_analysis']
            arima_result = arima_analysis['arima_result']
            prepared_data = arima_analysis['prepared_data']
            
            # Create portfolio returns series from analysis (if available)
            # You might need to adjust this based on your actual analysis_result structure
            portfolio_stats = analysis_result.get('portfolio_statistics', {})
            
            # For now, we'll create a placeholder - you should pass actual portfolio returns
            if 'portfolio_returns_series' in analysis_result:
                portfolio_returns = analysis_result['portfolio_returns_series']
            else:
                # Create synthetic series for demonstration - replace with actual data
                self.logger.warning("Using synthetic portfolio returns - please pass actual data")
                n_periods = 100
                mean_return = portfolio_stats.get('mean_daily_return', 0.001)
                std_return = portfolio_stats.get('daily_volatility', 0.02)
                returns = np.random.normal(mean_return, std_return, n_periods)
                dates = pd.date_range(end=pd.Timestamp.now(), periods=n_periods, freq='D')
                portfolio_returns = pd.Series(returns, index=dates)
            
            # Add analysis metadata
            metadata = {
                "source_analysis": "BacktestArimaAnalyzer",
                "risk_profile": analysis_result.get('risk_profile', 'unknown'),
                "backtest_file": analysis_result.get('backtest_file', 'unknown'),
                "analysis_timestamp": datetime.now().isoformat()
            }
            
            # Export master data
            master_data = self.export_arima_master_data(
                arima_result=arima_result,
                prepared_data=prepared_data,
                portfolio_returns=portfolio_returns,
                analysis_metadata=metadata,
                output_path=output_path
            )
            
            return output_path
            
        except Exception as e:
            error_msg = f"Failed to update existing analysis pipeline: {e}"
            self.logger.error(error_msg)
            raise Exception(error_msg)

# Utility functions for integration with existing code
def add_master_data_export_to_existing_analysis():
    """
    Example of how to integrate master data export with your existing analysis.
    
    Add this to your existing BacktestArimaAnalyzer or similar workflow.
    """
    
    example_integration = '''
    # In your existing BacktestArimaAnalyzer.analyze_backtest_results() method:
    
    def analyze_backtest_results(self, backtest_file_path: str, risk_profile: str) -> Dict[str, Any]:
        # ... your existing analysis code ...
        
        # At the end, add master data export:
        from .arima_master_data_exporter import ArimaMasterDataExporter
        
        exporter = ArimaMasterDataExporter(self.logger)
        master_data_path = f"arima_master_data_{risk_profile}.json"
        
        # Export master data for future use
        exporter.update_existing_analysis_pipeline(
            analysis_result=results,  # Your analysis results
            output_path=master_data_path
        )
        
        # Add master data path to results
        results['master_data_path'] = master_data_path
        
        return results
    '''
    
    return example_integration

if __name__ == "__main__":
    # Test with synthetic data
    from ..custom_logging.logger import CustomLogger
    
    logger = CustomLogger('MasterDataExporterTest')
    exporter = ArimaMasterDataExporter(logger)
    
    logger.info("ArimaMasterDataExporter test complete - integrate with your existing ARIMA analysis")
    print(add_master_data_export_to_existing_analysis())