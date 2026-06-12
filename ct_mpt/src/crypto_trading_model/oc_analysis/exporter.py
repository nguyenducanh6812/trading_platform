"""
Exporter module for OC (Open-Close) analysis results.
Follows SOLID principles with single responsibility for data export.
"""

from abc import ABC, abstractmethod
from typing import Dict, Any
import pandas as pd
import os
from pathlib import Path


class ExporterInterface(ABC):
    """Interface for exporters following the Interface Segregation Principle."""
    
    @abstractmethod
    def export_asset_data(self, data: pd.DataFrame, asset_name: str, output_path: str) -> None:
        """Export asset analysis data to file."""
        pass
    
    @abstractmethod
    def export_master_data(self, master_data: Dict[str, Any], output_path: str) -> None:
        """Export master data to file."""
        pass


class CSVExporter(ExporterInterface):
    """
    Concrete implementation for exporting data to CSV files.
    Follows Single Responsibility Principle - only responsible for CSV export.
    """
    
    def export_asset_data(self, data: pd.DataFrame, asset_name: str, output_path: str) -> None:
        """
        Export asset analysis data to CSV file.
        
        Args:
            data: DataFrame containing asset analysis results
            asset_name: Name of the asset (e.g., 'BTC', 'ETH')
            output_path: Path where the CSV file should be saved
            
        Raises:
            OSError: If the file cannot be written
            ValueError: If data is empty or invalid
        """
        if data.empty:
            raise ValueError(f"Cannot export empty data for asset {asset_name}")
        
        try:
            # Ensure output directory exists
            output_dir = Path(output_path).parent
            output_dir.mkdir(parents=True, exist_ok=True)
            
            # Export to CSV
            data.to_csv(output_path, index=False)
            
        except Exception as e:
            raise OSError(f"Failed to export {asset_name} data to {output_path}: {str(e)}")
    
    def export_master_data(self, master_data: Dict[str, Any], output_path: str) -> None:
        """
        Export master data to CSV file.
        
        Args:
            master_data: Dictionary containing master data
            output_path: Path where the CSV file should be saved
            
        Raises:
            OSError: If the file cannot be written
            ValueError: If master_data is empty or invalid
        """
        if not master_data:
            raise ValueError("Cannot export empty master data")
        
        try:
            # Ensure output directory exists
            output_dir = Path(output_path).parent
            output_dir.mkdir(parents=True, exist_ok=True)
            
            # Convert master data to DataFrame
            master_df = pd.DataFrame([master_data])
            
            # Export to CSV
            master_df.to_csv(output_path, index=False)
            
        except Exception as e:
            raise OSError(f"Failed to export master data to {output_path}: {str(e)}")


class ExcelExporter(ExporterInterface):
    """
    Concrete implementation for exporting data to Excel files.
    Follows Single Responsibility Principle - only responsible for Excel export.
    """
    
    def export_asset_data(self, data: pd.DataFrame, asset_name: str, output_path: str) -> None:
        """
        Export asset analysis data to Excel file.
        
        Args:
            data: DataFrame containing asset analysis results
            asset_name: Name of the asset (e.g., 'BTC', 'ETH')
            output_path: Path where the Excel file should be saved
            
        Raises:
            OSError: If the file cannot be written
            ValueError: If data is empty or invalid
        """
        if data.empty:
            raise ValueError(f"Cannot export empty data for asset {asset_name}")
        
        try:
            # Ensure output directory exists
            output_dir = Path(output_path).parent
            output_dir.mkdir(parents=True, exist_ok=True)
            
            # Export to Excel
            data.to_excel(output_path, index=False, sheet_name=asset_name)
            
        except Exception as e:
            raise OSError(f"Failed to export {asset_name} data to {output_path}: {str(e)}")
    
    def export_master_data(self, master_data: Dict[str, Any], output_path: str) -> None:
        """
        Export master data to Excel file.
        
        Args:
            master_data: Dictionary containing master data
            output_path: Path where the Excel file should be saved
            
        Raises:
            OSError: If the file cannot be written
            ValueError: If master_data is empty or invalid
        """
        if not master_data:
            raise ValueError("Cannot export empty master data")
        
        try:
            # Ensure output directory exists
            output_dir = Path(output_path).parent
            output_dir.mkdir(parents=True, exist_ok=True)
            
            # Convert master data to DataFrame
            master_df = pd.DataFrame([master_data])
            
            # Export to Excel
            master_df.to_excel(output_path, index=False, sheet_name='Master_Data')
            
        except Exception as e:
            raise OSError(f"Failed to export master data to {output_path}: {str(e)}")


class IndividualAssetExcelExporter:
    """
    Excel exporter for writing individual asset files with their own master data sheet.
    Each asset gets its own file with data sheet and master data sheet.
    """
    
    def export_single_asset_with_master(self, asset_name: str, analysis_df: pd.DataFrame, 
                                       mean_diff_oc: float, output_path: str) -> str:
        """
        Export single asset analysis and its master data to an Excel file with two sheets.
        
        Args:
            asset_name: Name of the asset (e.g., 'BTC', 'ETH')
            analysis_df: DataFrame containing asset analysis results
            mean_diff_oc: Mean_Diff_OC value for this asset
            output_path: Path where the Excel file should be saved
            
        Returns:
            Path to the exported Excel file
            
        Raises:
            ValueError: If empty data
            OSError: If file cannot be written
        """
        if analysis_df.empty:
            raise ValueError(f"Cannot export empty data for asset {asset_name}")
        
        try:
            # Ensure output directory exists
            output_dir = Path(output_path).parent
            output_dir.mkdir(parents=True, exist_ok=True)
            
            # Try to create Excel writer
            try:
                with pd.ExcelWriter(output_path, engine='openpyxl') as writer:
                    # Write asset analysis data to main sheet
                    analysis_df.to_excel(writer, sheet_name=f'{asset_name}_Analysis', index=False)
                    
                    # Write master data for this asset to a separate sheet
                    master_data = {
                        'Asset': asset_name,
                        'Mean_Diff_OC': mean_diff_oc,
                        'Total_Records': len(analysis_df),
                        'Date_Range_Start': analysis_df['Timestamp'].min() if 'Timestamp' in analysis_df.columns else 'N/A',
                        'Date_Range_End': analysis_df['Timestamp'].max() if 'Timestamp' in analysis_df.columns else 'N/A'
                    }
                    
                    master_df = pd.DataFrame([master_data])
                    master_df.to_excel(writer, sheet_name='Master_Data', index=False)
                
                return output_path
                
            except (ImportError, ModuleNotFoundError) as ie:
                # openpyxl not available, fallback to CSV files
                print(f"Warning: openpyxl not available ({str(ie)}). Creating CSV files instead.")
                
                # Create CSV for asset data
                csv_path = output_path.replace('.xlsx', '.csv')
                analysis_df.to_csv(csv_path, index=False)
                
                # Create master data CSV
                master_data = {
                    'Asset': asset_name,
                    'Mean_Diff_OC': mean_diff_oc,
                    'Total_Records': len(analysis_df),
                    'Date_Range_Start': analysis_df['Timestamp'].min() if 'Timestamp' in analysis_df.columns else 'N/A',
                    'Date_Range_End': analysis_df['Timestamp'].max() if 'Timestamp' in analysis_df.columns else 'N/A'
                }
                
                master_df = pd.DataFrame([master_data])
                master_csv_path = output_path.replace('.xlsx', '_master.csv')
                master_df.to_csv(master_csv_path, index=False)
                
                return csv_path
            
        except Exception as e:
            raise OSError(f"Failed to export to {output_path}: {str(e)}")


class MultiAssetExporter:
    """
    High-level exporter for managing multiple asset exports.
    Follows Single Responsibility Principle and Dependency Inversion Principle.
    """
    
    def __init__(self, exporter: ExporterInterface):
        """
        Initialize with an exporter implementation.
        
        Args:
            exporter: Exporter implementation following ExporterInterface
        """
        self.exporter = exporter
        self.individual_asset_exporter = IndividualAssetExcelExporter()
    
    def export_all_assets(self, asset_analyses: Dict[str, pd.DataFrame], 
                         output_dir: str, file_format: str = 'csv') -> Dict[str, str]:
        """
        Export analysis results for all assets to separate files.
        
        Args:
            asset_analyses: Dictionary mapping asset names to their analysis DataFrames
            output_dir: Directory where files should be saved
            file_format: File format ('csv' or 'excel')
            
        Returns:
            Dictionary mapping asset names to their output file paths
            
        Raises:
            ValueError: If invalid file format or empty data
            OSError: If files cannot be written
        """
        if not asset_analyses:
            raise ValueError("No asset analyses to export")
        
        # Determine file extension
        extension = 'csv' if file_format == 'csv' else 'xlsx'
        
        exported_files = {}
        
        for asset_name, analysis_df in asset_analyses.items():
            # Create output file path
            filename = f"{asset_name}_oc_analysis.{extension}"
            output_path = os.path.join(output_dir, filename)
            
            # Export asset data
            self.exporter.export_asset_data(analysis_df, asset_name, output_path)
            exported_files[asset_name] = output_path
        
        return exported_files
    
    def export_assets_with_individual_master_data(self, asset_analyses: Dict[str, pd.DataFrame], 
                                                  mean_diff_ocs: Dict[str, float], 
                                                  output_dir: str) -> Dict[str, str]:
        """
        Export each asset to its own Excel file with individual master data sheet.
        
        Args:
            asset_analyses: Dictionary mapping asset names to their analysis DataFrames
            mean_diff_ocs: Dictionary mapping asset names to their Mean_Diff_OC values
            output_dir: Directory where files should be saved
            
        Returns:
            Dictionary mapping asset names to their output file paths
            
        Raises:
            ValueError: If empty data
            OSError: If files cannot be written
        """
        if not asset_analyses:
            raise ValueError("No asset analyses to export")
        if not mean_diff_ocs:
            raise ValueError("No mean diff OC data to export")
        
        exported_files = {}
        
        for asset_name, analysis_df in asset_analyses.items():
            if asset_name not in mean_diff_ocs:
                raise ValueError(f"No Mean_Diff_OC data found for asset {asset_name}")
            
            # Create output file path
            filename = f"{asset_name}_oc_analysis.xlsx"
            output_path = os.path.join(output_dir, filename)
            
            # Export asset data with its master data
            exported_path = self.individual_asset_exporter.export_single_asset_with_master(
                asset_name=asset_name,
                analysis_df=analysis_df,
                mean_diff_oc=mean_diff_ocs[asset_name],
                output_path=output_path
            )
            
            exported_files[asset_name] = exported_path
        
        return exported_files
    
    def export_master_data(self, mean_diff_ocs: Dict[str, float], 
                          output_dir: str, file_format: str = 'csv') -> str:
        """
        Export master data containing Mean_Diff_OC values for all assets.
        
        Args:
            mean_diff_ocs: Dictionary mapping asset names to their Mean_Diff_OC values
            output_dir: Directory where file should be saved
            file_format: File format ('csv' or 'excel')
            
        Returns:
            Path to the exported master data file
            
        Raises:
            ValueError: If invalid file format or empty data
            OSError: If file cannot be written
        """
        if not mean_diff_ocs:
            raise ValueError("No mean diff OC data to export")
        
        # Determine file extension
        extension = 'csv' if file_format == 'csv' else 'xlsx'
        
        # Create master data dictionary
        master_data = {}
        for asset_name, mean_diff_oc in mean_diff_ocs.items():
            master_data[f'Mean_Diff_OC_{asset_name}'] = mean_diff_oc
        
        # Create output file path
        filename = f"oc_master_data.{extension}"
        output_path = os.path.join(output_dir, filename)
        
        # Export master data
        self.exporter.export_master_data(master_data, output_path)
        
        return output_path


class ExporterFactory:
    """
    Factory class for creating exporters.
    Follows Open/Closed Principle - open for extension, closed for modification.
    """
    
    @staticmethod
    def create_exporter(export_format: str) -> ExporterInterface:
        """
        Create an exporter based on the specified format.
        
        Args:
            export_format: Format type ('csv' or 'excel')
            
        Returns:
            ExporterInterface implementation
            
        Raises:
            ValueError: If export_format is not supported
        """
        exporters = {
            'csv': CSVExporter,
            'excel': ExcelExporter
        }
        
        if export_format not in exporters:
            raise ValueError(f"Unsupported export format: {export_format}. "
                           f"Available formats: {list(exporters.keys())}")
        
        return exporters[export_format]()