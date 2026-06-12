"""
Example usage script for the OC (Open-Close) Analysis module.
Demonstrates how to use the module following the business requirements.
"""

import os
import sys
from pathlib import Path

# Add the parent directory to the Python path to import the module
current_dir = Path(__file__).parent
project_root = current_dir.parent.parent.parent
sys.path.insert(0, str(project_root))

from crypto_trading_model.oc_analysis.oc_analyzer import OCAnalysisOrchestrator


def main():
    """
    Main example function demonstrating OC analysis usage.
    """
    print("=== OC (Open-Close) Analysis Example ===")
    print()
    
    # Define paths
    input_file = "src/crypto_trading_model/data/test_data.csv"
    output_dir = "oc_analysis_results"
    
    # Create output directory
    os.makedirs(output_dir, exist_ok=True)
    
    try:
        # Example 1: Full analysis with CSV export
        print("1. Running full OC analysis with CSV export...")
        orchestrator_csv = OCAnalysisOrchestrator(
            reader_type='crypto',
            export_format='csv'
        )
        
        results_csv = orchestrator_csv.run_full_analysis(
            input_file_path=input_file,
            output_dir=output_dir,
            asset_names=['BTC', 'ETH']
        )
        
        print(f"✓ CSV analysis completed. Results in: {results_csv['output_directory']}")
        print()
        
        # Example 2: Full analysis with Excel export
        print("2. Running full OC analysis with Excel export...")
        orchestrator_excel = OCAnalysisOrchestrator(
            reader_type='crypto',
            export_format='excel'
        )
        
        excel_output_dir = os.path.join(output_dir, "excel_format")
        results_excel = orchestrator_excel.run_full_analysis(
            input_file_path=input_file,
            output_dir=excel_output_dir,
            asset_names=['BTC', 'ETH']
        )
        
        print(f"✓ Excel analysis completed. Results in: {results_excel['output_directory']}")
        print()
        
        # Example 3: Single asset analysis
        print("3. Analyzing single asset (BTC only)...")
        single_output_dir = os.path.join(output_dir, "btc_only")
        
        analysis_data, mean_diff_oc = orchestrator_csv.analyze_single_asset(
            input_file_path=input_file,
            asset_name='BTC',
            output_dir=single_output_dir
        )
        
        print(f"✓ BTC analysis completed. Mean_Diff_OC: {mean_diff_oc:.6f}")
        print(f"   First few records:")
        for i, record in enumerate(analysis_data[:3]):
            print(f"   Record {i+1}: OC={record['OC']:.2f}, Diff_OC={record.get('Diff_OC', 'NaN')}")
        print()
        
        # Example 4: Summary statistics only (no file export)
        print("4. Getting summary statistics (no file export)...")
        summary_stats = orchestrator_csv.get_summary_statistics(
            input_file_path=input_file,
            asset_names=['BTC', 'ETH']
        )
        
        print("✓ Summary statistics:")
        for asset_name, stats in summary_stats.items():
            print(f"   {asset_name}:")
            print(f"     Mean OC: {stats['mean_oc']:.2f}")
            print(f"     Std OC: {stats['std_oc']:.2f}")
            print(f"     Mean Diff_OC: {stats['mean_diff_oc']:.6f}")
            print(f"     Std Diff_OC: {stats['std_diff_oc']:.6f}")
            print(f"     Valid Diff_OC count: {stats['count_valid_diff_oc']}")
        print()
        
        # Example 5: Custom asset list
        print("5. Analyzing custom asset list (ETH only)...")
        custom_results = orchestrator_csv.run_full_analysis(
            input_file_path=input_file,
            output_dir=os.path.join(output_dir, "custom_assets"),
            asset_names=['ETH']  # Only ETH
        )
        
        print(f"✓ Custom asset analysis completed for: {list(custom_results['mean_diff_ocs'].keys())}")
        print()
        
        print("=== All Examples Completed Successfully! ===")
        print()
        print("Generated directories and files:")
        print(f"- {output_dir}/")
        print("  - BTC_oc_analysis.csv")
        print("  - ETH_oc_analysis.csv") 
        print("  - oc_master_data.csv")
        print(f"  - excel_format/")
        print("    - BTC_oc_analysis.xlsx")
        print("    - ETH_oc_analysis.xlsx")
        print("    - oc_master_data.xlsx")
        print(f"  - btc_only/")
        print("    - BTC_oc_analysis.csv")
        print(f"  - custom_assets/")
        print("    - ETH_oc_analysis.csv")
        print("    - oc_master_data.csv")
        
    except Exception as e:
        print(f"❌ Error during analysis: {str(e)}")
        raise


def demonstrate_modular_usage():
    """
    Demonstrate how to use individual components of the module.
    """
    print("=== Modular Component Usage Example ===")
    print()
    
    try:
        # Import individual components
        from crypto_trading_model.oc_analysis.data_reader import DataReaderFactory
        from crypto_trading_model.oc_analysis.calculator import OCCalculator, AssetOCAnalyzer
        from crypto_trading_model.oc_analysis.exporter import ExporterFactory
        
        # Step 1: Read data using data reader
        print("1. Reading data using DataReader...")
        reader = DataReaderFactory.create_reader('crypto')
        data = reader.read_data("src/crypto_trading_model/data/test_data.csv")
        print(f"✓ Loaded {len(data)} rows")
        
        # Step 2: Perform calculations using calculator
        print("2. Performing calculations using Calculator...")
        calculator = OCCalculator()
        analyzer = AssetOCAnalyzer(calculator)
        
        btc_analysis, btc_mean = analyzer.analyze_asset(data, 'BTC')
        print(f"✓ BTC analysis completed. Mean_Diff_OC: {btc_mean:.6f}")
        
        # Step 3: Export using exporter
        print("3. Exporting results using Exporter...")
        exporter = ExporterFactory.create_exporter('csv')
        
        output_path = "oc_analysis_results/modular_btc_analysis.csv"
        exporter.export_asset_data(btc_analysis, 'BTC', output_path)
        print(f"✓ Exported to: {output_path}")
        
        print("✓ Modular usage demonstration completed!")
        
    except Exception as e:
        print(f"❌ Error in modular demonstration: {str(e)}")
        raise


if __name__ == "__main__":
    # Change to project root directory for relative paths to work
    project_root = Path(__file__).parent.parent.parent.parent
    os.chdir(project_root)
    
    # Run main examples
    main()
    
    print()
    
    # Run modular demonstration
    demonstrate_modular_usage()