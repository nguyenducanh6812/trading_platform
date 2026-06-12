import logging
import sys
from pathlib import Path

class CustomLogger:
    """Custom logging setup for the trading model."""
    
    def __init__(self, name: str, log_file: str = 'trading_model.log', level: int = logging.INFO):
        """
        Initialize the logger.

        Args:
            name: Logger name.
            log_file: Path to log file.
            level: Logging level.
        """
        self.logger = logging.getLogger(name)
        self.logger.setLevel(level)
        self._setup_handlers(log_file)

    def _setup_handlers(self, log_file: str):
        """Set up console and file handlers."""
        if not self.logger.handlers:
            console_handler = logging.StreamHandler(sys.stdout)
            console_handler.setFormatter(logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s'))
            self.logger.addHandler(console_handler)

            file_handler = logging.FileHandler(log_file)
            file_handler.setFormatter(logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s'))
            self.logger.addHandler(file_handler)

    def info(self, message: str):
        """Log an info message."""
        self.logger.info(message)

    def error(self, message: str):
        """Log an error message."""
        self.logger.error(message)

    def warning(self, message: str):
        """Log a warning message."""
        self.logger.warning(message)