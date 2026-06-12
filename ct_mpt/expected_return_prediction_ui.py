#!/usr/bin/env python3
"""
Expected Return Prediction GUI
==============================

Simple tkinter-based GUI for running expected return prediction.
Allows users to select asset type and provide file directories for OC analysis results.
"""

import sys
import os
import tkinter as tk
from tkinter import ttk, filedialog, messagebox, scrolledtext
from pathlib import Path
import threading
from typing import Optional
import logging
from datetime import datetime, date, timedelta

# Add src directory to Python path
src_path = Path(__file__).parent / "src"
sys.path.insert(0, str(src_path))

# Import will be done dynamically when needed
ExpectedReturnPredictor = None
PredictionConfig = None

class ExpectedReturnGUI:
    """Simple GUI for Expected Return Prediction."""
    
    def __init__(self, root):
        """Initialize the GUI."""
        self.root = root
        self.root.title("Expected Return Prediction Tool")
        self.root.geometry("800x600")
        
        # Initialize variables
        self.oc_file_path = tk.StringVar()
        self.arima_file_path = tk.StringVar()
        self.output_file_path = tk.StringVar()
        self.asset_code = tk.StringVar(value="BTC")
        self.date_range_mode = tk.BooleanVar(value=False)
        self.start_date = tk.StringVar(value=(datetime.now() - timedelta(days=30)).strftime("%Y-%m-%d"))
        self.end_date = tk.StringVar(value=datetime.now().strftime("%Y-%m-%d"))
        
        # Create GUI components
        self._create_widgets()
        
        # Setup logging to redirect to GUI
        self._setup_logging()
        
    def _create_widgets(self):
        """Create and layout GUI widgets."""
        # Main frame
        main_frame = ttk.Frame(self.root, padding="10")
        main_frame.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        
        # Configure grid weights
        self.root.columnconfigure(0, weight=1)
        self.root.rowconfigure(0, weight=1)
        main_frame.columnconfigure(1, weight=1)
        
        # Title
        title_label = ttk.Label(main_frame, text="Expected Return Prediction Tool", 
                               font=("Arial", 16, "bold"))
        title_label.grid(row=0, column=0, columnspan=3, pady=(0, 20))
        
        # Asset selection
        ttk.Label(main_frame, text="Asset Code:").grid(row=1, column=0, sticky=tk.W, pady=5)
        asset_combo = ttk.Combobox(main_frame, textvariable=self.asset_code, 
                                  values=["BTC", "ETH", "LTC", "XRP", "ADA", "DOT", "SOL"],
                                  width=10)
        asset_combo.grid(row=1, column=1, sticky=(tk.W, tk.E), pady=5, padx=(5, 0))
        
        # OC Analysis file selection
        ttk.Label(main_frame, text="OC Analysis File:").grid(row=2, column=0, sticky=tk.W, pady=5)
        oc_entry = ttk.Entry(main_frame, textvariable=self.oc_file_path, width=50)
        oc_entry.grid(row=2, column=1, sticky=(tk.W, tk.E), pady=5, padx=(5, 0))
        oc_browse_btn = ttk.Button(main_frame, text="Browse", 
                                  command=lambda: self._browse_file(self.oc_file_path, "Excel files", "*.xlsx"))
        oc_browse_btn.grid(row=2, column=2, pady=5, padx=(5, 0))
        
        # ARIMA file selection
        ttk.Label(main_frame, text="ARIMA Results File:").grid(row=3, column=0, sticky=tk.W, pady=5)
        arima_entry = ttk.Entry(main_frame, textvariable=self.arima_file_path, width=50)
        arima_entry.grid(row=3, column=1, sticky=(tk.W, tk.E), pady=5, padx=(5, 0))
        arima_browse_btn = ttk.Button(main_frame, text="Browse", 
                                     command=lambda: self._browse_file(self.arima_file_path, "JSON files", "*.json"))
        arima_browse_btn.grid(row=3, column=2, pady=5, padx=(5, 0))
        
        # Output file selection (optional)
        ttk.Label(main_frame, text="Prediction Output File (optional):").grid(row=4, column=0, sticky=tk.W, pady=5)
        output_entry = ttk.Entry(main_frame, textvariable=self.output_file_path, width=50)
        output_entry.grid(row=4, column=1, sticky=(tk.W, tk.E), pady=5, padx=(5, 0))
        output_browse_btn = ttk.Button(main_frame, text="Browse", 
                                      command=lambda: self._browse_save_file(self.output_file_path, "Excel files", "*.xlsx"))
        output_browse_btn.grid(row=4, column=2, pady=5, padx=(5, 0))
        
        # Output file helper text
        output_help = ttk.Label(main_frame, text="(Leave empty to auto-generate: {ASSET}_with_predictions.xlsx)", 
                               font=("Arial", 8), foreground="gray")
        output_help.grid(row=4, column=1, columnspan=2, sticky=tk.W, pady=(25, 5), padx=(5, 0))
        
        # Separator for prediction mode section
        separator = ttk.Separator(main_frame, orient='horizontal')
        separator.grid(row=5, column=0, columnspan=3, sticky=(tk.W, tk.E), pady=10)
        
        # Date range prediction mode
        ttk.Label(main_frame, text="Prediction Mode:", font=("Arial", 10, "bold")).grid(row=6, column=0, sticky=tk.W, pady=(5, 0))
        
        # Date range checkbox
        date_range_check = ttk.Checkbutton(main_frame, text="Predict for specific date range", 
                                          variable=self.date_range_mode,
                                          command=self._toggle_date_selection)
        date_range_check.grid(row=7, column=0, columnspan=2, sticky=tk.W, pady=5)
        
        # Start date selection
        ttk.Label(main_frame, text="Start Date (YYYY-MM-DD):").grid(row=8, column=0, sticky=tk.W, pady=5)
        self.start_date_entry = ttk.Entry(main_frame, textvariable=self.start_date, width=15, state="disabled")
        self.start_date_entry.grid(row=8, column=1, sticky=tk.W, pady=5, padx=(5, 0))
        
        # End date selection
        ttk.Label(main_frame, text="End Date (YYYY-MM-DD):").grid(row=9, column=0, sticky=tk.W, pady=5)
        self.end_date_entry = ttk.Entry(main_frame, textvariable=self.end_date, width=15, state="disabled")
        self.end_date_entry.grid(row=9, column=1, sticky=tk.W, pady=5, padx=(5, 0))
        
        # Helper text for date format
        date_help = ttk.Label(main_frame, text="(Format: 2024-01-15 for January 15, 2024)", 
                             font=("Arial", 8), foreground="gray")
        date_help.grid(row=8, column=2, sticky=tk.W, pady=5, padx=(5, 0))
        
        # Quick date range buttons
        self.last_week_btn = ttk.Button(main_frame, text="Last Week", 
                                       command=self._set_last_week_range, state="disabled")
        self.last_week_btn.grid(row=10, column=0, sticky=tk.W, pady=5)
        
        self.last_month_btn = ttk.Button(main_frame, text="Last Month", 
                                        command=self._set_last_month_range, state="disabled")
        self.last_month_btn.grid(row=10, column=1, sticky=tk.W, pady=5, padx=(5, 0))
        
        self.last_3_months_btn = ttk.Button(main_frame, text="Last 3 Months", 
                                           command=self._set_last_3_months_range, state="disabled")
        self.last_3_months_btn.grid(row=10, column=2, sticky=tk.W, pady=5, padx=(5, 0))
        
        # Auto-fill button
        auto_fill_btn = ttk.Button(main_frame, text="Auto-fill Default Paths", 
                                  command=self._auto_fill_paths)
        auto_fill_btn.grid(row=11, column=0, columnspan=2, pady=10, sticky=tk.W)
        
        # Run prediction button
        self.run_btn = ttk.Button(main_frame, text="Run Prediction", 
                                 command=self._run_prediction, style="Accent.TButton")
        self.run_btn.grid(row=12, column=0, columnspan=3, pady=20)
        
        # Progress bar
        self.progress = ttk.Progressbar(main_frame, mode='indeterminate')
        self.progress.grid(row=13, column=0, columnspan=3, sticky=(tk.W, tk.E), pady=(0, 10))
        
        # Results display
        ttk.Label(main_frame, text="Results:").grid(row=14, column=0, sticky=tk.W)
        self.results_text = scrolledtext.ScrolledText(main_frame, height=15, width=80)
        self.results_text.grid(row=15, column=0, columnspan=3, sticky=(tk.W, tk.E, tk.N, tk.S), pady=5)
        
        # Configure grid weights for resizing
        main_frame.rowconfigure(15, weight=1)
        
    def _browse_file(self, var: tk.StringVar, file_type: str, pattern: str):
        """Browse for input file."""
        filename = filedialog.askopenfilename(
            title=f"Select {file_type}",
            filetypes=[(file_type, pattern), ("All files", "*.*")]
        )
        if filename:
            var.set(filename)
            
    def _browse_save_file(self, var: tk.StringVar, file_type: str, pattern: str):
        """Browse for output file."""
        filename = filedialog.asksaveasfilename(
            title=f"Save {file_type}",
            filetypes=[(file_type, pattern), ("All files", "*.*")],
            defaultextension=pattern[1:]  # Remove the *
        )
        if filename:
            var.set(filename)
            
    def _auto_fill_paths(self):
        """Auto-fill default file paths based on asset code."""
        asset = self.asset_code.get().strip()
        if not asset:
            messagebox.showwarning("Warning", "Please select an asset code first.")
            return
            
        # Set default paths
        base_dir = Path("oc_analysis_results")
        self.oc_file_path.set(str(base_dir / f"{asset}_oc_analysis.xlsx"))
        self.arima_file_path.set(str(base_dir / f"{asset}_arima_results.json"))
        
        # Suggest prediction output path (simple naming for backtester integration)
        if self.date_range_mode.get():
            prediction_filename = f"{asset}_with_predictions.xlsx"
        else:
            prediction_filename = f"{asset}_with_predictions_full.xlsx"
        
        self.output_file_path.set(str(base_dir / prediction_filename))
        
        self._log_message(f"Auto-filled default paths for {asset} including prediction output file")
        
    def _toggle_date_selection(self):
        """Toggle the date selection controls based on checkbox state."""
        if self.date_range_mode.get():
            self.start_date_entry.config(state="normal")
            self.end_date_entry.config(state="normal")
            self.last_week_btn.config(state="normal")
            self.last_month_btn.config(state="normal")
            self.last_3_months_btn.config(state="normal")
        else:
            self.start_date_entry.config(state="disabled")
            self.end_date_entry.config(state="disabled")
            self.last_week_btn.config(state="disabled")
            self.last_month_btn.config(state="disabled")
            self.last_3_months_btn.config(state="disabled")
            
    def _set_last_week_range(self):
        """Set date range to last week."""
        end_date = datetime.now()
        start_date = end_date - timedelta(days=7)
        self.start_date.set(start_date.strftime("%Y-%m-%d"))
        self.end_date.set(end_date.strftime("%Y-%m-%d"))
        self._log_message(f"Set date range to last week: {start_date.strftime('%Y-%m-%d')} to {end_date.strftime('%Y-%m-%d')}")
        
    def _set_last_month_range(self):
        """Set date range to last month."""
        end_date = datetime.now()
        start_date = end_date - timedelta(days=30)
        self.start_date.set(start_date.strftime("%Y-%m-%d"))
        self.end_date.set(end_date.strftime("%Y-%m-%d"))
        self._log_message(f"Set date range to last month: {start_date.strftime('%Y-%m-%d')} to {end_date.strftime('%Y-%m-%d')}")
        
    def _set_last_3_months_range(self):
        """Set date range to last 3 months."""
        end_date = datetime.now()
        start_date = end_date - timedelta(days=90)
        self.start_date.set(start_date.strftime("%Y-%m-%d"))
        self.end_date.set(end_date.strftime("%Y-%m-%d"))
        self._log_message(f"Set date range to last 3 months: {start_date.strftime('%Y-%m-%d')} to {end_date.strftime('%Y-%m-%d')}")
        
    def _validate_date_format(self, date_str: str) -> bool:
        """Validate that the date string is in YYYY-MM-DD format."""
        try:
            datetime.strptime(date_str, "%Y-%m-%d")
            return True
        except ValueError:
            return False
        
    def _setup_logging(self):
        """Setup logging to redirect to both GUI text widget and console."""
        class GUILogHandler(logging.Handler):
            def __init__(self, text_widget):
                super().__init__()
                self.text_widget = text_widget
                
            def emit(self, record):
                msg = self.format(record)
                self.text_widget.insert(tk.END, msg + "\n")
                self.text_widget.see(tk.END)
                self.text_widget.update()
        
        # Setup logging
        logger = logging.getLogger()
        logger.setLevel(logging.DEBUG)  # Set to DEBUG for more detailed logging
        
        # Remove existing handlers
        for handler in logger.handlers[:]:
            logger.removeHandler(handler)
        
        # Create formatter
        formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
        
        # Add console handler for error tracing
        console_handler = logging.StreamHandler(sys.stdout)
        console_handler.setLevel(logging.DEBUG)
        console_handler.setFormatter(formatter)
        logger.addHandler(console_handler)
        
        # Add GUI handler
        gui_handler = GUILogHandler(self.results_text)
        gui_handler.setLevel(logging.INFO)
        gui_handler.setFormatter(formatter)
        logger.addHandler(gui_handler)
        
        # Log initial message
        logger.info("Logging system initialized - output will appear in both console and GUI")
        
    def _log_message(self, message: str, level: str = "INFO"):
        """Log a message to the results text area."""
        timestamp = Path(__file__).stat().st_mtime  # Simple timestamp
        log_line = f"{level}: {message}\n"
        self.results_text.insert(tk.END, log_line)
        self.results_text.see(tk.END)
        self.results_text.update()
        
    def _validate_inputs(self) -> bool:
        """Validate user inputs."""
        asset = self.asset_code.get().strip()
        oc_file = self.oc_file_path.get().strip()
        arima_file = self.arima_file_path.get().strip()
        
        if not asset:
            messagebox.showerror("Error", "Please select an asset code.")
            return False
            
        if not oc_file:
            messagebox.showerror("Error", "Please select an OC analysis file.")
            return False
            
        if not arima_file:
            messagebox.showerror("Error", "Please select an ARIMA results file.")
            return False
            
        if not Path(oc_file).exists():
            messagebox.showerror("Error", f"OC analysis file not found: {oc_file}")
            return False
            
        if not Path(arima_file).exists():
            messagebox.showerror("Error", f"ARIMA results file not found: {arima_file}")
            return False
        
        # Validate date range mode settings
        if self.date_range_mode.get():
            start_date_str = self.start_date.get().strip()
            end_date_str = self.end_date.get().strip()
            
            if not start_date_str:
                messagebox.showerror("Error", "Please enter a start date when date range mode is enabled.")
                return False
            if not end_date_str:
                messagebox.showerror("Error", "Please enter an end date when date range mode is enabled.")
                return False
            if not self._validate_date_format(start_date_str):
                messagebox.showerror("Error", "Please enter a valid start date in YYYY-MM-DD format (e.g., 2024-01-15).")
                return False
            if not self._validate_date_format(end_date_str):
                messagebox.showerror("Error", "Please enter a valid end date in YYYY-MM-DD format (e.g., 2024-01-15).")
                return False
            
            # Validate date range logic
            try:
                start_date_obj = datetime.strptime(start_date_str, "%Y-%m-%d")
                end_date_obj = datetime.strptime(end_date_str, "%Y-%m-%d")
                if start_date_obj >= end_date_obj:
                    messagebox.showerror("Error", "Start date must be earlier than end date.")
                    return False
            except ValueError as e:
                messagebox.showerror("Error", f"Date validation error: {str(e)}")
                return False
            
        return True
        
    def _run_prediction(self):
        """Run the expected return prediction in a separate thread."""
        if not self._validate_inputs():
            return
            
        # Disable the run button and start progress
        self.run_btn.config(state="disabled")
        self.progress.start()
        
        # Clear previous results
        self.results_text.delete(1.0, tk.END)
        
        # Run prediction in a separate thread
        thread = threading.Thread(target=self._run_prediction_thread)
        thread.daemon = True
        thread.start()
        
    def _run_prediction_thread(self):
        """Run prediction in a separate thread to prevent GUI freezing."""
        import traceback
        logger = logging.getLogger(__name__)
        
        try:
            logger.info("=== Starting Expected Return Prediction ===")
            logger.info(f"Asset: {self.asset_code.get()}")
            logger.info(f"OC File: {self.oc_file_path.get()}")
            logger.info(f"ARIMA File: {self.arima_file_path.get()}")
            logger.info(f"Date Range Mode: {self.date_range_mode.get()}")
            if self.date_range_mode.get():
                logger.info(f"Date Range: {self.start_date.get()} to {self.end_date.get()}")
            
            # Import modules dynamically
            logger.info("Importing prediction modules...")
            from crypto_trading_model.expected_return_prediction import ExpectedReturnPredictor, PredictionConfig
            logger.info("Modules imported successfully")
            
            # Create configuration
            logger.info("Creating prediction configuration...")
            start_date = None
            end_date = None
            if self.date_range_mode.get():
                start_date = self.start_date.get().strip()
                end_date = self.end_date.get().strip()
                
            config = PredictionConfig(
                oc_file_path=self.oc_file_path.get().strip(),
                arima_file_path=self.arima_file_path.get().strip(),
                asset_code=self.asset_code.get().strip(),
                output_file_path=self.output_file_path.get().strip() or None,
                start_date=start_date,
                end_date=end_date
            )
            logger.info("Configuration created successfully")
            
            # Initialize predictor
            logger.info("Initializing predictor...")
            predictor = ExpectedReturnPredictor()
            logger.info("Predictor initialized successfully")
            
            # Run prediction
            logger.info("Starting prediction calculation...")
            results = predictor.predict_returns(config)
            logger.info("Prediction completed successfully")
            
            # Display summary in GUI thread
            self.root.after(0, self._display_results, results)
            
        except ImportError as e:
            error_msg = f"Import Error: {str(e)}"
            logger.error(error_msg)
            logger.error("Full traceback:")
            logger.error(traceback.format_exc())
            self.root.after(0, self._display_error, error_msg)
        except FileNotFoundError as e:
            error_msg = f"File Not Found: {str(e)}"
            logger.error(error_msg)
            logger.error("Full traceback:")
            logger.error(traceback.format_exc())
            self.root.after(0, self._display_error, error_msg)
        except Exception as e:
            error_msg = f"Prediction Error: {str(e)}"
            logger.error(error_msg)
            logger.error("Full traceback:")
            logger.error(traceback.format_exc())
            self.root.after(0, self._display_error, error_msg)
        finally:
            # Re-enable button and stop progress in GUI thread
            self.root.after(0, self._finish_prediction)
            
    def _display_results(self, results):
        """Display prediction results."""
        # Check if we have AR lag validation information
        ar_validation_info = ""
        if hasattr(results, 'prediction_summary') and 'ar_lag_validation' in results.prediction_summary:
            ar_val = results.prediction_summary['ar_lag_validation']
            ar_validation_info = f"""
AR Lag Data Validation:
  Expected AR Columns: {ar_val.get('ar_lag_order', 'N/A')} (Ar.L1 to Ar.L{ar_val.get('ar_lag_order', 'N/A')})
  Present AR Columns: {len(ar_val.get('present_ar_columns', []))}
  Missing AR Columns: {len(ar_val.get('missing_ar_columns', []))}
  Rows with Complete AR Lags: {ar_val.get('complete_rows', 0)}/{ar_val.get('total_rows', 0)} ({ar_val.get('completeness_ratio', 0)*100:.1f}%)
"""
            if ar_val.get('missing_ar_columns'):
                ar_validation_info += f"  ⚠️  Missing: {', '.join(ar_val['missing_ar_columns'])}\n"

        # Add mode information
        mode_info = ""
        if self.date_range_mode.get():
            mode_info = f"Mode: Date Range Prediction ({self.start_date.get()} to {self.end_date.get()})\n"
        else:
            mode_info = "Mode: Full Dataset Prediction\n"
        
        summary = f"""
=== PREDICTION COMPLETED SUCCESSFULLY ===

{mode_info}
Asset: {results.asset_code}
AR Lag Order: {results.ar_lag_order}
Total Data Points: {results.total_data_points}
Valid Predictions: {results.valid_predictions}
Success Rate: {(results.valid_predictions/results.total_data_points)*100:.1f}%
Output File: {results.output_file_path}
{ar_validation_info}
Prediction Statistics:
"""
        
        # Add prediction statistics
        for calc_col in results.prediction_summary.get('calculation_columns', []):
            if 'error' not in calc_col and calc_col['valid_count'] > 0:
                col_name = calc_col['column']
                summary += f"""
  {col_name}:
    Valid Count: {calc_col['valid_count']}
    Mean: {calc_col['mean']:.6f}
    Std Dev: {calc_col['std']:.6f}
    Range: [{calc_col['min']:.6f}, {calc_col['max']:.6f}]"""
        
        self._log_message(summary)
        messagebox.showinfo("Success", "Prediction completed successfully!")
        
    def _display_error(self, error_msg: str):
        """Display error message."""
        self._log_message(f"ERROR: {error_msg}", "ERROR")
        messagebox.showerror("Error", f"Prediction failed: {error_msg}")
        
    def _finish_prediction(self):
        """Re-enable controls after prediction completes."""
        self.run_btn.config(state="normal")
        self.progress.stop()

def main():
    """Main entry point for the GUI application."""
    root = tk.Tk()
    
    # Set up modern theme if available
    try:
        style = ttk.Style(root)
        if "clam" in style.theme_names():
            style.theme_use("clam")
    except:
        pass  # Use default theme if modern theme not available
    
    app = ExpectedReturnGUI(root)
    
    # Center the window
    root.update_idletasks()
    width = root.winfo_width()
    height = root.winfo_height()
    x = (root.winfo_screenwidth() // 2) - (width // 2)
    y = (root.winfo_screenheight() // 2) - (height // 2)
    root.geometry(f"{width}x{height}+{x}+{y}")
    
    root.mainloop()

if __name__ == "__main__":
    main()