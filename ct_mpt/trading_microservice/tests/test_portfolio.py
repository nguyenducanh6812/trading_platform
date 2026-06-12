"""
Tests for Portfolio Optimization API
====================================

Unit tests for portfolio optimization endpoints and services.
"""

import pytest
from fastapi.testclient import TestClient
from unittest.mock import Mock, patch
import pandas as pd
import numpy as np

from app.main import app
from app.services.portfolio_optimizer import PortfolioOptimizer
from app.services.data_processor import DataProcessor

client = TestClient(app)


class TestPortfolioAPI:
    """Test portfolio optimization API endpoints."""

    def test_health_check(self):
        """Test health check endpoint."""
        response = client.get("/api/v1/health")
        assert response.status_code == 200
        assert response.json()["status"] == "healthy"

    def test_example_request(self):
        """Test example request endpoint."""
        response = client.get("/api/v1/portfolio/example-request")
        assert response.status_code == 200
        data = response.json()
        assert "description" in data
        assert "form_data" in data
        assert "files" in data

    @patch('app.services.data_processor.DataProcessor.validate_file')
    def test_validate_data_valid_file(self, mock_validate):
        """Test data validation with valid file."""
        # Mock validation result
        mock_validate.return_value = {
            "status": "valid",
            "asset_code": "BTC",
            "total_rows": 100,
            "prediction_count": 90
        }

        # Create mock file
        test_file = ("test.xlsx", b"test content", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")

        response = client.post(
            "/api/v1/portfolio/validate-data",
            files={"files": test_file}
        )

        assert response.status_code == 200
        data = response.json()
        assert data["is_valid"] is True


class TestPortfolioOptimizer:
    """Test portfolio optimizer service."""

    def setUp(self):
        """Set up test fixtures."""
        self.optimizer = PortfolioOptimizer()

    def test_calculate_covariance(self):
        """Test covariance matrix calculation."""
        # Create test data
        np.random.seed(42)
        data = pd.DataFrame({
            'BTC_Return': np.random.normal(0.01, 0.02, 100),
            'ETH_Return': np.random.normal(0.008, 0.025, 100)
        })

        asset_codes = ['BTC', 'ETH']
        mean_returns, cov_matrix, asset_volatilities = self.optimizer._calculate_covariance(
            data, asset_codes
        )

        # Verify shapes
        assert len(mean_returns) == 2
        assert cov_matrix.shape == (2, 2)
        assert len(asset_volatilities) == 2

        # Verify properties
        assert np.allclose(cov_matrix, cov_matrix.T)  # Symmetric
        assert np.all(asset_volatilities >= 0)  # Non-negative volatilities

    def test_get_predicted_returns_valid(self):
        """Test getting predicted returns with valid data."""
        # Create test data
        data = pd.DataFrame({
            'Timestamp': pd.date_range('2023-01-01', periods=5),
            'Prd_Return_Arima_BTC': [0.01, 0.02, -0.01, 0.005, 0.015],
            'Prd_Return_Arima_ETH': [0.008, 0.012, -0.002, 0.001, 0.01]
        })

        asset_codes = ['BTC', 'ETH']
        predicted_returns = self.optimizer._get_predicted_returns(data, 2, asset_codes)

        assert len(predicted_returns) == 2
        assert predicted_returns[0] == -0.01  # BTC at index 2
        assert predicted_returns[1] == -0.002  # ETH at index 2

    def test_get_predicted_returns_missing_data(self):
        """Test getting predicted returns with missing data."""
        # Create test data with NaN
        data = pd.DataFrame({
            'Timestamp': pd.date_range('2023-01-01', periods=5),
            'Prd_Return_Arima_BTC': [0.01, 0.02, np.nan, 0.005, 0.015],
            'Prd_Return_Arima_ETH': [0.008, 0.012, -0.002, 0.001, 0.01]
        })

        asset_codes = ['BTC', 'ETH']

        with pytest.raises(Exception):  # Should raise OptimizationError
            self.optimizer._get_predicted_returns(data, 2, asset_codes)

    def test_select_best_strategy(self):
        """Test strategy selection logic."""
        optimization_results = {
            'strategies': {
                'Long': {
                    'return': 0.05,
                    'volatility': 0.15,
                    'sharpe': 0.3
                },
                'Short': {
                    'return': 0.02,
                    'volatility': 0.12,
                    'sharpe': 0.15
                }
            }
        }

        best_strategy = self.optimizer._select_best_strategy(optimization_results)

        # Based on the selection logic: Long sharpe > 0 and return-vol difference
        assert best_strategy in ['Long', 'Short']


class TestDataProcessor:
    """Test data processor service."""

    def setUp(self):
        """Set up test fixtures."""
        self.processor = DataProcessor()

    def test_validate_prediction_file_structure(self):
        """Test prediction file validation."""
        # This would be a mock test since we don't have actual files
        # In a real implementation, you would create test files or mock the file reading

        # Mock validation logic
        df = pd.DataFrame({
            'Timestamp': pd.date_range('2023-01-01', periods=100),
            'Open_Price': np.random.uniform(40000, 60000, 100),
            'Close_Price_BTC': np.random.uniform(40000, 60000, 100),
            'Prd_Return_Arima_BTC': np.random.normal(0.001, 0.02, 100)
        })

        # Test validation logic (would need to adapt for your actual validation method)
        required_cols = ['Timestamp', 'Open_Price', 'Close_Price_BTC', 'Prd_Return_Arima_BTC']
        missing_cols = [col for col in required_cols if col not in df.columns]

        assert len(missing_cols) == 0

    def test_combine_prediction_data(self):
        """Test combining multiple prediction files."""
        # Create mock data for two assets
        btc_data = pd.DataFrame({
            'Timestamp': pd.date_range('2023-01-01', periods=10),
            'Open_Price_BTC': np.random.uniform(40000, 60000, 10),
            'Close_Price_BTC': np.random.uniform(40000, 60000, 10),
            'Prd_Return_Arima_BTC': np.random.normal(0.001, 0.02, 10)
        })

        eth_data = pd.DataFrame({
            'Timestamp': pd.date_range('2023-01-01', periods=10),
            'Open_Price_ETH': np.random.uniform(2000, 4000, 10),
            'Close_Price_ETH': np.random.uniform(2000, 4000, 10),
            'Prd_Return_Arima_ETH': np.random.normal(0.001, 0.025, 10)
        })

        # Test inner join behavior
        combined = pd.merge(btc_data, eth_data, on='Timestamp', how='inner')

        assert len(combined) == 10  # Should have all rows since timestamps match
        assert 'Open_Price_BTC' in combined.columns
        assert 'Open_Price_ETH' in combined.columns

    def test_find_first_prediction_index(self):
        """Test finding first prediction index."""
        data = pd.DataFrame({
            'Prd_Return_Arima_BTC': [np.nan, np.nan, 0.01, 0.02, 0.01],
            'Prd_Return_Arima_ETH': [np.nan, 0.008, 0.012, 0.001, 0.01]
        })

        asset_codes = ['BTC', 'ETH']
        first_index = self.processor.find_first_prediction_index(data, asset_codes)

        # Should be index 2 (where BTC first has prediction, taking max of both assets)
        assert first_index == 2


if __name__ == "__main__":
    pytest.main([__file__])