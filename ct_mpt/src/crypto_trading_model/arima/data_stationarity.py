"""
Data Stationarity Testing and Preparation for ARIMA
====================================================

This module implements stationarity testing and data preparation according to 
the Model Formula specifications:

1. Test stationarity using ADF test and KPSS test
2. Apply differencing if non-stationary: ΔY_t = Y_t - Y_{t-1}
3. Demean the data appropriately based on stationarity
"""

import pandas as pd
import numpy as np
from dataclasses import dataclass
from typing import Optional, Tuple, Dict, Any
from statsmodels.tsa.stattools import adfuller, kpss
from ..custom_logging.logger import CustomLogger

class StationarityError(Exception):
    """Exception raised for stationarity testing errors."""
    pass

@dataclass
class StationarityTestResult:
    """Results from stationarity testing."""
    is_stationary: bool
    adf_statistic: float
    adf_pvalue: float
    kpss_statistic: float
    kpss_pvalue: float
    adf_critical_values: Dict[str, float]
    kpss_critical_values: Dict[str, float]
    differencing_required: bool
    test_summary: str

@dataclass
class PreparedData:
    """Container for prepared ARIMA data."""
    original_series: pd.Series
    prepared_series: pd.Series  # Either demeaned original or demeaned differenced
    is_differenced: bool
    mean_value: float
    stationarity_result: StationarityTestResult
    preparation_log: str

class DataStationarity:
    """
    Handles stationarity testing and data preparation for ARIMA modeling.
    
    Implements the Model Formula steps:
    1. ADF and KPSS stationarity tests
    2. Differencing if non-stationary
    3. Appropriate demeaning based on stationarity
    """
    
    def __init__(self, logger: CustomLogger):
        """
        Initialize the DataStationarity class.
        
        Args:
            logger: Custom logger instance
        """
        self.logger = logger
    
    def test_stationarity(self, series: pd.Series, significance_level: float = 0.05) -> StationarityTestResult:
        """
        Test stationarity using both ADF and KPSS tests.
        
        Args:
            series: Time series to test
            significance_level: Significance level for tests (default 0.05)
            
        Returns:
            StationarityTestResult with comprehensive test results
            
        Raises:
            StationarityError: If testing fails
        """
        try:
            self.logger.info(f"Testing stationarity for series with {len(series)} observations")
            
            # Remove any NaN values
            clean_series = series.dropna()
            if len(clean_series) < 10:
                raise StationarityError(f"Insufficient data for stationarity testing: {len(clean_series)} observations")
            
            # Perform ADF test
            adf_result = self._perform_adf_test(clean_series)
            
            # Perform KPSS test  
            kpss_result = self._perform_kpss_test(clean_series)
            
            # Determine stationarity based on both tests
            is_stationary = self._determine_stationarity(
                adf_result, kpss_result, significance_level
            )
            
            # Create comprehensive result
            result = StationarityTestResult(
                is_stationary=is_stationary,
                adf_statistic=adf_result['adf_stat'],
                adf_pvalue=adf_result['adf_pvalue'],
                kpss_statistic=kpss_result['kpss_stat'],
                kpss_pvalue=kpss_result['kpss_pvalue'],
                adf_critical_values=adf_result['adf_critical'],
                kpss_critical_values=kpss_result['kpss_critical'],
                differencing_required=not is_stationary,
                test_summary=self._generate_test_summary(adf_result, kpss_result, is_stationary)
            )
            
            self.logger.info(f"Stationarity test completed: {'Stationary' if is_stationary else 'Non-stationary'}")
            self.logger.info(f"ADF p-value: {adf_result['adf_pvalue']:.6f}, KPSS p-value: {kpss_result['kpss_pvalue']:.6f}")
            
            return result
            
        except Exception as e:
            error_msg = f"Stationarity testing failed: {e}"
            self.logger.error(error_msg)
            raise StationarityError(error_msg)
    
    def _perform_adf_test(self, series: pd.Series) -> Dict[str, Any]:
        """Perform Augmented Dickey-Fuller test."""
        try:
            result = adfuller(series, autolag='AIC')
            return {
                'adf_stat': result[0],
                'adf_pvalue': result[1],
                'adf_critical': result[4]
            }
        except Exception as e:
            raise StationarityError(f"ADF test failed: {e}")
    
    def _perform_kpss_test(self, series: pd.Series) -> Dict[str, Any]:
        """Perform Kwiatkowski-Phillips-Schmidt-Shin test."""
        try:
            result = kpss(series, regression='c', nlags='auto')
            return {
                'kpss_stat': result[0],
                'kpss_pvalue': result[1],
                'kpss_critical': result[3]
            }
        except Exception as e:
            raise StationarityError(f"KPSS test failed: {e}")
    
    def _determine_stationarity(self, adf_result: Dict, kpss_result: Dict, 
                              significance_level: float) -> bool:
        """
        Determine stationarity based on both ADF and KPSS tests.
        
        Decision logic:
        - ADF: H0 = non-stationary, reject if p < α (stationary)
        - KPSS: H0 = stationary, reject if p < α (non-stationary)
        - Both tests must agree for definitive conclusion
        """
        adf_stationary = adf_result['adf_pvalue'] < significance_level
        kpss_stationary = kpss_result['kpss_pvalue'] >= significance_level
        
        if adf_stationary and kpss_stationary:
            return True  # Both tests indicate stationarity
        elif not adf_stationary and not kpss_stationary:
            return False  # Both tests indicate non-stationarity
        else:
            # Tests disagree - be conservative and assume non-stationary
            self.logger.warning("ADF and KPSS tests disagree. Being conservative: assuming non-stationary")
            return False
    
    def _generate_test_summary(self, adf_result: Dict, kpss_result: Dict, 
                             is_stationary: bool) -> str:
        """Generate a human-readable test summary."""
        summary = []
        summary.append("=== STATIONARITY TEST RESULTS ===")
        summary.append(f"Overall Result: {'STATIONARY' if is_stationary else 'NON-STATIONARY'}")
        summary.append("")
        summary.append("Augmented Dickey-Fuller (ADF) Test:")
        summary.append(f"  Statistic: {adf_result['adf_stat']:.6f}")
        summary.append(f"  p-value: {adf_result['adf_pvalue']:.6f}")
        summary.append(f"  Critical Values: {adf_result['adf_critical']}")
        summary.append(f"  Conclusion: {'Stationary' if adf_result['adf_pvalue'] < 0.05 else 'Non-stationary'}")
        summary.append("")
        summary.append("Kwiatkowski-Phillips-Schmidt-Shin (KPSS) Test:")
        summary.append(f"  Statistic: {kpss_result['kpss_stat']:.6f}")
        summary.append(f"  p-value: {kpss_result['kpss_pvalue']:.6f}")
        summary.append(f"  Critical Values: {kpss_result['kpss_critical']}")
        summary.append(f"  Conclusion: {'Stationary' if kpss_result['kpss_pvalue'] >= 0.05 else 'Non-stationary'}")
        
        return "\n".join(summary)
    
    def apply_differencing(self, series: pd.Series) -> pd.Series:
        """
        Apply first differencing: ΔY_t = Y_t - Y_{t-1}
        
        Args:
            series: Original time series
            
        Returns:
            Differenced series (one observation shorter)
        """
        self.logger.info("Applying first differencing to make series stationary")
        differenced = series.diff().dropna()
        self.logger.info(f"Differencing complete: {len(series)} -> {len(differenced)} observations")
        return differenced
    
    def demean_data(self, series: pd.Series, is_differenced: bool) -> Tuple[pd.Series, float]:
        """
        Demean the data according to Model Formula specifications.
        
        For stationary series: Ȳ_t = Y_t - mean(Y_t)
        For non-stationary series: ΔȲ_t = ΔY_t - mean(ΔY_t)
        
        Args:
            series: Series to demean (original or differenced)
            is_differenced: Whether this is a differenced series
            
        Returns:
            Tuple of (demeaned_series, mean_value)
        """
        mean_value = series.mean()
        demeaned_series = series - mean_value
        
        series_type = "differenced" if is_differenced else "original"
        self.logger.info(f"Demeaning {series_type} series: mean = {mean_value:.8f}")
        self.logger.info(f"Demeaned series mean: {demeaned_series.mean():.10f} (should be ~0)")
        
        return demeaned_series, mean_value
    
    def prepare_data(self, series: pd.Series, force_differencing: bool = False) -> PreparedData:
        """
        Complete data preparation pipeline for ARIMA modeling.
        
        Steps:
        1. Test stationarity
        2. Apply differencing if needed
        3. Demean appropriately
        
        Args:
            series: Original time series
            force_differencing: Force differencing even if stationary (for testing)
            
        Returns:
            PreparedData object with all preparation results
            
        Raises:
            StationarityError: If preparation fails
        """
        try:
            self.logger.info(f"Starting ARIMA data preparation for series: {series.name or 'unnamed'}")
            
            # Step 1: Test stationarity
            stationarity_result = self.test_stationarity(series)
            
            # Step 2: Apply differencing if needed
            is_differenced = False
            working_series = series.copy()
            
            if stationarity_result.differencing_required or force_differencing:
                working_series = self.apply_differencing(series)
                is_differenced = True
                
                # Re-test stationarity after differencing
                if not force_differencing:
                    post_diff_result = self.test_stationarity(working_series)
                    if not post_diff_result.is_stationary:
                        self.logger.warning("Series still non-stationary after differencing. May need d=2 or other transformations.")
            
            # Step 3: Demean the data
            demeaned_series, mean_value = self.demean_data(working_series, is_differenced)
            
            # Generate preparation log
            prep_log = self._generate_preparation_log(stationarity_result, is_differenced, mean_value)
            
            # Create result object
            prepared_data = PreparedData(
                original_series=series,
                prepared_series=demeaned_series,
                is_differenced=is_differenced,
                mean_value=mean_value,
                stationarity_result=stationarity_result,
                preparation_log=prep_log
            )
            
            self.logger.info("ARIMA data preparation completed successfully")
            self.logger.info(f"Final prepared series: {len(demeaned_series)} observations, differenced={is_differenced}")
            
            return prepared_data
            
        except Exception as e:
            error_msg = f"ARIMA data preparation failed: {e}"
            self.logger.error(error_msg)
            raise StationarityError(error_msg)
    
    def _generate_preparation_log(self, stationarity_result: StationarityTestResult, 
                                is_differenced: bool, mean_value: float) -> str:
        """Generate a comprehensive preparation log."""
        log = []
        log.append("=== ARIMA DATA PREPARATION LOG ===")
        log.append(f"Original series stationarity: {'YES' if stationarity_result.is_stationary else 'NO'}")
        log.append(f"Differencing applied: {'YES' if is_differenced else 'NO'}")
        log.append(f"Series type for demeaning: {'Differenced' if is_differenced else 'Original'}")
        log.append(f"Mean value removed: {mean_value:.8f}")
        log.append("")
        log.append("Stationarity Test Summary:")
        log.append(stationarity_result.test_summary)
        log.append("")
        log.append("Next Steps:")
        log.append("1. Use prepared (demeaned) series for ARIMA model fitting")
        log.append("2. Find optimal (p,d,q) parameters using AIC minimization")
        log.append(f"3. Use d={1 if is_differenced else 0} as starting point for d parameter")
        
        return "\n".join(log)
    
    def validate_for_arima(self, prepared_data: PreparedData, min_observations: int = 50) -> bool:
        """
        Validate that prepared data is suitable for ARIMA modeling.
        
        Args:
            prepared_data: Prepared data object
            min_observations: Minimum number of observations required
            
        Returns:
            True if data is suitable for ARIMA modeling
            
        Raises:
            StationarityError: If data is not suitable
        """
        series = prepared_data.prepared_series
        
        # Check minimum observations
        if len(series) < min_observations:
            raise StationarityError(f"Insufficient observations for ARIMA: {len(series)} < {min_observations}")
        
        # Check for excessive missing values
        if series.isna().sum() > len(series) * 0.1:
            raise StationarityError(f"Too many missing values: {series.isna().sum()}/{len(series)}")
        
        # Check for extreme values or infinite values
        if not np.isfinite(series).all():
            raise StationarityError("Series contains infinite or NaN values after preparation")
        
        # Check variance (avoid constant series)
        if series.var() < 1e-10:
            raise StationarityError("Series has near-zero variance - not suitable for ARIMA")
        
        self.logger.info("ARIMA validation passed: data is suitable for modeling")
        return True