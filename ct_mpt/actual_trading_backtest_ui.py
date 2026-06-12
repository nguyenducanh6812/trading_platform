"""
Actual Trading Backtest UI Launcher
===================================

Simple launcher script for the Actual Trading Backtest UI.
Run this script to start the graphical interface.
"""

import sys
from pathlib import Path

# Add the src directory to Python path
sys.path.insert(0, str(Path(__file__).parent / "src"))

def main():
    """Launch the Actual Trading Backtest UI."""
    try:
        print("Starting Actual Trading Backtest UI...")
        print("=" * 50)
        
        print("Step 1: Testing tkinter availability...")
        import tkinter as tk
        test_root = tk.Tk()
        test_root.withdraw()  # Hide test window
        test_root.destroy()
        print("[OK] tkinter is working")
        
        print("Step 2: Importing required modules...")
        from crypto_trading_model.actual_trading_backtest import ActualTradingBacktestUI
        print("[OK] Modules imported successfully")
        
        print("Step 3: Creating UI instance...")
        app = ActualTradingBacktestUI()
        print("[OK] UI instance created")
        
        print("Step 4: Starting UI...")
        print("Opening GUI window...")
        print("(Close the window or press Ctrl+C to exit)")
        print()
        
        # Run the UI - call mainloop directly to avoid threading issues
        print("  Starting tkinter mainloop...")
        print("     (Window should appear now)")
        try:
            app.root.mainloop()
            print("[OK] UI closed normally")
        except KeyboardInterrupt:
            print("[INFO] Interrupted by user (Ctrl+C)")
            app.root.destroy()
        except Exception as e:
            print(f"[ERROR] Mainloop error: {e}")
            raise
        
    except ImportError as e:
        print(f"[ERROR] Failed to import required modules: {e}")
        print("Please ensure all dependencies are installed.")
        print("\nTry running:")
        print("  python -m pip install pandas numpy openpyxl pyyaml")
        sys.exit(1)
        
    except Exception as e:
        print(f"[ERROR] {e}")
        print("\nFull error details:")
        import traceback
        traceback.print_exc()
        
        print("\nTroubleshooting:")
        print("1. Make sure you have Python 3.8+ installed")
        print("2. Install required packages: pip install pandas numpy openpyxl")
        print("3. Try running debug_ui_launch.py for detailed diagnostics")
        sys.exit(1)

if __name__ == "__main__":
    main()