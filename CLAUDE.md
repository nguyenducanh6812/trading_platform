# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with this repository.

## Project Overview

**Spring Boot 3.4.4 trading platform** with **Java 21**, **Spring Modulith** for modular architecture and **Camunda BPM** for workflow orchestration. Designed for cryptocurrency trading focusing on BTC and ETH.

**Architecture**: Module-based DDD structure designed for microservices-ready modules. Each module is self-contained with domain logic, application services, infrastructure, and interfaces.

## Essential Commands

### Database Setup (PostgreSQL)

**Option 1: Docker Compose (Recommended)**
```bash
# Start PostgreSQL container (preferred approach)
docker compose up -d postgres

# Verify container is running
docker compose ps
```

**Option 2: Manual Installation**
```bash
# Install PostgreSQL (if not using Docker)
# Ubuntu/Debian: sudo apt-get install postgresql postgresql-contrib
# macOS: brew install postgresql
# Windows: Download from https://www.postgresql.org/download/windows/

# Create database and user
sudo -u postgres psql
CREATE DATABASE trading_platform;
CREATE USER trading_user WITH ENCRYPTED PASSWORD 'trading_password';
GRANT ALL PRIVILEGES ON DATABASE trading_platform TO trading_user;
ALTER USER trading_user CREATEDB;
\q
```

**Database Configuration:**
- **Database**: `trading_platform`
- **Username**: `trading_user`
- **Password**: `trading_password`
- **Host**: `localhost`
- **Port**: `5432`

### Build & Run
```bash
# Build and run
./mvnw clean compile
./mvnw spring-boot:run

# Access points
http://localhost:8080/camunda (demo/demo)
http://localhost:8080/swagger-ui.html
```

### Key Technologies Stack
- **Spring Boot 3.4.4** + Java 21
- **Spring Modulith 1.2.3** for modular architecture
- **Camunda BPM 7.23.0** for workflow orchestration
- **OpenFeign** for HTTP clients + **MapStruct** for object mapping
- **SpringDoc OpenAPI** for API documentation
- **PostgreSQL** for persistence

## Architecture Overview

### DDD Module Structure
Each module follows **4-layer DDD architecture**:
- **Domain**: Entities, Value Objects, Services, Repository Contracts
- **Application**: Use Cases, Orchestration Services  
- **Infrastructure**: External APIs, Database, Repositories
- **Interface**: REST Controllers, Camunda Workers

### Current Implementation Status
✅ **Completed**: Market Data Module (full DDD implementation)
- Domain layer with `Price`, `OHLCV`, `TimeRange`, `DataQualityMetrics`
- Asset-specific repositories (BTC/ETH separate tables)
- Strategy pattern for external data clients (Bybit default, extensible)
- Bulk data processing with chunking for large historical datasets (2021-present)
- Type-safe enums and constants replacing hardcoded strings
- Professional rate limiting with Resilience4j
- Linear futures data support (not spot trading)
- Camunda external task worker for `fetch-instruments-data`
- REST API endpoints with comprehensive Swagger documentation

⏳ **Pending**: Portfolio optimization, Strategy execution, Forecasting, Backtesting, Reporting modules

## Critical Implementation Patterns

### Code Style Rules
**Use Lombok** for: JPA Entities, Service Classes, Configuration Classes
**Use Records** for: Value Objects, DTOs, Events, API Models

### Camunda External Task Workers - DDD Pattern

**❌ WRONG - Business Logic in Worker**:
```java
@ExternalTaskSubscription("predict-expected-return-arima")
public void handleTask(ExternalTask task, ExternalTaskService service) {
    // ❌ Worker handling business logic loops
    for (TradingInstrument instrument : instruments) {
        ForecastResponse response = executeARIMAForecastUseCase.execute(request);
        if (response.isSuccessful()) {
            successfulForecasts++;
        } else {
            // ❌ Complex business error analysis in worker
            if (response.errorMessage().contains("Insufficient data")) {
                hasCriticalErrors = true;
            }
        }
    }
    
    // ❌ Business decisions in worker
    if (hasCriticalErrors) {
        service.handleBpmnError(task, "CRITICAL_ERROR", errorMessage);
    }
}
```

**✅ CORRECT - Thin Orchestration Layer**:
```java
@ExternalTaskSubscription("predict-expected-return-arima")
public class ExecuteARIMAForecastTaskWorker implements ExternalTaskHandler {
    
    private final ExecuteARIMAForecastUseCase executeARIMAForecastUseCase;
    
    public void execute(ExternalTask task, ExternalTaskService service) {
        try {
            // ✅ Extract orchestration input only
            OrchestrationInput input = extractOrchestrationInput(task);
            
            // ✅ Delegate ALL business logic to use case/service
            Map<String, ForecastResponse> responses = executeBatchForecasts(input);
            BatchForecastResult batchResult = analyzeBatchResults(responses);
            
            // ✅ Simple orchestration logic only
            if (batchResult.hasCriticalErrors()) {
                service.handleBpmnError(task, "ARIMA_CRITICAL_ERROR", batchResult.criticalErrorMessage());
                return;
            }
            
            // ✅ Return orchestration data only (no business data)
            Map<String, Object> result = createOrchestrationOutput(input, batchResult);
            service.complete(task, result);
            
        } catch (InvalidProcessVariablesException e) {
            service.handleBpmnError(task, "INVALID_PROCESS_VARIABLES", e.getMessage());
        } catch (Exception e) {
            int retries = task.getRetries() != null ? task.getRetries() - 1 : 2;
            service.handleFailure(task, e.getMessage(), e.toString(), retries, 60000L);
        }
    }
    
    // ✅ Simple delegation to use case (no business logic)
    private Map<String, ForecastResponse> executeBatchForecasts(OrchestrationInput input) {
        // Delegate to use case - worker just orchestrates
        return useCase.executeBatchForecasts(input);
    }
}
```

### External Task Worker Rules
1. **Single Responsibility**: Orchestration only, not business logic
2. **Dependency**: Inject domain use cases, not application services
3. **Process Variables**: Only orchestration data (`executionId`, `taskCompleted`), never business data
4. **Error Handling**: `handleBpmnError()` for business errors, `handleFailure()` for technical errors
5. **No Business Logic**: Workers should NOT contain loops, conditionals, or business decisions
6. **Delegation Pattern**: Extract input → Delegate to use case → Handle errors → Return variables
7. **Batch Operations**: Use case handles multiple items, not worker loops
8. **Error Analysis**: Use case determines critical vs non-critical errors, not worker

### Critical Camunda Annotations

**❌ WRONG**:
```java
@ExternalTaskSubscription(
    topicName = "fetch-data",
    processDefinitionKey = "Process_Name",  // ❌ Invalid
    maxTasks = 1  // ❌ Invalid
)
```

**✅ CORRECT**:
```java
@ExternalTaskSubscription(
    topicName = "fetch-instruments-data",
    lockDuration = 300000L  // ✅ Only valid parameters
)
```

**Valid parameters**: `topicName`, `lockDuration`, `processDefinitionKey`, `includeExtensionProperties`, `variableNames`
**Invalid parameters**: `maxTasks`

### Pragmatic Constants Pattern

**❌ WRONG - Over-abstraction**:
```java
// ProcessVariables.java - Too many constants
public static final String TOPIC_FETCH_INSTRUMENTS_DATA = "fetch-instruments-data";
public static final String PROCESS_FETCH_INSTRUMENT_DATA = "Process_Fetch_Instrument_Data";
public static final long LOCK_DURATION_FETCH_DATA = 300000L;

// Worker - Hard to read, must jump to constants file
@ExternalTaskSubscription(
    topicName = TOPIC_FETCH_INSTRUMENTS_DATA,
    processDefinitionKey = PROCESS_FETCH_INSTRUMENT_DATA,
    lockDuration = LOCK_DURATION_FETCH_DATA
)
```

**✅ CORRECT - Pragmatic approach**:
```java
// ProcessVariables.java - Only shared values
public static final String INSTRUMENT_CODES = "instrumentCodes";
public static final String START_DATE = "startDate";
public static final String END_DATE = "endDate";
public static final String LAUNCH_NEW_INSTRUMENTS = "launchNewInstruments";
public static final String RESOURCE = "resource";

// Worker - Readable with inline unique values, constants for shared variables
@ExternalTaskSubscription(
    topicName = "fetch-instruments-data",                    // Inline - unique to worker
    processDefinitionKey = "Process_Fetch_Instrument_Data",  // Inline - unique to worker
    lockDuration = 300000,                                   // Inline - unique to worker
    variableNames = {INSTRUMENT_CODES, START_DATE, END_DATE, LAUNCH_NEW_INSTRUMENTS, RESOURCE}  // Constants - shared
)
```

**Rules for Constants**:
- ✅ **Use constants for**: Process variables shared across multiple workers/components
- ❌ **Don't use constants for**: Topic names, process definition keys, lock durations unique to one worker
- ✅ **Principle**: "Make constants only when there's actual reuse, not just because you can"

### Worker Architecture Principles

**❌ WRONG - Worker handles business complexity**:
- Loops over business objects (instruments, forecasts, etc.)
- Complex conditional logic for business rules
- Error categorization and analysis
- Business decision making (success/failure determination)
- Data transformation and processing

**✅ CORRECT - Worker as thin orchestration layer**:
- Extracts input from process variables
- Single delegation call to use case/service
- Simple orchestration checks (not business logic)
- Returns process flow variables only
- Error handling for technical vs business failures

**Key Pattern**:
```java
// Worker method structure
public void execute(ExternalTask task, ExternalTaskService service) {
    try {
        Input input = extractInput(task);           // Extract
        Result result = useCase.execute(input);     // Delegate  
        if (result.hasCriticalErrors()) {           // Simple check
            service.handleBpmnError(...);          // Handle
        } else {
            service.complete(task, toVariables(result)); // Return
        }
    } catch (ValidationException e) {
        service.handleBpmnError(...);              // Business error
    } catch (Exception e) {
        service.handleFailure(...);               // Technical error
    }
}
```

### Docker Compose Configuration Best Practices

**❌ WRONG - Mismatched Configuration**:
```yaml
# compose.yaml - Wrong database settings
services:
  postgres:
    environment:
      - 'POSTGRES_DB=mydatabase'      # ❌ Different from application.yaml
      - 'POSTGRES_USER=myuser'        # ❌ Different from application.yaml
      - 'POSTGRES_PASSWORD=secret'    # ❌ Different from application.yaml
    ports:
      - '5432'                        # ❌ No host port mapping

# application.yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/trading_platform  # ❌ Mismatch!
    username: trading_user                                   # ❌ Mismatch!
    password: trading_password                               # ❌ Mismatch!
```

**✅ CORRECT - Consistent Configuration**:
```yaml
# compose.yaml - Matches application.yaml exactly
services:
  postgres:
    image: 'postgres:latest'
    environment:
      - 'POSTGRES_DB=trading_platform'     # ✅ Matches application.yaml
      - 'POSTGRES_USER=trading_user'       # ✅ Matches application.yaml
      - 'POSTGRES_PASSWORD=trading_password' # ✅ Matches application.yaml
    ports:
      - '5432:5432'                        # ✅ Explicit host:container mapping
    volumes:
      - postgres_data:/var/lib/postgresql/data  # ✅ Data persistence

volumes:
  postgres_data:                             # ✅ Named volume for data

# application.yaml - Matches compose.yaml exactly
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/trading_platform  # ✅ Match!
    username: trading_user                                   # ✅ Match!
    password: trading_password                               # ✅ Match!
```

**Configuration Rules**:
- ✅ **Database name**: Must match between `POSTGRES_DB` and JDBC URL
- ✅ **Username**: Must match between `POSTGRES_USER` and `spring.datasource.username`
- ✅ **Password**: Must match between `POSTGRES_PASSWORD` and `spring.datasource.password`
- ✅ **Port mapping**: Use explicit `host:container` format (`5432:5432`)
- ✅ **Data persistence**: Always use named volumes for database data
- ❌ **Common mistake**: Copy-paste Docker Compose templates without updating credentials

## Key Dependencies

### Camunda External Task Client
```xml
<dependency>
    <groupId>org.camunda.bpm</groupId>
    <artifactId>camunda-external-task-client</artifactId>
</dependency>
<dependency>
    <groupId>org.camunda.bpm</groupId>
    <artifactId>camunda-external-task-client-spring</artifactId>
</dependency>
```

### Configuration
```yaml
camunda:
  bpm:
    client:
      base-url: http://localhost:8080/engine-rest
      worker-id: trading-platform-worker
      max-tasks: 5
      lock-duration: 300000
```

## Implementation Guidelines

1. **Follow DDD Architecture**: 4-layer structure (Domain, Application, Infrastructure, Interface)
2. **Spring Modulith**: Create `package-info.java` for module boundaries
3. **Asset-Specific Design**: Separate repositories/tables for BTC, ETH
4. **Event-Driven Communication**: Use Spring Modulith events between modules
5. **Microservices Ready**: Structure modules for future extraction
6. **Strategy Pattern**: Use `ExternalDataClientFactory` for multiple data source providers
7. **Bulk Processing**: Process large datasets in chunks (90-day chunks, 100-record batches) to avoid memory issues
8. **Type Safety**: Use enums for TradingInstrument, DataSourceType, BybitMarketType instead of hardcoded strings
9. **Pragmatic Constants**: Use constants only for truly shared values. Worker-specific values like topic names and lock durations should remain inline for better readability. Only centralize process variables that are reused across multiple workers or components.
10. **Rate Limiting**: Use Resilience4j for professional rate limiting instead of Thread.sleep
11. **Database Configuration Consistency**: Ensure `application.yaml` and `compose.yaml` database settings match exactly
12. **Spring Modulith Named Interfaces**: Use `@NamedInterface` annotation on both package-info.java and individual classes/enums to expose types across modules. Both the module package and the specific type need the annotation for proper cross-module access.

## Trading Model Overview

**3-Phase Model** (see `Model Formula.md`):
- **Phase 1**: Maximum Sharpe Ratio Optimization (Long/Short/Hedging strategies)
- **Phase 2**: Risk Preference Adjustment (Risk Averse/Neutral/Lover)
- **Phase 3**: ARIMA Forecasting (Stationarity testing, model selection)

**Specifications**: Daily/Weekly/Monthly rebalancing, 4% risk-free rate, BTC (0.001 min), ETH (0.01 min)

## Current Workflow

**Fetch Instrument Data Process** (`Process_Fetch_Instrument_Data`):
- Topic: `fetch-instruments-data`
- Instruments: BTC, ETH
- Date Range: 2021-03-15 to present
- Data Sources: Strategy pattern with Bybit default (configurable via `resource` parameter)

**Process Variables for Camunda**:
- `instrumentCodes`: List<String> (required) - e.g., ["BTC", "ETH"]
- `startDate`: String (required if launchNewInstruments=false) - format: "YYYY-MM-DD"
- `endDate`: String (required if launchNewInstruments=false) - format: "YYYY-MM-DD"  
- `launchNewInstruments`: Boolean (optional, default: false) - uses default date range if true
- `resource`: String (optional, default: "bybit") - data source provider

**External Data Strategy Pattern**:
- `ExternalDataClientFactory`: Factory for selecting data source strategies
- `ExternalDataClientStrategy`: Common interface for all data providers
- `BybitDataClientStrategy`: Default implementation for Bybit API
- Easy to add new providers (Binance, Coinbase, etc.) by implementing the strategy interface

**Bulk Data Processing Architecture**:
- **Time Chunking**: Large date ranges split into 90-day chunks to avoid memory issues
- **Batch Processing**: Each chunk processed in 100-record batches with validation
- **Memory Management**: Periodic saves, garbage collection, and progress tracking
- **Fault Tolerance**: Individual chunk/batch failures don't stop entire process
- **Performance**: Reduced API request size (200 records/request) with rate limiting

**Available Endpoints**:
- `POST /api/v1/market-data/fetch-btc-eth-historical`
- `GET /api/v1/market-data/instruments/{symbol}`
- `GET /api/v1/market-data/instruments/{symbol}/data-sufficiency`