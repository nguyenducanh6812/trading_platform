#!/usr/bin/env python3
"""
OC ARIMA Analysis Runner
========================

Command-line interface for running OC ARIMA analysis.
Provides both CLI and GUI options.
"""

import sys
import os
import argparse
from pathlib import Path

# Add src directory to Python path
src_path = Path(__file__).parent / "src"
sys.path.insert(0, str(src_path))

def run_cli_analysis(args):
    """Run ARIMA analysis via command line."""
    from crypto_trading_model.arima.oc_arima_main import OCArimaMain, ArimaJobConfig
    
    try:
        print("=== OC ARIMA Analysis (CLI Mode) ===")
        print()
        
        # Create job configuration
        job_config = ArimaJobConfig(
            asset_code=args.asset,
            p=args.p,
            d=args.d,
            q=args.q,
            file_directory=args.directory,
            column_name=args.column,
            output_directory=args.output,
            optimize_parameters=args.optimize,
            max_p=args.max_p
        )
        
        # Initialize and run analysis
        arima_main = OCArimaMain()
        
        # Validate inputs
        print("Validating inputs...")
        arima_main.validate_inputs(job_config)
        print("✓ Inputs validated successfully")
        
        # Run analysis
        print(f"Running ARIMA({args.p}, {args.d}, {args.q}) analysis for {args.asset}...")
        results = arima_main.run_analysis(job_config)
        
        # Display summary
        print("\n" + "="*60)
        print("ANALYSIS SUMMARY")
        print("="*60)
        print(f"Asset: {results['asset_code']}")
        print(f"Model: ARIMA({results['model_parameters']['p']}, {results['model_parameters']['d']}, {results['model_parameters']['q']})")
        print(f"AIC: {results['model_fit']['aic']:.4f}")
        print(f"BIC: {results['model_fit']['bic']:.4f}")
        print(f"Data points used: {results['data_summary']['valid_points']}")
        print(f"Results saved to: {results['files']['output_json']}")
        
        print(f"\nCoefficients:")
        for name, value in results['coefficients'].items():
            significance = " ***" if results['coefficient_significance'][name] else ""
            print(f"  {name}: {value:.6f}{significance}")
        
        print(f"\nModel Diagnostics:")
        print(f"  Data is stationary: {results['model_diagnostics']['is_stationary']}")
        print(f"  Residuals are white noise: {results['model_diagnostics']['residuals_white_noise']}")
        
        print("\n✓ Analysis completed successfully!")
        
    except Exception as e:
        print(f"✗ Error: {str(e)}")
        return 1
    
    return 0

def run_gui():
    """Run ARIMA analysis via GUI."""
    try:
        from crypto_trading_model.arima.oc_arima_ui import OCArimaUI
        
        print("Starting OC ARIMA Analysis GUI...")
        app = OCArimaUI()
        app.run()
        
    except ImportError as e:
        print(f"GUI not available: {str(e)}")
        print("Please install tkinter to use GUI mode")
        return 1
    except Exception as e:
        print(f"Error starting GUI: {str(e)}")
        return 1
    
    return 0

def create_argument_parser():
    """Create command-line argument parser."""
    parser = argparse.ArgumentParser(
        description='OC ARIMA Analysis Tool',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Run with GUI
  python run_oc_arima.py --gui
  
  # Run CLI analysis for BTC with AR(1) model
  python run_oc_arima.py --asset BTC --p 1 --directory oc_analysis_results
  
  # Run CLI analysis for ETH with parameter optimization
  python run_oc_arima.py --asset ETH --optimize --max-p 15 --directory oc_analysis_results
  
  # Run CLI analysis with custom column
  python run_oc_arima.py --asset ETH --p 2 --directory oc_analysis_results --column Demean_Diff_OC
        """
    )
    
    # Mode selection
    parser.add_argument('--gui', action='store_true', 
                       help='Run in GUI mode (default if no other args provided)')
    
    # Analysis parameters
    parser.add_argument('--asset', type=str, default='BTC',
                       help='Asset code (default: BTC)')
    parser.add_argument('--p', type=int, default=1,
                       help='AR parameter p (default: 1)')
    parser.add_argument('--d', type=int, default=0,
                       help='Differencing parameter d (default: 0)')
    parser.add_argument('--q', type=int, default=0,
                       help='MA parameter q (default: 0)')
    parser.add_argument('--optimize', action='store_true',
                       help='Auto-optimize P parameter using AIC minimization')
    parser.add_argument('--max-p', type=int, default=10,
                       help='Maximum P value to test during optimization (default: 10)')
    
    # File parameters
    parser.add_argument('--directory', type=str, default='oc_analysis_results',
                       help='Directory containing OC analysis files (default: oc_analysis_results)')
    parser.add_argument('--column', type=str, default='Demean_Diff_OC',
                       help='Column name to analyze (default: Demean_Diff_OC)')
    parser.add_argument('--output', type=str,
                       help='Output directory for results (default: same as input directory)')
    
    # Utility options
    parser.add_argument('--verbose', '-v', action='store_true',
                       help='Enable verbose logging')
    
    return parser

def main():
    """Main entry point."""
    parser = create_argument_parser()
    args = parser.parse_args()
    
    # Setup logging
    import logging
    log_level = logging.DEBUG if args.verbose else logging.INFO
    logging.basicConfig(
        level=log_level,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    # Determine mode
    if args.gui or len(sys.argv) == 1:
        # GUI mode (default if no arguments or explicit --gui)
        return run_gui()
    else:
        # CLI mode
        return run_cli_analysis(args)

if __name__ == "__main__":
    sys.exit(main())