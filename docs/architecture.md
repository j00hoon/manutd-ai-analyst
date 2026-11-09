# Manchester United AI Analyst — System Architecture

## 1. Project Overview

Manchester United AI Analyst is a full-stack football analytics platform designed to collect, persist, analyze, and eventually interpret football data using AI.

The project follows a phased development approach:

1. Football data collection and persistence
2. Match and player statistics analytics
3. Redis caching and frontend visualization
4. MCP-based tool integration
5. AI-powered analysis with Spring AI and Amazon Bedrock
6. Agent execution orchestration and automation

The architecture separates **data collection**, **persistence**, **business analytics**, and **AI processing**.

---

## 2. Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| Backend | Java 25, Spring Boot 4 | REST APIs and business logic |
| Build | Maven Wrapper | Dependency management and testing |
| Frontend | React, TypeScript, Vite | Web application |
| UI | Tailwind CSS | Styling |
| Data Fetching | TanStack Query, Axios | Frontend API communication |
| Charts | Recharts | Data visualization |
| Database | PostgreSQL 17 | Persistent football data |
| ORM | Spring Data JPA, Hibernate | Database access |
| Migration | Flyway | Database schema versioning |
| Cache | Redis | Planned analytics caching |
| Infrastructure | Docker Compose | Local PostgreSQL and Redis |
| Football Provider | API-Football | Live football data |
| Testing | JUnit, Mockito, Testcontainers | Automated verification |
| AI Framework | Spring AI | Planned AI integration |
| LLM | Amazon Bedrock | Planned football analysis |
| Tool Protocol | MCP | Planned AI tool integration |

**Implementation note:** A technology appearing in the stack does not necessarily mean its full functionality has been implemented. Redis caching, MCP, and Bedrock integration are planned.

---

## 3. High-Level Architecture

```text
                  +--------------------------+
                  |          User            |
                  +-------------+------------+
                                |
                                v
                  +--------------------------+
                  |      React Frontend      |
                  |    TypeScript + Vite     |
                  +-------------+------------+
                                |
                                | HTTP / REST
                                v
                  +--------------------------+
                  |     Spring Boot API      |
                  |       Controllers        |
                  +-------------+------------+
                                |
                                v
                  +--------------------------+
                  |      Service Layer       |
                  |                          |
                  | - MatchService           |
                  | - TeamAnalyticsService   |
                  | - PlayerStatisticsService|
                  +-------------+------------+
                                |
                      +---------+---------+
                      |                   |
                      v                   v
             +----------------+   +----------------+
             |  PostgreSQL    |   |     Redis      |
             |  Persisted     |   |  Cache         |
             |  Football Data |   |  (Planned)     |
             +----------------+   +----------------+


               External Data Collection
               ========================

             +------------------------+
             |      API-Football      |
             +-----------+------------+
                         |
                         | HTTP / JSON
                         v
             +------------------------+
             |  FootballDataProvider  |
             |    MOCK / LIVE         |
             +-----------+------------+
                         |
                         v
             +------------------------+
             |  Synchronization       |
             |  Services              |
             +-----------+------------+
                         |
                         v
             +------------------------+
             |      PostgreSQL        |
             +------------------------+
```

**Important:** REST analytics requests read persisted data. They do not directly trigger external API-Football synchronization.

---

## 4. Repository Structure

```text
manutd-ai-analyst/
|
|-- AGENTS.md
|-- README.md
|
|-- backend/
|   |-- src/
|   |   |-- main/
|   |   |   |-- java/com/manutd/analyst/
|   |   |   |   |-- client/
|   |   |   |   |-- config/
|   |   |   |   |-- controller/
|   |   |   |   |-- dto/
|   |   |   |   |-- entity/
|   |   |   |   |-- repository/
|   |   |   |   |-- service/
|   |   |   |
|   |   |   |-- resources/
|   |   |       |-- db/migration/
|   |   |       |-- application.properties
|   |   |
|   |   |-- test/
|   |
|   |-- mvnw.cmd
|   |-- pom.xml
|
|-- frontend/
|   |-- src/
|   |   |-- pages/
|   |   |-- components/
|   |   |-- ...
|   |-- package.json
|
|-- infrastructure/
|   |-- docker-compose.yaml
|
|-- docs/
|   |-- architecture.md
|   |-- roadmap.md
|   |-- plans/
|   |-- handoff/
|
|-- scripts/
    |-- verify-backend.ps1
    |-- verify-frontend.ps1
```

Some documentation and verification-script paths belong to the Development Harness setup and may not exist until that setup is completed.

---

## 5. Backend Architecture

The backend uses a layered Spring Boot architecture.

### 5.1 Controller Layer

Responsibilities:

- Expose REST endpoints.
- Validate request parameters.
- Return structured JSON responses.
- Delegate business logic to services.
- Handle API errors consistently.

Controllers should not contain complex analytics calculations or directly access external football providers.

### 5.2 Service Layer

Responsibilities:

- Coordinate business operations.
- Calculate football analytics.
- Manage synchronization workflows.
- Enforce MOCK/LIVE separation.
- Handle missing data and statistics coverage.

Examples:

- `MatchService`
- `FootballSyncService`
- `TeamAnalyticsService`
- `FootballStatisticsSyncService`
- `PlayerStatisticsService` (Step 4 verification pending)

### 5.3 Repository Layer

Uses Spring Data JPA to access PostgreSQL.

Responsibilities:

- Retrieve persisted football data.
- Support source-scoped queries.
- Load match and player statistics.
- Support transactional persistence and idempotent synchronization.

### 5.4 Entity Layer

Maps database tables to Java entities.

Existing foundational entities include:

- `TeamEntity`
- `CompetitionEntity`
- `MatchEntity`
- `TeamMatchStatisticsEntity`

Step 4 introduces:

- `PlayerEntity`
- `PlayerMatchStatisticsEntity`
- `MatchPlayerStatisticsCoverageEntity`

The Step 4 entities require final repository verification before being treated as completed functionality.

---

## 6. Football Data Provider Architecture

The application supports two football data modes.

| Mode | Provider | Purpose |
|---|---|---|
| `MOCK` | `MockFootballDataProvider` | Development and deterministic testing |
| `LIVE` | `ApiFootballDataProvider` | Real API-Football data |

The provider abstraction allows synchronization services to use a common interface without depending on the provider implementation.

### 6.1 MOCK Mode

- Uses synthetic match data.
- Does not require an external API key.
- Must identify synthetic data explicitly.
- Must not fabricate unavailable player statistics.
- Must not mix with LIVE analytics.

### 6.2 LIVE Mode

- Uses API-Football.
- Requires a valid API key.
- Supports real fixture and team statistics retrieval.
- May be limited by subscription, season availability, and API quota.
- May return incomplete or unavailable statistics.

### 6.3 Data Integrity Rules

- Preserve provider identifiers.
- Keep `MOCK` and `LIVE` records isolated.
- Never substitute missing statistics with fabricated values.
- Distinguish unavailable data from synchronization failures.
- Never expose API credentials in logs or source control.

---

## 7. Synchronization Architecture

Football data is synchronized separately from user-facing analytics requests.

### 7.1 Match Synchronization

```text
FootballSyncRunner
        |
        v
FootballSyncService
        |
        v
FootballDataProvider
        |
        v
MatchPersistenceService
        |
        v
PostgreSQL
```

Responsibilities:

- Fetch recent completed matches.
- Validate provider data.
- Persist teams, competitions, and matches.
- Perform idempotent upserts.
- Preserve existing records on provider failure.

### 7.2 Team Statistics Synchronization

```text
FootballStatisticsSyncRunner
        |
        v
FootballStatisticsSyncService
        |
        v
FootballDataProvider
        |
        v
TeamMatchStatisticsPersistenceService
        |
        v
PostgreSQL
```

Statistics include:

- Shots
- Shots on target
- Ball possession

### 7.3 Player Statistics Synchronization

Phase 2 Step 4 introduces a separate workflow:

```text
FootballPlayerStatisticsSyncRunner
        |
        v
FootballPlayerStatisticsSyncService
        |
        v
FootballDataProvider
        |
        v
PlayerMatchStatisticsPersistenceService
        |
        v
PostgreSQL
```

The intended startup synchronization order is:

1. Match synchronization
2. Team statistics synchronization
3. Player statistics synchronization

All synchronization operations must remain explicitly enabled rather than running on every analytics GET request.

---

## 8. Database Architecture

PostgreSQL stores normalized football data.

### 8.1 Core Relationships

```text
teams --------------------+
                          |
competitions --------+    |
                     |    |
                     v    v
                   matches
                     |
                     +---- team_match_statistics
                     |
                     +---- player_match_statistics
                     |            |
                     |            v
                     |          players
                     |
                     +---- match_player_statistics_coverage
```

### 8.2 Flyway Migrations

| Version | Description | Status |
|---|---|---|
| V1 | Core teams, competitions, matches | Completed |
| V2 | Match analytics foundation | Completed |
| V3 | Player statistics foundation | Final verification pending |

**Migration rules:**

- Never edit applied Flyway migrations.
- Introduce schema changes using a new migration.
- Preserve existing database records.
- Validate entity mappings against the migrated schema.
- Test migrations with PostgreSQL Testcontainers.

### 8.3 Player Transfers

A player should have a stable identity independent of team membership.

```text
Player
  |
  +-- Match 1 -> Manchester United
  |
  +-- Match 2 -> Manchester United
  |
  +-- Match 3 -> Another Club
```

Each player-match statistics record stores the team represented in that match.

Manchester United analytics must include only the player's Manchester United match statistics.

---

## 9. Analytics Architecture

Analytics calculations are performed in Java using persisted PostgreSQL data.

### 9.1 Match Analytics

Current supported metrics include:

- Wins, draws, and losses
- Win percentage
- Goals scored and conceded
- Average goals per match
- Clean sheets
- Home and away performance
- Recent match trends
- Shots and possession when available

### 9.2 Player Analytics

Step 4 targets:

- Provider-reported appearances
- Minutes played
- Goals and assists
- Shots and shots on target
- Average rating
- Passing statistics
- Defensive statistics
- Fouls and cards
- Penalty statistics

These values must be computed from provider-reported records, without assuming missing values are zero.

### 9.3 Statistics Coverage

The system distinguishes four coverage states.

| Status | Meaning |
|---|---|
| `NOT_SYNCED` | Statistics have not been successfully checked |
| `UNAVAILABLE` | Provider returned no usable statistics |
| `PARTIAL` | Only some required statistics are available |
| `COMPLETE` | Required statistics are available |

A provider request failure must not automatically change existing coverage to `UNAVAILABLE`.

---

## 10. REST API Architecture

### 10.1 Match APIs

```http
GET /api/v1/teams/manchester-united/matches?limit=5
GET /api/v1/matches/{fixtureId}/statistics
```

### 10.2 Match Analytics APIs

```http
GET /api/v1/teams/manchester-united/analytics/summary?limit=20
GET /api/v1/teams/manchester-united/analytics/trends?limit=10
```

### 10.3 Player APIs

Phase 2 Step 4 targets:

```http
GET /api/v1/teams/manchester-united/players?season=2026
GET /api/v1/teams/manchester-united/players/{providerPlayerId}/statistics?season=2026
GET /api/v1/matches/{fixtureId}/players
```

All analytics APIs should:

- Read from PostgreSQL.
- Respect the active data source.
- Avoid external provider calls.
- Validate request parameters.
- Return structured error responses.
- Include coverage metadata when appropriate.

---

## 11. Redis Caching Architecture — Planned

Redis is available in the local infrastructure, but application-level analytics caching is planned for Phase 2 Step 5.

Proposed request flow:

```text
REST Controller
      |
      v
Analytics Service
      |
      v
Redis Cache
      |
      +-- Cache Hit --> Return cached response
      |
      +-- Cache Miss
              |
              v
          PostgreSQL
              |
              v
          Calculate
              |
              v
          Store in Redis
              |
              v
          Return response
```

Caching requirements:

- Use Spring Cache.
- Configure TTL.
- Separate MOCK and LIVE cache keys.
- Include relevant season, competition, and pagination parameters.
- Invalidate affected cache entries after synchronization.
- Avoid returning stale analytics after successful updates.

---

## 12. Future AI Architecture — Planned

The AI layer will be implemented after the football analytics foundation is stable.

### 12.1 MCP Integration

Phase 3 will introduce MCP servers exposing football-data tools.

Example tools:

- Get recent Manchester United matches
- Get team analytics
- Get player season statistics
- Compare player statistics

### 12.2 Spring AI and Amazon Bedrock

Phase 4 will introduce natural-language football analysis.

```text
User Question
      |
      v
Spring Boot AI Endpoint
      |
      v
Spring AI + Amazon Bedrock
      |
      v
MCP Tools / Football Data Services
      |
      v
PostgreSQL
      |
      v
Grounded AI Response
```

AI-generated answers should use available football data and clearly communicate missing information.

### 12.3 Runtime AI Agent Harness

Phase 5 will add agent execution management.

Planned capabilities:

- Tool execution lifecycle
- Execution state tracking
- Retry and timeout handling
- Error recovery
- Observability and structured logging
- Validation of tool results

This Runtime Agent Harness is separate from the Codex Development Harness used to build the project.

---

## 13. Testing Strategy

| Test Type | Purpose |
|---|---|
| JUnit | Business logic verification |
| Mockito | Mocked dependencies and service isolation |
| MockMvc | REST controller contract verification |
| Testcontainers | Real PostgreSQL integration testing |
| Flyway validation | Database migration verification |
| Maven clean test | Backend regression testing |
| Frontend build | TypeScript and frontend build verification |

### Backend Verification

From the repository root:

```powershell
cd backend
.\mvnw.cmd clean test
```

### Frontend Verification

```powershell
cd frontend
npm run build
```

### Git Verification

```powershell
git status
git diff --check
```

Do not mark an implementation complete solely because code generation finished. Verify the build, tests, database migration, and relevant REST APIs.

---

## 14. Development Harness

The Codex Development Harness stores persistent project instructions and progress outside the conversation context.

| File | Responsibility |
|---|---|
| `AGENTS.md` | Project-wide development rules |
| `docs/architecture.md` | System design and data flows |
| `docs/roadmap.md` | Phase status and future work |
| `docs/plans/` | Detailed task requirements |
| `docs/handoff/current.md` | Current progress and session handoff |
| `scripts/verify-backend.ps1` | Backend verification |
| `scripts/verify-frontend.ps1` | Frontend verification |

### New Codex Session Workflow

1. Read `AGENTS.md`.
2. Read `docs/roadmap.md`.
3. Read `docs/handoff/current.md`.
4. Inspect `git status` and actual source code.
5. Read the current task plan.
6. Continue implementation.
7. Run required verification.
8. Update the handoff document.
9. Report results without automatically committing or pushing.

---

## 15. Architecture Principles

1. **Separation of concerns:** Controllers, services, providers, and repositories have distinct responsibilities.
2. **Database-first analytics:** User-facing analytics read persisted data.
3. **Idempotent synchronization:** Repeated synchronization must not create duplicate records.
4. **Source isolation:** MOCK and LIVE data must never be mixed.
5. **Data integrity:** Missing football statistics must not be fabricated.
6. **Migration safety:** Applied Flyway migrations remain immutable.
7. **Testability:** Calculations and synchronization behavior must be independently testable.
8. **Incremental delivery:** Implement and verify one development step at a time.
9. **Security:** Keep credentials outside source control.
10. **AI grounding:** Future AI responses must be grounded in available football data.

---

## 16. Current Implementation Status

| Component | Status |
|---|---|
| Spring Boot backend | Implemented |
| React frontend foundation | Implemented |
| PostgreSQL and Flyway V1/V2 | Implemented |
| Match persistence | Implemented |
| Team match statistics | Implemented |
| Match analytics APIs | Implemented |
| Player statistics | Final verification pending |
| Redis caching | Planned |
| Frontend analytics dashboard | Planned |
| MCP integration | Planned |
| Amazon Bedrock integration | Planned |
| Runtime AI Agent Harness | Planned |

**Current priority:** Finish Phase 2 Step 4 verification, establish the Development Harness, and then proceed with Redis caching.