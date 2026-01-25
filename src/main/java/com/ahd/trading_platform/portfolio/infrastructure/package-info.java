/**
 * Infrastructure Layer - Technical implementations for Portfolio module.
 *
 * Contains:
 * - Persistence: JPA entities, repository implementations, entity mappers
 * - Database: PostgreSQL schema, Liquibase migrations
 * - External Integrations: Future integrations with trading platforms
 *
 * This layer provides concrete implementations of domain repository contracts
 * and handles all technical concerns like database access, caching, and external APIs.
 *
 * Database Tables:
 * - portfolios: Main portfolio data
 * - positions: Instrument positions within portfolios
 * - trades: Trade execution history
 *
 * INTERNAL ONLY: This layer is not exposed outside the module.
 * Implementation details are hidden behind domain repository interfaces.
 */
@org.springframework.lang.NonNullApi
package com.ahd.trading_platform.portfolio.infrastructure;
