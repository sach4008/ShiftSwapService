# Shift Swap Service

A small service that lets an employee request a shift swap with another employee, and lets
their shared manager approve or reject it. Full design rationale — data model, state machine,
API, business rules, error codes, security, deployment — is in [DESIGN.md](DESIGN.md).

## Status

Feature-complete for the v1 scope in [DESIGN.md](DESIGN.md): domain layer (guarded state machine), service
layer (business rules, authorization, expiry, transaction boundary), web layer (controllers,
DTOs, identity resolution, centralized error mapping), seed data, and the full test suite
(unit, MockMvc integration, and concurrency) are all in place and passing.

## Requirements

- Java 21
- No local Maven install needed — use the wrapper (`./mvnw` / `mvnw.cmd`)

## Build & test

```bash
./mvnw verify
```

On Windows: `mvnw.cmd verify`

## Run locally

```bash
./mvnw spring-boot:run
```

Runs with the `local` Spring profile (in-memory H2, zero setup) and seeds a small org on
startup: employee `1` (Alice, VP) → `2` (Priya, manager) → `3`/`4`/`5` (Bob/Carol/Dave,
associates), each with one future shift. Swagger UI is at `http://localhost:8080/swagger-ui.html`.

```bash
# Bob (3) requests to swap his shift (1) with Carol's shift (2)
curl -X POST http://localhost:8080/api/v1/swap-requests \
  -H "X-User-Id: 3" -H "Content-Type: application/json" \
  -d '{"requesterShiftId":1,"targetShiftId":2,"reason":"Doctor appointment"}'

# Priya (2), their shared manager, approves it
curl -X POST http://localhost:8080/api/v1/swap-requests/{id}/approve \
  -H "X-User-Id: 2" -H "Content-Type: application/json" \
  -d '{"resolutionNote":"Coverage confirmed"}'
```

## Run via Docker Compose

```bash
cp .env.example .env   # set DB_USER / DB_PASSWORD
docker compose up --build
```

Runs the app against a real Postgres 16 instance (`docker` profile). `.env` is git-ignored;
`docker-compose.yml` fails fast if `DB_USER` / `DB_PASSWORD` are unset.

**Config:** all runtime config is environment variables (`DB_HOST`, `DB_PORT`, `DB_NAME`,
`DB_USER`, `DB_PASSWORD`, `SPRING_PROFILES_ACTIVE`) — nothing is hardcoded in the image.

**Health:** `GET /actuator/health` reports `UP`/`DOWN` and includes a DB ping; `docker-compose.yml`
gates the `app` container's startup on `db`'s own healthcheck (`depends_on: condition:
service_healthy`), so `app` never starts against a database that isn't ready yet.

**Rollback:** `docker compose down && docker compose up -d --build <previous-tag>` — since app
containers are stateless and the schema is additive-only in this deliverable's demo profile,
rolling back the app image doesn't require any database action. See DESIGN.md §13.6 for the
production-scale rollback story (immutable image tags, corrective forward migrations).

## Deploy

See DESIGN.md §13 for the full deployment/rollout/rollback/monitoring notes, including two
things this deliverable deliberately leaves as documented-but-not-built recommendations rather
than shipped artifacts: Flyway-managed schema migrations (both profiles here use Hibernate's
`ddl-auto: update`) and a blocking CI vulnerability gate (Trivy currently runs report-only). This
deliverable targets local Docker Compose as its one environment; the same profile-based config
(`local` vs `docker`) is the seam for a real staging/prod environment.
