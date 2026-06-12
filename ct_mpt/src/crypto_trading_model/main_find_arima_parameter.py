#!/usr/bin/env python
"""
Main ARIMA Parameter Finder
===========================

File: main_find_arima_parameter.py

This script runs ARIMA analysis on your backtest results to find optimal parameters (p,d,q)
and AR/MA coefficients, then exports everything to JSON master data files.

Workflow:
1. Load existing backtest results from main.py
2. Run ARIMA analysis on portfolio returns
3. Find optimal ARIMA(p,d,q) parameters  
4. Extract significant AR/MA coefficients
5. Export master data JSON files for later use

Usage:
    python main_find_arima_parameter.py
    
    # Or with specific parameters:
    python main_find_arima_parameter.py --risk-profile neutral --backtest-file backtest_results_neutral.xlsx
    
Output:
    - arima_master_data_{risk_profile}.json (for each viable profile)
    - arima_analysis_results/ (detailed analysis reports)
"""

import sys
import argparse
from pathlib import Path
import pandas as pd
import numpy as np
from datetime import datetime

import sys
import argparse
from pathlib import Path
import pandas as pd
import numpy as np
from datetime import datetime
from typing import Dict, List, Optional, Any

# Add the src directory to Python path so we can import crypto_trading_model
if __name__ == "__main__":
    # Get current file location: .../src/crypto_trading_model/main_find_arima_parameter.py
    current_file = Path(__file__).resolve()
    
    # Go up to src directory: .../src/
    src_dir = current_file.parent.parent
    
    # Add src to Python path
    sys.path.insert(0, str(src_dir))

# Now use absolute imports that work with the package structure
from crypto_trading_model.custom_logging.logger import CustomLogger
from crypto_trading_model.config.config_manager import ConfigManager
from crypto_trading_model.arima.backtest_arima_analyzer import BacktestArimaAnalyzer

def find_arima_parameters_for_profile(risk_profile: str, backtest_file: Optional[str] = None, 
                                    logger: CustomLogger = None) -> Dict[str, Any]:
    """
    Find ARIMA parameters for a specific risk profile.
    
    Args:
        risk_profile: Risk profile to analyze ('neutral', 'averse', 'lover')
        backtest_file: Optional specific backtest file path
        logger: Logger instance
        
    Returns:
        Dictionary with analysis results and file paths
    """
    if logger is None:
        logger = CustomLogger(f'ArimaParameterFinder_{risk_profile}')
    
    logger.info(f"🔬 FINDING ARIMA PARAMETERS FOR {risk_profile.upper()} PROFILE")
    logger.info("=" * 60)
    
    try:
        # Determine backtest file path
        if backtest_file is None:
            # Look for standard backtest results
            possible_files = [
                f"backtest_results_{risk_profile}.xlsx",
                f"../backtest_results_{risk_profile}.xlsx",
                f"../../backtest_results_{risk_profile}.xlsx",
                f"backtest_results/backtest_results_{risk_profile}.xlsx"
            ]
            
            backtest_file = None
            for file_path in possible_files:
                if Path(file_path).exists():
                    backtest_file = file_path
                    logger.info(f"✅ Found backtest file: {backtest_file}")
                    break
            
            if backtest_file is None:
                error_msg = f"❌ No backtest results found for {risk_profile} profile"
                logger.error(error_msg)
                logger.info("💡 Run main.py first to generate backtest results")
                return {'status': 'error', 'error': error_msg}
        
        # Validate backtest file exists
        if not Path(backtest_file).exists():
            error_msg = f"❌ Backtest file not found: {backtest_file}"
            logger.error(error_msg)
            return {'status': 'error', 'error': error_msg}
        
        # Initialize ARIMA analyzer
        analyzer = BacktestArimaAnalyzer(logger)
        
        # Run comprehensive ARIMA analysis
        logger.info(f"\n📊 Running ARIMA analysis on {backtest_file}...")
        arima_results = analyzer.analyze_backtest_results(backtest_file, risk_profile)
        
        # Check if forecasting is viable
        if arima_results['forecast_ready']:
            logger.info(f"✅ ARIMA ANALYSIS SUCCESSFUL!")
            logger.info(f"   Model: ARIMA{arima_results['arima_analysis']['analysis_summary']['optimal_order']}")
            logger.info(f"   AIC: {arima_results['arima_analysis']['analysis_summary']['best_aic']:.4f}")
            logger.info(f"   Significant Coefficients: {arima_results['arima_analysis']['analysis_summary']['num_significant_coefficients']}")
            
            # Master data JSON should be automatically generated
            master_data_path = arima_results.get('master_data_path', f"arima_master_data_{risk_profile}.json")
            
            if Path(master_data_path).exists():
                logger.info(f"✅ Master data exported: {master_data_path}")
            else:
                logger.warning(f"⚠️ Master data file not found: {master_data_path}")
            
            # Save detailed analysis results
            output_dir = Path("arima_analysis_results")
            output_dir.mkdir(exist_ok=True)
            analyzer.save_analysis_results(arima_results, str(output_dir))
            
            return {
                'status': 'success',
                'risk_profile': risk_profile,
                'forecast_ready': True,
                'arima_model': arima_results['arima_analysis']['analysis_summary']['optimal_order'],
                'aic': arima_results['arima_analysis']['analysis_summary']['best_aic'],
                'significant_coefficients': arima_results['arima_analysis']['analysis_summary']['num_significant_coefficients'],
                'master_data_path': master_data_path,
                'backtest_file': backtest_file,
                'analysis_results': arima_results
            }
            
        else:
            logger.warning(f"⚠️ ARIMA FORECASTING NOT VIABLE for {risk_profile} profile")
            logger.info("📊 Reasons:")
            
            insights = arima_results.get('trading_insights', {})
            if 'arima_insights' in insights:
                for insight in insights['arima_insights']:
                    logger.info(f"   • {insight}")
            
            logger.info("\n💡 Recommendations:")
            if 'integration_recommendations' in insights:
                for i, rec in enumerate(insights['integration_recommendations'][:3], 1):
                    logger.info(f"   {i}. {rec}")
            
            return {
                'status': 'not_viable',
                'risk_profile': risk_profile,
                'forecast_ready': False,
                'reasons': insights.get('arima_insights', []),
                'recommendations': insights.get('integration_recommendations', []),
                'backtest_file': backtest_file,
                'analysis_results': arima_results
            }
            
    except Exception as e:
        error_msg = f"ARIMA parameter finding failed for {risk_profile}: {e}"
        logger.error(error_msg)
        import traceback
        traceback.print_exc()
        return {'status': 'error', 'error': error_msg}

def find_arima_parameters_all_profiles(config_manager: ConfigManager, logger: CustomLogger) -> Dict[str, Any]:
    """
    Find ARIMA parameters for all configured risk profiles.
    
    Args:
        config_manager: Configuration manager
        logger: Logger instance
        
    Returns:
        Dictionary with results for all profiles
    """
    logger.info("🔬 FINDING ARIMA PARAMETERS FOR ALL RISK PROFILES")
    logger.info("=" * 60)
    
    try:
        config = config_manager.get_config()
        risk_profiles = config.risk_profiles.available
        
        logger.info(f"Risk profiles to analyze: {risk_profiles}")
        
        results = {
            'timestamp': datetime.now().isoformat(),
            'profiles_analyzed': len(risk_profiles),
            'profiles': {}
        }
        
        successful_profiles = []
        failed_profiles = []
        not_viable_profiles = []
        
        for profile in risk_profiles:
            logger.info(f"\n{'='*20} {profile.upper()} PROFILE {'='*20}")
            
            profile_result = find_arima_parameters_for_profile(profile, logger=logger)
            results['profiles'][profile] = profile_result
            
            if profile_result['status'] == 'success':
                successful_profiles.append(profile)
                logger.info(f"✅ {profile}: SUCCESS - Forecasting viable")
                
            elif profile_result['status'] == 'not_viable':
                not_viable_profiles.append(profile)
                logger.info(f"⚠️ {profile}: NOT VIABLE - Forecasting not recommended")
                
            else:
                failed_profiles.append(profile)
                logger.error(f"❌ {profile}: FAILED - {profile_result.get('error', 'Unknown error')}")
        
        # Summary
        results['summary'] = {
            'successful_profiles': successful_profiles,
            'not_viable_profiles': not_viable_profiles,
            'failed_profiles': failed_profiles,
            'success_count': len(successful_profiles),
            'not_viable_count': len(not_viable_profiles),
            'failed_count': len(failed_profiles)
        }
        
        logger.info(f"\n📊 FINAL SUMMARY")
        logger.info("=" * 40)
        logger.info(f"Total profiles analyzed: {len(risk_profiles)}")
        logger.info(f"✅ Successful (Forecast Ready): {len(successful_profiles)} - {successful_profiles}")
        logger.info(f"⚠️ Not Viable: {len(not_viable_profiles)} - {not_viable_profiles}")
        logger.info(f"❌ Failed: {len(failed_profiles)} - {failed_profiles}")
        
        if successful_profiles:
            logger.info(f"\n🎯 NEXT STEPS:")
            logger.info(f"1. Master data JSON files created for: {successful_profiles}")
            logger.info(f"2. Run main_apply_arima.py to use these parameters for forecasting")
            logger.info(f"3. Generated files:")
            for profile in successful_profiles:
                master_data_path = f"arima_master_data_{profile}.json"
                if Path(master_data_path).exists():
                    logger.info(f"   📁 {master_data_path}")
        
        return results
        
    except Exception as e:
        error_msg = f"Failed to analyze all profiles: {e}"
        logger.error(error_msg)
        return {'status': 'error', 'error': error_msg}

def validate_environment(logger: CustomLogger) -> bool:
    """Validate that the environment is ready for ARIMA analysis."""
    
    logger.info("🔍 VALIDATING ENVIRONMENT")
    logger.info("-" * 30)
    
    try:
        # Check required packages
        required_packages = ['statsmodels', 'scipy', 'pandas', 'numpy']
        missing_packages = []
        
        for package in required_packages:
            try:
                __import__(package)
                logger.info(f"✅ {package}: Available")
            except ImportError:
                missing_packages.append(package)
                logger.error(f"❌ {package}: Missing")
        
        if missing_packages:
            logger.error(f"❌ Missing required packages: {missing_packages}")
            logger.info(f"💡 Install with: pip install {' '.join(missing_packages)}")
            return False
        
        # Check for backtest results
        backtest_patterns = [
            "backtest_results_*.xlsx",
            "../backtest_results_*.xlsx",
            "backtest_results/backtest_results_*.xlsx"
        ]
        
        found_files = []
        for pattern in backtest_patterns:
            found_files.extend(list(Path(".").glob(pattern)))
        
        if found_files:
            logger.info(f"✅ Found {len(found_files)} backtest result files:")
            for file in found_files[:5]:  # Show first 5
                logger.info(f"   📁 {file}")
        else:
            logger.warning(f"⚠️ No backtest results found")
            logger.info(f"💡 Run main.py first to generate backtest results")
            return False
        
        logger.info(f"✅ Environment validation passed")
        return True
        
    except Exception as e:
        logger.error(f"Environment validation failed: {e}")
        return False

def main():
    """Main function for ARIMA parameter finding."""
    
    # Parse command line arguments
    parser = argparse.ArgumentParser(description='Find ARIMA parameters for crypto trading strategy')
    parser.add_argument('--risk-profile', type=str, choices=['neutral', 'averse', 'lover'],
                       help='Specific risk profile to analyze')
    parser.add_argument('--backtest-file', type=str,
                       help='Specific backtest file to analyze')
    parser.add_argument('--output-dir', type=str, default='arima_analysis_results',
                       help='Output directory for analysis results')
    parser.add_argument('--validate-only', action='store_true',
                       help='Only validate environment without running analysis')
    
    args = parser.parse_args()
    
    # Initialize logger
    logger = CustomLogger('ArimaParameterFinder')
    
    print("🔬 ARIMA PARAMETER FINDER")
    print("=" * 50)
    print("This script analyzes your backtest results to find optimal ARIMA parameters")
    print("and exports them to JSON master data files for forecasting.")
    print()
    
    # Validate environment
    if not validate_environment(logger):
        print("❌ Environment validation failed. Please fix issues above.")
        return 1
    
    if args.validate_only:
        print("✅ Environment validation passed. Ready for ARIMA analysis.")
        return 0
    
    try:
        # Initialize configuration
        config_manager = ConfigManager(logger)
        
        if args.risk_profile:
            # Analyze specific profile
            logger.info(f"Analyzing specific risk profile: {args.risk_profile}")
            
            result = find_arima_parameters_for_profile(
                risk_profile=args.risk_profile,
                backtest_file=args.backtest_file,
                logger=logger
            )
            
            if result['status'] == 'success':
                print(f"\n✅ SUCCESS: ARIMA parameters found for {args.risk_profile}")
                print(f"📁 Master data: {result['master_data_path']}")
                print(f"🎯 Model: ARIMA{result['arima_model']}")
                return 0
            elif result['status'] == 'not_viable':
                print(f"\n⚠️ NOT VIABLE: ARIMA forecasting not recommended for {args.risk_profile}")
                print("See analysis results for details.")
                return 0
            else:
                print(f"\n❌ FAILED: {result.get('error', 'Unknown error')}")
                return 1
                
        else:
            # Analyze all profiles
            logger.info("Analyzing all configured risk profiles")
            
            results = find_arima_parameters_all_profiles(config_manager, logger)
            
            if 'summary' in results:
                summary = results['summary']
                
                if summary['success_count'] > 0:
                    print(f"\n✅ SUCCESS: {summary['success_count']} profiles ready for forecasting")
                    print(f"📁 Profiles: {summary['successful_profiles']}")
                    print(f"🎯 Ready to run main_apply_arima.py")
                    return 0
                else:
                    print(f"\n⚠️ NO VIABLE PROFILES: ARIMA forecasting not viable for any profile")
                    print(f"💡 This may indicate efficient markets or need for strategy improvements")
                    return 0
            else:
                print(f"\n❌ ANALYSIS FAILED: {results.get('error', 'Unknown error')}")
                return 1
                
    except Exception as e:
        logger.error(f"Main execution failed: {e}")
        import traceback
        traceback.print_exc()
        return 1

if __name__ == "__main__":
    exit(main())