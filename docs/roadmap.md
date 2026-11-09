# Manchester United AI Analyst — Development Roadmap

## Project Overview

Build a Manchester United football analytics platform using Java, Spring Boot, React, PostgreSQL, Redis, and AI technologies.

The project will progressively introduce football statistics, caching, MCP, Amazon Bedrock, and AI Agent Harness capabilities.

## Phase 1 — Project Foundation

**Status:** Completed

- [x] Spring Boot backend setup
- [x] React + TypeScript frontend setup
- [x] PostgreSQL and Redis Docker configuration
- [x] FootballDataProvider abstraction
- [x] Mock and Live provider modes
- [x] Initial match REST API

## Phase 2 — Football Data & Analytics

### Step 1 — Database Foundation
**Status:** Completed

- [x] Flyway V1 migration
- [x] Teams, competitions, and matches schema
- [x] JPA entities and repositories

### Step 2 — Match Persistence
**Status:** Completed

- [x] Match synchronization
- [x] Idempotent database upsert
- [x] PostgreSQL-backed match retrieval

### Step 3 — Match Analytics
**Status:** Completed

- [x] Flyway V2 migration
- [x] Team match statistics
- [x] Match analytics REST APIs
- [x] Statistics synchronization
- [x] API verification

### Step 4 — Player Statistics
**Status:** Implementation in progress — final verification pending

- [ ] Confirm Flyway V3 migration
- [ ] Verify player entities and repositories
- [ ] Verify API-Football player statistics integration
- [ ] Verify player statistics synchronization
- [ ] Verify player analytics REST APIs
- [ ] Complete integration and API testing

### Step 5 — Redis Caching
**Status:** Planned

- [ ] Implement Spring Cache with Redis
- [ ] Cache match analytics responses
- [ ] Cache player statistics responses
- [ ] Configure TTL
- [ ] Invalidate caches after synchronization
- [ ] Test MOCK/LIVE cache isolation

### Step 6 — Frontend Analytics Dashboard
**Status:** Planned

- [ ] Connect analytics APIs to React
- [ ] Display match statistics charts
- [ ] Display player statistics
- [ ] Implement player comparison UI

## Phase 3 — MCP Integration

**Status:** Planned

- [ ] Implement MCP servers
- [ ] Expose football data tools
- [ ] Integrate MCP client
- [ ] Test tool invocation

## Phase 4 — AI Football Analyst

**Status:** Planned

- [ ] Integrate Spring AI
- [ ] Connect Amazon Bedrock
- [ ] Implement natural-language football analysis
- [ ] Ground AI responses in persisted statistics

## Phase 5 — Runtime AI Agent Harness

**Status:** Planned

- [ ] Implement agent execution workflow
- [ ] Add