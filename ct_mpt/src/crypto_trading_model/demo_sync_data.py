"""
Demonstration Script for Sync Data Module
========================================

This script demonstrates how to use the new sync_data module to synchronize
cryptocurrency data from Bybit API following the requirements.

Usage:
    python demo_sync_data.py
"""

from datetime import datetime, timedelta
from sync_data import SyncDataMain, SyncDataConfig
import sys

def demo_sync_data():
    """Demonstrate the sync_data module functionality."""
    
    print("🚀 Crypto Trading Model - Sync Data Module Demo")
    print("=" * 50)
    
    # Create configuration
    config = SyncDataConfig()
    config.default_symbols = ["BTCUSDT", "ETHUSDT"]  # Default symbols to sync
    config.enable_logging = True
    config.log_level = "INFO"
    
    # Initialize the main application
    app = SyncDataMain(config)
    
    try:
        # Test API connection
        print("\n1. Testing API Connection...")
        if not app.test_api_connection():
            print("❌ API connection failed. Please check your internet connection.")
            return False
        
        # Set date range (last 7 days)
        end_date = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
        start_date = end_date - timedelta(days=7)
        
        print(f"\n2. Synchronizing data from {start_date.strftime('%Y-%m-%d')} to {end_date.strftime('%Y-%m-%d')}")
        
        # Example 1: Sync single asset (BTC)
        print("\n📈 Example 1: Sync single asset (BTC)")
        success_btc = app.sync_single_asset(
            asset_code="BTC",
            start_date=start_date,
            end_date=end_date,
            export_csv=True
        )
        
        if success_btc:
            print("✅ BTC data synchronized successfully!")
        else:
            print("❌ Failed to sync BTC data")
        
        # Example 2: Sync multiple assets
        print("\n📈 Example 2: Sync multiple assets (BTC, ETH)")
        success_multi = app.sync_multiple_assets(
            asset_codes=["BTC", "ETH"],
            start_date=start_date,
            end_date=end_date,
            export_csv=True,
            create_combined_csv=True
        )
        
        if success_multi:
            print("✅ Multiple assets synchronized successfully!")
        else:
            print("❌ Failed to sync multiple assets")
        
        # Example 3: List available symbols (first 10)
        print("\n📋 Example 3: List available symbols")
        app.list_available_symbols(limit=10)
        
        print("\n🎉 Demo completed successfully!")
        print("\nGenerated files should be in: ./sync_data_output/")
        print("Files follow the naming pattern: {ASSET_CODE}_{START_DATE}_{END_DATE}.csv")
        print("Example: BTC_2024-01-01_2024-01-07.csv")
        
        return True
        
    except Exception as e:
        print(f"❌ Demo failed with error: {str(e)}")
        return False
        
    finally:
        # Clean up resources
        app.cleanup()

def demo_sync_requirements():
    """Demonstrate sync according to the exact requirements."""
    
    print("\n" + "=" * 60)
    print("📋 REQUIREMENTS DEMONSTRATION")
    print("=" * 60)
    print("Requirement: Get new asset data for BTC, ETH")
    print("Input: Asset code, from date, to date")
    print("Output: CSV file with columns:")
    print("  - TIMESTAMP")
    print("  - OPEN_PRICE_{ASSET}")
    print("  - CLOSE_PRICE_{ASSET}")
    print("  - HIGH_{ASSET}")
    print("  - LOW_{ASSET}")
    print("  - VOLUME_{ASSET}")
    print("  - TURNOVER_{ASSET}")
    print("API: https://api.bybit.com/v5/market/kline")
    print()
    
    # Initialize app
    app = SyncDataMain()
    
    try:
        # Demonstrate exact requirement usage
        print("🔄 Executing requirement: Sync BTC data for January 2024")
        
        success = app.sync_single_asset(
            asset_code="BTC",
            start_date=datetime(2024, 1, 1),
            end_date=datetime(2024, 1, 31),
            export_csv=True
        )
        
        if success:
            print("✅ Requirement fulfilled successfully!")
            print("📁 Output: CSV file generated with proper column naming")
            print("📊 Columns: TIMESTAMP, OPEN_PRICE_BTC, CLOSE_PRICE_BTC, HIGH_BTC, LOW_BTC, VOLUME_BTC, TURNOVER_BTC")
        else:
            print("❌ Requirement execution failed")
            
        return success
        
    except Exception as e:
        print(f"❌ Requirement demo failed: {str(e)}")
        return False
        
    finally:
        app.cleanup()

if __name__ == "__main__":
    print("Starting Sync Data Module Demonstration...")
    
    # Run basic demo
    demo_success = demo_sync_data()
    
    # Run requirements demo
    req_success = demo_sync_requirements()
    
    if demo_success and req_success:
        print("\n🎉 All demonstrations completed successfully!")
        print("\n💡 To use the module in your code:")
        print("```python")
        print("from crypto_trading_model.sync_data import SyncDataMain")
        print("from datetime import datetime")
        print("")
        print("app = SyncDataMain()")
        print("app.sync_single_asset('BTC', datetime(2024,1,1), datetime(2024,1,31))")
        print("```")
        print("\n💡 To use from command line:")
        print("python -m crypto_trading_model.sync_data.main --asset BTC --start-date 2024-01-01 --end-date 2024-01-31")
        
        sys.exit(0)
    else:
        print("\n❌ Some demonstrations failed. Check the output above for details.")
        sys.exit(1)