"""
OC ARIMA UI Module
==================

Simple UI for configuring and running ARIMA analysis on OC results.
Built with tkinter following modular monolith architecture.
"""

import tkinter as tk
from tkinter import ttk, filedialog, messagebox, scrolledtext
from pathlib import Path
import threading
import json
from typing import Optional
import logging

from .oc_arima_main import OCArimaMain, ArimaJobConfig, OCArimaMainError

logger = logging.getLogger(__name__)

class ArimaUIError(Exception):
    """Exception raised for UI-related errors."""
    pass

class OCArimaUI:
    """
    Simple UI for OC ARIMA analysis.
    Provides form-based interface for parameter configuration and analysis execution.
    """
    
    def __init__(self):
        """Initialize the ARIMA UI."""
        self.root = tk.Tk()
        self.root.title("OC ARIMA Analysis Tool")
        self.root.geometry("800x700")
        self.root.resizable(True, True)
        
        # Initialize ARIMA main module
        self.arima_main = OCArimaMain()
        
        # UI state
        self.is_running = tk.BooleanVar(value=False)
        
        # Create UI components
        self.setup_styles()
        self.create_widgets()
        self.load_default_values()
        
    def setup_styles(self):
        """Setup UI styles."""
        style = ttk.Style()
        
        # Configure Action button style
        style.configure('Action.TButton',
                       font=('Arial', 10, 'bold'))
        
    def create_widgets(self):
        """Create and layout all UI widgets."""
        # Configure root grid
        self.root.columnconfigure(0, weight=1)
        self.root.rowconfigure(4, weight=1)
        
        # Main container
        main_frame = ttk.Frame(self.root, padding="15")
        main_frame.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        main_frame.columnconfigure(0, weight=1)
        main_frame.rowconfigure(4, weight=1)
        
        # Create sections
        self.create_asset_section(main_frame, 0)
        self.create_parameters_section(main_frame, 1)
        self.create_file_section(main_frame, 2)
        self.create_column_section(main_frame, 3)
        self.create_action_section(main_frame, 4)
        self.create_results_section(main_frame, 5)
        
    def create_asset_section(self, parent, row):
        """Create the asset selection section."""
        frame = ttk.LabelFrame(parent, text="Asset Selection", padding="15")
        frame.grid(row=row, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 15))
        
        ttk.Label(frame, text="Asset Code:").grid(row=0, column=0, sticky=tk.W, pady=(0, 5))
        self.asset_var = tk.StringVar(value="BTC")
        asset_combo = ttk.Combobox(frame, textvariable=self.asset_var, 
                                  values=["BTC", "ETH"], width=10, state="readonly")
        asset_combo.grid(row=0, column=1, sticky=tk.W, padx=(10, 0), pady=(0, 5))
        
    def create_parameters_section(self, parent, row):
        """Create the ARIMA parameters section."""
        frame = ttk.LabelFrame(parent, text="ARIMA Parameters", padding="15")
        frame.grid(row=row, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 15))
        
        # P Parameter
        ttk.Label(frame, text="P (AR parameter):").grid(row=0, column=0, sticky=tk.W, pady=(0, 5))
        self.p_var = tk.IntVar(value=1)
        self.p_spinbox = ttk.Spinbox(frame, from_=0, to=10, textvariable=self.p_var, width=10)
        self.p_spinbox.grid(row=0, column=1, sticky=tk.W, padx=(10, 0), pady=(0, 5))
        
        # D Parameter (fixed at 0)
        ttk.Label(frame, text="D (Differencing):").grid(row=1, column=0, sticky=tk.W, pady=(0, 5))
        self.d_var = tk.IntVar(value=0)
        d_entry = ttk.Entry(frame, textvariable=self.d_var, width=10, state="readonly")
        d_entry.grid(row=1, column=1, sticky=tk.W, padx=(10, 0), pady=(0, 5))
        ttk.Label(frame, text="(Fixed at 0 for AR model)", font=('Arial', 8)).grid(row=1, column=2, sticky=tk.W, padx=(5, 0))
        
        # Q Parameter (fixed at 0)
        ttk.Label(frame, text="Q (MA parameter):").grid(row=2, column=0, sticky=tk.W, pady=(0, 5))
        self.q_var = tk.IntVar(value=0)
        q_entry = ttk.Entry(frame, textvariable=self.q_var, width=10, state="readonly")
        q_entry.grid(row=2, column=1, sticky=tk.W, padx=(10, 0), pady=(0, 5))
        ttk.Label(frame, text="(Fixed at 0 for AR model)", font=('Arial', 8)).grid(row=2, column=2, sticky=tk.W, padx=(5, 0))
        
        # Parameter optimization option
        self.optimize_var = tk.BooleanVar(value=False)
        optimize_check = ttk.Checkbutton(frame, text="Auto-optimize P parameter", variable=self.optimize_var, command=self._on_optimize_toggled)
        optimize_check.grid(row=3, column=0, columnspan=2, sticky=tk.W, pady=(10, 5))
        
        # Max P parameter (only enabled when optimization is on)
        ttk.Label(frame, text="Max P to test:").grid(row=4, column=0, sticky=tk.W, pady=(0, 5))
        self.max_p_var = tk.IntVar(value=10)
        self.max_p_spinbox = ttk.Spinbox(frame, from_=5, to=50, textvariable=self.max_p_var, width=10, state="disabled")
        self.max_p_spinbox.grid(row=4, column=1, sticky=tk.W, padx=(10, 0), pady=(0, 5))
        ttk.Label(frame, text="(Range: 5-50)", font=('Arial', 8)).grid(row=4, column=2, sticky=tk.W, padx=(5, 0))
    
    def create_file_section(self, parent, row):
        """Create the file selection section."""
        frame = ttk.LabelFrame(parent, text="File Selection", padding="15")
        frame.grid(row=row, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 15))
        
        ttk.Label(frame, text="OC Analysis Directory:").grid(row=0, column=0, sticky=tk.W, pady=(0, 5))
        
        file_frame = ttk.Frame(frame)
        file_frame.grid(row=1, column=0, sticky=(tk.W, tk.E), pady=(0, 5))
        file_frame.columnconfigure(0, weight=1)
        
        self.directory_var = tk.StringVar(value="oc_analysis_results")
        directory_entry = ttk.Entry(file_frame, textvariable=self.directory_var)
        directory_entry.grid(row=0, column=0, sticky=(tk.W, tk.E), padx=(0, 10))
        
        browse_btn = ttk.Button(file_frame, text="Browse", command=self.browse_directory)
        browse_btn.grid(row=0, column=1)
    
    def create_column_section(self, parent, row):
        """Create the column selection section."""
        frame = ttk.LabelFrame(parent, text="Data Column", padding="15")
        frame.grid(row=row, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 15))
        
        ttk.Label(frame, text="Column Name:").grid(row=0, column=0, sticky=tk.W, pady=(0, 5))
        self.column_var = tk.StringVar(value="Demean_Diff_OC")
        column_entry = ttk.Entry(frame, textvariable=self.column_var, width=20)
        column_entry.grid(row=0, column=1, sticky=tk.W, padx=(10, 0), pady=(0, 5))
        
        ttk.Label(frame, text="(Default: Demean_Diff_OC)", font=('Arial', 8)).grid(row=1, column=1, sticky=tk.W, padx=(10, 0))
    
    def create_action_section(self, parent, row):
        """Create the action buttons section."""
        frame = ttk.Frame(parent)
        frame.grid(row=row, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 15))
        
        # Run Analysis button
        self.run_btn = ttk.Button(frame, text="Run ARIMA Analysis", 
                                 style='Action.TButton',
                                 command=self.run_analysis)
        self.run_btn.grid(row=0, column=0, padx=(0, 10))
        
        # Clear Results button
        clear_btn = ttk.Button(frame, text="Clear Results", 
                              command=self.clear_results)
        clear_btn.grid(row=0, column=1, padx=(0, 10))
        
        # Status label
        self.status_var = tk.StringVar(value="Ready to run analysis")
        status_label = ttk.Label(frame, textvariable=self.status_var)
        status_label.grid(row=0, column=2, sticky=tk.W, padx=(20, 0))
    
    def create_results_section(self, parent, row):
        """Create the results display section."""
        frame = ttk.LabelFrame(parent, text="Analysis Results", padding="15")
        frame.grid(row=row, column=0, columnspan=2, sticky=(tk.W, tk.E, tk.N, tk.S))
        
        # Results text area
        self.results_text = scrolledtext.ScrolledText(frame, height=15, width=70)
        self.results_text.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        
        # Configure grid weights for the results frame
        frame.columnconfigure(0, weight=1)
        frame.rowconfigure(0, weight=1)
        
        # Save Results button
        save_btn = ttk.Button(frame, text="Save Results", command=self.save_results)
        save_btn.grid(row=1, column=0, sticky=tk.W, pady=(10, 0))
    
    def load_default_values(self):
        """Load default values into the form."""
        # Default values are already set in variable declarations
        self.log_message("OC ARIMA Analysis Tool initialized")
        self.log_message("Configure parameters and click 'Run ARIMA Analysis' to start")
    
    def browse_directory(self):
        """Open directory browser dialog."""
        directory = filedialog.askdirectory(
            title="Select OC Analysis Results Directory",
            initialdir=self.directory_var.get() if Path(self.directory_var.get()).exists() else "."
        )
        if directory:
            self.directory_var.set(directory)
    
    def _on_optimize_toggled(self):
        """Handle optimization checkbox toggle."""
        if self.optimize_var.get():
            # Enable max_p spinbox and disable p spinbox when optimization is on
            self.max_p_spinbox.config(state="normal")
            # Optionally disable the P parameter spinbox since it will be optimized
            if hasattr(self, 'p_spinbox'):
                self.p_spinbox.config(state="disabled")
        else:
            # Disable max_p spinbox and enable p spinbox when optimization is off
            self.max_p_spinbox.config(state="disabled")
            if hasattr(self, 'p_spinbox'):
                self.p_spinbox.config(state="normal")
    
    def run_analysis(self):
        """Run ARIMA analysis in background thread."""
        if self.is_running.get():
            messagebox.showwarning("Analysis Running", "Analysis is already running!")
            return
        
        # Validate inputs
        try:
            self.validate_inputs()
        except ArimaUIError as e:
            messagebox.showerror("Input Error", str(e))
            return
        
        # Run analysis in background
        self.is_running.set(True)
        self.run_btn.config(state='disabled')
        self.status_var.set("Running analysis...")
        
        analysis_thread = threading.Thread(target=self._run_analysis_thread, daemon=True)
        analysis_thread.start()
    
    def _run_analysis_thread(self):
        """Run analysis in background thread."""
        try:
            # Create job configuration
            job_config = ArimaJobConfig(
                asset_code=self.asset_var.get(),
                p=self.p_var.get(),
                d=self.d_var.get(),
                q=self.q_var.get(),
                file_directory=self.directory_var.get(),
                column_name=self.column_var.get(),
                optimize_parameters=self.optimize_var.get(),
                max_p=self.max_p_var.get()
            )
            
            # Run analysis
            self.log_message(f"\n{'='*50}")
            self.log_message("Starting ARIMA analysis...")
            self.log_message(f"Asset: {job_config.asset_code}")
            self.log_message(f"Model: ARIMA({job_config.p}, {job_config.d}, {job_config.q})")
            
            results = self.arima_main.run_analysis(job_config)
            
            # Display results
            self.display_results(results)
            
            self.root.after(0, lambda: self.status_var.set("Analysis completed successfully!"))
            
        except Exception as e:
            self.log_message(f"\nERROR: {str(e)}")
            self.root.after(0, lambda: self.status_var.set(f"Analysis failed: {str(e)}"))
        
        finally:
            self.root.after(0, lambda: [
                self.run_btn.config(state='normal'),
                self.is_running.set(False)
            ])
    
    def validate_inputs(self):
        """Validate user inputs."""
        # Asset code
        if not self.asset_var.get().strip():
            raise ArimaUIError("Asset code cannot be empty")
        
        # Parameters
        if self.p_var.get() < 0:
            raise ArimaUIError("P parameter cannot be negative")
        
        # Directory
        if not Path(self.directory_var.get()).exists():
            raise ArimaUIError(f"Directory does not exist: {self.directory_var.get()}")
        
        # Column name
        if not self.column_var.get().strip():
            raise ArimaUIError("Column name cannot be empty")
    
    def display_results(self, results):
        """Display analysis results in the text area."""
        self.log_message(f"\n{'='*50}")
        self.log_message("ANALYSIS RESULTS")
        self.log_message(f"{'='*50}")
        
        # Model information
        self.log_message(f"\nModel: ARIMA({results['model_parameters']['p']}, {results['model_parameters']['d']}, {results['model_parameters']['q']})")
        self.log_message(f"Asset: {results['asset_code']}")
        
        # Model fit statistics
        self.log_message(f"\nModel Fit Statistics:")
        self.log_message(f"  AIC: {results['model_fit']['aic']:.4f}")
        self.log_message(f"  BIC: {results['model_fit']['bic']:.4f}")
        self.log_message(f"  Log-Likelihood: {results['model_fit']['log_likelihood']:.4f}")
        
        # Coefficients
        self.log_message(f"\nModel Coefficients:")
        for name, value in results['coefficients'].items():
            significance = "***" if results['coefficient_significance'][name] else ""
            self.log_message(f"  {name}: {value:.6f} {significance}")
        
        # Data summary
        self.log_message(f"\nData Summary:")
        self.log_message(f"  Total points: {results['data_summary']['total_points']}")
        self.log_message(f"  Valid points: {results['data_summary']['valid_points']}")
        self.log_message(f"  Mean: {results['data_summary']['data_mean']:.6f}")
        self.log_message(f"  Std Dev: {results['data_summary']['data_std']:.6f}")
        
        # Model diagnostics
        self.log_message(f"\nModel Diagnostics:")
        stationarity = results['model_diagnostics']['is_stationary']
        self.log_message(f"  Data is stationary: {stationarity}")
        white_noise = results['model_diagnostics']['residuals_white_noise']
        self.log_message(f"  Residuals are white noise: {white_noise}")
        
        # Files
        self.log_message(f"\nFiles:")
        self.log_message(f"  Input: {results['files']['input_file']}")
        self.log_message(f"  Output JSON: {results['files']['output_json']}")
        
        self.log_message(f"\nAnalysis completed at: {results['analysis_timestamp']}")
        
        # Store results for saving
        self.current_results = results
    
    def log_message(self, message):
        """Add message to results text area."""
        self.results_text.insert(tk.END, message + "\n")
        self.results_text.see(tk.END)
        self.root.update_idletasks()
    
    def clear_results(self):
        """Clear the results text area."""
        self.results_text.delete(1.0, tk.END)
        self.status_var.set("Results cleared")
        self.current_results = None
    
    def save_results(self):
        """Save current results to file."""
        if not hasattr(self, 'current_results') or self.current_results is None:
            messagebox.showwarning("No Results", "No analysis results to save")
            return
        
        filename = filedialog.asksaveasfilename(
            defaultextension=".json",
            filetypes=[("JSON files", "*.json"), ("Text files", "*.txt"), ("All files", "*.*")],
            title="Save Analysis Results"
        )
        
        if filename:
            try:
                if filename.endswith('.json'):
                    with open(filename, 'w') as f:
                        json.dump(self.current_results, f, indent=2)
                else:
                    with open(filename, 'w') as f:
                        f.write(self.results_text.get(1.0, tk.END))
                
                messagebox.showinfo("Success", f"Results saved to {filename}")
                
            except Exception as e:
                messagebox.showerror("Error", f"Failed to save results: {str(e)}")
    
    def run(self):
        """Start the UI application."""
        self.root.mainloop()

def main():
    """Main entry point for the UI application."""
    try:
        app = OCArimaUI()
        app.run()
    except Exception as e:
        print(f"Error starting OC ARIMA UI: {str(e)}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()