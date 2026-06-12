"""
Crypto Trading Model - Sync Data Dashboard
==========================================

A simple GUI dashboard for triggering cryptocurrency data synchronization operations.
This dashboard provides an easy-to-use interface for the sync_data module.

Features:
- Sync daily data for default assets (BTC, ETH)
- Sync new asset with custom date range
- Real-time status updates and progress tracking
- Export data to CSV files

Usage:
    python crypto_sync_dashboard.py
"""

import tkinter as tk
from tkinter import ttk, messagebox, scrolledtext, filedialog
from datetime import datetime, timedelta, timezone
import threading
import sys
import os
from pathlib import Path

# Add the src directory to Python path to import our modules
sys.path.insert(0, str(Path(__file__).parent / "src"))

try:
    from crypto_trading_model.sync_data import SyncDataMain, SyncDataConfig
    from crypto_trading_model.sync_data.models import IntervalType
except ImportError as e:
    print(f"Error importing sync_data module: {e}")
    print("Please ensure you're running this from the project root directory.")
    sys.exit(1)


class SyncDashboard:
    """Main dashboard class for cryptocurrency data synchronization."""
    
    def __init__(self):
        self.root = tk.Tk()
        self.root.title("Crypto Trading Model - Data Sync Dashboard")
        self.root.geometry("800x600")
        self.root.resizable(True, True)
        
        # Initialize sync components
        self.config = SyncDataConfig()
        self.config.default_symbols = ["BTCUSDT", "ETHUSDT"]
        self.sync_app = SyncDataMain(self.config)
        
        # GUI variables
        self.is_syncing = tk.BooleanVar(value=False)
        self.available_assets = ["BTC", "ETH"]  # Default fallback
        
        self.setup_ui()
        self.setup_styles()
        
        # Load available assets from API after UI is set up
        self._load_available_assets()
        
    def setup_styles(self):
        """Setup custom styles for the UI."""
        style = ttk.Style()
        style.theme_use('clam')
        
        # Configure custom styles
        style.configure('Title.TLabel', font=('Arial', 16, 'bold'))
        style.configure('Header.TLabel', font=('Arial', 12, 'bold'))
        style.configure('Action.TButton', font=('Arial', 10, 'bold'))
        
    def setup_ui(self):
        """Setup the main user interface."""
        # Main container with padding
        main_frame = ttk.Frame(self.root, padding="20")
        main_frame.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        
        # Configure grid weights for responsiveness
        self.root.columnconfigure(0, weight=1)
        self.root.rowconfigure(0, weight=1)
        main_frame.columnconfigure(1, weight=1)
        
        # Title
        title_label = ttk.Label(main_frame, text="🚀 Crypto Data Sync Dashboard", 
                               style='Title.TLabel')
        title_label.grid(row=0, column=0, columnspan=2, pady=(0, 20))
        
        # Create sections
        self.create_daily_sync_section(main_frame, row=1)
        self.create_custom_sync_section(main_frame, row=2)
        self.create_status_section(main_frame, row=3)
        self.create_log_section(main_frame, row=4)
        
        # Configure row weights
        for i in range(5):
            main_frame.rowconfigure(i, weight=1 if i == 4 else 0)
    
    def create_daily_sync_section(self, parent, row):
        """Create the daily sync section."""
        frame = ttk.LabelFrame(parent, text="📅 Daily Data Sync", padding="15")
        frame.grid(row=row, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 15))
        
        description = ttk.Label(frame, 
                               text="Sync latest data for BTC and ETH (last 7 days)")
        description.grid(row=0, column=0, columnspan=2, sticky=tk.W, pady=(0, 10))
        
        # Daily sync button
        self.daily_sync_btn = ttk.Button(frame, text="🔄 Sync Daily Data", 
                                        style='Action.TButton',
                                        command=self.start_daily_sync)
        self.daily_sync_btn.grid(row=1, column=0, sticky=tk.W)
        
        # Status for daily sync
        self.daily_status_label = ttk.Label(frame, text="Ready")
        self.daily_status_label.grid(row=1, column=1, sticky=tk.W, padx=(20, 0))
    
    def create_custom_sync_section(self, parent, row):
        """Create the custom asset sync section."""
        frame = ttk.LabelFrame(parent, text="📈 Custom Asset Sync", padding="15")
        frame.grid(row=row, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 15))
        
        # Asset selection
        ttk.Label(frame, text="Asset Code:").grid(row=0, column=0, sticky=tk.W, pady=(0, 5))
        self.asset_var = tk.StringVar(value="BTC")
        self.asset_combo = ttk.Combobox(frame, textvariable=self.asset_var, 
                                       values=self.available_assets, width=8, state="readonly")
        self.asset_combo.grid(row=0, column=1, sticky=tk.W, padx=(10, 0), pady=(0, 5))
        
        # Date range
        ttk.Label(frame, text="From Date:").grid(row=1, column=0, sticky=tk.W, pady=(0, 5))
        self.start_date_var = tk.StringVar(value=(datetime.now() - timedelta(days=30)).strftime('%Y-%m-%d'))
        start_date_entry = ttk.Entry(frame, textvariable=self.start_date_var, width=12)
        start_date_entry.grid(row=1, column=1, sticky=tk.W, padx=(10, 0), pady=(0, 5))
        
        ttk.Label(frame, text="To Date:").grid(row=2, column=0, sticky=tk.W, pady=(0, 5))
        self.end_date_var = tk.StringVar(value=datetime.now().strftime('%Y-%m-%d'))
        end_date_entry = ttk.Entry(frame, textvariable=self.end_date_var, width=12)
        end_date_entry.grid(row=2, column=1, sticky=tk.W, padx=(10, 0), pady=(0, 5))
        
        # Interval selection
        ttk.Label(frame, text="Interval:").grid(row=3, column=0, sticky=tk.W, pady=(0, 10))
        self.interval_var = tk.StringVar(value="D")
        interval_combo = ttk.Combobox(frame, textvariable=self.interval_var, 
                                     values=["1", "5", "15", "30", "60", "D", "W", "M"],
                                     width=8, state="readonly")
        interval_combo.grid(row=3, column=1, sticky=tk.W, padx=(10, 0), pady=(0, 10))
        
        # Custom sync button
        self.custom_sync_btn = ttk.Button(frame, text="📊 Sync Custom Asset", 
                                         style='Action.TButton',
                                         command=self.start_custom_sync)
        self.custom_sync_btn.grid(row=4, column=0, sticky=tk.W)
        
        # Status for custom sync
        self.custom_status_label = ttk.Label(frame, text="Ready")
        self.custom_status_label.grid(row=4, column=1, sticky=tk.W, padx=(20, 0))
    
    def create_status_section(self, parent, row):
        """Create the status section."""
        frame = ttk.LabelFrame(parent, text="📊 Sync Status", padding="15")
        frame.grid(row=row, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 15))
        
        # Progress bar
        self.progress_var = tk.DoubleVar()
        self.progress_bar = ttk.Progressbar(frame, variable=self.progress_var, 
                                          mode='indeterminate', length=300)
        self.progress_bar.grid(row=0, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 10))
        
        # Status text
        self.status_text = tk.StringVar(value="Ready to sync data")
        status_label = ttk.Label(frame, textvariable=self.status_text)
        status_label.grid(row=1, column=0, sticky=tk.W)
        
        # Test connection button
        test_btn = ttk.Button(frame, text="🔗 Test API Connection", 
                             command=self.test_connection)
        test_btn.grid(row=1, column=1, sticky=tk.E)
    
    def create_log_section(self, parent, row):
        """Create the log section."""
        frame = ttk.LabelFrame(parent, text="📝 Sync Log", padding="15")
        frame.grid(row=row, column=0, columnspan=2, sticky=(tk.W, tk.E, tk.N, tk.S), pady=(0, 0))
        
        # Log text area
        self.log_text = scrolledtext.ScrolledText(frame, height=12, width=70)
        self.log_text.grid(row=0, column=0, columnspan=2, sticky=(tk.W, tk.E, tk.N, tk.S))
        
        # Configure grid weights for the log frame
        frame.columnconfigure(0, weight=1)
        frame.rowconfigure(0, weight=1)
        
        # Clear log button
        clear_btn = ttk.Button(frame, text="🗑️ Clear Log", command=self.clear_log)
        clear_btn.grid(row=1, column=0, sticky=tk.W, pady=(10, 0))
        
        # Export log button
        export_btn = ttk.Button(frame, text="💾 Export Log", command=self.export_log)
        export_btn.grid(row=1, column=1, sticky=tk.E, pady=(10, 0))
    
    def log_message(self, message, level="INFO"):
        """Add a message to the log."""
        timestamp = datetime.now().strftime("%H:%M:%S")
        log_entry = f"[{timestamp}] {level}: {message}\n"
        
        self.log_text.insert(tk.END, log_entry)
        self.log_text.see(tk.END)
        self.root.update_idletasks()
    
    def clear_log(self):
        """Clear the log text area."""
        self.log_text.delete(1.0, tk.END)
    
    def export_log(self):
        """Export log to a file."""
        filename = filedialog.asksaveasfilename(
            defaultextension=".txt",
            filetypes=[("Text files", "*.txt"), ("All files", "*.*")],
            title="Export Log"
        )
        if filename:
            try:
                with open(filename, 'w') as f:
                    f.write(self.log_text.get(1.0, tk.END))
                self.log_message(f"Log exported to {filename}")
            except Exception as e:
                self.log_message(f"Failed to export log: {str(e)}", "ERROR")
    
    def test_connection(self):
        """Test API connection."""
        def _test():
            self.log_message("Testing API connection...")
            try:
                if self.sync_app.test_api_connection():
                    self.log_message("✅ API connection successful!")
                    self.status_text.set("API connection: OK")
                else:
                    self.log_message("❌ API connection failed", "ERROR")
                    self.status_text.set("API connection: Failed")
            except Exception as e:
                self.log_message(f"❌ Connection test error: {str(e)}", "ERROR")
                self.status_text.set("API connection: Error")
        
        # Run in background thread
        threading.Thread(target=_test, daemon=True).start()
    
    def _load_available_assets(self):
        """Load available assets from API in background."""
        def _load():
            try:
                self._safe_log("Loading available assets from API...")
                
                # Get symbols from API
                symbols = self.sync_app.synchronizer.get_available_symbols()
                
                # Extract asset codes (remove USDT suffix)
                asset_codes = []
                popular_assets = ["BTC", "ETH", "ADA", "DOT", "MATIC", "SOL", "AVAX", "LINK"]
                
                for symbol in symbols:
                    if symbol.endswith('USDT'):
                        asset_code = symbol[:-4]
                        asset_codes.append(asset_code)
                
                # Filter to popular assets for better UX (or show all if you prefer)
                available_assets = [asset for asset in popular_assets if asset in asset_codes]
                
                # Add any other USDT pairs that aren't in popular list (limited to first 20)
                other_assets = [asset for asset in asset_codes if asset not in available_assets]
                available_assets.extend(sorted(other_assets)[:20])
                
                if available_assets:
                    self.available_assets = available_assets
                    # Update the combo box values
                    self.root.after(0, self._update_asset_dropdown)
                    self._safe_log(f"Loaded {len(available_assets)} available assets")
                else:
                    self._safe_log("Using default assets (BTC, ETH)", "WARNING")
                    
            except Exception as e:
                self._safe_log(f"Failed to load assets from API: {str(e)}", "ERROR")
                self._safe_log("Using default assets (BTC, ETH)", "WARNING")
        
        # Run in background thread
        threading.Thread(target=_load, daemon=True).start()
    
    def _safe_log(self, message, level="INFO"):
        """Safe logging that works even if UI isn't ready."""
        try:
            if hasattr(self, 'log_text'):
                self.root.after(0, lambda: self.log_message(message, level))
            else:
                print(f"[{level}] {message}")
        except Exception:
            print(f"[{level}] {message}")
    
    def _update_asset_dropdown(self):
        """Update the asset dropdown with loaded values."""
        if hasattr(self, 'asset_combo'):
            self.asset_combo['values'] = self.available_assets
            # Keep current selection if valid, otherwise reset to first option
            current_value = self.asset_var.get()
            if current_value not in self.available_assets:
                self.asset_var.set(self.available_assets[0] if self.available_assets else "BTC")
    
    def start_daily_sync(self):
        """Start daily data synchronization."""
        if self.is_syncing.get():
            messagebox.showwarning("Sync in Progress", "A sync operation is already running!")
            return
        
        def _sync():
            try:
                self.is_syncing.set(True)
                self.daily_sync_btn.config(state='disabled')
                self.custom_sync_btn.config(state='disabled')
                self.progress_bar.start()
                
                self.daily_status_label.config(text="Syncing...")
                self.status_text.set("Syncing daily data...")
                self.log_message("🔄 Starting daily sync for BTC and ETH (last 7 days)")
                
                # Calculate date range in UTC
                end_date = datetime.now(timezone.utc).replace(hour=0, minute=0, second=0, microsecond=0)
                start_date = end_date - timedelta(days=7)
                
                # Sync multiple assets
                success = self.sync_app.sync_multiple_assets(
                    asset_codes=["BTC", "ETH"],
                    start_date=start_date,
                    end_date=end_date,
                    interval=IntervalType.DAILY,
                    export_csv=True,
                    create_combined_csv=True
                )
                
                if success:
                    self.log_message("✅ Daily sync completed successfully!")
                    self.daily_status_label.config(text="✅ Complete")
                    self.status_text.set("Daily sync: Completed")
                    messagebox.showinfo("Success", "Daily data sync completed successfully!")
                else:
                    self.log_message("❌ Daily sync failed", "ERROR")
                    self.daily_status_label.config(text="❌ Failed")
                    self.status_text.set("Daily sync: Failed")
                    messagebox.showerror("Error", "Daily sync failed. Check log for details.")
                
            except Exception as e:
                self.log_message(f"❌ Daily sync error: {str(e)}", "ERROR")
                self.daily_status_label.config(text="❌ Error")
                self.status_text.set("Daily sync: Error")
                messagebox.showerror("Error", f"Daily sync failed: {str(e)}")
            
            finally:
                self.is_syncing.set(False)
                self.daily_sync_btn.config(state='normal')
                self.custom_sync_btn.config(state='normal')
                self.progress_bar.stop()
        
        # Run in background thread
        threading.Thread(target=_sync, daemon=True).start()
    
    def start_custom_sync(self):
        """Start custom asset synchronization."""
        if self.is_syncing.get():
            messagebox.showwarning("Sync in Progress", "A sync operation is already running!")
            return
        
        # Validate inputs
        asset_code = self.asset_var.get().strip().upper()
        if not asset_code or asset_code not in self.available_assets:
            messagebox.showerror("Error", f"Please select a valid asset from: {', '.join(self.available_assets)}")
            return
        
        try:
            # Parse dates and convert to UTC timezone-aware datetime objects
            start_date = datetime.strptime(self.start_date_var.get(), '%Y-%m-%d').replace(tzinfo=timezone.utc)
            end_date = datetime.strptime(self.end_date_var.get(), '%Y-%m-%d').replace(tzinfo=timezone.utc)
        except ValueError:
            messagebox.showerror("Error", "Invalid date format. Use YYYY-MM-DD")
            return
        
        if start_date >= end_date:
            messagebox.showerror("Error", "Start date must be before end date")
            return
        
        def _sync():
            try:
                self.is_syncing.set(True)
                self.daily_sync_btn.config(state='disabled')
                self.custom_sync_btn.config(state='disabled')
                self.progress_bar.start()
                
                self.custom_status_label.config(text="Syncing...")
                self.status_text.set(f"Syncing {asset_code} data...")
                self.log_message(f"🔄 Starting custom sync for {asset_code} "
                               f"({start_date.strftime('%Y-%m-%d')} to {end_date.strftime('%Y-%m-%d')})")
                
                # Get interval
                interval = IntervalType(self.interval_var.get())
                
                # Sync single asset
                success = self.sync_app.sync_single_asset(
                    asset_code=asset_code,
                    start_date=start_date,
                    end_date=end_date,
                    interval=interval,
                    export_csv=True
                )
                
                if success:
                    self.log_message(f"✅ Custom sync for {asset_code} completed successfully!")
                    self.custom_status_label.config(text="✅ Complete")
                    self.status_text.set(f"{asset_code} sync: Completed")
                    messagebox.showinfo("Success", f"{asset_code} data sync completed successfully!")
                else:
                    self.log_message(f"❌ Custom sync for {asset_code} failed", "ERROR")
                    self.custom_status_label.config(text="❌ Failed")
                    self.status_text.set(f"{asset_code} sync: Failed")
                    messagebox.showerror("Error", f"{asset_code} sync failed. Check log for details.")
                
            except Exception as e:
                self.log_message(f"❌ Custom sync error: {str(e)}", "ERROR")
                self.custom_status_label.config(text="❌ Error")
                self.status_text.set(f"{asset_code} sync: Error")
                messagebox.showerror("Error", f"Custom sync failed: {str(e)}")
            
            finally:
                self.is_syncing.set(False)
                self.daily_sync_btn.config(state='normal')
                self.custom_sync_btn.config(state='normal')
                self.progress_bar.stop()
        
        # Run in background thread
        threading.Thread(target=_sync, daemon=True).start()
    
    def run(self):
        """Start the dashboard application."""
        self.log_message("🚀 Crypto Sync Dashboard started")
        self.log_message("Ready to sync cryptocurrency data")
        
        # Handle window closing
        def on_closing():
            if self.is_syncing.get():
                if messagebox.askokcancel("Quit", "Sync in progress. Do you want to quit?"):
                    self.sync_app.cleanup()
                    self.root.destroy()
            else:
                self.sync_app.cleanup()
                self.root.destroy()
        
        self.root.protocol("WM_DELETE_WINDOW", on_closing)
        self.root.mainloop()


def main():
    """Main entry point for the dashboard application."""
    try:
        dashboard = SyncDashboard()
        dashboard.run()
    except KeyboardInterrupt:
        print("\nDashboard interrupted by user")
    except Exception as e:
        print(f"Error starting dashboard: {str(e)}")
        messagebox.showerror("Startup Error", f"Failed to start dashboard: {str(e)}")


if __name__ == "__main__":
    main()