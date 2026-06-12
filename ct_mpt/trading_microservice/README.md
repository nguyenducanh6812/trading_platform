# Trading Backtest Microservice

A FastAPI-based microservice that provides portfolio optimization and realistic trading simulation capabilities.

## Features

- **Portfolio Optimization API** (Step 1): Generate optimal portfolio weights using ARIMA predictions
- **Actual Trading Simulation API** (Step 2): Apply realistic trading constraints (fees, rebalancing, account management)

## Architecture

```
┌─────────────────────────────────────────┐
│           Trading Microservice          │
├─────────────────────────────────────────┤
│  POST /api/v1/optimize-portfolio        │
│  POST /api/v1/simulate-trading          │
│  GET  /api/v1/health                    │
│  GET  /docs                             │
└─────────────────────────────────────────┘
```

## Project Structure

```
trading_microservice/
├── app/
│   ├── __init__.py
│   ├── main.py                  # FastAPI application
│   ├── api/
│   │   ├── __init__.py
│   │   ├── v1/
│   │   │   ├── __init__.py
│   │   │   ├── endpoints/
│   │   │   │   ├── __init__.py
│   │   │   │   ├── portfolio.py     # Portfolio optimization
│   │   │   │   └── trading.py       # Trading simulation
│   │   │   └── models.py            # Pydantic models
│   │   └── dependencies.py
│   ├── core/
│   │   ├── __init__.py
│   │   ├── config.py               # Configuration
│   │   └── exceptions.py           # Custom exceptions
│   ├── services/
│   │   ├── __init__.py
│   │   ├── portfolio_optimizer.py  # Step 1: Portfolio optimization
│   │   ├── trading_simulator.py    # Step 2: Trading simulation
│   │   └── data_processor.py       # Data processing utilities
│   └── utils/
│       ├── __init__.py
│       ├── logger.py              # Logging utilities
│       └── validators.py          # Input validation
├── tests/
│   ├── __init__.py
│   ├── test_portfolio.py
│   └── test_trading.py
├── requirements.txt
├── Dockerfile
├── docker-compose.yml
└── README.md
```

## Quick Start

### Option 1: Using the Setup Script (Recommended)

```bash
# First, set up the environment (handles dependency issues)
python scripts/setup_environment.py

# Then start the service
python scripts/start_service.py dev

# Or use auto-setup mode
python scripts/start_service.py dev --auto-setup

# Other modes
python scripts/start_service.py prod     # Production mode
python scripts/start_service.py docker  # Using Docker
python scripts/start_service.py test    # Run tests
python scripts/start_service.py setup   # Just run setup
```

### Option 2: Using Docker Compose

```bash
# Build and run with Docker Compose
docker-compose up --build

# Access the API
curl http://localhost:8000/api/v1/health
```

### Option 3: Manual Setup

```bash
# Try different installation methods if you encounter issues:

# Method 1: Full requirements
pip install -r requirements.txt

# Method 2: Minimal requirements (if full install fails)
pip install -r requirements-minimal.txt

# Method 3: Individual packages (last resort)
pip install fastapi uvicorn pandas numpy openpyxl pydantic python-dotenv

# Create environment file
cp .env.example .env

# Create directories
mkdir -p uploads results logs

# Run the service
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### If You Encounter Installation Issues

See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for detailed solutions to common problems.

## API Documentation

Access the interactive API documentation at:
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## API Endpoints

### 1. Portfolio Optimization (`POST /api/v1/optimize-portfolio`)

Generate optimal portfolio weights using ARIMA predictions.

**Input:**
- Asset prediction files (Excel/CSV with ARIMA forecasts)
- Risk profile (averse, neutral, lover)
- Optimization method (traditional, smart_grid, compare)
- Date range and other parameters

**Output:**
- Optimal portfolio weights over time
- Strategy decisions (Long, Short, Market_Neutral)
- Performance metrics

### 2. Trading Simulation (`POST /api/v1/simulate-trading`)

Apply realistic trading constraints to portfolio decisions.

**Input:**
- Portfolio optimization results (from step 1)
- Trading configuration (fees, leverage, account setup)
- Rebalancing frequency

**Output:**
- Realistic trading account simulation
- Fee impact analysis
- Account rebalancing history
- Final performance metrics

## Configuration

Environment variables:

- `ENVIRONMENT`: Development/Production mode
- `LOG_LEVEL`: Logging level
- `MAX_FILE_SIZE`: Maximum upload file size
- `CORS_ORIGINS`: Allowed CORS origins

## Health Check

```bash
curl http://localhost:8000/api/v1/health
```

## Error Handling

The microservice provides detailed error responses with:
- Error codes
- Descriptive messages
- Validation details
- Request tracking IDs