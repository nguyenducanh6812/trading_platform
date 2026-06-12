"""
Logging Configuration
=====================

Centralized logging setup for the microservice.
"""

import logging
import logging.config
import sys
from typing import Dict, Any
from pythonjsonlogger import jsonlogger

from app.core.config import settings


class CustomLogger:
    """Enhanced logger with JSON formatting support."""

    def __init__(self, name: str):
        self.logger = logging.getLogger(name)

    def info(self, message: str, **kwargs):
        """Log info message with optional extra data."""
        self.logger.info(message, extra=kwargs)

    def error(self, message: str, **kwargs):
        """Log error message with optional extra data."""
        self.logger.error(message, extra=kwargs)

    def warning(self, message: str, **kwargs):
        """Log warning message with optional extra data."""
        self.logger.warning(message, extra=kwargs)

    def debug(self, message: str, **kwargs):
        """Log debug message with optional extra data."""
        self.logger.debug(message, extra=kwargs)


def setup_logging() -> None:
    """Setup logging configuration."""

    log_config: Dict[str, Any] = {
        "version": 1,
        "disable_existing_loggers": False,
        "formatters": {
            "standard": {
                "format": settings.LOG_FORMAT
            },
            "json": {
                "()": jsonlogger.JsonFormatter,
                "format": "%(asctime)s %(name)s %(levelname)s %(message)s"
            }
        },
        "handlers": {
            "console": {
                "class": "logging.StreamHandler",
                "level": settings.LOG_LEVEL,
                "formatter": "json" if settings.ENVIRONMENT == "production" else "standard",
                "stream": sys.stdout
            }
        },
        "loggers": {
            "": {  # Root logger
                "handlers": ["console"],
                "level": settings.LOG_LEVEL,
                "propagate": False
            },
            "uvicorn": {
                "handlers": ["console"],
                "level": "INFO",
                "propagate": False
            },
            "uvicorn.access": {
                "handlers": ["console"],
                "level": "INFO",
                "propagate": False
            }
        }
    }

    logging.config.dictConfig(log_config)


def get_logger(name: str) -> CustomLogger:
    """Get a custom logger instance."""
    return CustomLogger(name)