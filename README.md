# Manchester United AI Football Analyst

Phase 1 provides a Spring Boot REST backend for health checks and Manchester United match data. AI, MCP, Bedrock, and agent features are intentionally out of scope for this phase.

## Prerequisites

- Java 25
- Docker (optional, for PostgreSQL and Redis)
- An API-Football key only when using live mode

## Start infrastructure

From the repository root:

```bash
docker compose --env-file infrastructure/.env -f infrastructure/docker-compose.yaml up -d
```

PostgreSQL is exposed on `localhost:5434` and Redis on `localhost:6380`. Neither service is required for the Phase 1 health endpoint or mock match endpoint.

## Configuration

All configuration is supplied through environment variables. No API keys or database passwords should be committed.

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | Backend HTTP port |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5434/manutd` | JDBC connection URL |
| `DATABASE_USERNAME` | `manutd` | PostgreSQL user |
| `DATABASE_PASSWORD` | falls back to `DB_PASSWORD` | PostgreSQL password |
| `DB_PASSWORD` | empty | Compose/backend compatibility fallback |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6380` | Redis port |
| `REDIS_PASSWORD` | empty | Optional Redis password |
| `FOOTBALL_DATA_MODE` | `mock` | `mock` or `live` provider selection |
| `FOOTBALL_API_KEY` | empty | Required only in live mode |
| `FOOTBALL_API_BASE_URL` | `https://v3.football.api-sports.io` | API-Football base URL |
| `FOOTBALL_API_MAX_SEASONS` | `5` | Maximum seasons searched in live mode |
| `FOOTBALL_SYNC_ENABLED` | `false` | Run one local match synchronization during application startup |
| `FOOTBALL_SYNC_LIMIT` | `20` | Number of recent matches requested by the startup synchronization |
| `FOOTBALL_STATISTICS_SYNC_ENABLED` | `false` | Run one statistics synchronization for persisted matches during startup |
| `FOOTBALL_STATISTICS_SYNC_LIMIT` | `20` | Number of recent persisted matches whose team statistics are requested |

PowerShell live-mode example:

```powershell
$env:FOOTBALL_DATA_MODE = "live"
$env:FOOTBALL_API_KEY = "your-key"
cd backend
.\mvnw.cmd spring-boot:run
```

Mock mode needs no secrets:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The mock endpoint returns deterministic demonstration records explicitly marked `SYNTHETIC`; they are not real match results.

## Persist matches locally

Synchronization is disabled by default and is never triggered by a GET request. To run one explicitly enabled mock synchronization at application startup:

```powershell
$env:DB_PASSWORD = "your-local-database-password"
$env:FOOTBALL_DATA_MODE = "mock"
$env:FOOTBALL_SYNC_ENABLED = "true"
$env:FOOTBALL_SYNC_LIMIT = "5"
cd backend
.\mvnw.cmd spring-boot:run
```

Mock rows remain marked with `data_source = 'MOCK'` and `synthetic = true`. For live synchronization, set `FOOTBALL_DATA_MODE=live` and provide `FOOTBALL_API_KEY`; do not commit the key. Stop the application after the startup log reports the synchronization result, then unset `FOOTBALL_SYNC_ENABLED` if subsequent starts should not synchronize.

Verify persisted rows without changing them:

```powershell
docker exec manutd-postgres psql -X -U manutd -d manutd -c "SELECT provider_fixture_id, kickoff_time, status, data_source, synthetic FROM matches ORDER BY kickoff_time DESC LIMIT 20;"
```

## Synchronize match statistics

Statistics synchronization is disabled by default, runs only once during application startup when explicitly enabled, and never runs from an analytics GET request. Matches must already be persisted. If match and statistics synchronization are enabled together, match synchronization runs first.

For live API-Football data:

```powershell
$env:DB_PASSWORD = "your-local-database-password"
$env:FOOTBALL_DATA_MODE = "live"
$env:FOOTBALL_API_KEY = "your-api-key"
$env:FOOTBALL_SYNC_ENABLED = "true"
$env:FOOTBALL_SYNC_LIMIT = "20"
$env:FOOTBALL_STATISTICS_SYNC_ENABLED = "true"
$env:FOOTBALL_STATISTICS_SYNC_LIMIT = "20"

cd backend
.\mvnw.cmd spring-boot:run
```

After the startup logs report both synchronization results, disable the two sync flags for ordinary API use:

```powershell
$env:FOOTBALL_SYNC_ENABLED = "false"
$env:FOOTBALL_STATISTICS_SYNC_ENABLED = "false"
```

Mock mode does not invent shots, shots on target, or possession. Explicit mock statistics synchronization records those metrics as unavailable for persisted synthetic fixtures.

API-Football `/fixtures/statistics` is requested once for each persisted fixture selected by the statistics sync limit. This consumes API quota in addition to fixture synchronization. Use a conservative limit, do not repeatedly restart with synchronization enabled, and consult the provider dashboard for the current quota. A provider failure is not stored as `UNAVAILABLE`; existing statistics remain unchanged.

Verify the Flyway version and persisted statistics:

```powershell
docker exec manutd-postgres psql -X -U manutd -d manutd -c "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
docker exec manutd-postgres psql -X -U manutd -d manutd -c "SELECT m.provider_fixture_id, t.name, s.shots, s.shots_on_target, s.possession_percentage, s.coverage_status, s.data_source, s.synthetic, s.provider_checked_at FROM team_match_statistics s JOIN matches m ON m.id = s.match_id JOIN teams t ON t.id = s.team_id ORDER BY m.kickoff_time DESC, t.name LIMIT 40;"
```

## API examples

```bash
curl http://localhost:8080/api/v1/health
curl "http://localhost:8080/api/v1/teams/manchester-united/matches?limit=5"
curl "http://localhost:8080/api/v1/teams/manchester-united/analytics/summary?limit=20"
curl "http://localhost:8080/api/v1/teams/manchester-united/analytics/trends?limit=10"
curl "http://localhost:8080/api/v1/matches/123456/statistics"
```

The match limit must be between 1 and 20. The configured CORS origin is `http://localhost:5173`.
Analytics summary and trend limits must be between 1 and 100. Analytics responses are calculated only from persisted rows and include `dataSource`, `synthetic`, and explicit statistics coverage. Missing statistics remain `null`; they are never converted to zero. A persisted fixture without synchronized statistics returns `200 OK` with `NOT_SYNCED`, while a fixture absent from the active data source returns `404`.

PowerShell request examples:

```powershell
Invoke-RestMethod "http://localhost:8080/api/v1/teams/manchester-united/analytics/summary?limit=20"
Invoke-RestMethod "http://localhost:8080/api/v1/teams/manchester-united/analytics/trends?limit=10"
Invoke-RestMethod "http://localhost:8080/api/v1/matches/123456/statistics"
```

## Tests

```powershell
cd backend
.\mvnw.cmd test
```
