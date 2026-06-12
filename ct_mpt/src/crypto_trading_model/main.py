#!/usr/bin/env python
"""
Crypto Trading Model Runner
"""
import json
import sys
from pathlib import Path
import pandas as pd
import numpy as np

# Add src to Python path when running directly
if __name__ == "__main__":
    src_path = Path(__file__).resolve().parent.parent
    sys.path.insert(0, str(src_path))

from crypto_trading_model.config.config_manager import ConfigManager
from crypto_trading_model.data.data_loader import DataLoader
from crypto_trading_model.data.data_processor import DataProcessor
from crypto_trading_model.optimization.optimizer import Optimizer
from crypto_trading_model.backtest.backtester import Backtester
from crypto_trading_model.reporting.exporter import Exporter
from crypto_trading_model.reporting.metrics import MetricsCalculator
from crypto_trading_model.custom_logging.logger import CustomLogger

class PandasJSONEncoder(json.JSONEncoder):
    """Custom JSON encoder for pandas and NumPy types."""
    def default(self, obj):
        if isinstance(obj, pd.Timestamp):
            return obj.isoformat()
        if isinstance(obj, (np.floating, np.integer)):
            return obj.item()
        if isinstance(obj, np.ndarray):
            return obj.tolist()
        if pd.isna(obj):
            return None
        return super().default(obj)

def main():
    """Main function to run the trading model."""
    logger = CustomLogger('CryptoTradingModel')
    config_manager = ConfigManager(logger)
    config = config_manager.load_config()

    data_loader = DataLoader(logger)
    market_data = data_loader.load_data(config.data_file_path)
    data_processor = DataProcessor(logger, config.strategy.lookback_period)
    market_data = data_processor.calculate_returns(market_data)
    _, mean_returns, cov_matrix = data_processor.compute_metrics(market_data)

    rf_rate = ((1 + config.risk_free.default_rate) ** (1/365)) - 1
    # debug
    rf_rate = 0.0001075

    optimizer = Optimizer(
        logger, 
        mean_returns, 
        cov_matrix, 
        rf_rate,
        weight_bounds=None,  # Strategies load bounds from config
        config_manager=config_manager
    )

    exporter = Exporter(logger)
    metrics_calculator = MetricsCalculator(logger, config.risk_free.default_rate)
    backtester = Backtester(
        logger, 
        data_processor, 
        optimizer, 
        exporter, 
        metrics_calculator,
        config_manager
    )

    # Only run for configured risk profiles
    results = {}
    for profile in config.risk_profiles.available:
        results[profile] = {}
        excel_path = f"{config.output.output_dir}/{Path(config.output.excel_path).stem}_{profile}.xlsx"
        
        # Validate data type
        if not isinstance(market_data, pd.DataFrame):
            logger.error(f"Expected DataFrame for backtest, got {type(market_data)}")
            raise TypeError(f"Expected DataFrame for backtest, got {type(market_data)}")
            
        # Run backtest
        backtest_results = backtester.backtest(
            market_data, 
            profile, 
            config.strategy.rebalancing_frequency, 
            config.strategy.lookback_period,
            config.output.export_excel, 
            excel_path
        )

        metrics = metrics_calculator.calculate_metrics(backtest_results)
        
        # Convert non-serializable types in backtest_results
        for col in backtest_results.columns:
            if backtest_results[col].dtype == 'datetime64[ns]':
                backtest_results[col] = backtest_results[col].astype(str)
            elif backtest_results[col].dtype in ['float64', 'int64']:
                backtest_results[col] = backtest_results[col].replace(np.nan, None)
                
        results[profile][config.strategy.rebalancing_frequency] = {
            'backtest_results': backtest_results.to_dict('records'),
            'metrics': metrics
        }

    # Ensure output directory exists
    output_dir = Path(config.output.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    json_path = output_dir / 'results_summary.json'
    
    try:
        with open(json_path, 'w') as f:
            json.dump(results, f, indent=4, cls=PandasJSONEncoder)
    except Exception as e:
        logger.error(f"Failed to save JSON: {e}")
        raise

if __name__ == "__main__":
    main()