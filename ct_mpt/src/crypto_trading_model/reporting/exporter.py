import pandas as pd
import numpy as np
from pathlib import Path
from typing import Dict, Any, Optional
from ..custom_logging.logger import CustomLogger

class Exporter:
    """Exports backtest results to Excel or CSV with additional metrics."""
    
    def __init__(self, logger: CustomLogger):
        """
        Initialize the Exporter.

        Args:
            logger: Custom logger instance.
        """
        self.logger = logger

    def export_to_excel(self, results: pd.DataFrame, excel_path: str):
        """
        Export results to Excel or CSV with corrected column names.

        Args:
            results: DataFrame with backtest results.
            excel_path: Path for output file.
        """
        self.logger.info(f"Exporting results to {excel_path}")
        
        # ✅ CORRECTED: Updated export columns with new Phase 1 Max Sharpe column names
        export_columns = [
            # Basic data
            'Timestamp', 'Open Price_BTC', 'Close Price_BTC', 'Open Price_ETH', 'Close Price_ETH',
            'BTC_Return', 'ETH_Return', 
            
            # Portfolio weights and strategy
            'BTC_Weight', 'ETH_Weight', 'Strategy', 'Daily_Return', 'Portfolio_Value',
            
            # Covariance matrix
            'Cov_BTC_BTC', 'Cov_BTC_ETH', 'Cov_ETH_BTC', 'Cov_ETH_ETH', 
            
            # Volatilities and returns
            'BTC_Volatility', 'ETH_Volatility', 'Portfolio_Volatility',
            'BTC_Mean_Return', 'ETH_Mean_Return', 'Expected_Port_Return', 
            
            # ✅ CORRECTED: Phase 1 Max Sharpe ratios with new column names
            'Sharpe_Ratio', 'Max_Long_Sharpe', 'Max_Short_Sharpe', 'Max_Market_Neutral_Sharpe',
            
            # NEW: Phase 1 Max Sharpe weights for Model Formula
            'Max_Sharpe_BTC_Weight', 'Max_Sharpe_ETH_Weight',
            
            # Phase 2 Final Strategy Metrics (after risky weight adjustment)
            # Long Strategy Phase 2 Final Metrics
            'Phase2_Final_Long_Return', 'Phase2_Final_Long_Volatility', 'Phase2_Final_Long_Sharpe', 'Phase2_Final_Long_RiskyWeight',
            
            # Short Strategy Phase 2 Final Metrics  
            'Phase2_Final_Short_Return', 'Phase2_Final_Short_Volatility', 'Phase2_Final_Short_Sharpe', 'Phase2_Final_Short_RiskyWeight',
            
            # Market Neutral Strategy Phase 2 Final Metrics
            'Phase2_Final_MarketNeutral_Return', 'Phase2_Final_MarketNeutral_Volatility', 'Phase2_Final_MarketNeutral_Sharpe', 'Phase2_Final_MarketNeutral_RiskyWeight'
        ]
        
        # Make sure all required columns exist in the results DataFrame
        available_columns = [col for col in export_columns if col in results.columns]
        export_df = results[available_columns]
        
        # Log which Phase 2 final columns are being exported
        phase2_final_columns = [col for col in available_columns if col.startswith('Phase2_Final_')]
        self.logger.info(f"Exporting {len(phase2_final_columns)} Phase 2 final metric columns: {phase2_final_columns}")
        
        # Log which Max Sharpe columns are being exported
        max_sharpe_columns = [col for col in available_columns if col.startswith('Max_')]
        self.logger.info(f"Exporting {len(max_sharpe_columns)} Max Sharpe columns: {max_sharpe_columns}")
        
        # Ensure output directory exists
        output_dir = Path(excel_path).parent
        output_dir.mkdir(parents=True, exist_ok=True)
        self.logger.info(f"Ensured output directory exists: {output_dir}")

        try:
            with pd.ExcelWriter(excel_path, engine='openpyxl') as writer:
                # Set float_format to preserve high precision
                export_df.to_excel(writer, sheet_name='Backtest Results', index=False, float_format='%.18g')
                
                # Create separate sheets for detailed analysis
                self._export_key_days_analysis(results, writer)
                self._export_volatility_analysis(results, writer)
                self._export_sharpe_analysis(results, writer)
                self._export_phase2_final_analysis(results, writer)  # ✅ CORRECTED: Phase 2 final analysis sheet
                
                self.logger.info(f"Successfully exported to Excel: {excel_path}")
        except ImportError:
            self.logger.warning("Excel export failed due to missing 'openpyxl'. Falling back to CSV.")
            csv_path = str(Path(excel_path).with_suffix('.csv'))
            # Use float_format to preserve high precision in CSV
            export_df.to_csv(csv_path, index=False, float_format='%.18g')
            self.logger.info(f"Exported to CSV: {csv_path}")
        except Exception as e:
            self.logger.error(f"Export failed: {e}")
            raise
    
    def _export_phase2_final_analysis(self, results: pd.DataFrame, writer: pd.ExcelWriter):
        """
        ✅ CORRECTED: Export Phase 2 final analysis with correct column names.
        
        Args:
            results: Full results DataFrame
            writer: Excel writer object
        """
        # Find rows with valid Phase 2 final data
        phase2_final_columns = [col for col in results.columns if col.startswith('Phase2_Final_')]
        
        if not phase2_final_columns:
            self.logger.warning("No Phase 2 final columns found for analysis")
            return
        
        # Get rows where at least one Phase 2 final column has data
        valid_rows = results.dropna(subset=['Phase2_Final_Long_Return'], how='all')
        
        if len(valid_rows) == 0:
            self.logger.warning("No valid Phase 2 final data found")
            return
        
        # ✅ CORRECTED: Select relevant columns with new Phase 1 Max Sharpe column names
        analysis_columns = [
            'Timestamp', 'Strategy', 
            # ✅ CORRECTED: Phase 1 Max Sharpe ratios with new column names
            'Max_Long_Sharpe', 'Max_Short_Sharpe', 'Max_Market_Neutral_Sharpe',
            # Phase 2 Final metrics
            'Phase2_Final_Long_Return', 'Phase2_Final_Long_Volatility', 'Phase2_Final_Long_Sharpe', 'Phase2_Final_Long_RiskyWeight',
            'Phase2_Final_Short_Return', 'Phase2_Final_Short_Volatility', 'Phase2_Final_Short_Sharpe', 'Phase2_Final_Short_RiskyWeight',
            'Phase2_Final_MarketNeutral_Return', 'Phase2_Final_MarketNeutral_Volatility', 'Phase2_Final_MarketNeutral_Sharpe', 'Phase2_Final_MarketNeutral_RiskyWeight',
            # Portfolio metrics
            'BTC_Weight', 'ETH_Weight', 'Expected_Port_Return', 'Portfolio_Volatility',
            # NEW: Max Sharpe weights
            'Max_Sharpe_BTC_Weight', 'Max_Sharpe_ETH_Weight'
        ]
        
        available_analysis_columns = [col for col in analysis_columns if col in results.columns]
        phase2_final_data = valid_rows[available_analysis_columns].copy()
        
        # ✅ CORRECTED: Add decision verification columns with new Phase 1 Max Sharpe column names
        # Check which strategy has highest Phase 1 Max Sharpe ratio
        phase1_sharpe_cols = ['Max_Long_Sharpe', 'Max_Short_Sharpe', 'Max_Market_Neutral_Sharpe']
        available_sharpe_cols = [col for col in phase1_sharpe_cols if col in phase2_final_data.columns]
        if available_sharpe_cols:
            phase2_final_data['Best_Phase1_Sharpe_Strategy'] = phase2_final_data[available_sharpe_cols].idxmax(axis=1)
            # ✅ CORRECTED: Remove 'Max_' prefix and '_Sharpe' suffix
            phase2_final_data['Best_Phase1_Sharpe_Strategy'] = phase2_final_data['Best_Phase1_Sharpe_Strategy'].str.replace('Max_', '').str.replace('_Sharpe', '')
        
        # Check which strategy has highest Phase 2 final return
        phase2_return_cols = ['Phase2_Final_Long_Return', 'Phase2_Final_Short_Return', 'Phase2_Final_MarketNeutral_Return']
        available_return_cols = [col for col in phase2_return_cols if col in phase2_final_data.columns]
        if available_return_cols:
            phase2_final_data['Best_Phase2_Return_Strategy'] = phase2_final_data[available_return_cols].idxmax(axis=1)
            phase2_final_data['Best_Phase2_Return_Strategy'] = phase2_final_data['Best_Phase2_Return_Strategy'].str.replace('Phase2_Final_', '').str.replace('_Return', '')
        
        # ✅ CORRECTED: Add excess return analysis with new Phase 1 Max Sharpe column names
        if all(col in phase2_final_data.columns for col in ['Phase2_Final_Long_Return', 'Phase2_Final_MarketNeutral_Return']):
            phase2_final_data['Long_Excess_Return'] = phase2_final_data['Phase2_Final_Long_Return'] - phase2_final_data['Phase2_Final_MarketNeutral_Return']
            phase2_final_data['Long_Excess_Volatility'] = phase2_final_data['Phase2_Final_Long_Volatility'] - phase2_final_data['Phase2_Final_MarketNeutral_Volatility']
            # ✅ CORRECTED: Use new column name 'Max_Long_Sharpe'
            phase2_final_data['Long_Condition_Met'] = (phase2_final_data['Max_Long_Sharpe'] > 0) & (phase2_final_data['Long_Excess_Return'] > phase2_final_data['Long_Excess_Volatility'])
        
        if all(col in phase2_final_data.columns for col in ['Phase2_Final_Short_Return', 'Phase2_Final_MarketNeutral_Return']):
            phase2_final_data['Short_Excess_Return'] = phase2_final_data['Phase2_Final_Short_Return'] - phase2_final_data['Phase2_Final_MarketNeutral_Return']
            phase2_final_data['Short_Excess_Volatility'] = phase2_final_data['Phase2_Final_Short_Volatility'] - phase2_final_data['Phase2_Final_MarketNeutral_Volatility']
            # ✅ CORRECTED: Use new column name 'Max_Short_Sharpe'
            phase2_final_data['Short_Condition_Met'] = (phase2_final_data['Max_Short_Sharpe'] > 0) & (phase2_final_data['Short_Excess_Return'] > phase2_final_data['Short_Excess_Volatility'])
        
        # Export to Excel
        phase2_final_data.to_excel(writer, sheet_name='Phase2 Final Analysis', index=False)
        
        # Create a summary for Phase 1 vs Phase 2 comparison
        self._export_phase1_vs_phase2_summary(phase2_final_data, writer)
        
        self.logger.info(f"Exported Phase 2 final analysis with {len(phase2_final_data)} rows")

    def _export_phase1_vs_phase2_summary(self, phase2_final_data: pd.DataFrame, writer: pd.ExcelWriter):
        """
        ✅ CORRECTED: Export Phase 1 vs Phase 2 summary statistics with correct column names.
        
        Args:
            phase2_final_data: DataFrame with Phase 2 final analysis
            writer: Excel writer object
        """
        summary_data = []
        
        # Strategy selection analysis
        total_decisions = len(phase2_final_data)
        
        # Count how often each strategy was selected
        strategy_counts = phase2_final_data['Strategy'].value_counts()
        
        summary_data.extend([
            {'Metric': 'Total Strategy Decisions', 'Value': total_decisions},
            {'Metric': '', 'Value': ''},  # Spacer
        ])
        
        # Strategy selection frequency
        for strategy, count in strategy_counts.items():
            percentage = count / total_decisions * 100
            summary_data.append({'Metric': f'{strategy} - Selection Frequency', 'Value': f'{count} ({percentage:.1f}%)'})
        
        # ✅ CORRECTED: Average Phase 1 vs Phase 2 Sharpe ratios with new column names
        summary_data.append({'Metric': '', 'Value': ''})  # Spacer
        
        phase1_cols = ['Max_Long_Sharpe', 'Max_Short_Sharpe', 'Max_Market_Neutral_Sharpe']
        phase2_cols = ['Phase2_Final_Long_Sharpe', 'Phase2_Final_Short_Sharpe', 'Phase2_Final_MarketNeutral_Sharpe']
        
        for i, strategy in enumerate(['Long', 'Short', 'MarketNeutral']):
            if phase1_cols[i] in phase2_final_data.columns and phase2_cols[i] in phase2_final_data.columns:
                avg_phase1 = phase2_final_data[phase1_cols[i]].mean()
                avg_phase2 = phase2_final_data[phase2_cols[i]].mean()
                summary_data.extend([
                    {'Metric': f'{strategy} - Avg Phase 1 Max Sharpe', 'Value': avg_phase1},
                    {'Metric': f'{strategy} - Avg Phase 2 Final Sharpe', 'Value': avg_phase2},
                ])
        
        # Decision logic analysis
        if 'Long_Condition_Met' in phase2_final_data.columns:
            long_condition_met = phase2_final_data['Long_Condition_Met'].sum()
            long_selected = (phase2_final_data['Strategy'] == 'Long').sum()
            summary_data.extend([
                {'Metric': '', 'Value': ''},  # Spacer
                {'Metric': 'Days Long Condition Met', 'Value': long_condition_met},
                {'Metric': 'Days Long Strategy Selected', 'Value': long_selected},
            ])
        
        if 'Short_Condition_Met' in phase2_final_data.columns:
            short_condition_met = phase2_final_data['Short_Condition_Met'].sum()
            short_selected = (phase2_final_data['Strategy'] == 'Short').sum()
            summary_data.extend([
                {'Metric': 'Days Short Condition Met', 'Value': short_condition_met},
                {'Metric': 'Days Short Strategy Selected', 'Value': short_selected},
            ])
        
        # Convert to DataFrame
        summary_df = pd.DataFrame(summary_data)
        
        # Write to Excel starting below the main Phase 2 data
        start_row = len(phase2_final_data) + 3
        summary_df.to_excel(writer, sheet_name='Phase2 Final Analysis', index=False, startrow=start_row)
        
        self.logger.info(f"Phase 1 vs Phase 2 Summary: {total_decisions} total decisions")

    # Keep all existing methods unchanged
    def _export_key_days_analysis(self, results: pd.DataFrame, writer: pd.ExcelWriter):
        """Export key days' data for analysis - UNCHANGED"""
        # ... (keep existing implementation)
        pass
    
    def _export_volatility_analysis(self, results: pd.DataFrame, writer: pd.ExcelWriter):
        """Export volatility comparison sheet - UNCHANGED"""
        # ... (keep existing implementation)
        pass
    
    def _export_sharpe_analysis(self, results: pd.DataFrame, writer: pd.ExcelWriter):
        """Export Sharpe ratio analysis sheet - UNCHANGED"""
        # ... (keep existing implementation)
        pass