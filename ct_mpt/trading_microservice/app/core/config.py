"""
Configuration Settings
======================

Centralized configuration management using Pydantic settings.
"""

from pydantic_settings import BaseSettings
from typing import List, Optional
import os


class Settings(BaseSettings):
    """Application settings."""

    # Environment
    ENVIRONMENT: str = "development"
    DEBUG: bool = True

    # API Configuration
    API_V1_STR: str = "/api/v1"
    PROJECT_NAME: str = "Trading Microservice"

    # CORS
    CORS_ORIGINS: List[str] = ["*"]

    # File Upload
    MAX_FILE_SIZE: int = 50 * 1024 * 1024  # 50MB
    ALLOWED_FILE_TYPES: List[str] = [".xlsx", ".csv"]
    UPLOAD_DIR: str = "uploads"
    RESULTS_DIR: str = "results"

    # Logging
    LOG_LEVEL: str = "INFO"
    LOG_FORMAT: str = "%(asctime)s - %(name)s - %(levelname)s - %(message)s"

    # Trading Configuration Defaults
    DEFAULT_TRADING_FEE: float = 0.0015
    DEFAULT_LEVERAGE_SCALE: float = 1.5
    DEFAULT_RISK_FREE_RATE: float = 0.0001075

    # Optimization Settings
    MAX_OPTIMIZATION_ITERATIONS: int = 1000
    OPTIMIZATION_TOLERANCE: float = 1e-6

    # Resource Limits
    MAX_PROCESSING_TIME: int = 300  # 5 minutes
    MAX_DATA_ROWS: int = 10000

    class Config:
        env_file = ".env"
        case_sensitive = True


# Global settings instance
settings = Settings()

# Ensure directories exist
os.makedirs(settings.UPLOAD_DIR, exist_ok=True)
os.makedirs(settings.RESULTS_DIR, exist_ok=True)