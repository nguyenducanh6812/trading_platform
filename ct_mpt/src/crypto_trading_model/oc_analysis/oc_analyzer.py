"""
Main OC Analysis orchestrator module.
Coordinates all components following the Facade pattern and SOLID principles.
"""

from typing import Dict, Tuple, Optional
import os
from pathlib import Path

from .data_reader import DataReaderFactory
from .calculator import OCCalculator, MultiAssetOCAnalyzer
from .exporter import ExporterFactory, MultiAssetExporter


class OCAnalysisOrchestrator:
    """
    Main orchestrator for OC (Open-Close) analysis workflow.
    Follows Facade pattern to provide a simple interface for complex operations.
    Follows Dependency Inversion Principle by depending on abstractions.
    """
    
    def __init__(self, reader_type: str = 'crypto', export_format: str = 'csv'):
        """
        Initialize the OC Analysis orchestrator.
        
        Args:
            reader_type: Type of data reader to use ('crypto')
            export_format: Export format for results ('csv' or 'excel')
        """
        # Initialize components using factories (Dependency Injection)
        self.data_reader = DataReaderFactory.create_reader(reader_type)
        self.calculator = OCCalculator()
        self.multi_asset_analyzer = MultiAssetOCAnalyzer(self.calculator)
        
        self.exporter = ExporterFactory.create_exporter(export_format)
        self.multi_asset_exporter = MultiAssetExporter(self.exporter)
        
        self.export_format = export_format
    
    def run_full_analysis(self, 
                         sync_data_folder: str,
                         output_dir: str,
                         asset_names: Optional[list] = None,
                         configured_mean_diff_ocs: Optional[Dict[str, float]] = None) -> Dict[str, str]:
        """
        Run complete OC analysis workflow from data reading to export.
        
        Args:
            sync_data_folder: Path to sync_data_output folder containing BTC.csv and ETH.csv
            output_dir: Directory where analysis results should be saved
            asset_names: List of asset names to analyze (defaults to ['BTC', 'ETH'])
            configured_mean_diff_ocs: Optional dict of pre-configured mean_diff_oc values per asset.
                                    If provided, these values will be used for demeaning instead of 
                                    calculating from data. Set values to 0.0 for raw analysis.
            
        Returns:
            Dictionary with analysis results and file paths:
            {
                'sync_data_folder': str,
                'output_directory': str,
                'asset_files': Dict[str, str],  # Each file contains analysis data + master data sheet
                'mean_diff_ocs': Dict[str, float]
            }
            
        Raises:
            FileNotFoundError: If sync data folder or files don't exist
            ValueError: If data validation fails
            OSError: If output files cannot be written
        """
        if asset_names is None:
            asset_names = ['BTC', 'ETH']
        
        # Step 1: Read data
        print(f"Reading data from: {sync_data_folder}")
        data = self.data_reader.read_data(sync_data_folder)
        print(f"Loaded {len(data)} rows of data")
        
        # Step 2: Perform analysis
        print(f"Analyzing assets: {asset_names}")
        
        if configured_mean_diff_ocs is not None:
            print("Using configured mean_diff_oc values from config")
            asset_analyses, mean_diff_ocs = self.multi_asset_analyzer.analyze_all_assets_with_configured_means(
                data, asset_names, configured_mean_diff_ocs
            )
            print("Configured Mean_Diff_OC values used:")
        else:
            print("Calculating mean_diff_oc values from data")
            asset_analyses, mean_diff_ocs = self.multi_asset_analyzer.analyze_all_assets(data, asset_names)
            print("Calculated Mean_Diff_OC values:")
            
        for asset_name, mean_value in mean_diff_ocs.items():
            print(f"  {asset_name}: {mean_value:.6f}")
        
        # Step 3: Export results
        print(f"Exporting results to: {output_dir}")
        
        # Export each asset to its own file with individual master data sheet
        asset_files = self.multi_asset_exporter.export_assets_with_individual_master_data(
            asset_analyses, mean_diff_ocs, output_dir
        )
        
        print("Export completed successfully!")
        print("Generated files:")
        for asset_name, file_path in asset_files.items():
            print(f"  {asset_name}: {file_path} (includes master data sheet)")
        
        return {
            'sync_data_folder': sync_data_folder,
            'output_directory': output_dir,
            'asset_files': asset_files,
            'mean_diff_ocs': mean_diff_ocs
        }
    
    def analyze_single_asset(self, 
                           sync_data_folder: str,
                           asset_name: str,
                           output_dir: Optional[str] = None,
                           configured_mean_diff_oc: Optional[float] = None) -> Tuple[Dict, float]:
        """
        Analyze a single asset and optionally export results.
        
        Args:
            sync_data_folder: Path to sync_data_output folder containing crypto data
            asset_name: Name of the asset to analyze (e.g., 'BTC', 'ETH')
            output_dir: Directory where results should be saved (optional)
            configured_mean_diff_oc: Optional pre-configured mean_diff_oc value to use for demeaning
            
        Returns:
            Tuple of (analysis_results_dict, mean_diff_oc) where:
                - analysis_results_dict: Dictionary with analysis data
                - mean_diff_oc: Mean of Diff_OC values (configured or calculated)
        """
        # Read data
        data = self.data_reader.read_data(sync_data_folder)
        
        # Perform analysis
        if configured_mean_diff_oc is not None:
            configured_means = {asset_name: configured_mean_diff_oc}
            asset_analyses, mean_diff_ocs = self.multi_asset_analyzer.analyze_all_assets_with_configured_means(
                data, [asset_name], configured_means
            )
        else:
            asset_analyses, mean_diff_ocs = self.multi_asset_analyzer.analyze_all_assets(data, [asset_name])
        
        analysis_df = asset_analyses[asset_name]
        mean_diff_oc = mean_diff_ocs[asset_name]
        
        # Export if output directory is provided
        if output_dir:
            asset_files = self.multi_asset_exporter.export_all_assets(
                {asset_name: analysis_df}, output_dir, self.export_format
            )
            print(f"Exported {asset_name} analysis to: {asset_files[asset_name]}")
        
        # Convert DataFrame to dictionary for return
        analysis_dict = analysis_df.to_dict('records')
        
        return analysis_dict, mean_diff_oc
    
    def get_summary_statistics(self, 
                             sync_data_folder: str,
                             asset_names: Optional[list] = None) -> Dict[str, Dict[str, float]]:
        """
        Get summary statistics for OC analysis without exporting files.
        
        Args:
            sync_data_folder: Path to sync_data_output folder containing crypto data
            asset_names: List of asset names to analyze (defaults to ['BTC', 'ETH'])
            
        Returns:
            Dictionary mapping asset names to their summary statistics:
            {
                'BTC': {
                    'mean_oc': float,
                    'mean_diff_oc': float,
                    'std_diff_oc': float,
                    'count_valid_diff_oc': int
                },
                ...
            }
        """
        if asset_names is None:
            asset_names = ['BTC', 'ETH']
        
        # Read data and perform analysis
        data = self.data_reader.read_data(sync_data_folder)
        asset_analyses, mean_diff_ocs = self.multi_asset_analyzer.analyze_all_assets(data, asset_names)
        
        summary_stats = {}
        
        for asset_name in asset_names:
            analysis_df = asset_analyses[asset_name]
            
            # Calculate additional statistics
            oc_values = analysis_df['OC']
            diff_oc_values = analysis_df['Diff_OC'].dropna()  # Remove NaN values
            
            summary_stats[asset_name] = {
                'mean_oc': float(oc_values.mean()),
                'std_oc': float(oc_values.std()),
                'mean_diff_oc': mean_diff_ocs[asset_name],
                'std_diff_oc': float(diff_oc_values.std()),
                'count_valid_diff_oc': len(diff_oc_values)
            }
        
        return summary_stats