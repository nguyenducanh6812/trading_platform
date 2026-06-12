"""
Actual Trading Backtest UI Module
=================================

Simple UI for configuring and running realistic trading backtest simulations.
Built with tkinter following modular monolith architecture.
"""

import tkinter as tk
from tkinter import ttk, filedialog, messagebox, scrolledtext
from pathlib import Path
import threading
import json
from typing import Optional
import logging
import pandas as pd

from .backtest_simulator import ActualTradingBacktest
from .config import ActualTradingConfig, RebalancingFrequency
from .validators import DataValidator, ConfigValidator

logger = logging.getLogger(__name__)

class TradingBacktestUIError(Exception):
    """Exception raised for UI-related errors."""
    pass

class ActualTradingBacktestUI:
    """
    Simple UI for Actual Trading Backtest.
    Provides form-based interface for parameter configuration and simulation execution.
    """
    
    def __init__(self):
        """Initialize the Trading Backtest UI."""
        print("  Creating tkinter root window...")
        self.root = tk.Tk()
        print("  [OK] Root window created")
        
        print("  Setting window properties...")
        self.root.title("Actual Trading Backtest Tool")
        self.root.geometry("900x800")
        self.root.resizable(True, True)
        print("  [OK] Window properties set")
        
        print("  Initializing UI state...")
        # UI state
        self.is_running = tk.BooleanVar(value=False)
        self.current_results = None
        print("  [OK] UI state initialized")
        
        print("  Setting up styles...")
        # Create UI components
        self.setup_styles()
        print("  [OK] Styles configured")
        
        print("  Creating widgets...")
        self.create_widgets()
        print("  [OK] Widgets created")
        
        print("  Loading default values...")
        self.load_default_values()
        print("  [OK] UI initialization complete")
        
    def setup_styles(self):
        """Setup UI styles."""
        style = ttk.Style()
        
        # Configure Action button style
        style.configure('Action.TButton',
                       font=('Arial', 10, 'bold'))
        
        # Configure Success style
        style.configure('Success.TLabel',
                       foreground='green',
                       font=('Arial', 9, 'bold'))
        
        # Configure Error style
        style.configure('Error.TLabel',
                       foreground='red',
                       font=('Arial', 9, 'bold'))
        
    def create_widgets(self):
        """Create and layout all UI widgets."""
        # Configure root grid
        self.root.columnconfigure(0, weight=1)
        self.root.rowconfigure(6, weight=1)
        
        # Main container
        main_frame = ttk.Frame(self.root, padding="15")
        main_frame.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        main_frame.columnconfigure(0, weight=1)
        main_frame.rowconfigure(6, weight=1)
        
        # Create sections
        self.create_account_section(main_frame, 0)
        self.create_trading_section(main_frame, 1)
        self.create_rebalancing_section(main_frame, 2)
        self.create_file_section(main_frame, 3)
        self.create_output_section(main_frame, 4)
        self.create_action_section(main_frame, 5)
        self.create_results_section(main_frame, 6)
        
    def create_account_section(self, parent, row):
        """Create the account configuration section."""
        frame = ttk.LabelFrame(parent, text="Account Configuration", padding="15")
        frame.grid(row=row, column=0, sticky=(tk.W, tk.E), pady=(0, 15))
        frame.columnconfigure(1, weight=1)
        
        # Total Capital
        ttk.Label(frame, text="Total Capital (USD):").grid(row=0, column=0, sticky=tk.W, pady=(0, 8))
        self.total_capital_var = tk.DoubleVar(value=1000.0)
        capital_entry = ttk.Entry(frame, textvariable=self.total_capital_var, width=15)
        capital_entry.grid(row=0, column=1, sticky=tk.W, padx=(10, 0), pady=(0, 8))
        
        # Trading Portion
        ttk.Label(frame, text="Trading Portion:").grid(row=1, column=0, sticky=tk.W, pady=(0, 8))
        self.trading_portion_var = tk.DoubleVar(value=0.5)
        trading_scale = ttk.Scale(frame, from_=0.1, to=0.9, variable=self.trading_portion_var, 
                                 orient=tk.HORIZONTAL, length=200)
        trading_scale.grid(row=1, column=1, sticky=tk.W, padx=(10, 0), pady=(0, 8))
        
        self.trading_portion_label = ttk.Label(frame, text="50% (500 USD Trading, 500 USD Saving)")
        self.trading_portion_label.grid(row=1, column=2, sticky=tk.W, padx=(10, 0), pady=(0, 8))
        
        # Bind scale to update label
        trading_scale.config(command=self._update_trading_portion_label)
        
    def create_trading_section(self, parent, row):
        """Create the trading parameters section."""
        frame = ttk.LabelFrame(parent, text="Trading Parameters", padding="15")
        frame.grid(row=row, column=0, sticky=(tk.W, tk.E), pady=(0, 15))
        frame.columnconfigure(1, weight=1)
        
        # Trading Fee
        ttk.Label(frame, text="Trading Fee:").grid(row=0, column=0, sticky=tk.W, pady=(0, 8))
        self.trading_fee_var = tk.DoubleVar(value=0.0015)
        fee_spinbox = ttk.Spinbox(frame, from_=0.0001, to=0.01, increment=0.0001, 
                                 textvariable=self.trading_fee_var, width=15, format="%.4f")
        fee_spinbox.grid(row=0, column=1, sticky=tk.W, padx=(10, 0), pady=(0, 8))
        ttk.Label(frame, text="(0.15% default)", font=('Arial', 8)).grid(row=0, column=2, sticky=tk.W, padx=(5, 0))
        
        # Leverage Scale
        ttk.Label(frame, text="Leverage Scale:").grid(row=1, column=0, sticky=tk.W, pady=(0, 8))
        self.leverage_var = tk.DoubleVar(value=1.5)
        leverage_spinbox = ttk.Spinbox(frame, from_=1.0, to=5.0, increment=0.1, 
                                      textvariable=self.leverage_var, width=15, format="%.1f")
        leverage_spinbox.grid(row=1, column=1, sticky=tk.W, padx=(10, 0), pady=(0, 8))
        ttk.Label(frame, text="(1.5x default)", font=('Arial', 8)).grid(row=1, column=2, sticky=tk.W, padx=(5, 0))
        
        # Asset Decimal Places
        ttk.Label(frame, text="BTC Decimal Places:").grid(row=2, column=0, sticky=tk.W, pady=(0, 8))
        self.btc_decimals_var = tk.IntVar(value=3)
        btc_spinbox = ttk.Spinbox(frame, from_=0, to=8, textvariable=self.btc_decimals_var, width=10)
        btc_spinbox.grid(row=2, column=1, sticky=tk.W, padx=(10, 0), pady=(0, 8))
        
        ttk.Label(frame, text="ETH Decimal Places:").grid(row=3, column=0, sticky=tk.W, pady=(0, 8))
        self.eth_decimals_var = tk.IntVar(value=2)
        eth_spinbox = ttk.Spinbox(frame, from_=0, to=8, textvariable=self.eth_decimals_var, width=10)
        eth_spinbox.grid(row=3, column=1, sticky=tk.W, padx=(10, 0), pady=(0, 8))
        
    def create_rebalancing_section(self, parent, row):
        """Create the rebalancing configuration section."""
        frame = ttk.LabelFrame(parent, text="Rebalancing Strategy", padding="15")
        frame.grid(row=row, column=0, sticky=(tk.W, tk.E), pady=(0, 15))
        frame.columnconfigure(1, weight=1)
        
        # Rebalancing Frequency
        ttk.Label(frame, text="Rebalancing Frequency:").grid(row=0, column=0, sticky=tk.W, pady=(0, 8))
        self.rebalancing_var = tk.StringVar(value="monthly")
        rebalancing_combo = ttk.Combobox(frame, textvariable=self.rebalancing_var, 
                                        values=["monthly", "quarterly", "yearly", "custom"], 
                                        width=15, state="readonly")
        rebalancing_combo.grid(row=0, column=1, sticky=tk.W, padx=(10, 0), pady=(0, 8))
        rebalancing_combo.bind('<<ComboboxSelected>>', self._on_rebalancing_changed)
        
        # Custom Days (only shown when custom is selected)
        ttk.Label(frame, text="Custom Days:").grid(row=1, column=0, sticky=tk.W, pady=(0, 8))
        self.custom_days_var = tk.IntVar(value=30)
        self.custom_days_spinbox = ttk.Spinbox(frame, from_=1, to=365, textvariable=self.custom_days_var, 
                                              width=15, state="disabled")
        self.custom_days_spinbox.grid(row=1, column=1, sticky=tk.W, padx=(10, 0), pady=(0, 8))
        ttk.Label(frame, text="(Only for custom frequency)", font=('Arial', 8)).grid(row=1, column=2, sticky=tk.W, padx=(5, 0))
        
    def create_file_section(self, parent, row):
        """Create the file selection section."""
        frame = ttk.LabelFrame(parent, text="Input Data", padding="15")
        frame.grid(row=row, column=0, sticky=(tk.W, tk.E), pady=(0, 15))
        frame.columnconfigure(0, weight=1)
        
        ttk.Label(frame, text="Backtest Results File:").grid(row=0, column=0, sticky=tk.W, pady=(0, 5))
        
        file_frame = ttk.Frame(frame)
        file_frame.grid(row=1, column=0, sticky=(tk.W, tk.E), pady=(0, 10))
        file_frame.columnconfigure(0, weight=1)
        
        self.input_file_var = tk.StringVar(value="simple_backtest_results/simple_backtest_BTC_ETH_*.xlsx")
        file_entry = ttk.Entry(file_frame, textvariable=self.input_file_var)
        file_entry.grid(row=0, column=0, sticky=(tk.W, tk.E), padx=(0, 10))
        
        browse_btn = ttk.Button(file_frame, text="Browse", command=self.browse_input_file)
        browse_btn.grid(row=0, column=1)
        
        # File validation status
        self.file_status_var = tk.StringVar(value="No file selected")
        self.file_status_label = ttk.Label(frame, textvariable=self.file_status_var, font=('Arial', 8))
        self.file_status_label.grid(row=2, column=0, sticky=tk.W, pady=(5, 0))
        
    def create_output_section(self, parent, row):
        """Create the output configuration section."""
        frame = ttk.LabelFrame(parent, text="Output Configuration", padding="15")
        frame.grid(row=row, column=0, sticky=(tk.W, tk.E), pady=(0, 15))
        frame.columnconfigure(0, weight=1)
        
        ttk.Label(frame, text="Output File:").grid(row=0, column=0, sticky=tk.W, pady=(0, 5))
        
        output_frame = ttk.Frame(frame)
        output_frame.grid(row=1, column=0, sticky=(tk.W, tk.E), pady=(0, 10))
        output_frame.columnconfigure(0, weight=1)
        
        self.output_file_var = tk.StringVar(value="actual_trading_results.xlsx")
        output_entry = ttk.Entry(output_frame, textvariable=self.output_file_var)
        output_entry.grid(row=0, column=0, sticky=(tk.W, tk.E), padx=(0, 10))
        
        output_browse_btn = ttk.Button(output_frame, text="Browse", command=self.browse_output_file)
        output_browse_btn.grid(row=0, column=1)
        
        # Include summary option
        self.include_summary_var = tk.BooleanVar(value=True)
        summary_check = ttk.Checkbutton(frame, text="Include summary sheets", 
                                       variable=self.include_summary_var)
        summary_check.grid(row=2, column=0, sticky=tk.W, pady=(5, 0))
        
    def create_action_section(self, parent, row):
        """Create the action buttons section."""
        frame = ttk.Frame(parent)
        frame.grid(row=row, column=0, sticky=(tk.W, tk.E), pady=(0, 15))
        
        # Run Backtest button
        self.run_btn = ttk.Button(frame, text="Run Actual Trading Backtest", 
                                 style='Action.TButton',
                                 command=self.run_backtest)
        self.run_btn.grid(row=0, column=0, padx=(0, 10))
        
        # Validate Inputs button
        validate_btn = ttk.Button(frame, text="Validate Inputs", 
                                 command=self.validate_inputs_ui)
        validate_btn.grid(row=0, column=1, padx=(0, 10))
        
        # Clear Results button
        clear_btn = ttk.Button(frame, text="Clear Results", 
                              command=self.clear_results)
        clear_btn.grid(row=0, column=2, padx=(0, 10))
        
        # Progress bar
        self.progress_var = tk.DoubleVar()
        self.progress_bar = ttk.Progressbar(frame, variable=self.progress_var, 
                                           mode='indeterminate', length=200)
        self.progress_bar.grid(row=1, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(10, 0))
        
        # Status label
        self.status_var = tk.StringVar(value="Ready to run backtest")
        self.status_label = ttk.Label(frame, textvariable=self.status_var)
        self.status_label.grid(row=2, column=0, columnspan=3, sticky=tk.W, pady=(5, 0))
        
    def create_results_section(self, parent, row):
        """Create the results display section."""
        frame = ttk.LabelFrame(parent, text="Backtest Results", padding="15")
        frame.grid(row=row, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        frame.columnconfigure(0, weight=1)
        frame.rowconfigure(0, weight=1)
        
        # Results text area
        self.results_text = scrolledtext.ScrolledText(frame, height=20, width=80)
        self.results_text.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        
        # Action buttons for results
        result_actions = ttk.Frame(frame)
        result_actions.grid(row=1, column=0, sticky=(tk.W, tk.E), pady=(10, 0))
        
        save_btn = ttk.Button(result_actions, text="Save Results", command=self.save_results)
        save_btn.grid(row=0, column=0, padx=(0, 10))
        
        export_btn = ttk.Button(result_actions, text="Export to Excel", command=self.export_results)
        export_btn.grid(row=0, column=1, padx=(0, 10))
        
    def load_default_values(self):
        """Load default values and initialize UI state."""
        self._update_trading_portion_label()
        self.log_message("Actual Trading Backtest Tool initialized")
        self.log_message("Configure parameters and click 'Run Actual Trading Backtest' to start")
        self.log_message("[OK] All input fields set to default values")
        
    def _update_trading_portion_label(self, value=None):
        """Update trading portion label."""
        portion = self.trading_portion_var.get()
        total = self.total_capital_var.get()
        trading_amount = total * portion
        saving_amount = total * (1 - portion)
        
        label_text = f"{portion:.0%} ({trading_amount:.0f} USD Trading, {saving_amount:.0f} USD Saving)"
        self.trading_portion_label.config(text=label_text)
        
    def _on_rebalancing_changed(self, event=None):
        """Handle rebalancing frequency change."""
        if self.rebalancing_var.get() == "custom":
            self.custom_days_spinbox.config(state="normal")
        else:
            self.custom_days_spinbox.config(state="disabled")
            
    def browse_input_file(self):
        """Open file browser dialog for input file."""
        filetypes = [
            ("Excel files", "*.xlsx"),
            ("CSV files", "*.csv"),
            ("All files", "*.*")
        ]
        
        filename = filedialog.askopenfilename(
            title="Select Backtest Results File",
            filetypes=filetypes,
            initialdir="simple_backtest_results" if Path("simple_backtest_results").exists() else "."
        )
        
        if filename:
            self.input_file_var.set(filename)
            self.validate_input_file()
            
    def browse_output_file(self):
        """Open file browser dialog for output file."""
        filename = filedialog.asksaveasfilename(
            title="Select Output File",
            defaultextension=".xlsx",
            filetypes=[("Excel files", "*.xlsx"), ("CSV files", "*.csv"), ("All files", "*.*")]
        )
        
        if filename:
            self.output_file_var.set(filename)
            
    def validate_input_file(self):
        """Validate the selected input file."""
        file_path = self.input_file_var.get()
        
        if not file_path or not Path(file_path).exists():
            self.file_status_var.set("[ERROR] File does not exist")
            self.file_status_label.config(style="Error.TLabel")
            return False
            
        try:
            # Try to read the file
            if file_path.endswith('.xlsx'):
                data = pd.read_excel(file_path)
            elif file_path.endswith('.csv'):
                data = pd.read_csv(file_path)
            else:
                self.file_status_var.set("[ERROR] Unsupported file format")
                self.file_status_label.config(style="Error.TLabel")
                return False
                
            # Validate data structure
            validation_result = DataValidator.validate_backtest_data(data)
            
            if validation_result['is_valid']:
                self.file_status_var.set(f"[OK] Valid file ({len(data)} rows, {len(data.columns)} columns)")
                self.file_status_label.config(style="Success.TLabel")
                return True
            else:
                self.file_status_var.set(f"⚠️ File has issues: {len(validation_result['errors'])} errors")
                self.file_status_label.config(style="Error.TLabel")
                return False
                
        except Exception as e:
            self.file_status_var.set(f"[ERROR] Error reading file: {str(e)[:50]}...")
            self.file_status_label.config(style="Error.TLabel")
            return False
            
    def validate_inputs_ui(self):
        """Validate all user inputs and show results."""
        try:
            # Validate input file
            if not self.validate_input_file():
                messagebox.showerror("Validation Error", "Please select a valid input file")
                return
                
            # Create configuration
            config = self._create_config_from_inputs()
            
            # Validate configuration
            ConfigValidator.validate_trading_config(config)
            
            # All validations passed
            messagebox.showinfo("Validation Success", 
                              "[OK] All inputs are valid!\n\n" +
                              f"Total Capital: ${config.account.total_capital:.2f}\n" +
                              f"Trading Account: ${config.account.initial_trading_balance:.2f}\n" +
                              f"Saving Account: ${config.account.initial_saving_balance:.2f}\n" +
                              f"Trading Fee: {config.trading.trading_fee:.4f}\n" +
                              f"Leverage: {config.trading.leverage_scale}x\n" +
                              f"Rebalancing: {config.rebalancing.frequency.value}")
            
            self.log_message("[OK] Input validation passed successfully")
            
        except Exception as e:
            messagebox.showerror("Validation Error", str(e))
            self.log_message(f"[ERROR] Input validation failed: {str(e)}")
            
    def _create_config_from_inputs(self):
        """Create ActualTradingConfig from UI inputs."""
        # Map rebalancing frequency
        freq_map = {
            "monthly": RebalancingFrequency.MONTHLY,
            "quarterly": RebalancingFrequency.QUARTERLY,
            "yearly": RebalancingFrequency.YEARLY,
            "custom": RebalancingFrequency.CUSTOM
        }
        
        rebalancing_freq = freq_map[self.rebalancing_var.get()]
        custom_days = self.custom_days_var.get() if rebalancing_freq == RebalancingFrequency.CUSTOM else None
        
        # Create configuration
        config = ActualTradingConfig.create_custom(
            total_capital=self.total_capital_var.get(),
            trading_portion=self.trading_portion_var.get(),
            rebalancing_frequency=rebalancing_freq,
            custom_rebalancing_days=custom_days,
            trading_fee=self.trading_fee_var.get(),
            leverage_scale=self.leverage_var.get()
        )
        
        # Update asset decimal places
        config.assets.btc_decimal_places = self.btc_decimals_var.get()
        config.assets.eth_decimal_places = self.eth_decimals_var.get()
        
        return config
        
    def run_backtest(self):
        """Run actual trading backtest in background thread."""
        print("BUTTON CLICKED: Run Actual Trading Backtest")
        print("=" * 50)
        
        try:
            if self.is_running.get():
                print("Already running - showing warning")
                messagebox.showwarning("Backtest Running", "Backtest is already running!")
                return
            
            print("Step 1: Checking if already running... OK")
            
            # Validate inputs first
            print("Step 2: Starting input validation...")
            print("Step 2a: Validating input file...")
            if not self.validate_input_file():
                print("Step 2a: Input file validation FAILED")
                messagebox.showerror("Input Error", "Please select a valid input file")
                return
            print("Step 2a: Input file validation PASSED")
                
            print("Step 2b: Creating config from inputs...")
            config = self._create_config_from_inputs()
            print("Step 2b: Config created successfully")
            
            print("Step 2c: Validating trading config...")
            from .validators import ConfigValidator
            ConfigValidator.validate_trading_config(config)
            print("Step 2c: Trading config validation PASSED")
            
            print("Step 2: All input validation COMPLETED")
            
            # Start backtest in background
            print("Step 3: Setting up UI for background execution...")
            self.is_running.set(True)
            print("Step 3a: Set is_running = True")
            
            self.run_btn.config(state='disabled')
            print("Step 3b: Disabled run button")
            
            self.progress_bar.start()
            print("Step 3c: Started progress bar")
            
            self.status_var.set("Running backtest simulation...")
            print("Step 3d: Updated status message")
            
            print("Step 4: Preparing data for background thread...")
            # Get all UI values in main thread to avoid threading issues
            thread_data = {
                'input_file': self.input_file_var.get(),
                'output_file': self.output_file_var.get(),
                'include_summary': self.include_summary_var.get(),
                'config': config  # We already created this in main thread
            }
            print("Step 4a: Thread data prepared")
            
            print("Step 4b: Creating background thread...")
            backtest_thread = threading.Thread(target=self._run_backtest_thread, args=(thread_data,), daemon=True)
            print("Step 4c: Thread created")
            
            print("Step 4d: About to start thread...")
            print("If it freezes here, the issue is in thread.start()")
            backtest_thread.start()
            print("Step 4e: Thread started successfully!")
            print("Step 4f: run_backtest() method completed")
            print("=" * 50)
            
        except Exception as e:
            print(f"CRITICAL ERROR in run_backtest(): {e}")
            import traceback
            print("Full traceback:")
            traceback.print_exc()
            try:
                messagebox.showerror("Critical Error", f"Unexpected error: {str(e)}")
            except:
                pass
        
    def _run_backtest_thread(self, thread_data):
        """Run backtest in background thread."""
        print("BACKGROUND THREAD: Started!")
        print("Thread ID:", threading.current_thread().ident)
        print("Thread name:", threading.current_thread().name)
        
        try:
            print("THREAD: Getting data from thread_data...")
            input_file = thread_data['input_file']
            output_file = thread_data['output_file']
            include_summary = thread_data['include_summary']
            config = thread_data['config']
            print(f"THREAD: Got input file: {input_file}")
            
            print("THREAD: About to call log_message...")
            self.log_message(f"\n{'='*60}")
            print("THREAD: First log_message completed")
            
            self.log_message("STARTING ACTUAL TRADING BACKTEST")
            print("THREAD: Second log_message completed")
            
            self.log_message(f"{'='*60}")
            print("THREAD: Third log_message completed")
            
            self.log_message(f"Loading data from: {Path(input_file).name}")
            print("THREAD: Fourth log_message completed")
            
            print("THREAD: About to read file...")
            if input_file.endswith('.xlsx'):
                print("THREAD: Reading Excel file...")
                data = pd.read_excel(input_file)
                print("THREAD: Excel file read completed")
            else:
                print("THREAD: Reading CSV file...")
                data = pd.read_csv(input_file)
                print("THREAD: CSV file read completed")
                
            print(f"THREAD: File loaded successfully, shape: {data.shape}")
            self.log_message(f"[OK] Loaded {len(data)} days of backtest data")
            print("THREAD: File loading log message completed")
            
            print("THREAD: Configuration already created in main thread")
            # Configuration was already created in main thread, no need to access UI variables
            
            self.log_message(f"Configuration:")
            self.log_message(f"  Total Capital: ${config.account.total_capital:.2f}")
            self.log_message(f"  Trading Account: ${config.account.initial_trading_balance:.2f}")
            self.log_message(f"  Saving Account: ${config.account.initial_saving_balance:.2f}")
            self.log_message(f"  Trading Fee: {config.trading.trading_fee:.4f} ({config.trading.trading_fee*100:.2f}%)")
            self.log_message(f"  Leverage Scale: {config.trading.leverage_scale}x")
            self.log_message(f"  Rebalancing: {config.rebalancing.frequency.value}")
            if config.rebalancing.custom_days:
                self.log_message(f"  Custom Days: {config.rebalancing.custom_days}")
                
            # Validate data before running backtest
            self.log_message("\nValidating input data...")
            from .validators import DataValidator
            try:
                validation_result = DataValidator.validate_backtest_data(data)
                self.log_message(f"[OK] Data validation passed")
                self.log_message(f"  Columns found: {len(data.columns)}")
                self.log_message(f"  Required columns present: BTC_Weight, ETH_Weight, Strategy, etc.")
            except Exception as validation_error:
                self.log_message(f"[ERROR] Data validation failed: {validation_error}")
                self.log_message(f"\\nAvailable columns in your file:")
                for i, col in enumerate(data.columns):
                    self.log_message(f"  {i+1}. {col}")
                self.log_message(f"\\nExpected columns:")
                self.log_message(f"  - BTC_Weight, ETH_Weight, Risky_Weight")
                self.log_message(f"  - Strategy, Daily_Return") 
                self.log_message(f"  - Columns with 'BTC' and 'open'/'close' in name")
                self.log_message(f"  - Columns with 'ETH' and 'open'/'close' in name")
                raise validation_error
            
            # Run backtest
            self.log_message("\nStarting simulation...")
            print("THREAD: About to create ActualTradingBacktest instance...")
            backtest = ActualTradingBacktest(config)
            print("THREAD: ActualTradingBacktest instance created")
            
            print("THREAD: About to call run_backtest...")
            results = backtest.run_backtest(data)
            print("THREAD: run_backtest completed!")
            
            # Display results
            print("THREAD: About to display results...")
            self.display_backtest_results(backtest, results)
            print("THREAD: Results displayed")
            
            # Export results if requested
            print("THREAD: About to export results...")
            if output_file:
                print(f"THREAD: Exporting to: {output_file}")
                backtest.export_results(
                    output_file, 
                    include_summary=include_summary
                )
                self.log_message(f"\n[SAVE] Results exported to: {output_file}")
                print("THREAD: Export completed")
            
            self.current_results = results
            self.root.after(0, lambda: self.status_var.set("[SUCCESS] Backtest completed successfully!"))
            
        except Exception as e:
            error_msg = str(e)
            self.log_message(f"\n[ERROR] {error_msg}")
            # More detailed error information
            import traceback
            self.log_message("\nFull error details:")
            self.log_message(traceback.format_exc())
            self.root.after(0, lambda: self.status_var.set(f"[ERROR] Backtest failed: {error_msg[:50]}..."))
            
        finally:
            self.root.after(0, lambda: [
                self.run_btn.config(state='normal'),
                self.progress_bar.stop(),
                self.is_running.set(False)
            ])
            
    def display_backtest_results(self, backtest, results_df):
        """Display backtest results in the text area."""
        summary = backtest.get_simulation_summary()
        
        self.log_message(f"\n{'='*60}")
        self.log_message("BACKTEST RESULTS SUMMARY")
        self.log_message(f"{'='*60}")
        
        # Performance metrics
        self.log_message(f"Performance Metrics:")
        self.log_message(f"  Initial Trading Balance: ${summary['initial_trading_balance']:.2f}")
        self.log_message(f"  Final Trading Balance: ${summary['final_trading_balance']:.2f}")
        self.log_message(f"  Total P&L: ${summary['total_profit_loss']:.2f}")
        self.log_message(f"  Total Return: {summary['total_return_pct']:.2f}%")
        self.log_message(f"  Total Trading Days: {summary['total_trading_days']}")
        
        # Risk metrics
        self.log_message(f"\nRisk Metrics:")
        self.log_message(f"  Average Daily Return: {summary['average_daily_return']:.4f} ({summary['average_daily_return']*100:.2f}%)")
        self.log_message(f"  Daily Volatility: {summary['volatility']:.4f} ({summary['volatility']*100:.2f}%)")
        self.log_message(f"  Max Daily Return: {summary['max_daily_return']:.4f} ({summary['max_daily_return']*100:.2f}%)")
        self.log_message(f"  Min Daily Return: {summary['min_daily_return']:.4f} ({summary['min_daily_return']*100:.2f}%)")
        
        # Trading costs
        self.log_message(f"\nTrading Costs:")
        self.log_message(f"  Total Fees Paid: ${summary['total_fees_paid']:.2f}")
        self.log_message(f"  Fee Impact on Return: {(summary['total_fees_paid']/summary['initial_trading_balance']*100):.2f}%")
        
        # Rebalancing
        self.log_message(f"\nRebalancing:")
        self.log_message(f"  Total Rebalancing Events: {summary['total_rebalancing_events']}")
        
        # Strategy breakdown
        if 'Final_Decision' in results_df.columns:
            strategy_counts = results_df['Final_Decision'].value_counts()
            self.log_message(f"\nStrategy Breakdown:")
            for strategy, count in strategy_counts.items():
                percentage = count / len(results_df) * 100
                self.log_message(f"  {strategy}: {count} days ({percentage:.1f}%)")
        
        self.log_message(f"\n[SUCCESS] Backtest completed successfully!")
        
    def log_message(self, message):
        """Add message to results text area (thread-safe)."""
        # Schedule GUI update in main thread to avoid freezing
        self.root.after(0, self._update_log_display, message)
        
    def _update_log_display(self, message):
        """Update log display in main thread."""
        self.results_text.insert(tk.END, message + "\n")
        self.results_text.see(tk.END)
        self.root.update_idletasks()
        
    def clear_results(self):
        """Clear the results text area."""
        self.results_text.delete(1.0, tk.END)
        self.status_var.set("Results cleared")
        self.current_results = None
        self.log_message("Actual Trading Backtest Tool - Results Cleared")
        
    def save_results(self):
        """Save current results to file."""
        if self.current_results is None or self.current_results.empty:
            messagebox.showwarning("No Results", "No backtest results to save")
            return
            
        filename = filedialog.asksaveasfilename(
            defaultextension=".txt",
            filetypes=[("Text files", "*.txt"), ("JSON files", "*.json"), ("All files", "*.*")],
            title="Save Backtest Results"
        )
        
        if filename:
            try:
                with open(filename, 'w') as f:
                    f.write(self.results_text.get(1.0, tk.END))
                    
                messagebox.showinfo("Success", f"Results saved to {filename}")
                
            except Exception as e:
                messagebox.showerror("Error", f"Failed to save results: {str(e)}")
                
    def export_results(self):
        """Export current results to Excel."""
        if self.current_results is None or self.current_results.empty:
            messagebox.showwarning("No Results", "No backtest results to export")
            return
            
        filename = filedialog.asksaveasfilename(
            defaultextension=".xlsx",
            filetypes=[("Excel files", "*.xlsx"), ("CSV files", "*.csv")],
            title="Export Backtest Results"
        )
        
        if filename:
            try:
                if filename.endswith('.xlsx'):
                    self.current_results.to_excel(filename, index=False)
                else:
                    self.current_results.to_csv(filename, index=False)
                    
                messagebox.showinfo("Success", f"Results exported to {filename}")
                
            except Exception as e:
                messagebox.showerror("Error", f"Failed to export results: {str(e)}")
        
    def run(self):
        """Start the UI application."""
        print("  Starting tkinter mainloop...")
        print("     (Window should appear now)")
        try:
            self.root.mainloop()
            print("  [OK] Mainloop completed normally")
        except KeyboardInterrupt:
            print("  [INFO] Interrupted by user (Ctrl+C)")
            self.root.destroy()
        except Exception as e:
            print(f"  [ERROR] Mainloop error: {e}")
            raise


def main():
    """Main entry point for the UI application."""
    try:
        app = ActualTradingBacktestUI()
        app.run()
    except Exception as e:
        print(f"Error starting Actual Trading Backtest UI: {str(e)}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    main()