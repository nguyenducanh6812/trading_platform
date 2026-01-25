# Trading Platform Architecture

## 🏗️ Layer Architecture (DDD + Hexagonal)

```
┌─────────────────────────────────────────────────┐
│              CLIENT (HTTP/UI)                    │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│         INTERFACES LAYER (Adapters)              │
│  • REST Controllers                              │
│  • Camunda Workers                               │
│  • DTOs (Request/Response)                       │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│        APPLICATION LAYER (Use Cases)             │
│  • Application Services                          │
│  • Use Cases (Business Workflows)                │
│  • DTOs Conversion                               │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│          DOMAIN LAYER (Core Business)            │
│  • Entities (Market, MarketInstrument)           │
│  • Value Objects (OHLCV, Price)                  │
│  • Repository Interfaces (Ports)                 │
│  • Domain Services                               │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│      INFRASTRUCTURE LAYER (Adapters)             │
│  • JPA Entities                                  │
│  • Repository Implementations                    │
│  • External API Clients                          │
│  • Mappers (Entity ↔ Domain)                     │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│         EXTERNAL SYSTEMS                         │
│  • PostgreSQL Database                           │
│  • Bybit API                                     │
│  • Camunda BPM Engine                            │
└──────────────────────────────────────────────────┘
```

---

## 📊 Request Flow Example

**Endpoint:** `GET /api/v1/market-data/markets/LINEAR/instruments`

```
1. Client
   ↓ HTTP Request
2. MarketDataController.java (Interfaces)
   ↓ Call method
3. MarketDataApplicationService.java (Application)
   ↓ Delegate
4. GetInstrumentsByMarketUseCase.java (Application)
   ↓ Use repository interfaces
5. MarketRepository.java (Domain - Interface)
   MarketDataRepository.java (Domain - Interface)
   ↓ Implemented by
6. MarketRepositoryImpl.java (Infrastructure)
   MarketDataRepositoryImpl.java (Infrastructure)
   ↓ Use JPA
7. MarketInstrumentJpaRepository.java (Infrastructure)
   ↓ SQL Query
8. PostgreSQL Database
   ↓ JPA Entities
9. MarketEntity → Market (Domain)
   MarketInstrumentEntity → MarketInstrument (Domain)
   ↓ DTO Conversion
10. InstrumentsByMarketResponse (DTO)
    ↓ JSON Response
11. Client
```

---

## 🎯 Key Architectural Patterns

### 1. **Dependency Inversion Principle**

```
Application Layer ──depends on──> Repository Interface (Domain)
                                          ↑
                                          │ implements
                                          │
                      Infrastructure ────┘
```

**Benefit:** Business logic independent of database/framework

### 2. **Separation of Concerns - Three Object Types**

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────┐
│ Domain Object   │     │ JPA Entity       │     │ DTO         │
├─────────────────┤     ├──────────────────┤     ├─────────────┤
│ • Business      │ ←→  │ • @Entity        │ ←→  │ • API       │
│   logic         │     │ • Database       │     │   contract  │
│ • Validation    │     │   mapping        │     │ • JSON      │
│ • Rules         │     │ • Audit fields   │     └─────────────┘
└─────────────────┘     └──────────────────┘

Example: MarketInstrument ← MarketInstrumentEntity ← InstrumentInfo
```

### 3. **Strategy Pattern - Asset-Specific Repositories**

```
AssetSpecificRepositoryFactory
         │
         ├──> BtcExpectedReturnPredictionRepository (btc_expected_return_prediction)
         │
         └──> EthExpectedReturnPredictionRepository (eth_expected_return_prediction)
```

**Benefit:** Each crypto has optimized table schema

### 4. **Ports & Adapters (Hexagonal)**

```
        ┌─────────────────────────┐
        │   DOMAIN (Core)         │
        │   Business Logic        │
        └──────────┬──────────────┘
                   │
        ┌──────────┴──────────────┐
        │                         │
    ┌───▼────┐              ┌────▼───┐
    │ Input  │              │ Output │
    │ Ports  │              │ Ports  │
    └───┬────┘              └────┬───┘
        │                         │
    ┌───▼────────┐          ┌────▼──────────┐
    │ REST       │          │ Repository    │
    │ Camunda    │          │ External API  │
    │ (Adapters) │          │ (Adapters)    │
    └────────────┘          └───────────────┘
```

---

## 📁 Module Structure

```
marketdata/
├── domain/
│   ├── entities/
│   │   ├── Market.java                    ← Aggregate Root
│   │   └── MarketInstrument.java          ← Aggregate Root
│   ├── valueobjects/
│   │   ├── OHLCV.java                     ← Value Object
│   │   └── DataQualityMetrics.java        ← Value Object
│   └── repositories/
│       ├── MarketRepository.java          ← Port (Interface)
│       └── MarketDataRepository.java      ← Port (Interface)
│
├── application/
│   ├── usecases/
│   │   ├── GetAllMarketsUseCase.java      ← Business Workflow
│   │   └── GetInstrumentsByMarketUseCase.java
│   ├── services/
│   │   └── MarketDataApplicationService.java ← Orchestrator
│   └── dto/
│       ├── MarketResponse.java            ← API DTO
│       └── InstrumentsByMarketResponse.java
│
├── infrastructure/
│   ├── persistence/
│   │   ├── entities/
│   │   │   ├── MarketEntity.java          ← JPA Entity
│   │   │   └── MarketInstrumentEntity.java
│   │   ├── repositories/
│   │   │   ├── MarketRepositoryImpl.java  ← Adapter
│   │   │   └── MarketInstrumentJpaRepository.java
│   │   └── mappers/
│   │       └── MarketDataMapper.java      ← Entity ↔ Domain
│   └── external/
│       └── BybitClient.java               ← External API Adapter
│
└── interfaces/
    ├── rest/
    │   └── MarketDataController.java      ← REST Adapter
    ├── camunda/
    │   └── FetchInstrumentDataTaskWorker.java ← Camunda Adapter
    └── api/
        └── MarketDataPort.java            ← Cross-Module Interface
```

---

## ✅ Architecture Quality

### **Strengths**

| Aspect | Benefit |
|--------|---------|
| **Testability** | Each layer tested independently with mocks |
| **Maintainability** | Changes isolated to specific layers |
| **Framework Independence** | Domain doesn't know Spring/JPA |
| **Database Independence** | Can swap PostgreSQL → MongoDB |
| **API Evolution** | Add GraphQL without touching domain |
| **Team Scalability** | Modules work independently |
| **Microservices Ready** | Modules → Services migration path |

### **DDD Tactical Patterns Applied**

- ✅ **Entities:** Market, MarketInstrument, Portfolio (with identity)
- ✅ **Value Objects:** OHLCV, Price, Capital (immutable, no identity)
- ✅ **Aggregate Roots:** Portfolio manages Position/Trade
- ✅ **Repository Pattern:** Data persistence abstraction
- ✅ **Domain Services:** ARIMAPipeline (stateless business logic)
- ✅ **Factory Pattern:** Market.linear(), Portfolio.createWithMPT()

---

## 🔄 Data Flow Summary

```
HTTP Request
    ↓
Controller (HTTP → Java)
    ↓
Application Service (Orchestrate)
    ↓
Use Case (Business Workflow)
    ↓
Domain Repository Interface (Contract)
    ↓
Repository Implementation (Adapter)
    ↓
JPA Repository (Spring Data)
    ↓
Database
    ↓
JPA Entity → Domain Object (Mapper)
    ↓
Domain Object → DTO (Mapper)
    ↓
DTO → JSON (Spring MVC)
    ↓
HTTP Response
```

---

## 🎓 Key Takeaways

1. **Domain Layer = Core:** Protected from external changes
2. **Dependency Direction:** Always points inward toward domain
3. **Three Object Types:** Domain ≠ JPA Entity ≠ DTO
4. **Ports & Adapters:** Domain defines interfaces, infrastructure implements
5. **Spring Modulith:** `@NamedInterface` controls cross-module access

---

**Architecture Pattern:** DDD + Hexagonal + Spring Modulith
**Ready for:** Enterprise scale, team collaboration, microservices migration
