"""
Main Entry Point for Sync Data Module
====================================

This module provides the main entry point and CLI interface for the sync_data module.
It demonstrates how to use the sync_data functionality and provides a convenient
command-line interface for data synchronization operations.

Usage Examples:
    python -m crypto_trading_model.sync_data.main --asset BTC --start-date 2024-01-01 --end-date 2024-01-31
    python -m crypto_trading_model.sync_data.main --assets BTC,ETH --start-date 2024-01-01 --end-date 2024-01-31 --combined
"""

import argparse
import sys
from datetime import datetime, timedelta
from typing import List, Optional
import logging

from .synchronizer import DataSynchronizer, SynchronizationError
from .models import SyncDataConfig, SyncRequest, IntervalType, AssetType, ConfigValidationError

logger = logging.getLogger(__name__)

class SyncDataMain:
    """
    Main application class for the sync_data module.
    
    This class provides high-level operations and serves as the primary
    interface for using the sync_data functionality.
    """
    
    def __init__(self, config: Optional[SyncDataConfig] = None):
        """
        Initialize the main application.
        
        Args:
            config: Optional custom configuration (uses defaults if not provided)
        """
        self.config = config or SyncDataConfig()
        self.synchronizer = DataSynchronizer(self.config)
    
    def sync_single_asset(self, 
                         asset_code: str,
                         start_date: datetime,
                         end_date: datetime,
                         interval: IntervalType = IntervalType.DAILY,
                         export_csv: bool = True) -> bool:
        """
        Synchronize data for a single asset.
        
        Args:
            asset_code: Asset code (e.g., 'BTC', 'ETH')
            start_date: Start date for data retrieval
            end_date: End date for data retrieval
            interval: Time interval for data
            export_csv: Whether to export CSV file
            
        Returns:
            True if successful, False otherwise
        """
        try:
            request = SyncRequest(
                asset_code=asset_code,
                start_date=start_date,
                end_date=end_date,
                interval=interval,
                export_csv=export_csv
            )
            
            asset_data, csv_path = self.synchronizer.sync_asset_data(request)
            
            print(f"✓ Successfully synchronized {len(asset_data)} records for {asset_code}")
            if csv_path:
                print(f"✓ Data exported to: {csv_path}")
            
            return True
            
        except (SynchronizationError, ConfigValidationError) as e:
            print(f"✗ Failed to sync {asset_code}: {str(e)}")
            return False
        except Exception as e:
            print(f"✗ Unexpected error syncing {asset_code}: {str(e)}")
            logger.exception(f"Unexpected error syncing {asset_code}")
            return False
    
    def sync_multiple_assets(self,
                           asset_codes: List[str],
                           start_date: datetime,
                           end_date: datetime,
                           interval: IntervalType = IntervalType.DAILY,
                           export_csv: bool = True,
                           create_combined_csv: bool = False) -> bool:
        """
        Synchronize data for multiple assets.
        
        Args:
            asset_codes: List of asset codes
            start_date: Start date for data retrieval
            end_date: End date for data retrieval
            interval: Time interval for data
            export_csv: Whether to export individual CSV files
            create_combined_csv: Whether to create combined CSV
            
        Returns:
            True if at least one asset was successful, False otherwise
        """
        try:
            print(f"Synchronizing data for {len(asset_codes)} assets: {', '.join(asset_codes)}")
            print(f"Date range: {start_date.strftime('%Y-%m-%d')} to {end_date.strftime('%Y-%m-%d')}")
            
            results = self.synchronizer.sync_multiple_assets(
                asset_codes=asset_codes,
                start_date=start_date,
                end_date=end_date,
                interval=interval,
                export_csv=export_csv,
                create_combined_csv=create_combined_csv
            )
            
            # Display results
            summary = self.synchronizer.get_sync_summary(results)
            
            print(f"\n📊 Synchronization Summary:")
            print(f"  • Total assets: {summary['total_assets']}")
            print(f"  • Successful: {summary['assets_with_data']}")
            print(f"  • Success rate: {summary['success_rate']:.1f}%")
            print(f"  • Total records: {summary['total_records']:,}")
            print(f"  • CSV files created: {summary['assets_with_csv']}")
            
            if summary['earliest_date'] and summary['latest_date']:
                print(f"  • Date range: {summary['earliest_date'].strftime('%Y-%m-%d')} to {summary['latest_date'].strftime('%Y-%m-%d')}")
            
            # List successful assets
            successful_assets = [asset for asset, (data, _) in results.items() if data]
            if successful_assets:
                print(f"\n✓ Successfully synchronized: {', '.join(successful_assets)}")
            
            # List failed assets
            failed_assets = [asset for asset in asset_codes if asset not in results or not results[asset][0]]
            if failed_assets:
                print(f"\n✗ Failed to synchronize: {', '.join(failed_assets)}")
            
            return len(successful_assets) > 0
            
        except (SynchronizationError, ConfigValidationError) as e:
            print(f"✗ Failed to sync multiple assets: {str(e)}")
            return False
        except Exception as e:
            print(f"✗ Unexpected error: {str(e)}")
            logger.exception("Unexpected error in sync_multiple_assets")
            return False
    
    def test_api_connection(self) -> bool:
        """
        Test the API connection.
        
        Returns:
            True if connection is successful, False otherwise
        """
        try:
            print("Testing API connection...")
            
            if self.synchronizer.test_connection():
                print("✓ API connection successful")
                return True
            else:
                print("✗ API connection failed")
                return False
                
        except Exception as e:
            print(f"✗ API connection test error: {str(e)}")
            return False
    
    def list_available_symbols(self, category: AssetType = AssetType.LINEAR, limit: int = 50) -> bool:
        """
        List available trading symbols.
        
        Args:
            category: Asset category to query
            limit: Maximum number of symbols to display
            
        Returns:
            True if successful, False otherwise
        """
        try:
            print(f"Fetching available symbols for {category.value} category...")
            
            symbols = self.synchronizer.get_available_symbols(category)
            
            print(f"\n📋 Available symbols ({len(symbols)} total, showing first {limit}):")
            
            for i, symbol in enumerate(symbols[:limit]):
                print(f"  {i+1:3d}. {symbol}")
            
            if len(symbols) > limit:
                print(f"  ... and {len(symbols) - limit} more")
            
            return True
            
        except SynchronizationError as e:
            print(f"✗ Failed to fetch symbols: {str(e)}")
            return False
        except Exception as e:
            print(f"✗ Unexpected error: {str(e)}")
            return False
    
    def cleanup(self):
        """Clean up resources."""
        self.synchronizer.cleanup_resources()

def parse_date(date_string: str) -> datetime:
    """
    Parse date string in YYYY-MM-DD format.
    
    Args:
        date_string: Date string to parse
        
    Returns:
        Parsed datetime object
        
    Raises:
        ValueError: If date format is invalid
    """
    try:
        return datetime.strptime(date_string, '%Y-%m-%d')
    except ValueError:
        raise ValueError(f"Invalid date format: {date_string}. Expected YYYY-MM-DD")

def parse_asset_list(asset_string: str) -> List[str]:
    """
    Parse comma-separated asset list.
    
    Args:
        asset_string: Comma-separated asset codes
        
    Returns:
        List of asset codes
    """
    return [asset.strip().upper() for asset in asset_string.split(',') if asset.strip()]

def create_argument_parser() -> argparse.ArgumentParser:
    """Create command-line argument parser."""
    parser = argparse.ArgumentParser(
        description='Cryptocurrency Data Synchronization Tool',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Sync single asset
  python -m crypto_trading_model.sync_data.main --asset BTC --start-date 2024-01-01 --end-date 2024-01-31
  
  # Sync multiple assets
  python -m crypto_trading_model.sync_data.main --assets BTC,ETH,ADA --start-date 2024-01-01 --end-date 2024-01-31
  
  # Sync with combined CSV
  python -m crypto_trading_model.sync_data.main --assets BTC,ETH --start-date 2024-01-01 --end-date 2024-01-31 --combined
  
  # Test API connection
  python -m crypto_trading_model.sync_data.main --test-connection
  
  # List available symbols
  python -m crypto_trading_model.sync_data.main --list-symbols
        """
    )
    
    # Asset selection (mutually exclusive)
    asset_group = parser.add_mutually_exclusive_group()
    asset_group.add_argument('--asset', type=str, help='Single asset code to sync (e.g., BTC)')
    asset_group.add_argument('--assets', type=str, help='Comma-separated list of asset codes (e.g., BTC,ETH,ADA)')
    
    # Date range
    parser.add_argument('--start-date', type=str, help='Start date in YYYY-MM-DD format')
    parser.add_argument('--end-date', type=str, help='End date in YYYY-MM-DD format')
    
    # Options
    parser.add_argument('--interval', type=str, default='D', 
                       choices=['1', '3', '5', '15', '30', '60', '120', '240', '360', '720', 'D', 'W', 'M'],
                       help='Time interval (default: D for daily)')
    parser.add_argument('--no-csv', action='store_true', help='Skip CSV export')
    parser.add_argument('--combined', action='store_true', help='Create combined CSV for multiple assets')
    
    # Utility commands
    parser.add_argument('--test-connection', action='store_true', help='Test API connection')
    parser.add_argument('--list-symbols', action='store_true', help='List available trading symbols')
    parser.add_argument('--symbols-limit', type=int, default=50, help='Limit for symbols display (default: 50)')
    
    # Logging
    parser.add_argument('--verbose', '-v', action='store_true', help='Enable verbose logging')
    parser.add_argument('--quiet', '-q', action='store_true', help='Suppress output except errors')
    
    return parser

def main():
    """Main entry point for the sync_data module."""
    parser = create_argument_parser()
    args = parser.parse_args()
    
    # Setup logging
    if args.verbose:
        logging.basicConfig(level=logging.DEBUG)
    elif args.quiet:
        logging.basicConfig(level=logging.ERROR)
    else:
        logging.basicConfig(level=logging.INFO)
    
    # Create configuration
    config = SyncDataConfig()
    if args.verbose:
        config.log_level = "DEBUG"
    elif args.quiet:
        config.log_level = "ERROR"
    
    # Initialize main application
    app = SyncDataMain(config)
    
    try:
        success = True
        
        # Handle utility commands
        if args.test_connection:
            success = app.test_api_connection()
        elif args.list_symbols:
            success = app.list_available_symbols(limit=args.symbols_limit)
        else:
            # Handle data sync commands
            if not args.asset and not args.assets:
                print("Error: Must specify --asset or --assets for data synchronization")
                print("Use --help for usage information")
                sys.exit(1)
            
            if not args.start_date or not args.end_date:
                print("Error: Must specify --start-date and --end-date for data synchronization")
                print("Use --help for usage information")
                sys.exit(1)
            
            # Parse dates
            try:
                start_date = parse_date(args.start_date)
                end_date = parse_date(args.end_date)
            except ValueError as e:
                print(f"Error: {str(e)}")
                sys.exit(1)
            
            # Parse interval
            try:
                interval = IntervalType(args.interval)
            except ValueError:
                print(f"Error: Invalid interval '{args.interval}'")
                sys.exit(1)
            
            export_csv = not args.no_csv
            
            # Perform synchronization
            if args.asset:
                # Single asset
                success = app.sync_single_asset(
                    asset_code=args.asset,
                    start_date=start_date,
                    end_date=end_date,
                    interval=interval,
                    export_csv=export_csv
                )
            else:
                # Multiple assets
                asset_codes = parse_asset_list(args.assets)
                if not asset_codes:
                    print("Error: No valid asset codes found in --assets")
                    sys.exit(1)
                
                success = app.sync_multiple_assets(
                    asset_codes=asset_codes,
                    start_date=start_date,
                    end_date=end_date,
                    interval=interval,
                    export_csv=export_csv,
                    create_combined_csv=args.combined
                )
        
        # Exit with appropriate code
        sys.exit(0 if success else 1)
        
    except KeyboardInterrupt:
        print("\nOperation cancelled by user")
        sys.exit(1)
    except Exception as e:
        print(f"✗ Unexpected error: {str(e)}")
        if args.verbose:
            logger.exception("Unexpected error in main")
        sys.exit(1)
    finally:
        app.cleanup()

if __name__ == '__main__':
    main()