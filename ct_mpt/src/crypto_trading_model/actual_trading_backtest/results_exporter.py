"""
Results exporter for actual trading backtest.

Exports simulation results to Excel format following the required column structure.
"""

import pandas as pd
import numpy as np
from pathlib import Path
from typing import Dict, Any, Optional
from datetime import datetime
from ..custom_logging.logger import CustomLogger


class ActualTradingResultsExporter:
    """
    Exports actual trading backtest results to Excel.
    
    Follows Single Responsibility Principle - only handles result export.
    """
    
    def __init__(self, logger: Optional[CustomLogger] = None):
        """
        Initialize results exporter.
        
        Args:
            logger: Optional logger instance
        """
        self.logger = logger
    
    def export_to_excel(
        self,
        results_df: pd.DataFrame,
        output_path: str,
        include_summary: bool = True
    ) -> None:
        """
        Export results to Excel file with required column structure.
        
        Args:
            results_df: DataFrame with simulation results
            output_path: Path for output Excel file
            include_summary: Whether to include summary sheet
        """
        if self.logger:
            self.logger.info(f"Exporting actual trading backtest results to {output_path}")
        
        # Ensure output directory exists
        output_dir = Path(output_path).parent
        output_dir.mkdir(parents=True, exist_ok=True)
        
        try:
            with pd.ExcelWriter(output_path, engine='openpyxl') as writer:
                # Export main results
                self._export_main_results(results_df, writer)
                
                if include_summary:
                    # Export summary statistics
                    self._export_summary_statistics(results_df, writer)
                    
                    # Export rebalancing analysis
                    self._export_rebalancing_analysis(results_df, writer)
                    
                    # Export fee analysis
                    self._export_fee_analysis(results_df, writer)
            
            if self.logger:
                self.logger.info(f"Successfully exported results to {output_path}")
                
        except Exception as e:
            if self.logger:
                self.logger.error(f"Failed to export results: {str(e)}")
            raise
    
    def _export_main_results(self, results_df: pd.DataFrame, writer: pd.ExcelWriter) -> None:
        """Export main results with required columns."""
        
        # Define required columns as per specification
        required_columns = [
            'Timestamp',
            'trading_account_before',    # trading_account(T-1)
            'trading_account_after',     # trading_account(T)
            'trading_account_change',    # Daily change in trading account
            'savings_account_balance',   # Current savings account balance
            'savings_account_change',    # Daily change in savings account
            'total_account_balance',     # Total of trading + savings
            'withdraw_amount',           # Amount transferred during rebalancing
            'withdraw_direction',        # Direction of transfer
            'BTC_open_price',
            'BTC_close_price', 
            'ETH_open_price',
            'ETH_close_price',
            'BTC_weight',
            'ETH_weight',
            'Risky_Weight',
            'Final_Decision',            # Strategy (Long/Short)
            'Daily_Portfolio_Return',
            'Final_BTC_Weight',          # After leverage application
            'Final_ETH_Weight',          # After leverage application
            'Actual_BTC',                # Actual BTC quantity
            'Actual_ETH',                # Actual ETH quantity
            'Final_Portfolio_Return',    # Net return after fees
            'Profit_USD',                # Profit in USD
            'trading_fees_paid',         # Trading fees paid
            'rebalancing_occurred'       # Whether rebalancing occurred
        ]
        
        # Select available columns
        available_columns = [col for col in required_columns if col in results_df.columns]
        export_df = results_df[available_columns].copy()
        
        # Format numerical columns with appropriate precision
        numerical_columns = [
            'trading_account_before', 'trading_account_after', 'trading_account_change',
            'savings_account_balance', 'savings_account_change', 'total_account_balance',
            'withdraw_amount',
            'BTC_open_price', 'BTC_close_price', 'ETH_open_price', 'ETH_close_price',
            'BTC_weight', 'ETH_weight', 'Risky_Weight',
            'Daily_Portfolio_Return', 'Final_BTC_Weight', 'Final_ETH_Weight',
            'Actual_BTC', 'Actual_ETH', 'Final_Portfolio_Return', 'Profit_USD',
            'trading_fees_paid'
        ]
        
        for col in numerical_columns:
            if col in export_df.columns:
                export_df[col] = export_df[col].round(6)
        
        # Export to Excel
        export_df.to_excel(
            writer,
            sheet_name='Trading Results',
            index=False,
            float_format='%.6f'
        )
        
        if self.logger:
            self.logger.info(f"Exported {len(export_df)} rows of trading results")
    
    def _export_summary_statistics(self, results_df: pd.DataFrame, writer: pd.ExcelWriter) -> None:
        """Export summary statistics sheet."""
        
        summary_stats = []
        
        # Basic statistics
        if 'Final_Portfolio_Return' in results_df.columns:
            returns = results_df['Final_Portfolio_Return'].dropna()
            if len(returns) > 0:
                summary_stats.extend([
                    {'Metric': 'Total Trading Days', 'Value': len(returns)},
                    {'Metric': 'Average Daily Return', 'Value': returns.mean()},
                    {'Metric': 'Volatility (Daily)', 'Value': returns.std()},
                    {'Metric': 'Cumulative Return', 'Value': (1 + returns).prod() - 1},
                    {'Metric': 'Max Daily Return', 'Value': returns.max()},
                    {'Metric': 'Min Daily Return', 'Value': returns.min()},
                ])
        
        # Portfolio value statistics
        if 'trading_account_after' in results_df.columns:
            account_values = results_df['trading_account_after'].dropna()
            if len(account_values) > 0:
                initial_value = account_values.iloc[0] if len(account_values) > 0 else 0
                final_value = account_values.iloc[-1] if len(account_values) > 0 else 0
                
                summary_stats.extend([
                    {'Metric': '', 'Value': ''},  # Spacer
                    {'Metric': 'Initial Trading Account', 'Value': initial_value},
                    {'Metric': 'Final Trading Account', 'Value': final_value},
                    {'Metric': 'Total P&L', 'Value': final_value - initial_value},
                ])
        
        # Fee statistics
        if 'trading_fees_paid' in results_df.columns:
            fees = results_df['trading_fees_paid'].dropna()
            if len(fees) > 0:
                summary_stats.extend([
                    {'Metric': '', 'Value': ''},  # Spacer
                    {'Metric': 'Total Fees Paid', 'Value': fees.sum()},
                    {'Metric': 'Average Fee per Trade', 'Value': fees[fees > 0].mean()},
                    {'Metric': 'Number of Fee Events', 'Value': (fees > 0).sum()},
                ])
        
        # Strategy statistics
        if 'Final_Decision' in results_df.columns:
            strategies = results_df['Final_Decision'].value_counts()
            summary_stats.append({'Metric': '', 'Value': ''})  # Spacer
            
            for strategy, count in strategies.items():
                percentage = count / len(results_df) * 100
                summary_stats.append({
                    'Metric': f'{strategy} Strategy Days',
                    'Value': f'{count} ({percentage:.1f}%)'
                })
        
        # Convert to DataFrame and export
        if summary_stats:
            summary_df = pd.DataFrame(summary_stats)
            summary_df.to_excel(
                writer,
                sheet_name='Summary Statistics',
                index=False
            )
        
        if self.logger:
            self.logger.info("Exported summary statistics")
    
    def _export_rebalancing_analysis(self, results_df: pd.DataFrame, writer: pd.ExcelWriter) -> None:
        """Export rebalancing analysis sheet."""
        
        if 'rebalancing_occurred' not in results_df.columns:
            return
        
        # Find rebalancing events
        rebalancing_events = results_df[results_df['rebalancing_occurred'] == True].copy()
        
        if len(rebalancing_events) == 0:
            return
        
        # Select relevant columns for rebalancing analysis
        rebalancing_columns = [
            'Timestamp', 'trading_account_before', 'trading_account_after',
            'savings_account_balance', 'withdraw_amount', 'withdraw_direction',
            'transfer_amount', 'transfer_direction'
        ]
        
        available_rebalancing_columns = [
            col for col in rebalancing_columns 
            if col in rebalancing_events.columns
        ]
        
        rebalancing_df = rebalancing_events[available_rebalancing_columns]
        
        # Export rebalancing events
        rebalancing_df.to_excel(
            writer,
            sheet_name='Rebalancing Events',
            index=False
        )
        
        if self.logger:
            self.logger.info(f"Exported {len(rebalancing_df)} rebalancing events")
    
    def _export_fee_analysis(self, results_df: pd.DataFrame, writer: pd.ExcelWriter) -> None:
        """Export fee analysis sheet."""
        
        if 'trading_fees_paid' not in results_df.columns:
            return
        
        # Find days with trading fees
        fee_events = results_df[results_df['trading_fees_paid'] > 0].copy()
        
        if len(fee_events) == 0:
            return
        
        # Select relevant columns for fee analysis
        fee_columns = [
            'Timestamp', 'Final_Decision', 'trading_fees_paid',
            'BTC_weight', 'ETH_weight', 'Actual_BTC', 'Actual_ETH'
        ]
        
        available_fee_columns = [
            col for col in fee_columns 
            if col in fee_events.columns
        ]
        
        fee_df = fee_events[available_fee_columns]
        
        # Add cumulative fees
        fee_df = fee_df.copy()
        fee_df['Cumulative_Fees'] = fee_df['trading_fees_paid'].cumsum()
        
        # Export fee events
        fee_df.to_excel(
            writer,
            sheet_name='Fee Analysis',
            index=False
        )
        
        if self.logger:
            self.logger.info(f"Exported {len(fee_df)} fee events")
    
    def create_sample_row_dict(self) -> Dict[str, Any]:
        """
        Create sample row dictionary matching required output format.
        
        Returns:
            Dictionary with sample values for all required columns
        """
        return {
            'Timestamp': '2025-08-18',
            'trading_account_before': 500.0,
            'trading_account_after': 507.23,
            'BTC_open_price': 65000.0,
            'BTC_close_price': 65200.0,
            'ETH_open_price': 3500.0,
            'ETH_close_price': 3550.0,
            'BTC_weight': -1.00,
            'ETH_weight': 1.00,
            'Risky_Weight': 1.0308522853041016,
            'Final_Decision': 'Short',
            'Daily_Portfolio_Return': 0.0045,
            'Final_BTC_Weight': -1.96,
            'Final_ETH_Weight': 0.96,
            'Actual_BTC': -0.025,
            'Actual_ETH': 0.32,
            'Final_Portfolio_Return': 0.006175698561116933,
            'Profit_USD': 7.230380220000165
        }
    
    def validate_results_format(self, results_df: pd.DataFrame) -> Dict[str, Any]:
        """
        Validate that results DataFrame has required format.
        
        Args:
            results_df: Results DataFrame to validate
            
        Returns:
            Validation report dictionary
        """
        required_columns = [
            'Timestamp', 'trading_account_before', 'trading_account_after',
            'BTC_open_price', 'BTC_close_price', 'ETH_open_price', 'ETH_close_price',
            'BTC_weight', 'ETH_weight', 'Risky_Weight', 'Final_Decision',
            'Daily_Portfolio_Return', 'Final_BTC_Weight', 'Final_ETH_Weight',
            'Actual_BTC', 'Actual_ETH', 'Final_Portfolio_Return', 'Profit_USD'
        ]
        
        missing_columns = [col for col in required_columns if col not in results_df.columns]
        present_columns = [col for col in required_columns if col in results_df.columns]
        
        return {
            'is_valid': len(missing_columns) == 0,
            'total_required': len(required_columns),
            'present_columns': len(present_columns),
            'missing_columns': missing_columns,
            'validation_score': len(present_columns) / len(required_columns)
        }