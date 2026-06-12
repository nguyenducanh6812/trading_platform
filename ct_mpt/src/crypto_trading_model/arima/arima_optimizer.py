"""
ARIMA Parameter Optimization with Configurable Ranges
=====================================================

Updated version of arima_optimizer.py that uses configuration file
for parameter ranges instead of hard-coded values.

Key improvements:
1. Configurable p, d, q ranges from config.yaml
2. Configurable significance level and optimization parameters
3. Better logging of search space
4. Adaptive search strategies
"""

import pandas as pd
import numpy as np
import itertools
from dataclasses import dataclass
from typing import Optional, Tuple, Dict, List, Any
from statsmodels.tsa.arima.model import ARIMA
from statsmodels.stats.diagnostic import acorr_ljungbox
import warnings

from ..custom_logging.logger import CustomLogger
from ..config.config_manager import ConfigManager
from .data_stationarity import PreparedData

class ArimaOptimizationError(Exception):
    """Exception raised for ARIMA optimization errors."""
    pass

@dataclass
class ArimaResult:
    """Results from ARIMA optimization."""
    best_order: Tuple[int, int, int]  # (p, d, q)
    best_aic: float
    model_summary: str
    fitted_model: Any  # ARIMA model object
    significant_coefficients: Dict[str, float]
    model_diagnostics: Dict[str, Any]
    parameter_search_log: str
    forecast_ready: bool
    search_space_info: Dict[str, Any]  # NEW: Information about search space used

@dataclass
class CoefficientInfo:
    """Information about a model coefficient."""
    name: str
    value: float
    std_error: float
    p_value: float
    conf_interval: Tuple[float, float]
    is_significant: bool

class ArimaOptimizer:
    """
    Optimizes ARIMA model parameters using configurable ranges.
    
    Uses configuration file to determine:
    - Parameter search ranges (p, d, q)
    - Significance level for coefficient testing
    - Optimization parameters (max iterations, tolerance)
    """
    
    def __init__(self, logger: CustomLogger, config_manager: Optional[ConfigManager] = None):
        """
        Initialize the ArimaOptimizer with configurable parameters.
        
        Args:
            logger: Custom logger instance
            config_manager: Configuration manager (optional, will create if None)
        """
        self.logger = logger
        
        # Initialize configuration
        if config_manager is None:
            try:
                self.config_manager = ConfigManager(logger)
            except Exception as e:
                self.logger.warning(f"Could not load config manager: {e}")
                self.logger.info("Using default ARIMA parameters")
                self.config_manager = None
        else:
            self.config_manager = config_manager
        
        # Load ARIMA configuration
        self._load_arima_config()
        
        # Suppress ARIMA warnings during grid search
        warnings.filterwarnings('ignore', category=UserWarning)
        warnings.filterwarnings('ignore', category=RuntimeWarning)
    
    def _load_arima_config(self):
        """Load ARIMA configuration from config manager or use defaults."""
        
        if self.config_manager is not None:
            try:
                arima_config = self.config_manager.get_arima_config()
                
                # Set parameter ranges
                self.p_range = arima_config.get_p_range()
                self.d_range = arima_config.get_d_range()
                self.q_range = arima_config.get_q_range()
                
                # Set other parameters
                self.significance_level = arima_config.significance_level
                self.max_iterations = arima_config.max_iterations
                self.convergence_tolerance = arima_config.convergence_tolerance
                
                # Log loaded configuration
                self.logger.info("Loaded ARIMA configuration from config file:")
                self.logger.info(f"  p_range: {list(self.p_range)} (min={min(self.p_range)}, max={max(self.p_range)})")
                self.logger.info(f"  d_range: {list(self.d_range)} (min={min(self.d_range)}, max={max(self.d_range)})")
                self.logger.info(f"  q_range: {list(self.q_range)} (min={min(self.q_range)}, max={max(self.q_range)})")
                self.logger.info(f"  Significance level: {self.significance_level}")
                self.logger.info(f"  Total combinations: {arima_config.get_total_combinations()}")
                
            except Exception as e:
                self.logger.warning(f"Failed to load ARIMA config: {e}")
                self._use_default_config()
        else:
            self._use_default_config()
    
    def _use_default_config(self):
        """Use default ARIMA configuration."""
        self.logger.info("Using default ARIMA configuration:")
        
        # Default ranges (original hard-coded values)
        self.p_range = range(0, 9)  # 0-8 
        self.d_range = range(0, 2)  # 0-1  
        self.q_range = range(0, 9)  # 0-8
        
        # Default parameters
        self.significance_level = 0.1
        self.max_iterations = 1000
        self.convergence_tolerance = 1e-9
        
        self.logger.info(f"  p_range: {list(self.p_range)}")
        self.logger.info(f"  d_range: {list(self.d_range)}")
        self.logger.info(f"  q_range: {list(self.q_range)}")
        self.logger.info(f"  Total combinations: {len(self.p_range) * len(self.d_range) * len(self.q_range)}")
    
    def find_optimal_arima(self, prepared_data: PreparedData, 
                          custom_ranges: Optional[Dict[str, range]] = None) -> ArimaResult:
        """
        Find optimal ARIMA(p,d,q) parameters using AIC minimization.
        
        Args:
            prepared_data: Prepared data from DataStationarity
            custom_ranges: Optional custom parameter ranges {'p': range(...), 'd': range(...), 'q': range(...)}
            
        Returns:
            ArimaResult with optimal parameters and model details
            
        Raises:
            ArimaOptimizationError: If optimization fails
        """
        try:
            self.logger.info("Starting ARIMA parameter optimization using configurable AIC minimization")
            
            # Use custom ranges if provided
            if custom_ranges:
                self.logger.info("Using custom parameter ranges:")
                p_range = custom_ranges.get('p', self.p_range)
                d_range = custom_ranges.get('d', self.d_range) 
                q_range = custom_ranges.get('q', self.q_range)
                
                for param, param_range in [('p', p_range), ('d', d_range), ('q', q_range)]:
                    self.logger.info(f"  {param}_range: {list(param_range)}")
            else:
                p_range = self.p_range
                d_range = self.d_range
                q_range = self.q_range
            
            # Determine d parameter based on data preparation
            d_value = self._determine_d_parameter(prepared_data, d_range)
            
            # Perform grid search
            search_results = self._grid_search_arima(prepared_data.prepared_series, p_range, d_value, q_range)
            
            if not search_results:
                raise ArimaOptimizationError("No valid ARIMA models found during grid search")
            
            # Find best model
            best_result = min(search_results, key=lambda x: x['aic'])
            best_order = best_result['order']
            best_aic = best_result['aic']
            
            self.logger.info(f"Optimal ARIMA order found: {best_order} with AIC: {best_aic:.4f}")
            
            # Fit final model with best parameters
            final_model = self._fit_final_model(prepared_data.prepared_series, best_order)
            
            # Extract significant coefficients
            significant_coeffs = self._extract_significant_coefficients(final_model)
            
            # Perform model diagnostics
            diagnostics = self._perform_model_diagnostics(final_model, prepared_data.prepared_series)
            
            # Generate search log
            search_log = self._generate_search_log(search_results, best_order, p_range, d_range, q_range)
            
            # Create search space info
            search_space_info = {
                'p_range_used': list(p_range),
                'd_range_used': list(d_range),
                'q_range_used': list(q_range),
                'd_value_fixed': d_value,
                'total_combinations_tested': len(search_results),
                'total_combinations_possible': len(p_range) * len(q_range),  # d is fixed
                'significance_level': self.significance_level,
                'source': 'config_file' if self.config_manager else 'default'
            }
            
            # Create result object
            result = ArimaResult(
                best_order=best_order,
                best_aic=best_aic,
                model_summary=str(final_model.summary()),
                fitted_model=final_model,
                significant_coefficients=significant_coeffs,
                model_diagnostics=diagnostics,
                parameter_search_log=search_log,
                forecast_ready=len(significant_coeffs) > 0,
                search_space_info=search_space_info
            )
            
            self.logger.info(f"ARIMA optimization completed: {len(significant_coeffs)} significant coefficients found")
            self.logger.info(f"Search space: {search_space_info['total_combinations_tested']}/{search_space_info['total_combinations_possible']} models tested")
            
            return result
            
        except Exception as e:
            error_msg = f"ARIMA optimization failed: {e}"
            self.logger.error(error_msg)
            raise ArimaOptimizationError(error_msg)
    
    def _determine_d_parameter(self, prepared_data: PreparedData, d_range: range) -> int:
        """Determine the d parameter based on data preparation results and config range."""
        
        # Determine optimal d based on stationarity testing
        if prepared_data.is_differenced:
            optimal_d = 1
            self.logger.info("Data was differenced during preparation, suggesting d=1")
        else:
            optimal_d = 0
            self.logger.info("Data was already stationary, suggesting d=0")
        
        # Check if optimal d is within configured range
        if optimal_d in d_range:
            d_value = optimal_d
            self.logger.info(f"Using optimal d={d_value} (within configured range {list(d_range)})")
        else:
            # Use closest value in range
            d_value = min(d_range, key=lambda x: abs(x - optimal_d))
            self.logger.warning(f"Optimal d={optimal_d} not in configured range {list(d_range)}")
            self.logger.info(f"Using closest value d={d_value}")
        
        return d_value
    
    def _grid_search_arima(self, series: pd.Series, p_range: range, d_value: int, q_range: range) -> List[Dict[str, Any]]:
        """
        Perform grid search over ARIMA parameters.
        
        Args:
            series: Prepared time series
            p_range: Range of p parameters to test
            d_value: Fixed d parameter
            q_range: Range of q parameters to test
            
        Returns:
            List of successful model results with AIC values
        """
        results = []
        total_combinations = len(p_range) * len(q_range)
        
        self.logger.info(f"Grid search configuration:")
        self.logger.info(f"  p_range: {list(p_range)} ({len(p_range)} values)")
        self.logger.info(f"  d_value: {d_value} (fixed)")
        self.logger.info(f"  q_range: {list(q_range)} ({len(q_range)} values)")
        self.logger.info(f"  Total combinations: {total_combinations}")
        self.logger.info(f"  Max iterations per model: {self.max_iterations}")
        self.logger.info(f"  Convergence tolerance: {self.convergence_tolerance}")
        
        successful_fits = 0
        failed_fits = 0
        
        for p in p_range:
            for q in q_range:
                order = (p, d_value, q)
                
                try:
                    # Fit ARIMA model with configured parameters
                    model = ARIMA(series, order=order)
                    fitted_model = model.fit(
                        maxiter=self.max_iterations,
                        tolerance=self.convergence_tolerance,
                        disp=False
                    )
                    
                    # Store successful result
                    results.append({
                        'order': order,
                        'aic': fitted_model.aic,
                        'bic': fitted_model.bic,
                        'llf': fitted_model.llf,
                        'model': fitted_model
                    })
                    
                    successful_fits += 1
                    
                    # Log progress for significant models
                    if successful_fits % 10 == 0 or fitted_model.aic < 1000:
                        self.logger.info(f"Progress: {successful_fits}/{total_combinations} - Current: ARIMA{order}, AIC: {fitted_model.aic:.4f}")
                
                except Exception as e:
                    failed_fits += 1
                    # Only log if debugging or if many failures
                    if failed_fits % 20 == 0:
                        self.logger.warning(f"ARIMA{order} failed to fit: {str(e)[:100]}")
                    continue
        
        self.logger.info(f"Grid search completed: {successful_fits} successful fits, {failed_fits} failures")
        success_rate = (successful_fits / total_combinations) * 100 if total_combinations > 0 else 0
        self.logger.info(f"Success rate: {success_rate:.1f}%")
        
        if results:
            # Sort by AIC and log top 5
            results.sort(key=lambda x: x['aic'])
            self.logger.info("Top 5 models by AIC:")
            for i, result in enumerate(results[:5]):
                self.logger.info(f"  {i+1}. ARIMA{result['order']}: AIC={result['aic']:.4f}")
        
        return results
    
    def _fit_final_model(self, series: pd.Series, order: Tuple[int, int, int]) -> Any:
        """Fit the final ARIMA model with optimal parameters."""
        try:
            self.logger.info(f"Fitting final ARIMA{order} model with configured parameters...")
            model = ARIMA(series, order=order)
            fitted_model = model.fit(
                maxiter=self.max_iterations,
                tolerance=self.convergence_tolerance,
                disp=False
            )
            self.logger.info("Final model fitted successfully")
            return fitted_model
        except Exception as e:
            raise ArimaOptimizationError(f"Failed to fit final ARIMA{order} model: {e}")
    
    def _extract_significant_coefficients(self, model: Any) -> Dict[str, float]:
        """
        Extract significant coefficients using configured significance level.
        
        Uses the significance level from configuration (default 0.1) for both:
        - p-value threshold
        - Confidence interval calculation
        """
        significant_coeffs = {}
        
        try:
            # Get model parameters and their statistics
            params = model.params
            pvalues = model.pvalues
            conf_int = model.conf_int(alpha=self.significance_level)  # Use configured significance level
            
            self.logger.info(f"Analyzing {len(params)} model coefficients for significance...")
            self.logger.info(f"Using significance level: {self.significance_level} (p-value threshold and CI)")
            
            coefficient_details = []
            
            for param_name in params.index:
                coeff_value = params[param_name]
                p_value = pvalues[param_name]
                ci_lower = conf_int.loc[param_name, 0]
                ci_upper = conf_int.loc[param_name, 1]
                
                # Check significance criteria using configured level
                p_value_significant = p_value < self.significance_level
                ci_excludes_zero = (ci_lower > 0 and ci_upper > 0) or (ci_lower < 0 and ci_upper < 0)
                
                is_significant = p_value_significant and ci_excludes_zero
                
                coefficient_details.append(CoefficientInfo(
                    name=param_name,
                    value=coeff_value,
                    std_error=model.bse[param_name] if param_name in model.bse.index else np.nan,
                    p_value=p_value,
                    conf_interval=(ci_lower, ci_upper),
                    is_significant=is_significant
                ))
                
                if is_significant:
                    significant_coeffs[param_name] = coeff_value
                    self.logger.info(f"Significant coefficient: {param_name} = {coeff_value:.6f} (p={p_value:.4f}, CI=[{ci_lower:.6f}, {ci_upper:.6f}])")
                else:
                    reason = []
                    if not p_value_significant:
                        reason.append(f"p-value={p_value:.4f} >= {self.significance_level}")
                    if not ci_excludes_zero:
                        reason.append(f"CI=[{ci_lower:.6f}, {ci_upper:.6f}] includes 0")
                    self.logger.info(f"Non-significant coefficient: {param_name} = {coeff_value:.6f} ({', '.join(reason)})")
            
            self.logger.info(f"Coefficient analysis complete: {len(significant_coeffs)}/{len(params)} coefficients are significant")
            
            return significant_coeffs
            
        except Exception as e:
            self.logger.error(f"Failed to extract significant coefficients: {e}")
            return {}
    
    def _perform_model_diagnostics(self, model: Any, series: pd.Series) -> Dict[str, Any]:
        """Perform comprehensive model diagnostics."""
        diagnostics = {}
        
        try:
            # Basic model statistics
            diagnostics['aic'] = model.aic
            diagnostics['bic'] = model.bic
            diagnostics['log_likelihood'] = model.llf
            diagnostics['num_params'] = len(model.params)
            diagnostics['num_observations'] = len(series)
            
            # Residual analysis
            residuals = model.resid
            diagnostics['residual_mean'] = residuals.mean()
            diagnostics['residual_std'] = residuals.std()
            diagnostics['residual_skewness'] = residuals.skew()
            diagnostics['residual_kurtosis'] = residuals.kurtosis()
            
            # Ljung-Box test for residual autocorrelation
            try:
                ljung_box = acorr_ljungbox(residuals, lags=10, return_df=True)
                diagnostics['ljung_box_pvalue'] = ljung_box['lb_pvalue'].iloc[-1]  # Last lag p-value
                diagnostics['residuals_are_white_noise'] = diagnostics['ljung_box_pvalue'] > 0.05
            except Exception as e:
                self.logger.warning(f"Ljung-Box test failed: {e}")
                diagnostics['ljung_box_pvalue'] = np.nan
                diagnostics['residuals_are_white_noise'] = None
            
            # In-sample fit quality
            fitted_values = model.fittedvalues
            mse = np.mean((series - fitted_values) ** 2)
            mae = np.mean(np.abs(series - fitted_values))
            diagnostics['in_sample_mse'] = mse
            diagnostics['in_sample_mae'] = mae
            diagnostics['in_sample_rmse'] = np.sqrt(mse)
            
            self.logger.info(f"Model diagnostics: AIC={diagnostics['aic']:.4f}, RMSE={diagnostics['in_sample_rmse']:.6f}")
            self.logger.info(f"Residual analysis: mean={diagnostics['residual_mean']:.6f}, std={diagnostics['residual_std']:.6f}")
            
            if diagnostics.get('residuals_are_white_noise') is not None:
                white_noise_status = "PASS" if diagnostics['residuals_are_white_noise'] else "FAIL"
                self.logger.info(f"Ljung-Box test (residual autocorrelation): {white_noise_status} (p={diagnostics['ljung_box_pvalue']:.4f})")
            
        except Exception as e:
            self.logger.error(f"Model diagnostics failed: {e}")
            diagnostics['error'] = str(e)
        
        return diagnostics
    
    def _generate_search_log(self, search_results: List[Dict], best_order: Tuple[int, int, int],
                           p_range: range, d_range: range, q_range: range) -> str:
        """Generate a comprehensive search log with configuration info."""
        log = []
        log.append("=== ARIMA PARAMETER SEARCH LOG ===")
        log.append(f"Configuration source: {'config file' if self.config_manager else 'default values'}")
        log.append(f"Search space: p={list(p_range)}, d={list(d_range)}, q={list(q_range)}")
        log.append(f"Significance level: {self.significance_level}")
        log.append(f"Max iterations: {self.max_iterations}")
        log.append(f"Convergence tolerance: {self.convergence_tolerance}")
        log.append(f"Total models tested: {len(search_results)}")
        log.append(f"Optimal model: ARIMA{best_order}")
        log.append("")
        
        if search_results:
            # Sort by AIC for ranking
            sorted_results = sorted(search_results, key=lambda x: x['aic'])
            
            log.append("Top 10 models by AIC:")
            for i, result in enumerate(sorted_results[:10]):
                order = result['order']
                aic = result['aic']
                bic = result['bic']
                log.append(f"  {i+1:2d}. ARIMA{order}: AIC={aic:8.4f}, BIC={bic:8.4f}")
            
            log.append("")
            log.append("AIC Distribution:")
            aic_values = [r['aic'] for r in sorted_results]
            log.append(f"  Min AIC: {min(aic_values):8.4f}")
            log.append(f"  Max AIC: {max(aic_values):8.4f}")
            log.append(f"  Mean AIC: {np.mean(aic_values):8.4f}")
            log.append(f"  Std AIC: {np.std(aic_values):8.4f}")
        
        return "\n".join(log)
    
    def get_model_forecast_ready_info(self, arima_result: ArimaResult) -> Dict[str, Any]:
        """Get information about model readiness for forecasting with config info."""
        info = {
            'forecast_ready': arima_result.forecast_ready,
            'num_significant_coefficients': len(arima_result.significant_coefficients),
            'model_order': arima_result.best_order,
            'model_aic': arima_result.best_aic,
            'search_space_info': arima_result.search_space_info,
            'configuration_used': {
                'significance_level': self.significance_level,
                'max_iterations': self.max_iterations,
                'convergence_tolerance': self.convergence_tolerance,
                'config_source': 'config_file' if self.config_manager else 'default'
            }
        }
        
        if arima_result.forecast_ready:
            info['significant_coefficients'] = arima_result.significant_coefficients
            info['next_steps'] = [
                "Model is ready for forecasting implementation",
                "Use significant coefficients for prediction formula",
                "Implement forecasting logic as per Model Formula Step 5"
            ]
        else:
            info['issues'] = []
            if len(arima_result.significant_coefficients) == 0:
                info['issues'].append("No significant coefficients found")
            
            info['next_steps'] = [
                "Review model specification",
                f"Consider different parameter ranges in config (current: p={arima_result.search_space_info['p_range_used']}, q={arima_result.search_space_info['q_range_used']})",
                f"Adjust significance level (current: {self.significance_level})",
                "Check data preparation steps",
                "Validate significance criteria"
            ]
        
        return info

    def update_search_ranges(self, p_range: Optional[Tuple[int, int]] = None,
                           d_range: Optional[Tuple[int, int]] = None,
                           q_range: Optional[Tuple[int, int]] = None,
                           significance_level: Optional[float] = None) -> None:
        """
        Update ARIMA search parameters dynamically.
        
        Args:
            p_range: New (min, max) for p parameter
            d_range: New (min, max) for d parameter  
            q_range: New (min, max) for q parameter
            significance_level: New significance level
        """
        if self.config_manager:
            # Update through config manager
            update_kwargs = {}
            if p_range:
                update_kwargs['p_range'] = p_range
            if d_range:
                update_kwargs['d_range'] = d_range
            if q_range:
                update_kwargs['q_range'] = q_range
            if significance_level:
                update_kwargs['significance_level'] = significance_level
            
            if update_kwargs:
                self.config_manager.update_arima_config(**update_kwargs)
                self._load_arima_config()  # Reload config
                self.logger.info(f"Updated ARIMA search parameters: {update_kwargs}")
        else:
            # Update directly
            if p_range:
                self.p_range = range(p_range[0], p_range[1] + 1)
            if d_range:
                self.d_range = range(d_range[0], d_range[1] + 1)
            if q_range:
                self.q_range = range(q_range[0], q_range[1] + 1)
            if significance_level:
                self.significance_level = significance_level
            
            self.logger.info("Updated ARIMA search parameters directly")