Crypto Trading Model
A Python package implementing a cryptocurrency trading model based on Modern Portfolio Theory (MPT). The model optimizes portfolio weights for BTC and ETH using strategies like Long, Short, and Hedging, with support for different risk profiles (averse, neutral, lover).
Installation
pip install -r requirements.txt

Usage
Run the model:
cd src
python -m crypto_trading_model.main

Configuration is managed via config.yaml. See the file for details on available options.
Project Structure

src/crypto_trading_model/: Source code
config/: Configuration management
data/: Data loading and processing
optimization/: Portfolio optimization
backtest/: Backtesting
reporting/: Result exporting and metrics
logging/: Custom logging


tests/: Unit tests
config.yaml: Configuration file
pyproject.toml: Project metadata

Dependencies

pandas
numpy
scipy
pyyaml
openpyxl

License
MIT
