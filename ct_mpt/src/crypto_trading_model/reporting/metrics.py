import pandas as pd
import numpy as np
from typing import Dict
from ..custom_logging.logger import CustomLogger

class MetricsCalculator:
    """Calculates performance metrics for backtest results."""
    
    def __init__(self, logger: CustomLogger, rf_rate_annual: float):
        """
        Initialize the MetricsCalculator.

        Args:
            logger: Custom logger instance.
            rf_rate_annual: Annual risk-free rate.
        """
        self.logger = logger
        self.rf_rate_annual = rf_rate_annual

    def calculate_metrics(self, results: pd.DataFrame) -> Dict:
        """
        Calculate performance metrics.

        Args:
            results: DataFrame with backtest results.

        Returns:
            Dictionary of performance metrics.
        """
        portfolio_returns = results['Daily_Return']
        total_return = results['Portfolio_Value'].iloc[-1] / results['Portfolio_Value'].iloc[0] - 1
        annualized_return = (1 + total_return) ** (252 / len(portfolio_returns)) - 1
        volatility = portfolio_returns.std() * np.sqrt(252)
        sharpe_ratio = (annualized_return - self.rf_rate_annual) / volatility if volatility > 0 else float('inf')

        drawdown = results['Portfolio_Value'] / results['Portfolio_Value'].cummax() - 1
        max_drawdown = drawdown.min()

        self.logger.info(f"Performance metrics: Total Return={total_return:.4f}, Annualized={annualized_return:.4f}, Sharpe={sharpe_ratio:.4f}")
        return {
            'Total Return': total_return,
            'Annualized Return': annualized_return,
            'Volatility': volatility,
            'Sharpe Ratio': sharpe_ratio,
            'Max Drawdown': max_drawdown
        }