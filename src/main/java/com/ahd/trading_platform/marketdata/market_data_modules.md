# Market Data Module - Refined DDD Structure

## Core Module Architecture

```
modules/market_data_module/
│
├── domain/                                    # Pure Business Logic
│   ├── entities/
│   │   └── market_instrument.py              # Aggregate Root
│   │
│   ├── value_objects/
│   │   ├── price.py
│   │   ├── ohlcv.py
│   │   ├── time_range.py
│   │   └── data_quality_metrics.py
│   │
│   ├── repositories/                          # Abstract Contracts
│   │   └── market_data_repository.py
│   │
│   ├── services/                             # Domain Services
│   │   ├── price_normalization_service.py
│   │   ├── data_validation_service.py
│   │   └── quality_assessment_service.py
│   │
│   └── events/
│       ├── market_data_updated_event.py
│       └── data_quality_alert_event.py
│
├── application/                              # Use Cases & Workflows
│   ├── use_cases/
│   │   ├── get_historical_data.py
│   │   ├── get_latest_prices.py
│   │   └── assess_data_quality.py
│   │
│   ├── workflows/                            # Temporal Workflows
│   │   ├── data_sync_workflow.py
│   │   ├── quality_monitoring_workflow.py
│   │   └── historical_backfill_workflow.py
│   │
│   ├── activities/                           # Temporal Activities
│   │   ├── fetch_exchange_data_activity.py
│   │   ├── validate_data_activity.py
│   │   └── store_data_activity.py
│   │
│   └── dto/
│       ├── sync_request_dto.py
│       └── market_data_response_dto.py
│
├── infrastructure/                           # Technical Implementation
│   ├── persistence/
│   │   ├── repositories/
│   │   │   ├── btc_market_data_repository.py
│   │   │   ├── eth_market_data_repository.py
│   │   │   └── market_data_repository_factory.py
│   │   │
│   │   ├── models/                          # SQLAlchemy Models
│   │   │   ├── btc_market_data_model.py
│   │   │   ├── eth_market_data_model.py
│   │   │   └── market_instrument_model.py
│   │   │
│   │   └── migrations/                      # Database Migrations
│   │       └── alembic/
│   │
│   ├── external_apis/                       # Exchange Clients
│   │   ├── binance_client.py
│   │   ├── coinbase_client.py
│   │   └── exchange_client_factory.py
│   │
│   ├── camunda_workflow/                            # Camunda Infrastructure
│   │   ├── camunda_worker.py
│   │
│   └── monitoring/
│       ├── metrics_collector.py
│       └── health_checker.py
│
├── interfaces/                              # External APIs
│   ├── rest_api/
│   │   ├── controllers/
│   │   │   ├── market_data_controller.py
│   │   │   └── data_sync_controller.py
│   │   │
│   │   └── schemas/
│   │       └── api_schemas.py
│   │
│   └── cli/
│       └── market_data_commands.py
│
└── config/
    ├── database_config.py
    └── exchange_config.py
```

## Layer Responsibilities

### Domain Layer
**Purpose**: Contains pure business logic without external dependencies

**Key Components**:
- **MarketInstrument** (Aggregate Root): Represents tradeable assets (BTC, ETH, etc.)
- **Value Objects**: Price, OHLCV, TimeRange - immutable data structures
- **Domain Services**: Cross-entity business logic (price normalization, validation)
- **Repository Contracts**: Abstract interfaces for data access

**Data Flow**: Domain events trigger when market data is updated or quality issues detected

### Application Layer  
**Purpose**: Orchestrates business processes and coordinates workflows

**Key Components**:
- **Use Cases**: Simple operations like retrieving historical data
- **Camunda Workflows**: Complex orchestration (sync processes, monitoring)
- **DTOs**: Data transfer between layers

**Data Flow**: Use cases coordinate domain services, workflows orchestrate activities

### Infrastructure Layer
**Purpose**: Implements technical concerns and external integrations

**Key Components**:
- **Asset-Specific Repositories**: BTC/ETH repositories with optimized storage
- **SQLAlchemy Models**: Database schema for each asset type
- **Exchange Clients**: API integrations with rate limiting
- **Temporal Infrastructure**: Workflow execution environment

**Data Flow**: Repositories route to asset-specific tables, activities call external APIs

### Interfaces Layer
**Purpose**: Exposes functionality to external consumers

**Key Components**:
- **REST Controllers**: HTTP endpoints for market data access
- **CLI Commands**: Command-line interface for operations
- **API Schemas**: Request/response validation

**Data Flow**: Controllers validate input, delegate to use cases/workflows

## Asset-Specific Storage Strategy

### Performance-Driven Table Design
**Rationale**: Portfolio-focused queries (BTC+ETH only) avoid scanning irrelevant data from other assets. Each table optimized for specific asset characteristics and query patterns.

### Repository Pattern Implementation
- **MarketDataRepositoryFactory**: Routes to asset-specific repositories based on symbol
- **BtcMarketDataRepository**: Bitcoin-optimized with mining/whale transaction tracking
- **EthMarketDataRepository**: Ethereum-specific with gas prices, DeFi TVL, staking metrics
- **AdaMarketDataRepository**: Cardano-specific with epoch data, staking ratios

### Database Schema Strategy
```
Tables per Asset (Performance Optimized):
├── btc_market_data           # Bitcoin: mining hash rate, whale transactions
├── eth_market_data           # Ethereum: gas prices, DeFi metrics, L2 data
├── ada_market_data           # Cardano: epoch info, staking ratios, pool data
├── market_instruments        # Asset metadata and trading specifications
└── cross_asset_correlations  # Market-wide analysis (materialized view)
```

### Schema Evolution & Maintenance
- **Automated Table Creation**: New asset tables generated from templates
- **Migration Scripts**: Common field additions applied across all asset tables
- **Asset-Specific Indexing**: Optimized for each cryptocurrency's query patterns
- **Independent Scaling**: Different retention policies and archiving per asset

## Temporal Workflow Integration

### Workflow Orchestration
- **DataSyncWorkflow**: Coordinates multi-exchange data fetching
- **QualityMonitoringWorkflow**: Continuous data quality assessment
- **HistoricalBackfillWorkflow**: Recovers missing historical data

### Activity Implementation  
- **FetchExchangeDataActivity**: Calls external exchange APIs
- **ValidateDataActivity**: Runs domain validation services
- **StoreDataActivity**: Persists to asset-specific repositories

### Error Handling & Resilience
- Temporal provides built-in retry, timeout, and failure handling
- Activities handle rate limiting and circuit breaker patterns
- Workflows coordinate recovery from partial failures

## Data Flow Overview

### Write Path (Data Ingestion)
1. **Temporal Workflow** triggers sync process
2. **Fetch Activity** retrieves data from exchanges
3. **Validation Activity** applies domain rules
4. **Store Activity** routes to asset-specific repository
5. **Domain Events** notify other modules of updates

### Read Path (Data Access)
1. **REST Controller** receives request
2. **Use Case** validates and coordinates
3. **Repository Factory** routes to correct asset repository
4. **SQLAlchemy Model** queries optimized table
5. **Response** converts to domain objects

### Quality Monitoring Path
1. **Quality Workflow** runs on schedule
2. **Assessment Activity** analyzes recent data
3. **Domain Service** calculates quality metrics
4. **Alert Events** trigger notifications if thresholds breached

## Production Architecture Assessment

### Validated Design Decisions
**Asset-Specific Tables**: Performance-critical decision for portfolio queries. Selecting from BTC+ETH tables only (vs scanning unified table with all assets) provides significant query optimization and monitoring granularity.

**Repository Factory Pattern**: Appropriate here due to genuinely different storage characteristics per asset and asset-specific optimization requirements.

**Temporal Integration**: Workflows provide essential orchestration for complex multi-exchange sync operations with built-in retry and failure handling.

### Production Readiness Enhancements
- **Schema Evolution Strategy**: Template-based migration system for new assets
- **Cross-Asset Query Support**: Materialized views for market-wide analysis  
- **Asset-Specific Monitoring**: Independent metrics and alerting per cryptocurrency
- **Automated Table Management**: New asset onboarding through database migrations

This structure prioritizes domain-driven design principles while maintaining practical implementation concerns for a production crypto trading platform.