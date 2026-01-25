# Swagger/SpringDoc Configuration Best Practices

## Overview

This guide documents best practices for configuring Swagger/SpringDoc OpenAPI documentation in the Trading Platform. Proper configuration ensures APIs are organized logically and easy to navigate for developers.

## Table of Contents

- [Key Principles](#key-principles)
- [Configuration Structure](#configuration-structure)
- [Path Pattern Best Practices](#path-pattern-best-practices)
- [Common Mistakes](#common-mistakes)
- [Controller Design](#controller-design)
- [Verification](#verification)

---

## Key Principles

### 1. **Specific Over Generic**
- Use specific path patterns for each API group
- Avoid overly broad patterns like `/api/**` that catch everything
- Each group should have a unique, non-overlapping path prefix

### 2. **Consistent Versioning**
- Include API version in the path: `/api/v1/resource/**`
- Makes it easy to introduce breaking changes in future versions
- Clear versioning strategy for clients

### 3. **Module-Based Organization**
- One API group per bounded context/module
- Aligns with Spring Modulith architecture
- Each module exposes its own REST API

### 4. **Meaningful Names**
- Use descriptive display names that clearly indicate the API's purpose
- Group names should match the domain language (DDD)

---

## Configuration Structure

### ✅ CORRECT Configuration

```yaml
springdoc:
  api-docs:
    path: /api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    operationsSorter: method    # Sort by HTTP method
    tagsSorter: alpha           # Sort tags alphabetically
    displayRequestDuration: true

  group-configs:
    # Market Data Module
    - group: market-data
      display-name: Market Data API
      paths-to-match: /api/v1/market-data/**

    # Portfolio Module
    - group: portfolio
      display-name: Portfolio Management API
      paths-to-match: /api/v1/portfolios/**

    # Forecasting Module
    - group: forecasting
      display-name: Forecasting API
      paths-to-match: /api/v1/forecasting/**

    # Backtesting Module
    - group: backtesting
      display-name: Backtesting API
      paths-to-match: /api/v1/backtesting/**
```

### ❌ INCORRECT Configuration

```yaml
springdoc:
  group-configs:
    # ❌ BAD: Overly broad pattern catches everything
    - group: trading-platform-api
      display-name: Trading Platform API
      paths-to-match: /api/**

    # ❌ These will NEVER match because /api/** catches them first
    - group: market-data
      display-name: Market Data API
      paths-to-match: /api/market-data/**

    # ❌ Wrong path - doesn't match actual controller mapping
    - group: portfolio
      display-name: Portfolio Management API
      paths-to-match: /api/portfolio/**  # Controller uses /portfolios, not /portfolio
```

---

## Path Pattern Best Practices

### Pattern Hierarchy

SpringDoc matches patterns in the order they're defined. Once a path matches, it's assigned to that group.

**Rule:** Define patterns from **most specific** to **least specific**.

```yaml
group-configs:
  # ✅ GOOD: Specific patterns first
  - paths-to-match: /api/v1/market-data/instruments/historical/**
  - paths-to-match: /api/v1/market-data/**
  - paths-to-match: /api/v1/**

  # ❌ BAD: Broad pattern first (catches everything)
  - paths-to-match: /api/**
  - paths-to-match: /api/v1/market-data/**  # Never reached
```

### Pattern Conventions

| Pattern | Matches | Use Case |
|---------|---------|----------|
| `/api/v1/users/**` | All user endpoints | Single resource API group |
| `/api/v1/admin/**` | All admin endpoints | Role-based grouping |
| `/api/v{version}/orders/**` | Version-specific orders | Multi-version support |
| `/api/*/portfolios/**` | All portfolio versions | Cross-version grouping |

### Versioning Patterns

```yaml
# Option 1: Separate groups per version
- group: market-data-v1
  paths-to-match: /api/v1/market-data/**

- group: market-data-v2
  paths-to-match: /api/v2/market-data/**

# Option 2: Single group across versions
- group: market-data
  paths-to-match: /api/v*/market-data/**
```

---

## Common Mistakes

### 1. ❌ Overly Broad Catch-All Pattern

**Problem:**
```yaml
group-configs:
  - group: all-apis
    paths-to-match: /api/**  # Catches everything!
  - group: portfolio
    paths-to-match: /api/v1/portfolios/**  # Never reached
```

**Solution:**
```yaml
group-configs:
  - group: portfolio
    paths-to-match: /api/v1/portfolios/**
  - group: market-data
    paths-to-match: /api/v1/market-data/**
```

### 2. ❌ Path Mismatch with Controller

**Problem:**
```yaml
# Configuration says /api/portfolio/**
group-configs:
  - group: portfolio
    paths-to-match: /api/portfolio/**
```

```java
// But controller uses /api/v1/portfolios
@RestController
@RequestMapping("/api/v1/portfolios")  // Mismatch!
public class PortfolioController {
    // APIs won't appear in this group
}
```

**Solution:** Match the actual controller path exactly.

### 3. ❌ Missing Wildcard

**Problem:**
```yaml
group-configs:
  - group: portfolio
    paths-to-match: /api/v1/portfolios  # Missing /**
```

This only matches the exact path `/api/v1/portfolios`, not sub-paths like `/api/v1/portfolios/123`.

**Solution:**
```yaml
group-configs:
  - group: portfolio
    paths-to-match: /api/v1/portfolios/**  # Matches all sub-paths
```

### 4. ❌ Inconsistent Naming

**Problem:**
```java
@RestController
@RequestMapping("/api/v1/portfolio")  // Singular
public class PortfolioController {}

@RestController
@RequestMapping("/api/v1/portfolios")  // Plural
public class UserPortfolioController {}
```

**Solution:** Follow REST conventions - use plural nouns consistently:
- `/api/v1/portfolios` ✅
- `/api/v1/trades` ✅
- `/api/v1/instruments` ✅

---

## Controller Design

### REST Controller Best Practices

```java
@RestController
@RequestMapping("/api/v1/portfolios")  // Versioned, plural noun
@Tag(name = "Portfolio Management", description = "Endpoints for managing trading portfolios")
public class PortfolioController {

    @GetMapping
    @Operation(summary = "Get user's portfolios", description = "Returns all portfolios for the authenticated user")
    public ResponseEntity<List<PortfolioResponse>> getUserPortfolios(@RequestParam String userId) {
        // Implementation
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get portfolio by ID")
    public ResponseEntity<PortfolioResponse> getPortfolio(@PathVariable Long id) {
        // Implementation
    }

    @PostMapping
    @Operation(summary = "Create new portfolio", description = "Creates a new trading portfolio with specified configuration")
    public ResponseEntity<PortfolioResponse> createPortfolio(@Valid @RequestBody CreatePortfolioRequest request) {
        // Implementation
    }
}
```

### Key Points:

1. **@Tag** - Groups related operations in Swagger UI
2. **@Operation** - Provides summary and description for each endpoint
3. **Versioning** - Include version in base path (`/api/v1/`)
4. **Plural Nouns** - Use plural resource names (`/portfolios`, not `/portfolio`)
5. **HTTP Methods** - Use proper REST verbs (GET, POST, PUT, DELETE)

---

## Verification

### How to Verify Configuration

1. **Start the application**
   ```bash
   ./mvnw spring-boot:run
   ```

2. **Access Swagger UI**
   ```
   http://localhost:8080/swagger-ui.html
   ```

3. **Check the dropdown** (top-right)
   - Should show all configured API groups
   - Each group should display only its own endpoints

4. **Verify each group**
   - Select "Market Data API" → Should show only `/api/v1/market-data/**` endpoints
   - Select "Portfolio Management API" → Should show only `/api/v1/portfolios/**` endpoints
   - No group should show endpoints from another group

5. **Check for missing endpoints**
   - If an endpoint doesn't appear in any group, check:
     - Controller's `@RequestMapping` path
     - Swagger `paths-to-match` pattern
     - Pattern order in configuration

### Debugging Tips

**Problem:** Endpoint doesn't appear in expected group

**Checklist:**
1. ✅ Controller has `@RestController` annotation
2. ✅ Controller path matches the group's `paths-to-match` pattern
3. ✅ No broader pattern is catching the endpoint first
4. ✅ Application restarted after configuration changes
5. ✅ No compilation errors in the controller

**Example Debug Process:**

```java
// Controller
@RestController
@RequestMapping("/api/v1/portfolios")
public class PortfolioController {
    @GetMapping("/{id}")
    public ResponseEntity<PortfolioResponse> getPortfolio(@PathVariable Long id) { }
}
```

This endpoint's full path is: `/api/v1/portfolios/{id}`

Check configuration:
```yaml
group-configs:
  - group: portfolio
    paths-to-match: /api/v1/portfolios/**  # ✅ Matches!
```

---

## Module-Specific Guidelines

### Adding a New Module API

When creating a new module (e.g., Risk Management), follow these steps:

1. **Create the controller**
   ```java
   @RestController
   @RequestMapping("/api/v1/risk-management")
   @Tag(name = "Risk Management", description = "Risk analysis and monitoring")
   public class RiskManagementController {
       // Endpoints
   }
   ```

2. **Add Swagger group configuration**
   ```yaml
   springdoc:
     group-configs:
       - group: risk-management
         display-name: Risk Management API
         paths-to-match: /api/v1/risk-management/**
   ```

3. **Verify in Swagger UI**
   - Check that the new group appears in the dropdown
   - Verify all endpoints are visible
   - Ensure no endpoints appear in wrong groups

---

## Advanced Configuration

### Custom API Documentation

```yaml
springdoc:
  api-docs:
    path: /api-docs
    enabled: true
    groups:
      enabled: true

  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    # UI customization
    operationsSorter: method
    tagsSorter: alpha
    displayRequestDuration: true
    showExtensions: true
    showCommonExtensions: true
    # Try it out feature
    tryItOutEnabled: true
    filter: true
    # OAuth2 configuration (if needed)
    oauth2-redirect-url: /swagger-ui/oauth2-redirect.html

  # Global API info
  info:
    title: Trading Platform API
    description: Modern Portfolio Theory Trading Platform
    version: 1.0.0
    contact:
      name: Trading Platform Team
      email: support@tradingplatform.com

  # Show/hide endpoints
  show-actuator: false

  # Package scanning
  packages-to-scan: com.ahd.trading_platform
  paths-to-match: /api/**
```

### Security Annotations

```java
@RestController
@RequestMapping("/api/v1/portfolios")
@SecurityRequirement(name = "bearer-auth")  // Global security for all endpoints
public class PortfolioController {

    @GetMapping("/{id}")
    @Operation(
        summary = "Get portfolio by ID",
        security = @SecurityRequirement(name = "bearer-auth")  // Endpoint-specific
    )
    public ResponseEntity<PortfolioResponse> getPortfolio(@PathVariable Long id) {
        // Implementation
    }
}
```

---

## Quick Reference

### Configuration Checklist

- [ ] Each module has its own API group
- [ ] Path patterns match controller `@RequestMapping` exactly
- [ ] No overly broad patterns (avoid `/api/**`)
- [ ] Patterns include version (`/api/v1/`)
- [ ] Resource names are plural (`/portfolios`, `/trades`)
- [ ] Wildcard `/**` is present for sub-paths
- [ ] Controllers use `@Tag` for grouping
- [ ] Operations use `@Operation` for documentation
- [ ] Verified in Swagger UI after changes

### Common Patterns

```yaml
# Single resource module
- group: portfolios
  paths-to-match: /api/v1/portfolios/**

# Multiple related resources
- group: trading
  paths-to-match: /api/v1/(portfolios|trades|positions)/**

# Admin endpoints
- group: admin
  paths-to-match: /api/v1/admin/**

# Public endpoints
- group: public
  paths-to-match: /api/v1/public/**
```

---

## References

- [SpringDoc OpenAPI Documentation](https://springdoc.org/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [REST API Naming Conventions](https://restfulapi.net/resource-naming/)
- [Spring Boot REST Best Practices](https://spring.io/guides/tutorials/rest/)

---

**Last Updated:** 2025-12-23
**Version:** 1.0
**Maintainer:** Trading Platform Team
