"""
CSV Export Functionality for Cryptocurrency Data
===============================================

This module provides CSV export capabilities for cryptocurrency market data.
It handles proper column naming, formatting, and file organization following
the requirements specifications.
"""

import csv
import os
from typing import List, Dict, Any, Optional
from datetime import datetime
import logging
from pathlib import Path

from .models import AssetData, ExportConfig, ConfigValidationError

logger = logging.getLogger(__name__)

class ExportError(Exception):
    """Custom exception for export-related errors."""
    pass

class CsvExporter:
    """
    CSV exporter for cryptocurrency market data.
    
    This class handles the export of AssetData objects to CSV files with
    proper formatting and column naming as specified in requirements.
    """
    
    def __init__(self, config: ExportConfig):
        """
        Initialize the CSV exporter.
        
        Args:
            config: Export configuration settings
        """
        self.config = config
        self._ensure_output_directory()
    
    def _ensure_output_directory(self) -> None:
        """Create output directory if it doesn't exist."""
        try:
            output_path = Path(self.config.output_dir)
            output_path.mkdir(parents=True, exist_ok=True)
            logger.info(f"Output directory ensured: {output_path.absolute()}")
        except Exception as e:
            raise ExportError(f"Failed to create output directory {self.config.output_dir}: {str(e)}")
    
    def export_asset_data(self, 
                         asset_data: List[AssetData], 
                         asset_code: str,
                         start_date: datetime,
                         end_date: datetime,
                         filename: Optional[str] = None) -> str:
        """
        Export asset data to CSV file.
        
        Args:
            asset_data: List of AssetData objects to export
            asset_code: Asset code for column naming
            start_date: Start date for filename generation
            end_date: End date for filename generation
            filename: Optional custom filename (overrides template)
            
        Returns:
            Full path to the created CSV file
            
        Raises:
            ExportError: If export fails
        """
        if not asset_data:
            logger.warning(f"No data to export for asset {asset_code}")
            return ""
        
        # Generate filename
        if filename is None:
            filename = self.config.generate_filename(asset_code, start_date, end_date)
        
        # Ensure filename has .csv extension
        if not filename.lower().endswith('.csv'):
            filename += '.csv'
        
        output_path = Path(self.config.output_dir) / filename
        
        try:
            # Get column headers
            headers = AssetData.get_column_headers(asset_code)
            
            # Sort data by timestamp
            sorted_data = sorted(asset_data, key=lambda x: x.timestamp)
            
            logger.info(f"Exporting {len(sorted_data)} records to {output_path}")
            
            with open(output_path, 'w', newline='', encoding='utf-8') as csvfile:
                writer = csv.writer(csvfile)
                
                # Write headers if configured
                if self.config.include_headers:
                    writer.writerow(headers)
                
                # Write data rows
                for data_point in sorted_data:
                    row = self._convert_asset_data_to_row(data_point)
                    writer.writerow(row)
            
            logger.info(f"Successfully exported data to {output_path}")
            return str(output_path.absolute())
            
        except Exception as e:
            raise ExportError(f"Failed to export data to {output_path}: {str(e)}")
    
    def _convert_asset_data_to_row(self, asset_data: AssetData) -> List[Any]:
        """
        Convert AssetData object to CSV row.
        
        Args:
            asset_data: AssetData object to convert
            
        Returns:
            List of values for CSV row
        """
        # Format timestamp according to configuration
        formatted_timestamp = asset_data.timestamp.strftime(self.config.timestamp_format)
        
        return [
            formatted_timestamp,
            asset_data.open_price,
            asset_data.close_price,
            asset_data.high_price,
            asset_data.low_price,
            asset_data.volume,
            asset_data.turnover
        ]
    
    def export_multiple_assets(self, 
                              assets_data: Dict[str, List[AssetData]],
                              start_date: datetime,
                              end_date: datetime) -> Dict[str, str]:
        """
        Export data for multiple assets to separate CSV files.
        
        Args:
            assets_data: Dictionary mapping asset codes to their data
            start_date: Start date for filename generation
            end_date: End date for filename generation
            
        Returns:
            Dictionary mapping asset codes to their output file paths
            
        Raises:
            ExportError: If any export fails
        """
        export_results = {}
        failed_exports = []
        
        for asset_code, asset_data in assets_data.items():
            try:
                output_path = self.export_asset_data(
                    asset_data=asset_data,
                    asset_code=asset_code,
                    start_date=start_date,
                    end_date=end_date
                )
                if output_path:
                    export_results[asset_code] = output_path
                    logger.info(f"Successfully exported {asset_code} data to {output_path}")
                else:
                    logger.warning(f"No data exported for {asset_code}")
                    
            except Exception as e:
                failed_exports.append((asset_code, str(e)))
                logger.error(f"Failed to export {asset_code}: {str(e)}")
        
        # Report any failures
        if failed_exports:
            failed_assets = [asset for asset, _ in failed_exports]
            error_msg = f"Failed to export data for assets: {', '.join(failed_assets)}"
            logger.error(error_msg)
            
            # If all exports failed, raise an exception
            if len(failed_exports) == len(assets_data):
                raise ExportError(f"All exports failed. First error: {failed_exports[0][1]}")
        
        logger.info(f"Export completed. Successfully exported {len(export_results)} out of {len(assets_data)} assets")
        return export_results
    
    def create_combined_csv(self, 
                           assets_data: Dict[str, List[AssetData]],
                           start_date: datetime,
                           end_date: datetime,
                           filename: Optional[str] = None) -> str:
        """
        Create a combined CSV file with data from multiple assets.
        
        This method creates a single CSV with columns for all assets,
        aligned by timestamp.
        
        Args:
            assets_data: Dictionary mapping asset codes to their data
            start_date: Start date for filename generation
            end_date: End date for filename generation
            filename: Optional custom filename
            
        Returns:
            Full path to the created CSV file
            
        Raises:
            ExportError: If export fails
        """
        if not assets_data:
            raise ExportError("No asset data provided for combined export")
        
        # Generate filename
        if filename is None:
            asset_codes = "_".join(sorted(assets_data.keys()))
            filename = f"combined_{asset_codes}_{start_date.strftime(self.config.date_format)}_{end_date.strftime(self.config.date_format)}.csv"
        
        # Ensure filename has .csv extension
        if not filename.lower().endswith('.csv'):
            filename += '.csv'
        
        output_path = Path(self.config.output_dir) / filename
        
        try:
            # Collect all unique timestamps
            all_timestamps = set()
            for asset_data in assets_data.values():
                for data_point in asset_data:
                    all_timestamps.add(data_point.timestamp)
            
            sorted_timestamps = sorted(all_timestamps)
            
            # Create data index for quick lookup
            data_index = {}
            for asset_code, asset_data in assets_data.items():
                data_index[asset_code] = {
                    data_point.timestamp: data_point for data_point in asset_data
                }
            
            # Generate headers
            headers = ["TIMESTAMP"]
            for asset_code in sorted(assets_data.keys()):
                asset_headers = AssetData.get_column_headers(asset_code)[1:]  # Skip TIMESTAMP
                headers.extend(asset_headers)
            
            logger.info(f"Creating combined CSV with {len(sorted_timestamps)} timestamps and {len(assets_data)} assets")
            
            with open(output_path, 'w', newline='', encoding='utf-8') as csvfile:
                writer = csv.writer(csvfile)
                
                # Write headers
                if self.config.include_headers:
                    writer.writerow(headers)
                
                # Write data rows
                for timestamp in sorted_timestamps:
                    row = [timestamp.strftime(self.config.timestamp_format)]
                    
                    for asset_code in sorted(assets_data.keys()):
                        if timestamp in data_index[asset_code]:
                            data_point = data_index[asset_code][timestamp]
                            row.extend([
                                data_point.open_price,
                                data_point.close_price,
                                data_point.high_price,
                                data_point.low_price,
                                data_point.volume,
                                data_point.turnover
                            ])
                        else:
                            # Fill with empty values if data not available
                            row.extend(['', '', '', '', '', ''])
                    
                    writer.writerow(row)
            
            logger.info(f"Successfully created combined CSV: {output_path}")
            return str(output_path.absolute())
            
        except Exception as e:
            raise ExportError(f"Failed to create combined CSV {output_path}: {str(e)}")
    
    def get_export_summary(self, file_path: str) -> Dict[str, Any]:
        """
        Get summary information about an exported file.
        
        Args:
            file_path: Path to the exported CSV file
            
        Returns:
            Dictionary with file summary information
            
        Raises:
            ExportError: If file cannot be read
        """
        try:
            file_path_obj = Path(file_path)
            
            if not file_path_obj.exists():
                raise ExportError(f"File does not exist: {file_path}")
            
            # Get file stats
            file_stats = file_path_obj.stat()
            
            # Count rows
            row_count = 0
            with open(file_path, 'r', encoding='utf-8') as csvfile:
                reader = csv.reader(csvfile)
                row_count = sum(1 for _ in reader)
            
            # Adjust for header row
            data_rows = row_count - 1 if self.config.include_headers and row_count > 0 else row_count
            
            return {
                "file_path": str(file_path_obj.absolute()),
                "file_size_bytes": file_stats.st_size,
                "file_size_mb": round(file_stats.st_size / (1024 * 1024), 2),
                "created_time": datetime.fromtimestamp(file_stats.st_ctime),
                "modified_time": datetime.fromtimestamp(file_stats.st_mtime),
                "total_rows": row_count,
                "data_rows": data_rows,
                "has_headers": self.config.include_headers
            }
            
        except Exception as e:
            raise ExportError(f"Failed to get file summary for {file_path}: {str(e)}")
    
    def cleanup_old_files(self, days_to_keep: int = 30) -> int:
        """
        Clean up old export files.
        
        Args:
            days_to_keep: Number of days to keep files (older files will be deleted)
            
        Returns:
            Number of files deleted
            
        Raises:
            ExportError: If cleanup fails
        """
        try:
            output_path = Path(self.config.output_dir)
            if not output_path.exists():
                return 0
            
            cutoff_time = datetime.now().timestamp() - (days_to_keep * 24 * 60 * 60)
            deleted_count = 0
            
            for file_path in output_path.glob("*.csv"):
                if file_path.stat().st_mtime < cutoff_time:
                    try:
                        file_path.unlink()
                        deleted_count += 1
                        logger.info(f"Deleted old file: {file_path}")
                    except Exception as e:
                        logger.warning(f"Failed to delete file {file_path}: {str(e)}")
            
            logger.info(f"Cleanup completed. Deleted {deleted_count} files older than {days_to_keep} days")
            return deleted_count
            
        except Exception as e:
            raise ExportError(f"Failed to cleanup old files: {str(e)}")