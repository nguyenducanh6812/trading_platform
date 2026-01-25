# Trading Platform

A Spring Boot-based cryptocurrency trading platform implementing Modern Portfolio Theory (MPT) with ARIMA forecasting and Camunda BPM workflow orchestration.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Architecture](#architecture)
- [API Documentation](#api-documentation)
- [Development](#development)
- [Logging Strategy](#logging-strategy)
- [Cloud Deployment](#cloud-deployment)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

Before running the application, ensure you have the following installed:

- **Java 21** or higher
- **Maven 3.8+** (or use the included Maven wrapper `./mvnw`)
- **Docker Desktop** (for PostgreSQL database)

---

## Quick Start

### 1. Start the Database

The application requires PostgreSQL to be running with the schema initialized.

**Important:** For the first run or after database changes, start with a fresh database:

```bash
# Stop and remove existing database (if any)
docker compose down -v

# Start PostgreSQL with automatic schema initialization
docker compose up -d postgres

# Wait for initialization to complete (about 5-10 seconds)
# The init script automatically creates the trading_platform schema

# Verify the container is running
docker compose ps
```

**Expected output:**
```
NAME                  IMAGE               STATUS          PORTS
postgres              postgres:latest     Up X seconds    0.0.0.0:5432->5432/tcp
```

**What happens automatically:**
1. PostgreSQL container starts
2. `deployment/database/init-database.sql` executes automatically
3. Creates `trading_platform` schema
4. Grants permissions to `trading_user`

**For subsequent runs** (if database already exists):
```bash
# Just start the container
docker compose up -d postgres
```

### 2. Start the Application

Once PostgreSQL is running, start the Spring Boot application:

```bash
# Using Maven wrapper (recommended)
./mvnw spring-boot:run

# Or using installed Maven
mvn spring-boot:run
```

The application will:
- Connect to PostgreSQL at `localhost:5432`
- Run Liquibase database migrations automatically
- Start the embedded Tomcat server on port `8080`
- Initialize Camunda BPM engine

### 3. Access the Application

Once started, you can access:

| Service | URL | Credentials |
|---------|-----|-------------|
| **Camunda Cockpit** | http://localhost:8080/camunda | Username: `demo`<br>Password: `demo` |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | N/A |
| **API Docs (JSON)** | http://localhost:8080/api-docs | N/A |

---

## Architecture

### Technology Stack

- **Spring Boot 3.4.4** - Application framework
- **Java 21** - Programming language
- **Spring Modulith 1.2.3** - Modular architecture
- **Camunda BPM 7.23.0** - Workflow orchestration
- **PostgreSQL** - Primary database
- **Liquibase** - Database migration management
- **OpenFeign** - Declarative HTTP clients
- **MapStruct** - Object mapping
- **Resilience4j** - Rate limiting and fault tolerance

### Module Structure

The application follows **Domain-Driven Design (DDD)** with Spring Modulith:

```
trading_platform/
├── shared/                 # Shared value objects (TradingInstrument, OHLCV, etc.)
├── marketdata/             # Market data fetching and storage
│   ├── domain/            # Pure business logic (INTERNAL)
│   ├── application/       # Use cases and orchestration (INTERNAL)
│   ├── infrastructure/    # External integrations (INTERNAL)
│   └── interfaces/        # Public API (PUBLIC)
│       └── api/           # Cross-module API contracts
├── forecasting/            # ARIMA-based forecasting
│   ├── domain/            # ARIMA models and calculations (INTERNAL)
│   ├── application/       # Forecast use cases (INTERNAL)
│   ├── infrastructure/    # Persistence and data loading (INTERNAL)
│   └── interfaces/        # Public API (PUBLIC)
│       └── api/           # Cross-module API contracts
└── backtesting/            # Backtest validation and orchestration
    ├── domain/            # Validation logic (INTERNAL)
    ├── application/       # Validation use cases (INTERNAL)
    └── interfaces/        # Public API (PUBLIC)
```

**Architectural Principles:**
- ✅ Each module is self-contained and can be extracted as a microservice
- ✅ Only `interfaces/api` packages are exposed to other modules
- ✅ Domain, application, and infrastructure layers are internal
- ✅ DTOs prevent domain entity leakage across modules
- ✅ Anti-corruption layer via port interfaces

---

## API Documentation

### Market Data API

Fetch and manage cryptocurrency market data:

```bash
# Fetch historical data for BTC and ETH (default: 2021-03-15 to present)
POST http://localhost:8080/api/v1/market-data/fetch-btc-eth-historical

# Fetch custom historical data
POST http://localhost:8080/api/v1/market-data/fetch-historical
Content-Type: application/json

{
  "instruments": ["BTC", "ETH"],
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "resource": "bybit"
}

# Get instrument information
GET http://localhost:8080/api/v1/market-data/instruments/BTC

# Check data sufficiency
GET http://localhost:8080/api/v1/market-data/instruments/BTC/data-sufficiency
```

### Forecasting API

Execute ARIMA-based expected return predictions:

```bash
# Execute ARIMA forecast
POST http://localhost:8080/api/v1/forecasting/execute
Content-Type: application/json

{
  "instrumentCode": "BTC",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "modelVersion": "v1.0"
}
```

### Camunda Workflows

Start backtest process via Camunda:

1. Access Camunda Cockpit: http://localhost:8080/camunda
2. Navigate to **Tasklist**
3. Start **Process_Back_Test_MPT** process
4. Fill in the form:
   - Instrument Pair: e.g., "BTC,ETH"
   - Start Date: e.g., "2024-01-01"
   - End Date: e.g., "2024-12-31"
   - Model Version: e.g., "v1.0"

---

## Development

### Build the Project

```bash
# Clean and compile
./mvnw clean compile

# Run tests
./mvnw test

# Package as JAR
./mvnw package
```

### Database Management

```bash
# Start PostgreSQL (if already initialized)
docker compose up -d postgres

# Stop PostgreSQL (keeps data)
docker compose down

# Fresh database setup (destroys all data and recreates)
docker compose down -v && docker compose up -d postgres

# View PostgreSQL logs
docker compose logs -f postgres

# Access PostgreSQL CLI
docker exec -it trading_platform-postgres-1 psql -U trading_user -d trading_platform

# Verify schema exists
docker exec -it trading_platform-postgres-1 psql -U trading_user -d trading_platform -c "\dn"
```

**Database Initialization:**
- Schema creation: Automatic via `deployment/database/init-database.sql`
- Table creation: Automatic via Liquibase migrations
- All objects in `trading_platform` schema for consistency

### Database Connection Details

| Property | Value |
|----------|-------|
| **Host** | localhost |
| **Port** | 5432 |
| **Database** | trading_platform |
| **Username** | trading_user |
| **Password** | trading_password |

### Project Structure

```
trading_platform/
├── src/main/java/              # Java source code
├── src/main/resources/         # Configuration and resources
│   ├── application.yaml        # Application configuration
│   ├── db/changelog/           # Liquibase migrations
│   └── *.bpmn                  # Camunda workflow definitions
├── src/test/java/              # Test code
├── compose.yaml                # Docker Compose configuration
├── pom.xml                     # Maven dependencies
├── CLAUDE.md                   # Claude Code AI assistant guide
└── README.md                   # This file
```

---

## Logging Strategy

### Environment-Based Logging

The application uses **Spring Profiles** to configure logging differently for each environment:

#### Local Development (Default)
```bash
./mvnw spring-boot:run
# Logs to console only (INFO level)
```

#### Development Profile (with file logging)
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# Logs to both console and file: logs/trading-platform.log
# DEBUG level for application code
```

#### Production Profile (cloud-ready)
```bash
java -jar target/trading-platform.jar --spring.profiles.active=prod
# Logs to stdout/stderr in JSON format
# INFO level, optimized for cloud logging services
```

### Why Different Logging for Cloud?

❌ **Don't use file logging in production:**
- Container filesystems are ephemeral (logs lost on restart)
- Can't aggregate logs from multiple instances
- Wastes disk space and I/O
- Hard to search and monitor

✅ **Use stdout/stderr in production:**
- Cloud platforms capture logs automatically
- Centralized logging (CloudWatch, Azure Monitor, GCP Logging)
- Easy to set up alerts and dashboards
- Works with log aggregation tools (ELK, Datadog, Splunk)

### Log Configuration Files

| File | Purpose |
|------|---------|
| `application.yaml` | Default (console only, INFO level) |
| `application-dev.yaml` | Development (file + console, DEBUG level) |
| `application-prod.yaml` | Production (stdout, JSON format, INFO level) |

---

## Cloud Deployment

### Container Logging Best Practices

**1. Use stdout/stderr (already configured with `prod` profile)**
```bash
# Docker
docker run -e SPRING_PROFILES_ACTIVE=prod trading-platform

# Kubernetes
kubectl logs deployment/trading-platform --follow

# AWS ECS/Fargate
# Logs automatically sent to CloudWatch
```

**2. Structured Logging (JSON format)**

Production profile outputs JSON logs for better parsing:
```json
{"timestamp":"2025-12-21T07:30:00.000+0000","level":"INFO","thread":"main","logger":"c.a.t.TradingPlatformApplication","message":"Started TradingPlatformApplication"}
```

**3. Log Aggregation Services**

Recommended integrations:
- **AWS**: CloudWatch Logs + CloudWatch Insights
- **Azure**: Azure Monitor + Log Analytics
- **GCP**: Cloud Logging (formerly Stackdriver)
- **Kubernetes**: EFK Stack (Elasticsearch, Fluentd, Kibana)
- **Third-party**: Datadog, New Relic, Splunk

**4. Environment Variables**

Override logging in deployment:
```yaml
# Kubernetes ConfigMap/Deployment
env:
  - name: SPRING_PROFILES_ACTIVE
    value: prod
  - name: LOGGING_LEVEL_COM_AHD_TRADING_PLATFORM
    value: DEBUG  # Temporary debugging
```

### Docker Deployment

```dockerfile
# Dockerfile example
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/trading-platform.jar app.jar

# Use production profile
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: trading-platform
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: app
        image: trading-platform:latest
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: prod
        # Logs automatically captured by Kubernetes
```

---

## Troubleshooting

### PostgreSQL Not Starting

**Problem:** `docker compose up -d postgres` fails

**Solution:**
1. Ensure Docker Desktop is running
2. Check if port 5432 is already in use:
   ```bash
   # Windows
   netstat -ano | findstr :5432

   # Linux/Mac
   lsof -i :5432
   ```
3. Stop any existing PostgreSQL instances
4. Start with fresh database: `docker compose down -v && docker compose up -d postgres`

### Schema "trading_platform" Does Not Exist

**Problem:** Application fails with "schema trading_platform does not exist"

**Solution:**
The init script didn't run. Start with a fresh database:
```bash
# Destroy and recreate database
docker compose down -v
docker compose up -d postgres

# Wait 10 seconds for init script to complete
# Then start application
./mvnw spring-boot:run
```

### Application Fails to Connect to Database

**Problem:** Application logs show connection errors

**Solutions:**
1. Verify PostgreSQL is running: `docker compose ps`
2. Check database logs: `docker compose logs postgres`
3. Ensure credentials match in `application.yaml` and `compose.yaml`
4. Test connection manually:
   ```bash
   docker exec -it trading_platform-postgres-1 psql -U trading_user -d trading_platform
   ```

### Port 8080 Already in Use

**Problem:** Application fails to start because port 8080 is occupied

**Solutions:**
1. Find and kill the process using port 8080:
   ```bash
   # Windows
   netstat -ano | findstr :8080
   taskkill /PID <PID> /F

   # Linux/Mac
   lsof -ti:8080 | xargs kill -9
   ```
2. Or change the port in `application.yaml`:
   ```yaml
   server:
     port: 8081
   ```

### Liquibase Migration Errors

**Problem:** Database schema migration fails

**Solutions:**
1. Check Liquibase changelog files in `src/main/resources/db/changelog/`
2. Verify database connection and credentials
3. If needed, drop and recreate the schema:
   ```sql
   DROP SCHEMA trading_platform CASCADE;
   CREATE SCHEMA trading_platform;
   ```
4. Restart the application to re-run migrations

### Camunda Dashboard Not Loading

**Problem:** http://localhost:8080/camunda returns 404

**Solutions:**
1. Wait for application to fully start (check logs)
2. Verify Camunda autoconfiguration in logs
3. Try accessing: http://localhost:8080/camunda/app/welcome/default/

---

## License

Copyright © 2025 Trading Platform Team. All rights reserved.

---

## Support

For issues and questions:
- Check the [Troubleshooting](#troubleshooting) section
- Review `CLAUDE.md` for development guidelines
- Check application logs for detailed error messages
