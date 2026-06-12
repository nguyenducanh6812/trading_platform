#!/usr/bin/env python3
"""
Simple Dynamic Portfolio UI
============================

Simple UI for testing the SimpleDynamicBacktester implementation.
This follows the user's request for a simpler approach.
"""

import sys
import os
import tkinter as tk
from tkinter import ttk, filedialog, messagebox, scrolledtext
from pathlib import Path
import threading
from typing import Dict, List, Optional
import pandas as pd
import numpy as np
import json
from datetime import datetime

# Add src directory to Python path
src_path = Path(__file__).parent / "src"
sys.path.insert(0, str(src_path))

class SimpleDynamicPortfolioGUI:
    """Simple GUI for testing dynamic portfolio backtesting."""
    
    def __init__(self, root):
        """Initialize the GUI."""
        self.root = root
        self.root.title("Simple Dynamic Portfolio Backtester")
        self.root.geometry("800x600")
        
        # Data storage
        self.selected_assets = {}  # {asset_code: oc_file_path}
        self.backtest_config = {
            'risk_profile': 'neutral',
            'rebalance_frequency': 1,  # Daily rebalancing
            'lookback_period': 7,      # For covariance matrix calculation
            'optimization_method': 'traditional',  # 'traditional', 'smart_grid', or 'compare'
            'smart_grid_precision': 2,  # Decimal precision for Smart Grid
        }
        
        # Create GUI components
        self._create_widgets()
        
    def _create_widgets(self):
        """Create GUI widgets."""
        
        # Title
        title_label = ttk.Label(self.root, text="Simple Dynamic Portfolio Backtester", 
                               font=("Arial", 16, "bold"))
        title_label.pack(pady=10)
        
        # Strict mode warning
        warning_label = ttk.Label(self.root, text="STRICT MODE: Requires complete ARIMA predictions for all assets", 
                                 font=("Arial", 10, "italic"), foreground="red")
        warning_label.pack(pady=5)
        
        # Asset selection frame
        asset_frame = ttk.LabelFrame(self.root, text="Asset Selection - Prediction Files (*_with_predictions*.xlsx)", padding=10)
        asset_frame.pack(fill="x", padx=10, pady=5)
        
        # Asset input
        input_frame = ttk.Frame(asset_frame)
        input_frame.pack(fill="x", pady=5)
        
        ttk.Label(input_frame, text="Asset Code:").pack(side="left")
        self.asset_entry = ttk.Entry(input_frame, width=10)
        self.asset_entry.pack(side="left", padx=5)
        
        ttk.Button(input_frame, text="Select Prediction File", 
                  command=self._select_oc_file).pack(side="left", padx=5)
        
        ttk.Button(input_frame, text="Auto-Detect Assets", 
                  command=self._auto_detect_assets).pack(side="left", padx=5)
        
        # Selected assets list
        self.assets_text = scrolledtext.ScrolledText(asset_frame, height=5, width=80)
        self.assets_text.pack(fill="x", pady=5)
        
        # Configuration frame
        config_frame = ttk.LabelFrame(self.root, text="Configuration", padding=10)
        config_frame.pack(fill="x", padx=10, pady=5)
        
        # Risk profile
        risk_frame = ttk.Frame(config_frame)
        risk_frame.pack(fill="x", pady=2)
        ttk.Label(risk_frame, text="Risk Profile:").pack(side="left")
        self.risk_var = tk.StringVar(value="neutral")
        risk_combo = ttk.Combobox(risk_frame, textvariable=self.risk_var, 
                                 values=["averse", "neutral", "lover"], width=10)
        risk_combo.pack(side="left", padx=5)
        
        # Rebalance frequency
        rebal_frame = ttk.Frame(config_frame)
        rebal_frame.pack(fill="x", pady=2)
        ttk.Label(rebal_frame, text="Rebalance Frequency (days):").pack(side="left")
        self.rebal_var = tk.IntVar(value=1)
        rebal_spin = ttk.Spinbox(rebal_frame, from_=1, to=30, textvariable=self.rebal_var, width=10)
        rebal_spin.pack(side="left", padx=5)
        
        # Lookback period
        lookback_frame = ttk.Frame(config_frame)
        lookback_frame.pack(fill="x", pady=2)
        ttk.Label(lookback_frame, text="Lookback Period (days):").pack(side="left")
        self.lookback_var = tk.IntVar(value=7)
        lookback_spin = ttk.Spinbox(lookback_frame, from_=5, to=30, textvariable=self.lookback_var, width=10)
        lookback_spin.pack(side="left", padx=5)
        
        # Optimization method
        optim_frame = ttk.Frame(config_frame)
        optim_frame.pack(fill="x", pady=2)
        ttk.Label(optim_frame, text="Optimization Method:").pack(side="left")
        self.optim_var = tk.StringVar(value="traditional")
        optim_combo = ttk.Combobox(optim_frame, textvariable=self.optim_var, 
                                  values=["traditional", "smart_grid", "compare"], width=12)
        optim_combo.pack(side="left", padx=5)
        optim_combo.bind("<<ComboboxSelected>>", self._on_optimization_method_change)
        
        # Smart Grid precision (initially hidden)
        self.precision_frame = ttk.Frame(config_frame)
        ttk.Label(self.precision_frame, text="Smart Grid Precision (decimals):").pack(side="left")
        self.precision_var = tk.IntVar(value=2)
        precision_spin = ttk.Spinbox(self.precision_frame, from_=1, to=4, textvariable=self.precision_var, width=5)
        precision_spin.pack(side="left", padx=5)
        
        # Traditional weight precision option (initially shown)
        self.traditional_precision_frame = ttk.Frame(config_frame)
        self.traditional_precision_frame.pack(fill="x", pady=2)
        self.use_custom_precision_var = tk.BooleanVar(value=False)
        precision_check = ttk.Checkbutton(self.traditional_precision_frame, text="Override weight precision:", 
                                         variable=self.use_custom_precision_var)
        precision_check.pack(side="left")
        
        # Weight precision
        ttk.Label(self.traditional_precision_frame, text="Decimals:").pack(side="left", padx=(10, 5))
        self.weight_precision_var = tk.IntVar(value=16)
        weight_precision_spin = ttk.Spinbox(self.traditional_precision_frame, from_=0, to=16, 
                                           textvariable=self.weight_precision_var, width=5)
        weight_precision_spin.pack(side="left")
        
        # Method info label
        self.method_info_label = ttk.Label(config_frame, text="Traditional: Uses scipy optimization (faster)", 
                                          font=("Arial", 9, "italic"), foreground="blue")
        self.method_info_label.pack(fill="x", pady=2)
        
        # Date range frame
        date_frame = ttk.LabelFrame(self.root, text="Date Range (Optional)", padding=10)
        date_frame.pack(fill="x", padx=10, pady=5)
        
        # Enable date range checkbox
        self.use_date_range_var = tk.BooleanVar(value=False)
        date_check = ttk.Checkbutton(date_frame, text="Use custom date range:", 
                                    variable=self.use_date_range_var, 
                                    command=self._toggle_date_range)
        date_check.pack(anchor="w", pady=2)
        
        # Date selection frame
        self.date_selection_frame = ttk.Frame(date_frame)
        self.date_selection_frame.pack(fill="x", pady=5)
        
        # Start date
        start_frame = ttk.Frame(self.date_selection_frame)
        start_frame.pack(side="left", padx=10)
        ttk.Label(start_frame, text="Start Date:").pack()
        self.start_date_var = tk.StringVar(value="2021-04-15")
        self.start_date_entry = ttk.Entry(start_frame, textvariable=self.start_date_var, width=12)
        self.start_date_entry.pack()
        
        # End date
        end_frame = ttk.Frame(self.date_selection_frame)
        end_frame.pack(side="left", padx=10)
        ttk.Label(end_frame, text="End Date:").pack()
        self.end_date_var = tk.StringVar(value="2023-12-31")
        self.end_date_entry = ttk.Entry(end_frame, textvariable=self.end_date_var, width=12)
        self.end_date_entry.pack()
        
        # Initially disable date range controls
        self._toggle_date_range()
        
        # Buttons frame
        button_frame = ttk.Frame(self.root)
        button_frame.pack(fill="x", padx=10, pady=10)
        
        ttk.Button(button_frame, text="Clear Assets", 
                  command=self._clear_assets).pack(side="left", padx=5)
        
        ttk.Button(button_frame, text="Validate Data", 
                  command=self._validate_data).pack(side="left", padx=5)
        
        ttk.Button(button_frame, text="Run Simple Backtest", 
                  command=self._run_simple_backtest).pack(side="left", padx=5)
        
        # Results frame
        results_frame = ttk.LabelFrame(self.root, text="Results", padding=10)
        results_frame.pack(fill="both", expand=True, padx=10, pady=5)
        
        self.results_text = scrolledtext.ScrolledText(results_frame, height=15)
        self.results_text.pack(fill="both", expand=True)
        
    def _select_oc_file(self):
        """Select OC analysis file for an asset."""
        asset_code = self.asset_entry.get().strip().upper()
        
        if not asset_code:
            messagebox.showerror("Error", "Please enter an asset code")
            return
        
        # File dialog - look for prediction files
        file_path = filedialog.askopenfilename(
            title=f"Select Prediction file for {asset_code} (with ARIMA predictions)",
            filetypes=[
                ("Excel prediction files", "*_with_predictions*.xlsx"),
                ("Excel files", "*.xlsx"),
                ("CSV files", "*.csv"),
                ("All files", "*.*")
            ]
        )
        
        if file_path:
            self.selected_assets[asset_code] = file_path
            self._update_assets_display()
            self.asset_entry.delete(0, tk.END)
    
    def _update_assets_display(self):
        """Update the assets display."""
        self.assets_text.delete(1.0, tk.END)
        
        if not self.selected_assets:
            self.assets_text.insert(tk.END, "No assets selected")
            return
        
        self.assets_text.insert(tk.END, "Selected Assets:\n")
        for asset, file_path in self.selected_assets.items():
            self.assets_text.insert(tk.END, f"  {asset}: {file_path}\n")
    
    def _auto_detect_assets(self):
        """Auto-detect assets from OC analysis files in a directory."""
        directory = filedialog.askdirectory(
            title="Select directory containing prediction files (with ARIMA predictions)"
        )
        
        if not directory:
            return
        
        self._log(f"Auto-detecting assets in: {directory}")
        print(f"CONSOLE: Auto-detecting assets in: {directory}")
        
        try:
            detected_assets = {}
            directory_path = Path(directory)
            
            # Look for prediction files with ARIMA predictions
            patterns = [
                "*_with_predictions_full.xlsx",
                "*_with_predictions.xlsx",
                "*_prediction*.xlsx",
                "*_with_predictions_full.csv",
                "*_with_predictions.csv",
                "*_prediction*.csv"
            ]
            
            for pattern in patterns:
                for file_path in directory_path.glob(pattern):
                    # Extract asset code from filename
                    filename = file_path.stem
                    
                    # Try different extraction methods for prediction files
                    if "_with_predictions" in filename.lower():
                        asset_code = filename.split("_with_predictions")[0].upper()
                    elif "_prediction" in filename.lower():
                        asset_code = filename.split("_prediction")[0].upper()
                    else:
                        continue
                    
                    if asset_code and asset_code not in detected_assets:
                        detected_assets[asset_code] = str(file_path)
                        self._log(f"  Detected: {asset_code} -> {file_path.name}")
                        print(f"CONSOLE: Detected {asset_code}: {file_path.name}")
            
            if detected_assets:
                # Add detected assets to selection
                self.selected_assets.update(detected_assets)
                self._update_assets_display()
                
                success_msg = f"Auto-detected {len(detected_assets)} assets: {list(detected_assets.keys())}"
                self._log(success_msg)
                print(f"CONSOLE: {success_msg}")
                messagebox.showinfo("Auto-Detection Results", success_msg)
            else:
                error_msg = "No prediction files found in the selected directory. Looking for *_with_predictions*.xlsx files."
                self._log(error_msg)
                print(f"CONSOLE: {error_msg}")
                messagebox.showwarning("Auto-Detection", error_msg)
                
        except Exception as e:
            error_msg = f"Auto-detection failed: {str(e)}"
            self._log(error_msg)
            print(f"CONSOLE ERROR: {error_msg}")
            messagebox.showerror("Auto-Detection Error", error_msg)

    def _clear_assets(self):
        """Clear all selected assets."""
        self.selected_assets.clear()
        self._update_assets_display()
        self._log("Assets cleared")
        print("CONSOLE: Assets cleared")
    
    def _on_optimization_method_change(self, event=None):
        """Handle optimization method selection change."""
        method = self.optim_var.get()
        
        if method == "smart_grid" or method == "compare":
            # Show precision controls for Smart Grid
            self.precision_frame.pack(fill="x", pady=2)
            # Hide traditional precision controls
            self.traditional_precision_frame.pack_forget()
        else:
            # Hide precision controls for traditional
            self.precision_frame.pack_forget()
            # Show traditional precision controls
            self.traditional_precision_frame.pack(fill="x", pady=2)
        
        # Update info label
        if method == "traditional":
            info_text = "Traditional: Uses scipy optimization (faster) + optional weight precision override"
            color = "blue"
        elif method == "smart_grid":
            info_text = "Smart Grid: Exhaustive search with global optimum guarantee (slower)"
            color = "green"
        elif method == "compare":
            info_text = "Compare: Runs both methods and shows performance comparison"
            color = "purple"
        else:
            info_text = "Select optimization method"
            color = "black"
        
        self.method_info_label.config(text=info_text, foreground=color)
    
    def _toggle_date_range(self):
        """Toggle date range selection controls."""
        enabled = self.use_date_range_var.get()
        
        # Enable/disable date entry widgets
        state = "normal" if enabled else "disabled"
        self.start_date_entry.config(state=state)
        self.end_date_entry.config(state=state)
        
        if enabled:
            self._log("Custom date range enabled")
            print("CONSOLE: Custom date range enabled")
        else:
            self._log("Using full date range")
            print("CONSOLE: Using full date range")
    
    def _validate_data(self):
        """Validate selected data files."""
        if not self.selected_assets:
            messagebox.showerror("Error", "No assets selected")
            return
        
        self._log("Validating data files...")
        print("CONSOLE: Starting data validation...")
        
        try:
            # Test loading each file
            validation_results = []
            
            for asset, file_path in self.selected_assets.items():
                self._log(f"Validating {asset}: {file_path}")
                print(f"CONSOLE: Validating {asset}: {Path(file_path).name}")
                
                # Try to load file
                if file_path.endswith('.xlsx'):
                    df = pd.read_excel(file_path)
                else:
                    df = pd.read_csv(file_path)
                
                # Check STRICT required columns - Close_Price is now mandatory (check variations)
                print(f"CONSOLE DEBUG: {asset} columns: {list(df.columns)}")
                
                # Check for Close price column variations (including asset-specific names)
                close_price_variations = [
                    f'Close_Price_{asset}',  # Asset-specific: Close_Price_BTC, Close_Price_ETH
                    'Close_Price', 'Close', 'close_price', 'close', 'CLOSE_PRICE', 'CLOSE',
                    'Close_price', 'close_Price', 'ClosePrice', 'closePrice', 'ClosingPrice', 
                    'closing_price', 'Closing_Price', 'END_PRICE', 'End_Price', 'end_price'
                ]
                close_col_found = None
                for variation in close_price_variations:
                    if variation in df.columns:
                        close_col_found = variation
                        break
                
                # For prediction files, check required columns directly
                required_cols = ['Timestamp', 'Open_Price', f'Prd_Return_Arima_{asset}']
                missing_cols = [col for col in required_cols if col not in df.columns]
                
                # Check for prediction column
                pred_col_found = f'Prd_Return_Arima_{asset}' if f'Prd_Return_Arima_{asset}' in df.columns else None
                
                # Add close price check
                if close_col_found is None:
                    missing_cols.append(f"Close_Price (checked: {close_price_variations})")
                else:
                    print(f"CONSOLE DEBUG: {asset} found close price column: {close_col_found}")
                
                if missing_cols:
                    error_msg = f"STRICT MODE ERROR: Missing required columns for {asset}: {missing_cols}"
                    self._log(f"  {error_msg}")
                    print(f"CONSOLE ERROR: {error_msg}")
                    raise ValueError(f"Data validation failed - {error_msg}. All assets must have required columns.")
                else:
                    # Check predictions using the found prediction column
                    pred_count = df[pred_col_found].notna().sum()
                    total_rows = len(df)
                    
                    result_msg = f"{asset}: {total_rows} rows, {pred_count} predictions, returns: (Close-Open)/Open"
                    self._log(f"  {result_msg}")
                    print(f"CONSOLE: {result_msg}")
                    validation_results.append(f"{asset}: {pred_count}/{total_rows} predictions, intraday returns")
            
            self._log("Validation complete!")
            print("CONSOLE: ✓ Validation complete!")
            print("CONSOLE VALIDATION RESULTS:")
            for result in validation_results:
                print(f"  {result}")
            
            messagebox.showinfo("Validation Results", "\n".join(validation_results))
            
        except Exception as e:
            error_msg = f"Validation failed: {str(e)}"
            self._log(error_msg)
            print(f"CONSOLE ERROR - VALIDATION FAILED: {error_msg}")
            messagebox.showerror("Validation Error", error_msg)
    
    def _run_simple_backtest(self):
        """Run the simple dynamic backtest."""
        if not self.selected_assets:
            messagebox.showerror("Error", "No assets selected")
            return
        
        # Update config
        self.backtest_config.update({
            'risk_profile': self.risk_var.get(),
            'rebalance_frequency': self.rebal_var.get(),
            'lookback_period': self.lookback_var.get(),
            'optimization_method': self.optim_var.get(),
            'smart_grid_precision': self.precision_var.get(),
            'use_custom_precision': self.use_custom_precision_var.get(),
            'weight_precision': self.weight_precision_var.get()
        })
        
        self._log("Starting simple dynamic backtest...")
        self._log(f"Config: {self.backtest_config}")
        
        # Log optimization method selection
        method = self.backtest_config['optimization_method']
        if method == 'traditional':
            use_custom = self.backtest_config['use_custom_precision']
            if use_custom:
                precision = self.backtest_config['weight_precision']
                self._log(f"Using Traditional Optimization (scipy.optimize) with weight precision override: {precision} decimals")
            else:
                self._log("Using Traditional Optimization (scipy.optimize) with config default precision")
        elif method == 'smart_grid':
            precision = self.backtest_config['smart_grid_precision']
            self._log(f"Using Smart Grid Search (precision: {precision} decimals)")
        elif method == 'compare':
            self._log("Using Comparison Mode (Traditional vs Smart Grid)")
        
        # Run in separate thread
        thread = threading.Thread(target=self._run_backtest_thread)
        thread.daemon = True
        thread.start()
    
    def _run_backtest_thread(self):
        """Run backtest in separate thread."""
        try:
            # Load and combine data
            self._log("Loading and combining data...")
            print("CONSOLE: Starting backtest - loading data...")
            
            try:
                combined_data = self._load_dynamic_data()
                print(f"CONSOLE DEBUG: Combined data shape: {combined_data.shape}")
                print(f"CONSOLE DEBUG: Combined data columns: {list(combined_data.columns)}")
            except KeyError as e:
                print(f"CONSOLE ERROR: KeyError in _load_dynamic_data: {str(e)}")
                print(f"CONSOLE ERROR: KeyError details: {type(e).__name__}: {e.args}")
                import traceback
                print(f"CONSOLE TRACEBACK:\n{traceback.format_exc()}")
                raise
            
            # Find first prediction and decision start
            asset_codes = list(self.selected_assets.keys())
            first_pred_index = self._find_first_prediction_index(combined_data, asset_codes)
            decision_start_index = first_pred_index + self.backtest_config['lookback_period']
            
            first_pred_date = combined_data.loc[first_pred_index, 'Timestamp']
            decision_start_date = combined_data.loc[decision_start_index, 'Timestamp']
            
            self._log(f"First prediction: {first_pred_date.strftime('%Y-%m-%d')} (index {first_pred_index})")
            self._log(f"Decision starts: {decision_start_date.strftime('%Y-%m-%d')} (index {decision_start_index})")
            
            # Print key info to console
            print(f"CONSOLE: First prediction: {first_pred_date.strftime('%Y-%m-%d')} (index {first_pred_index})")
            print(f"CONSOLE: Decision starts: {decision_start_date.strftime('%Y-%m-%d')} (index {decision_start_index})")
            print(f"CONSOLE: Assets: {asset_codes}")
            
            # Use the SimpleDynamicBacktester class
            self._log("Initializing SimpleDynamicBacktester...")
            print("CONSOLE: Running SimpleDynamicBacktester in STRICT MODE...")
            results = self._run_with_simple_dynamic_backtester(combined_data, asset_codes)
            
            # Export results
            output_path = self._export_results(results, asset_codes)
            
            self._log(f"Backtest completed successfully!")
            self._log(f"Results saved to: {output_path}")
            
            # Print success to console
            print(f"CONSOLE: ✓ Backtest completed successfully!")
            print(f"CONSOLE: ✓ Results saved to: {output_path}")
            
            # Show summary
            summary = self._calculate_summary(results, decision_start_index)
            self._log("\\nSUMMARY:")
            print("CONSOLE SUMMARY:")
            for key, value in summary.items():
                self._log(f"  {key}: {value}")
                print(f"  {key}: {value}")
            
        except Exception as e:
            error_msg = f"Backtest failed: {str(e)}"
            self._log(error_msg)
            
            # Print to console for easy copying
            print(f"\nCONSOLE ERROR - BACKTEST FAILED:")
            print(f"Error: {error_msg}")
            print(f"Exception Type: {type(e).__name__}")
            print(f"Full Details: {str(e)}")
            print("-" * 50)
            
            self.root.after(0, lambda: messagebox.showerror("Backtest Error", error_msg))
    
    def _load_dynamic_data(self):
        """Load and combine data from selected files."""
        combined_data = None
        
        # Get date range if enabled
        date_filter = None
        if self.use_date_range_var.get():
            try:
                start_date = pd.to_datetime(self.start_date_var.get())
                end_date = pd.to_datetime(self.end_date_var.get())
                date_filter = (start_date, end_date)
                self._log(f"Using date range: {start_date.strftime('%Y-%m-%d')} to {end_date.strftime('%Y-%m-%d')}")
                print(f"CONSOLE: Date range: {start_date.strftime('%Y-%m-%d')} to {end_date.strftime('%Y-%m-%d')}")
            except Exception as e:
                error_msg = f"Invalid date format: {str(e)}"
                self._log(error_msg)
                print(f"CONSOLE ERROR: {error_msg}")
                raise ValueError(error_msg)
        
        for asset_code, file_path in self.selected_assets.items():
            self._log(f"Loading {asset_code} from {Path(file_path).name}")
            
            # Load file
            if file_path.endswith('.xlsx'):
                df = pd.read_excel(file_path)
            else:
                df = pd.read_csv(file_path)
            
            # Simple processing for prediction files - they already have the correct format
            df['Timestamp'] = pd.to_datetime(df['Timestamp'])
            
            # Prediction files already have asset-specific columns, just select what we need
            essential_cols = ['Timestamp', 'Open_Price', f'Close_Price_{asset_code}', f'Prd_Return_Arima_{asset_code}']
            
            # Verify all required columns exist
            missing_cols = [col for col in essential_cols if col not in df.columns]
            if missing_cols:
                error_msg = f"PREDICTION FILE ERROR: {asset_code} missing required columns: {missing_cols}"
                self._log(error_msg)
                print(f"CONSOLE ERROR: {error_msg}")
                print(f"CONSOLE DEBUG: Available columns: {list(df.columns)}")
                raise ValueError(error_msg)
            
            self._log(f"  {asset_code}: Using prediction file with all required columns")
            print(f"CONSOLE: {asset_code} - Using prediction file format")
            
            # Create asset-specific Open_Price column for consistency with backtester
            df[f'Open_Price_{asset_code}'] = df['Open_Price']
            essential_cols = ['Timestamp', f'Open_Price_{asset_code}', f'Close_Price_{asset_code}', f'Prd_Return_Arima_{asset_code}']
            df = df[essential_cols]
            
            # Merge with combined data using INNER join (corrected)
            if combined_data is None:
                combined_data = df
            else:
                combined_data = pd.merge(combined_data, df, on='Timestamp', how='inner')
        
        # Sort by timestamp
        combined_data = combined_data.sort_values('Timestamp').reset_index(drop=True)
        
        # Calculate returns for all assets using STRICT formula: (Close-Open)/Open
        for asset_code in self.selected_assets.keys():
            open_col = f'Open_Price_{asset_code}'
            close_col = f'Close_Price_{asset_code}'
            return_col = f'{asset_code}_Return'
            
            # STRICT MODE: No fallbacks - Close_Price must exist
            if close_col not in combined_data.columns:
                error_msg = f"STRICT MODE ERROR: {asset_code} missing {close_col} in combined data. Cannot calculate returns."
                self._log(error_msg)
                print(f"CONSOLE ERROR: {error_msg}")
                raise ValueError(error_msg)
            
            # STRICT: Only use correct intraday return formula
            combined_data[return_col] = (combined_data[close_col] - combined_data[open_col]) / combined_data[open_col]
            self._log(f"  {asset_code}: Calculated intraday returns (Close-Open)/Open")
            print(f"CONSOLE: {asset_code} - Calculated (Close-Open)/Open returns")
        
        # Remove rows with NaN returns
        return_cols = [f'{asset}_Return' for asset in self.selected_assets.keys()]
        combined_data = combined_data.dropna(subset=return_cols).reset_index(drop=True)
        
        # Apply date filter if specified
        if date_filter:
            start_date, end_date = date_filter
            date_mask = (combined_data['Timestamp'] >= start_date) & (combined_data['Timestamp'] <= end_date)
            filtered_data = combined_data[date_mask].reset_index(drop=True)
            
            self._log(f"Data filtered from {len(combined_data)} to {len(filtered_data)} rows")
            print(f"CONSOLE: Date filter applied: {len(combined_data)} -> {len(filtered_data)} rows")
            
            if len(filtered_data) == 0:
                raise ValueError(f"No data found in date range {start_date.strftime('%Y-%m-%d')} to {end_date.strftime('%Y-%m-%d')}")
            
            combined_data = filtered_data
        
        self._log(f"Final combined data: {len(combined_data)} rows")
        print(f"CONSOLE: Final data: {len(combined_data)} rows")
        return combined_data
    
    def _find_first_prediction_index(self, data, asset_codes):
        """Find first prediction index across all assets."""
        first_pred_index = None
        
        for asset in asset_codes:
            pred_col = f'Prd_Return_Arima_{asset}'
            valid_mask = data[pred_col].notna()
            if valid_mask.any():
                asset_first_index = valid_mask.idxmax()
                if first_pred_index is None or asset_first_index > first_pred_index:
                    first_pred_index = asset_first_index
        
        return first_pred_index
    
    def _run_with_simple_dynamic_backtester(self, data, asset_codes):
        """Run backtest using the SimpleDynamicBacktester class."""
        try:
            from crypto_trading_model.backtest.simple_dynamic_backtester import SimpleDynamicBacktester
            from crypto_trading_model.config.config_manager import ConfigManager
            from crypto_trading_model.data.data_processor import DataProcessor
            from crypto_trading_model.optimization.optimizer import Optimizer
            from crypto_trading_model.reporting.exporter import Exporter
            from crypto_trading_model.reporting.metrics import MetricsCalculator
            from crypto_trading_model.custom_logging.logger import CustomLogger
            
            # Create required components (minimal setup)
            logger = CustomLogger('SimpleDynamicBacktester')
            
            # Create minimal data processor (not used but required for inheritance)
            data_processor = None
            
            # Create minimal optimizer (will be overridden by SimpleDynamicBacktester)
            sample_returns = data[[f'{asset}_Return' for asset in asset_codes]].dropna()
            sample_mean_returns = sample_returns.mean().values  # Just for initialization, will use predicted returns
            cov_matrix = sample_returns.cov().values

            # Risk free rate hard code
            rf_rate = 0.0001075
            rf_rate = ((1 + 0.04) ** (1 / 365)) - 1
            
            try:
                config_manager = ConfigManager(logger)
            except:
                # Create minimal config manager if ConfigManager fails
                print("CONSOLE: ConfigManager failed, creating minimal config...")
                config_manager = self._create_minimal_config_manager(logger)
            
            optimizer = Optimizer(logger, sample_mean_returns, cov_matrix, rf_rate, config_manager=config_manager)
            exporter = Exporter(logger)
            metrics_calculator = MetricsCalculator(logger, rf_rate)
            
            # Create SimpleDynamicBacktester
            backtester = SimpleDynamicBacktester(
                logger=logger,
                data_processor=data_processor,
                optimizer=optimizer,
                exporter=exporter,
                metrics_calculator=metrics_calculator,
                config_manager=config_manager,
                asset_codes=asset_codes
            )
            
            # Set optimization method on backtester
            backtester.optimization_method = self.backtest_config['optimization_method']
            backtester.smart_grid_precision = self.backtest_config['smart_grid_precision']
            
            # Set weight precision override if enabled
            if self.backtest_config['use_custom_precision']:
                backtester.weight_precision = self.backtest_config['weight_precision']
            else:
                backtester.weight_precision = None  # Use config default
            
            # Run the backtest
            results = backtester.backtest_dynamic(
                data=data,
                risk_profile=self.backtest_config['risk_profile'],
                rebalance_frequency=self.backtest_config['rebalance_frequency'],
                lookback_period=self.backtest_config['lookback_period'],
                export_excel=False,
                excel_path=""
            )
            
            return results
            
        except ImportError as e:
            error_msg = f"CRITICAL ERROR: Could not import SimpleDynamicBacktester: {str(e)}. Please ensure all dependencies are installed."
            self._log(error_msg)
            print(f"CONSOLE ERROR: {error_msg}")  # Print to console for easy copying
            raise ImportError(error_msg)
        except Exception as e:
            # Check if it's a ConfigManager issue vs ARIMA prediction issue
            if "ConfigManager" in str(e):
                error_msg = f"CONFIGURATION ERROR: SimpleDynamicBacktester failed: {str(e)}. This indicates a configuration setup issue, not ARIMA predictions."
            else:
                error_msg = f"STRICT MODE FAILURE: SimpleDynamicBacktester failed: {str(e)}. This indicates missing ARIMA predictions or model validation issues."
            
            self._log(error_msg)
            print(f"CONSOLE ERROR: {error_msg}")  # Print to console for easy copying
            print(f"FULL EXCEPTION: {str(e)}")    # Print full exception details
            raise Exception(error_msg)
    
    
    
    def _export_results(self, results, asset_codes):
        """Export results to Excel file with enhanced volatility and covariance data."""
        output_dir = Path("simple_backtest_results")
        output_dir.mkdir(exist_ok=True)
        
        asset_str = "_".join(asset_codes)
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        output_path = output_dir / f"simple_backtest_{asset_str}_{timestamp}.xlsx"
        
        # Create Excel writer with multiple sheets
        with pd.ExcelWriter(output_path, engine='openpyxl') as writer:
            # Main results sheet
            results.to_excel(writer, sheet_name='Results', index=False)
            
            # Create covariance matrix summary sheet
            cov_cols = [col for col in results.columns if col.startswith('Cov_')]
            if cov_cols:
                # Get the last available covariance matrix
                last_valid_idx = results[cov_cols].dropna().index[-1] if len(results[cov_cols].dropna()) > 0 else None
                
                if last_valid_idx is not None:
                    # Extract covariance matrix
                    cov_matrix_data = []
                    for i, asset_i in enumerate(asset_codes):
                        row = []
                        for j, asset_j in enumerate(asset_codes):
                            cov_col = f'Cov_{asset_i}_{asset_j}'
                            value = results.loc[last_valid_idx, cov_col] if cov_col in results.columns else 0.0
                            row.append(value)
                        cov_matrix_data.append(row)
                    
                    # Create covariance matrix DataFrame
                    cov_df = pd.DataFrame(cov_matrix_data, index=asset_codes, columns=asset_codes)
                    cov_df.to_excel(writer, sheet_name='Covariance_Matrix', index=True)
            
            # Create volatility summary sheet
            vol_cols = ['Volatility_Long', 'Volatility_Short', 'Volatility_Market_Neutral', 'Portfolio_Volatility']
            vol_data = results[vol_cols].dropna()
            if len(vol_data) > 0:
                vol_summary = pd.DataFrame({
                    'Strategy': ['Long', 'Short', 'Market_Neutral', 'Portfolio'],
                    'Average_Volatility': [
                        vol_data['Volatility_Long'].mean(),
                        vol_data['Volatility_Short'].mean(), 
                        vol_data['Volatility_Market_Neutral'].mean(),
                        vol_data['Portfolio_Volatility'].mean()
                    ],
                    'Min_Volatility': [
                        vol_data['Volatility_Long'].min(),
                        vol_data['Volatility_Short'].min(),
                        vol_data['Volatility_Market_Neutral'].min(),
                        vol_data['Portfolio_Volatility'].min()
                    ],
                    'Max_Volatility': [
                        vol_data['Volatility_Long'].max(),
                        vol_data['Volatility_Short'].max(),
                        vol_data['Volatility_Market_Neutral'].max(),
                        vol_data['Portfolio_Volatility'].max()
                    ],
                    'Final_Volatility': [
                        vol_data['Volatility_Long'].iloc[-1],
                        vol_data['Volatility_Short'].iloc[-1],
                        vol_data['Volatility_Market_Neutral'].iloc[-1],
                        vol_data['Portfolio_Volatility'].iloc[-1]
                    ]
                })
                vol_summary.to_excel(writer, sheet_name='Volatility_Summary', index=False)
        
        return output_path
    
    def _calculate_summary(self, results, start_index):
        """Calculate summary statistics."""
        decision_data = results.iloc[start_index:]
        
        if len(decision_data) == 0:
            return {"Error": "No decision data available"}
        
        final_value = decision_data['Portfolio_Value'].iloc[-1]
        total_return = final_value - 1.0
        prediction_usage = decision_data['Used_Predicted_Returns'].sum()
        
        # Calculate volatility metrics
        portfolio_vol_data = decision_data['Portfolio_Volatility'].dropna()
        avg_portfolio_vol = portfolio_vol_data.mean() if len(portfolio_vol_data) > 0 else 0.0
        
        # Get final strategy volatilities
        final_long_vol = decision_data['Volatility_Long'].iloc[-1] if 'Volatility_Long' in decision_data.columns else "N/A"
        final_short_vol = decision_data['Volatility_Short'].iloc[-1] if 'Volatility_Short' in decision_data.columns else "N/A"
        final_mn_vol = decision_data['Volatility_Market_Neutral'].iloc[-1] if 'Volatility_Market_Neutral' in decision_data.columns else "N/A"
        
        # Calculate annualized metrics (assuming daily data)
        days_in_year = 252
        annualized_return = (final_value ** (days_in_year / len(decision_data)) - 1) * 100 if len(decision_data) > 0 else 0.0
        annualized_volatility = avg_portfolio_vol * np.sqrt(days_in_year) * 100 if avg_portfolio_vol > 0 else 0.0
        sharpe_ratio = annualized_return / annualized_volatility if annualized_volatility > 0 else 0.0
        
        summary = {
            "Total Days": len(results),
            "Decision Days": len(decision_data),
            "Final Portfolio Value": f"{final_value:.4f}",
            "Total Return": f"{total_return*100:.2f}%",
            "Annualized Return": f"{annualized_return:.2f}%",
            "Annualized Volatility": f"{annualized_volatility:.2f}%",
            "Sharpe Ratio": f"{sharpe_ratio:.4f}",
            "Avg Portfolio Volatility": f"{avg_portfolio_vol:.6f}",
            "Final Long Volatility": f"{final_long_vol:.6f}" if isinstance(final_long_vol, float) else final_long_vol,
            "Final Short Volatility": f"{final_short_vol:.6f}" if isinstance(final_short_vol, float) else final_short_vol,
            "Final Market Neutral Vol": f"{final_mn_vol:.6f}" if isinstance(final_mn_vol, float) else final_mn_vol,
            "Prediction Usage": f"{prediction_usage}/{len(decision_data)} days"
        }
        
        return summary
    
    def _log(self, message):
        """Log message to results text area."""
        timestamp = datetime.now().strftime("%H:%M:%S")
        log_message = f"[{timestamp}] {message}\\n"
        
        # Update UI from main thread
        self.root.after(0, lambda: self._append_to_results(log_message))
    
    def _create_minimal_config_manager(self, logger):
        """Create a minimal config manager when ConfigManager fails."""
        
        class MinimalConfigManager:
            """Minimal config manager for SimpleDynamicBacktester."""
            
            def __init__(self, logger):
                self.logger = logger
                self.logger.info("Using minimal config manager")
                
                # Create minimal config structure
                self._config = self._create_minimal_config()
            
            def _create_minimal_config(self):
                """Create minimal config structure."""
                class MinimalStrategy:
                    def get_strategy_bounds(self, strategy_type):
                        bounds_map = {
                            'long': (-1, 2),
                            'short': (-2, 1), 
                            'market_neutral': (-1, 1),
                            'gmvp': (-0.5, 2),
                            'max_return': (-2, 2),
                            'sharpe_max': (-2, 2)
                        }
                        return bounds_map.get(strategy_type, (-1, 1))
                
                class MinimalConfig:
                    def __init__(self):
                        self.strategy = MinimalStrategy()
                
                return MinimalConfig()
            
            def get_config(self):
                """Return minimal config."""
                return self._config
            
            def get_strategy_bounds(self):
                """Return default strategy bounds."""
                return {
                    'long': (-1, 2),
                    'short': (-2, 1), 
                    'market_neutral': (-1, 1),
                    'gmvp': (-0.5, 2),
                    'max_return': (-2, 2),
                    'sharpe_max': (-2, 2)
                }
            
            def get_risk_free_rate(self):
                """Return default risk-free rate."""
                return 0.0001075
            
            def get_optimization_settings(self):
                """Return default optimization settings."""
                return {
                    'max_iterations': 1000,
                    'tolerance': 1e-6
                }
        
        return MinimalConfigManager(logger)

    def _append_to_results(self, message):
        """Append message to results text area."""
        self.results_text.insert(tk.END, message)
        self.results_text.see(tk.END)
        self.root.update_idletasks()

def main():
    """Main function to run the GUI."""
    root = tk.Tk()
    app = SimpleDynamicPortfolioGUI(root)
    root.mainloop()

if __name__ == "__main__":
    main()