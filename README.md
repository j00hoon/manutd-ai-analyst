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

## API examples

```bash
curl http://localhost:8080/api/v1/health
curl "http://localhost:8080/api/v1/teams/manchester-united/matches?limit=5"
```

The match limit must be between 1 and 20. The configured CORS origin is `http://localhost:5173`.

## Tests

```powershell
cd backend
.\mvnw.cmd test
```
