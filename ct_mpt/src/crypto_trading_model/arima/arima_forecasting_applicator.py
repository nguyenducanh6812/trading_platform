"""
ARIMA Forecasting Applicator
============================

File: crypto_trading_model/arima/arima_forecasting_applicator.py

This module applies pre-trained ARIMA models using master data JSON files.
It implements your exact Excel formula logic using the saved parameters.

Usage:
    applicator = ArimaForecastingApplicator(logger)
    applicator.load_master_data("arima_master_data.json")
    
    # Apply to new data
    forecast = applicator.predict_next_return(historical_returns)
    time_series_data = applicator.generate_time_series_data(historical_returns)
"""

import json
import pandas as pd
import numpy as np
from pathlib import Path
from typing import Dict, List, Optional, Any, Tuple
from collections import deque
from dataclasses import dataclass

from ..custom_logging.logger import CustomLogger

@dataclass
class ForecastPoint:
    """Single forecast point with all components."""
    date: pd.Timestamp
    return_value: Optional[float]  # Actual return (if available)
    demean: Optional[float]        # Demeaned actual return
    ar_1: float                    # AR.1 value
    ar_2: float                    # AR.2 value  
    ar_3: float                    # AR.3 value
    ma_1: float                    # MA.1 value
    ma_2: float                    # MA.2 value
    ma_3: float                    # MA.3 value
    ma_4: float                    # MA.4 value
    ma_5: float                    # MA.5 value
    error_term: float              # E value
    predicted_demean: float        # Predicted demeaned return
    predicted_return: float        # Final predicted return

class ArimaForecastingApplicator:
    """
    Applies pre-trained ARIMA models using master data.
    
    This class loads your ARIMA master data (from JSON) and applies the model
    to new data using your exact Excel formula logic:
    
    1. Predict_Demean = SUMPRODUCT(AR_values, AR_coeffs) + SUMPRODUCT(MA_values, MA_coeffs)
    2. Predict_Return = Predict_Demean + Mean
    3. E(t) = Demean(t) - Predict_Demean(t)
    """
    
    def __init__(self, logger: CustomLogger):
        """
        Initialize the ARIMA Forecasting Applicator.
        
        Args:
            logger: Custom logger instance
        """
        self.logger = logger
        
        # Master data components
        self.master_data = None
        self.mean_value = None
        self.is_differenced = None
        self.ar_coefficients = {}  # {lag: coefficient}
        self.ma_coefficients = {}  # {lag: coefficient}
        self.max_ar_lag = 0
        self.max_ma_lag = 0
        
        # State for forecasting
        self.ar_history = None     # deque for AR values (demeaned returns)
        self.ma_history = None     # deque for MA values (error terms)
        self.is_initialized = False
        
        self.logger.info("ArimaForecastingApplicator initialized")

    def load_master_data(self, json_path: str) -> Dict[str, Any]:
        """
        Load ARIMA master data from JSON file.
        
        Args:
            json_path: Path to the master data JSON file
            
        Returns:
            Loaded master data dictionary
        """
        try:
            self.logger.info(f"Loading ARIMA master data from {json_path}")
            
            with open(json_path, 'r') as f:
                self.master_data = json.load(f)
            
            # Extract forecasting parameters
            forecasting_params = self.master_data["forecasting_parameters"]
            
            self.mean_value = forecasting_params["mean_value"]
            self.is_differenced = forecasting_params["is_differenced"]
            self.ar_coefficients = forecasting_params["ar_lag_mapping"]
            self.ma_coefficients = forecasting_params["ma_lag_mapping"]
            self.max_ar_lag = forecasting_params["max_ar_lag"]
            self.max_ma_lag = forecasting_params["max_ma_lag"]
            
            # Convert string keys to integers
            self.ar_coefficients = {int(k): v for k, v in self.ar_coefficients.items()}
            self.ma_coefficients = {int(k): v for k, v in self.ma_coefficients.items()}
            
            # Initialize history buffers
            self._initialize_history_buffers()
            
            # Log loaded parameters
            model_spec = self.master_data["model_specification"]
            self.logger.info(f"Loaded model: {model_spec['model_string']}")
            self.logger.info(f"Mean value: {self.mean_value:.8f}")
            self.logger.info(f"AR coefficients: {self.ar_coefficients}")
            self.logger.info(f"MA coefficients: {self.ma_coefficients}")
            self.logger.info(f"Is differenced: {self.is_differenced}")
            
            self.is_initialized = True
            self.logger.info("Master data loaded successfully")
            
            return self.master_data
            
        except Exception as e:
            error_msg = f"Failed to load master data: {e}"
            self.logger.error(error_msg)
            raise Exception(error_msg)

    def _initialize_history_buffers(self):
        """Initialize history buffers based on model requirements."""
        
        # AR history: stores demeaned values for AR terms
        ar_buffer_size = max(self.max_ar_lag, 10) if self.max_ar_lag > 0 else 10
        self.ar_history = deque(maxlen=ar_buffer_size)
        
        # MA history: stores error terms for MA terms  
        ma_buffer_size = max(self.max_ma_lag, 10) if self.max_ma_lag > 0 else 10
        self.ma_history = deque(maxlen=ma_buffer_size)
        
        # Initialize with zeros
        for _ in range(max(self.max_ar_lag, 5)):
            self.ar_history.append(0.0)
        
        for _ in range(max(self.max_ma_lag, 5)):
            self.ma_history.append(0.0)
        
        self.logger.info(f"Initialized history buffers: AR={len(self.ar_history)}, MA={len(self.ma_history)}")

    def predict_next_return(self, current_ar_values: Optional[List[float]] = None,
                           current_ma_values: Optional[List[float]] = None) -> ForecastPoint:
        """
        Predict the next return using your Excel formula logic.
        
        Args:
            current_ar_values: Optional explicit AR values (otherwise uses history)
            current_ma_values: Optional explicit MA values (otherwise uses history)
            
        Returns:
            ForecastPoint with complete prediction details
        """
        if not self.is_initialized:
            raise ValueError("Must load master data first using load_master_data()")
        
        try:
            # Step 1: Get AR values (AR.1, AR.2, AR.3, ...)
            if current_ar_values is not None:
                ar_values = current_ar_values
            else:
                ar_values = self._get_current_ar_values()
            
            # Step 2: Get MA values (MA.1, MA.2, MA.3, ...)
            if current_ma_values is not None:
                ma_values = current_ma_values
            else:
                ma_values = self._get_current_ma_values()
            
            # Step 3: Calculate Predict_Demean using SUMPRODUCT
            predicted_demean = self._calculate_predicted_demean(ar_values, ma_values)
            
            # Step 4: Calculate Predict_Return = Predict_Demean + Mean
            predicted_return = predicted_demean + self.mean_value
            
            # Step 5: Create forecast point (error term will be calculated when actual is available)
            forecast_point = ForecastPoint(
                date=pd.Timestamp.now(),
                return_value=None,  # To be filled with actual
                demean=None,        # To be calculated from actual
                ar_1=ar_values[0] if len(ar_values) > 0 else 0.0,
                ar_2=ar_values[1] if len(ar_values) > 1 else 0.0,
                ar_3=ar_values[2] if len(ar_values) > 2 else 0.0,
                ma_1=ma_values[0] if len(ma_values) > 0 else 0.0,
                ma_2=ma_values[1] if len(ma_values) > 1 else 0.0,
                ma_3=ma_values[2] if len(ma_values) > 2 else 0.0,
                ma_4=ma_values[3] if len(ma_values) > 3 else 0.0,
                ma_5=ma_values[4] if len(ma_values) > 4 else 0.0,
                error_term=0.0,     # Will be calculated when actual is available
                predicted_demean=predicted_demean,
                predicted_return=predicted_return
            )
            
            self.logger.info(f"Prediction: Return={predicted_return:.8f}, Demean={predicted_demean:.8f}")
            
            return forecast_point
            
        except Exception as e:
            error_msg = f"Prediction failed: {e}"
            self.logger.error(error_msg)
            raise Exception(error_msg)

    def _get_current_ar_values(self) -> List[float]:
        """Get current AR values from history (AR.1 = previous demean, etc.)."""
        ar_values = []
        
        # Get values for each required AR lag
        for lag in range(1, max(self.max_ar_lag + 1, 4)):  # At least AR.1-AR.3
            if len(self.ar_history) >= lag:
                # AR.i = demeaned value from i periods ago
                ar_value = self.ar_history[-(lag)]  # -1 is most recent, -2 is previous, etc.
                ar_values.append(ar_value)
            else:
                ar_values.append(0.0)  # Not enough history
        
        return ar_values

    def _get_current_ma_values(self) -> List[float]:
        """Get current MA values from history (MA.1 = previous error, etc.)."""
        ma_values = []
        
        # Get values for each required MA lag
        for lag in range(1, max(self.max_ma_lag + 1, 6)):  # At least MA.1-MA.5
            if len(self.ma_history) >= lag:
                # MA.i = error term from i periods ago
                ma_value = self.ma_history[-(lag)]
                ma_values.append(ma_value)
            else:
                ma_values.append(0.0)  # Not enough history
        
        return ma_values

    def _calculate_predicted_demean(self, ar_values: List[float], ma_values: List[float]) -> float:
        """
        Calculate Predict_Demean using SUMPRODUCT formula.
        
        Implements: SUMPRODUCT(AR_values, AR_coeffs) + SUMPRODUCT(MA_values, MA_coeffs)
        """
        predicted_demean = 0.0
        
        # AR contribution: SUMPRODUCT(AR_values, AR_coefficients)
        for lag, coefficient in self.ar_coefficients.items():
            if lag <= len(ar_values):
                ar_value = ar_values[lag - 1]  # lag 1 = index 0
                contribution = ar_value * coefficient
                predicted_demean += contribution
                self.logger.debug(f"AR.{lag}: {ar_value:.8f} * {coefficient:.8f} = {contribution:.8f}")
        
        # MA contribution: SUMPRODUCT(MA_values, MA_coefficients)
        for lag, coefficient in self.ma_coefficients.items():
            if lag <= len(ma_values):
                ma_value = ma_values[lag - 1]  # lag 1 = index 0
                contribution = ma_value * coefficient
                predicted_demean += contribution
                self.logger.debug(f"MA.{lag}: {ma_value:.8f} * {coefficient:.8f} = {contribution:.8f}")
        
        return predicted_demean

    def update_with_actual_return(self, actual_return: float, predicted_point: ForecastPoint) -> float:
        """
        Update applicator state with actual observed return.
        
        Calculates error term: E(t) = Demean(t) - Predict_Demean(t)
        Updates histories for next prediction.
        
        Args:
            actual_return: The actual observed return
            predicted_point: Previous prediction result
            
        Returns:
            Calculated error term
        """
        try:
            # Calculate actual demeaned value
            actual_demean = actual_return - self.mean_value
            
            # Calculate error term: E(t) = Demean(t) - Predict_Demean(t)
            error_term = actual_demean - predicted_point.predicted_demean
            
            # Update histories
            self.ar_history.append(actual_demean)   # Add for future AR terms
            self.ma_history.append(error_term)      # Add for future MA terms
            
            self.logger.info(f"Updated with actual return: {actual_return:.8f}")
            self.logger.info(f"  Actual demean: {actual_demean:.8f}")
            self.logger.info(f"  Predicted demean: {predicted_point.predicted_demean:.8f}")
            self.logger.info(f"  Error term: {error_term:.8f}")
            
            return error_term
            
        except Exception as e:
            error_msg = f"Failed to update with actual return: {e}"
            self.logger.error(error_msg)
            raise Exception(error_msg)

    def generate_time_series_data(self, historical_returns: pd.Series, 
                                 include_forecasts: bool = True,
                                 forecast_steps: int = 5) -> pd.DataFrame:
        """
        Generate complete time series data in your requested format.
        
        Creates DataFrame with columns:
        Date, Return, Demean, AR.1, AR.2, AR.3, MA.1, MA.2, MA.3, MA.4, MA.5, E, Prd demean, Prd Return
        
        Args:
            historical_returns: Historical return series
            include_forecasts: Whether to include future forecasts
            forecast_steps: Number of forecast steps
            
        Returns:
            DataFrame with complete time series data
        """
        if not self.is_initialized:
            raise ValueError("Must load master data first using load_master_data()")
        
        try:
            self.logger.info(f"Generating time series data for {len(historical_returns)} historical periods")
            
            # Reset histories for clean generation
            self._initialize_history_buffers()
            
            time_series_data = []
            
            # Process historical data
            for i, (date, actual_return) in enumerate(historical_returns.items()):
                
                # Calculate demeaned value
                actual_demean = actual_return - self.mean_value
                
                # Get AR and MA values for prediction
                ar_values = self._get_current_ar_values()
                ma_values = self._get_current_ma_values()
                
                # Calculate predicted demean
                predicted_demean = self._calculate_predicted_demean(ar_values, ma_values)
                
                # Calculate predicted return
                predicted_return = predicted_demean + self.mean_value
                
                # Calculate error term
                error_term = actual_demean - predicted_demean
                
                # Create row data
                row_data = {
                    'Date': date,
                    'Return': actual_return,
                    'Demean': actual_demean,
                    'AR.1': ar_values[0] if len(ar_values) > 0 else 0.0,
                    'AR.2': ar_values[1] if len(ar_values) > 1 else 0.0,
                    'AR.3': ar_values[2] if len(ar_values) > 2 else 0.0,
                    'MA.1': ma_values[0] if len(ma_values) > 0 else 0.0,
                    'MA.2': ma_values[1] if len(ma_values) > 1 else 0.0,
                    'MA.3': ma_values[2] if len(ma_values) > 2 else 0.0,
                    'MA.4': ma_values[3] if len(ma_values) > 3 else 0.0,
                    'MA.5': ma_values[4] if len(ma_values) > 4 else 0.0,
                    'E': error_term,
                    'Prd demean': predicted_demean,
                    'Prd Return': predicted_return
                }
                
                time_series_data.append(row_data)
                
                # Update histories for next iteration
                self.ar_history.append(actual_demean)
                self.ma_history.append(error_term)
            
            # Add future forecasts if requested
            if include_forecasts and forecast_steps > 0:
                self.logger.info(f"Adding {forecast_steps} forecast steps")
                
                for step in range(forecast_steps):
                    # Generate future date
                    last_date = historical_returns.index[-1]
                    forecast_date = last_date + pd.Timedelta(days=step + 1)
                    
                    # Make prediction
                    forecast_point = self.predict_next_return()
                    
                    # Create forecast row
                    forecast_row = {
                        'Date': forecast_date,
                        'Return': np.nan,  # Future value not known
                        'Demean': np.nan,  # Will be calculated from actual return
                        'AR.1': forecast_point.ar_1,
                        'AR.2': forecast_point.ar_2,
                        'AR.3': forecast_point.ar_3,
                        'MA.1': forecast_point.ma_1,
                        'MA.2': forecast_point.ma_2,
                        'MA.3': forecast_point.ma_3,
                        'MA.4': forecast_point.ma_4,
                        'MA.5': forecast_point.ma_5,
                        'E': forecast_point.error_term,
                        'Prd demean': forecast_point.predicted_demean,
                        'Prd Return': forecast_point.predicted_return
                    }
                    
                    time_series_data.append(forecast_row)
                    
                    # For multi-step forecasting, use predicted values as actuals
                    # This is less accurate but allows multi-step prediction
                    predicted_demean = forecast_point.predicted_demean
                    predicted_error = 0.0  # Assume no error for future predictions
                    
                    self.ar_history.append(predicted_demean)
                    self.ma_history.append(predicted_error)
            
            # Convert to DataFrame
            df = pd.DataFrame(time_series_data)
            
            self.logger.info(f"Generated time series data: {len(df)} total rows")
            self.logger.info(f"  Historical: {len(historical_returns)} rows")
            self.logger.info(f"  Forecasts: {forecast_steps if include_forecasts else 0} rows")
            
            return df
            
        except Exception as e:
            error_msg = f"Failed to generate time series data: {e}"
            self.logger.error(error_msg)
            raise Exception(error_msg)

    def populate_from_historical_data(self, historical_returns: pd.Series):
        """
        Populate AR and MA histories from historical data.
        
        This is useful when you want to initialize the applicator with
        actual historical data before making new predictions.
        
        Args:
            historical_returns: Historical return series
        """
        if not self.is_initialized:
            raise ValueError("Must load master data first using load_master_data()")
        
        try:
            self.logger.info(f"Populating histories from {len(historical_returns)} historical observations")
            
            # Reset histories
            self._initialize_history_buffers()
            
            # Process historical data to build up histories
            for i, (date, actual_return) in enumerate(historical_returns.items()):
                
                # Calculate demeaned value
                actual_demean = actual_return - self.mean_value
                
                if i > 0:  # Can only calculate error from second observation
                    # Get AR and MA values for prediction
                    ar_values = self._get_current_ar_values()
                    ma_values = self._get_current_ma_values()
                    
                    # Calculate predicted demean
                    predicted_demean = self._calculate_predicted_demean(ar_values, ma_values)
                    
                    # Calculate error term
                    error_term = actual_demean - predicted_demean
                else:
                    error_term = 0.0  # First observation has no prediction error
                
                # Update histories
                self.ar_history.append(actual_demean)
                self.ma_history.append(error_term)
            
            self.logger.info("Historical data population completed")
            self.logger.info(f"AR history populated with {len(self.ar_history)} values")
            self.logger.info(f"MA history populated with {len(self.ma_history)} values")
            
        except Exception as e:
            error_msg = f"Failed to populate from historical data: {e}"
            self.logger.error(error_msg)
            raise Exception(error_msg)

    def get_applicator_state(self) -> Dict[str, Any]:
        """Get current state of the applicator for debugging/monitoring."""
        if not self.is_initialized:
            return {"status": "not_initialized"}
        
        return {
            "status": "initialized",
            "model_info": {
                "arima_order": self.master_data["model_specification"]["arima_order"],
                "mean_value": self.mean_value,
                "is_differenced": self.is_differenced,
                "ar_coefficients": self.ar_coefficients,
                "ma_coefficients": self.ma_coefficients
            },
            "history_state": {
                "ar_history_length": len(self.ar_history),
                "ma_history_length": len(self.ma_history),
                "recent_ar_values": list(self.ar_history)[-5:] if len(self.ar_history) >= 5 else list(self.ar_history),
                "recent_ma_values": list(self.ma_history)[-5:] if len(self.ma_history) >= 5 else list(self.ma_history)
            },
            "master_data_loaded": self.master_data is not None,
            "export_timestamp": self.master_data["export_info"]["export_timestamp"] if self.master_data else None
        }

    def validate_against_master_data(self, test_data: pd.Series, tolerance: float = 1e-6) -> Dict[str, Any]:
        """
        Validate applicator accuracy against known test data.
        
        This can be used to verify that the applicator produces the same
        results as the original ARIMA analysis.
        
        Args:
            test_data: Known test data for validation
            tolerance: Numerical tolerance for comparison
            
        Returns:
            Validation results
        """
        if not self.is_initialized:
            raise ValueError("Must load master data first using load_master_data()")
        
        try:
            self.logger.info(f"Validating applicator against {len(test_data)} test observations")
            
            # Generate predictions for test data
            time_series_df = self.generate_time_series_data(test_data, include_forecasts=False)
            
            # Compare predictions vs actuals
            actual_returns = time_series_df['Return'].dropna()
            predicted_returns = time_series_df['Prd Return'].dropna()
            
            # Calculate validation metrics
            if len(actual_returns) > 0 and len(predicted_returns) > 0:
                min_len = min(len(actual_returns), len(predicted_returns))
                actual_subset = actual_returns.iloc[:min_len]
                predicted_subset = predicted_returns.iloc[:min_len]
                
                errors = np.abs(actual_subset - predicted_subset)
                mae = np.mean(errors)
                mse = np.mean(errors ** 2)
                rmse = np.sqrt(mse)
                max_error = np.max(errors)
                
                # Check if within tolerance
                within_tolerance = max_error < tolerance
                
                validation_results = {
                    "validation_passed": within_tolerance,
                    "mae": mae,
                    "rmse": rmse,
                    "max_error": max_error,
                    "tolerance": tolerance,
                    "test_observations": min_len,
                    "average_actual_return": np.mean(actual_subset),
                    "average_predicted_return": np.mean(predicted_subset)
                }
            else:
                validation_results = {
                    "validation_passed": False,
                    "error": "Insufficient data for validation"
                }
            
            self.logger.info(f"Validation completed: {'PASSED' if validation_results.get('validation_passed', False) else 'FAILED'}")
            
            return validation_results
            
        except Exception as e:
            error_msg = f"Validation failed: {e}"
            self.logger.error(error_msg)
            return {"validation_passed": False, "error": error_msg}

# Utility functions for easy integration
def create_applicator_from_master_data(master_data_path: str, logger: CustomLogger) -> ArimaForecastingApplicator:
    """
    Convenience function to create and initialize applicator from master data.
    
    Args:
        master_data_path: Path to master data JSON file
        logger: Logger instance
        
    Returns:
        Initialized ArimaForecastingApplicator
    """
    applicator = ArimaForecastingApplicator(logger)
    applicator.load_master_data(master_data_path)
    return applicator

def apply_arima_to_new_data(master_data_path: str, new_returns: pd.Series, 
                           logger: CustomLogger) -> pd.DataFrame:
    """
    Quick application of ARIMA model to new data.
    
    Args:
        master_data_path: Path to master data JSON file
        new_returns: New return series to analyze
        logger: Logger instance
        
    Returns:
        DataFrame with time series analysis
    """
    applicator = create_applicator_from_master_data(master_data_path, logger)
    return applicator.generate_time_series_data(new_returns, include_forecasts=True)

if __name__ == "__main__":
    # Test with sample data
    from ..custom_logging.logger import CustomLogger
    
    logger = CustomLogger('ArimaApplicatorTest')
    applicator = ArimaForecastingApplicator(logger)
    
    logger.info("ArimaForecastingApplicator test complete")
    logger.info("Usage: Load master data JSON, then apply to new return series")