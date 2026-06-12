"""
OC ARIMA Main Module
===================

Main orchestrator for OC ARIMA analysis following modular monolith architecture.
Provides high-level interface for running ARIMA analysis on OC results.
"""

import logging
from pathlib import Path
from typing import Dict, Any, Optional
from dataclasses import dataclass

from .oc_arima_analyzer import OCArimaAnalyzer, ArimaConfig, ArimaResults, ArimaResultsExporter, ArimaAnalyzerError
from .oc_data_reader import DataReaderError

logger = logging.getLogger(__name__)

class OCArimaMainError(Exception):
    """Exception raised for main ARIMA module errors."""
    pass

@dataclass
class ArimaJobConfig:
    """Configuration for a complete ARIMA analysis job."""
    asset_code: str
    p: int
    file_directory: str
    d: int = 0  # Default to 0 as per requirement
    q: int = 0  # Default to 0 as per requirement
    column_name: str = "Demean_Diff_OC"
    output_directory: Optional[str] = None
    optimize_parameters: bool = False  # If True, will search for optimal p
    max_p: int = 10  # Maximum p value to test when optimizing
    
    def get_file_path(self) -> str:
        """Get the full file path for the asset."""
        file_directory = Path(self.file_directory)
        
        # Try different file naming conventions
        possible_files = [
            file_directory / f"{self.asset_code}_oc_analysis.xlsx",
            file_directory / f"{self.asset_code}_oc_analysis.csv",
            file_directory / f"{self.asset_code.lower()}_oc_analysis.xlsx",
            file_directory / f"{self.asset_code.lower()}_oc_analysis.csv",
        ]
        
        for file_path in possible_files:
            if file_path.exists():
                return str(file_path)
        
        # If no file found, raise error with helpful message
        raise OCArimaMainError(f"No OC analysis file found for asset {self.asset_code} "
                              f"in directory {file_directory}. "
                              f"Expected files: {[f.name for f in possible_files]}")

class OCArimaMain:
    """
    Main ARIMA analysis orchestrator.
    Coordinates data reading, analysis, and result export.
    """
    
    def __init__(self):
        """Initialize the ARIMA main module."""
        self.analyzer = OCArimaAnalyzer()
        self.exporter = ArimaResultsExporter()
        
        # Setup logging
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        )
    
    def run_analysis(self, job_config: ArimaJobConfig) -> Dict[str, Any]:
        """
        Run complete ARIMA analysis workflow.
        
        Args:
            job_config: Configuration for the analysis job
            
        Returns:
            Dictionary with analysis results and file paths
            
        Raises:
            OCArimaMainError: If analysis workflow fails
        """
        try:
            logger.info("=" * 50)
            logger.info("Starting OC ARIMA Analysis")
            logger.info("=" * 50)
            logger.info(f"Asset: {job_config.asset_code}")
            logger.info(f"Model: ARIMA({job_config.p}, {job_config.d}, {job_config.q})")
            logger.info(f"Column: {job_config.column_name}")
            logger.info(f"Directory: {job_config.file_directory}")
            
            # Step 1: Get file path
            file_path = job_config.get_file_path()
            logger.info(f"Using file: {file_path}")
            
            # Step 2: Create ARIMA config
            arima_config = ArimaConfig(
                asset_code=job_config.asset_code,
                p=job_config.p,
                d=job_config.d,
                q=job_config.q,
                file_path=file_path,
                column_name=job_config.column_name,
                optimize_parameters=job_config.optimize_parameters,
                max_p=job_config.max_p
            )
            
            # Step 3: Run analysis
            logger.info("Running ARIMA analysis...")
            results = self.analyzer.analyze(arima_config)
            
            # Step 4: Determine output path
            if job_config.output_directory:
                output_dir = Path(job_config.output_directory)
            else:
                output_dir = Path(job_config.file_directory)
            
            output_file = output_dir / f"{job_config.asset_code}_arima_results.json"
            
            # Step 5: Export results
            logger.info("Exporting results...")
            json_path = self.exporter.export_to_json(results, str(output_file))
            
            # Step 6: Create summary
            summary = self._create_analysis_summary(results, json_path)
            
            logger.info("=" * 50)
            logger.info("OC ARIMA Analysis Completed Successfully")
            logger.info("=" * 50)
            
            return summary
            
        except Exception as e:
            if isinstance(e, (OCArimaMainError, ArimaAnalyzerError, DataReaderError)):
                raise
            else:
                raise OCArimaMainError(f"ARIMA analysis workflow failed: {str(e)}")
    
    def _create_analysis_summary(self, results: ArimaResults, json_path: str) -> Dict[str, Any]:
        """
        Create a summary of the analysis results.
        
        Args:
            results: ARIMA analysis results
            json_path: Path to exported JSON file
            
        Returns:
            Dictionary with analysis summary
        """
        summary = {
            'asset_code': results.asset_code,
            'model_parameters': {
                'p': results.p,
                'd': results.d,
                'q': results.q
            },
            'model_fit': {
                'aic': results.aic,
                'bic': results.bic,
                'log_likelihood': results.log_likelihood
            },
            'coefficients': results.coefficients,
            'coefficient_significance': {
                name: pval < 0.05 for name, pval in results.coefficient_pvalues.items()
            },
            'data_summary': {
                'total_points': results.data_info['total_points'],
                'valid_points': results.data_info['valid_points'],
                'data_mean': results.data_info['mean'],
                'data_std': results.data_info['std']
            },
            'model_diagnostics': {
                'is_stationary': results.stationarity_test.get('is_stationary'),
                'residuals_white_noise': results.ljung_box_test.get('white_noise')
            },
            'files': {
                'input_file': results.file_path,
                'output_json': json_path
            },
            'analysis_timestamp': results.analysis_timestamp
        }
        
        return summary
    
    def validate_inputs(self, job_config: ArimaJobConfig) -> bool:
        """
        Validate job configuration inputs.
        
        Args:
            job_config: Configuration to validate
            
        Returns:
            True if valid, raises exception if invalid
            
        Raises:
            OCArimaMainError: If validation fails
        """
        # Validate asset code
        if not job_config.asset_code or not job_config.asset_code.strip():
            raise OCArimaMainError("Asset code cannot be empty")
        
        # Validate parameters
        if job_config.p < 0:
            raise OCArimaMainError(f"Parameter p must be non-negative, got {job_config.p}")
        if job_config.d < 0:
            raise OCArimaMainError(f"Parameter d must be non-negative, got {job_config.d}")
        if job_config.q < 0:
            raise OCArimaMainError(f"Parameter q must be non-negative, got {job_config.q}")
        
        # Validate directory
        if not Path(job_config.file_directory).exists():
            raise OCArimaMainError(f"File directory does not exist: {job_config.file_directory}")
        
        # Validate column name
        if not job_config.column_name or not job_config.column_name.strip():
            raise OCArimaMainError("Column name cannot be empty")
        
        # Warn about non-AR model
        if job_config.d != 0 or job_config.q != 0:
            logger.warning(f"Model ARIMA({job_config.p}, {job_config.d}, {job_config.q}) "
                          f"is not a pure AR model. Requirement specifies AR(p,0,0)")
        
        return True

def create_sample_config(asset_code: str = "BTC", p: int = 1) -> ArimaJobConfig:
    """
    Create a sample configuration for testing.
    
    Args:
        asset_code: Asset code (default: "BTC")
        p: AR parameter (default: 1)
        
    Returns:
        Sample ArimaJobConfig
    """
    return ArimaJobConfig(
        asset_code=asset_code,
        p=p,
        file_directory="oc_analysis_results",
        d=0,
        q=0,
        column_name="Demean_Diff_OC"
    )