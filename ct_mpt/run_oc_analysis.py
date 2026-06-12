"""
Simple runner script for OC (Open-Close) Analysis.
Place this in the project root to run the analysis easily.
"""

import os
import sys
import yaml
from pathlib import Path

# Add src directory to Python path
src_path = Path(__file__).parent / "src"
sys.path.insert(0, str(src_path))

from crypto_trading_model.oc_analysis.oc_analyzer import OCAnalysisOrchestrator
from crypto_trading_model.config.config_manager import ConfigManager
from crypto_trading_model.custom_logging.logger import CustomLogger


def main():
    """Run OC analysis on the test data."""
    print("=== Running OC (Open-Close) Analysis ===")
    print()
    
    # Define paths relative to project root
    sync_data_folder = "sync_data_output"
    output_dir = "oc_analysis_results"
    
    # Check if sync data folder exists
    if not os.path.exists(sync_data_folder):
        print(f"ERROR: Sync data folder not found: {sync_data_folder}")
        print("Please ensure the sync_data_output folder exists with BTC.csv and ETH.csv files.")
        return
    
    # Check if required files exist
    btc_file = os.path.join(sync_data_folder, "BTC.csv")
    eth_file = os.path.join(sync_data_folder, "ETH.csv")
    
    if not os.path.exists(btc_file):
        print(f"ERROR: BTC data file not found: {btc_file}")
        return
    
    if not os.path.exists(eth_file):
        print(f"ERROR: ETH data file not found: {eth_file}")
        return
    
    # Create output directory
    os.makedirs(output_dir, exist_ok=True)
    
    try:
        # Initialize configuration and logging
        print("Loading configuration...")
        logger = CustomLogger('OCAnalysis')
        config_manager = ConfigManager(logger)
        config = config_manager.get_config()
        
        # Read OC analysis configuration directly from YAML
        # (since it's not included in the main config models yet)
        print("Reading OC analysis configuration from config.yaml...")
        config_path = config_manager.config_path
        with open(config_path, 'r') as file:
            yaml_data = yaml.safe_load(file)
        
        oc_config = yaml_data.get('oc_analysis', None)
        if oc_config is None:
            print("WARNING: No OC analysis configuration found in config.yaml, using defaults")
            mean_diff_ocs = {'BTC': 0.0, 'ETH': 0.0}
            export_format = 'csv'
        else:
            print("Found OC analysis configuration in config.yaml")
            # Extract mean_diff_oc values from config
            mean_diff_ocs = {}
            configured_means = oc_config.get('mean_diff_oc', {})
            default_mean = oc_config.get('default_mean_diff_oc', 0.0)
            
            print(f"Raw configured means: {configured_means}")
            print(f"Default mean: {default_mean}")
            
            # Set mean values for each asset
            for asset in ['BTC', 'ETH']:
                if asset in configured_means:
                    mean_diff_ocs[asset] = configured_means[asset]
                    print(f"  Using configured value for {asset}: {configured_means[asset]}")
                else:
                    mean_diff_ocs[asset] = default_mean
                    print(f"  Using default value for {asset}: {default_mean}")
                    
            # Get export format from config
            options = oc_config.get('options', {})
            export_format = options.get('export_format', 'csv')
        
        print(f"Using mean_diff_oc values: {mean_diff_ocs}")
        print(f"Export format: {export_format}")
        
        # Initialize the orchestrator with configuration
        print("Initializing OC Analysis orchestrator...")
        orchestrator = OCAnalysisOrchestrator(
            reader_type='crypto',
            export_format=export_format
        )
        
        # Run the full analysis with configured mean values
        print("Running full OC analysis with configured parameters...")
        results = orchestrator.run_full_analysis(
            sync_data_folder=sync_data_folder,
            output_dir=output_dir,
            asset_names=['BTC', 'ETH'],
            configured_mean_diff_ocs=mean_diff_ocs
        )
        
        print()
        print("=== Analysis Results ===")
        print("Analysis completed successfully!")
        print(f"Results saved to: {results['output_directory']}")
        print()
        print("Mean_Diff_OC Values:")
        for asset, mean_value in results['mean_diff_ocs'].items():
            print(f"   {asset}: {mean_value:.6f}")
        
        print()
        print("Generated Files:")
        for asset, file_path in results['asset_files'].items():
            print(f"   {asset} analysis: {file_path} (includes master data sheet)")
        
        print()
        print("OC Analysis completed successfully!")
        
    except Exception as e:
        print(f"Error during analysis: {str(e)}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    main()