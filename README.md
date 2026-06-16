# FORZE

FORZE is a professional construction budgeting monorepo. It contains a Java 26 / Spring Boot backend and a React / TypeScript / Vite frontend with authentication infrastructure, OpenAPI-driven client types, PostgreSQL/Flyway persistence, observability hooks, and a technical PDF renderer.

No construction-budget domain modules are implemented yet.

## Requirements

- Java 26
- Node.js 24
- npm 11
- Docker with Compose for PostgreSQL and optional OpenTelemetry collector

## Structure

- `backend/`: Spring Boot backend.
- `frontend/`: React/Vite frontend.
- `docs/ai/`: canonical implementation context for agents.
- `docker-compose.yml`: PostgreSQL and optional OTLP collector.
- `.env.example`: local environment template.

## Environment

Copy `.env.example` to `.env` and set local secrets. Do not commit real values.

Required sensitive values do not have safe fallbacks:

- `DB_PASSWORD`
- `FLYWAY_PASSWORD`
- `JWT_SECRET`
- `ADMIN_INITIAL_PASSWORD` when admin bootstrap is enabled

Canonical timezone:

```text
America/Guayaquil
```

## Local Services

```bash
docker compose up -d
docker compose ps
```

Optional OTLP collector:

```bash
docker compose --profile observability up -d
```

## Backend

```bash
cd backend
./mvnw test
./mvnw verify
./mvnw spring-boot:run
```

Implemented technical endpoints:

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `GET /actuator/health`
- `GET /actuator/info`
- `GET /v3/api-docs`

Security notes:

- Access tokens are JWT bearer tokens.
- Refresh tokens are random, stored only as SHA-256 hashes, rotated on refresh, revocable, and sent through an HttpOnly cookie.
- CSRF is enabled with a cookie token repository and explicitly ignored for the stateless auth API.
- CORS allows credentials and is configured by `CORS_ALLOWED_ORIGINS`.
- Admin bootstrap is dev-only and idempotent.

## Frontend

```bash
cd frontend
npm ci
npm run api:generate
npm run typecheck
npm run lint
npm run test
npm run build
npm run test:e2e
```

Routes:

- `/`: FORZE identity and login access.
- `/login`: React Hook Form + Zod login flow.
- `/app`: protected session surface using `/api/auth/me`.

The frontend uses `openapi-fetch` and `openapi-react-query` as the API client foundation. Refresh tokens are never stored in localStorage.

## OpenAPI

The backend is the source of truth:

```bash
cd frontend
npm run api:generate
```

Generated types live in `frontend/src/lib/api/generated/schema.d.ts` and should be regenerated from `/v3/api-docs` when the backend is running with PostgreSQL available.

## Direct Dependencies

Backend responsibilities:

- Spring Boot starters: Web MVC, Security, Validation, Data JPA, Flyway, Actuator, Thymeleaf.
- PostgreSQL driver and Flyway PostgreSQL support: persistence and migrations.
- JJWT: access JWT issuing and validation.
- Spring Modulith: modular boundary verification.
- springdoc-openapi: OpenAPI generation.
- Micrometer Tracing + OpenTelemetry OTLP exporter: trace propagation/export.
- OpenHTMLtoPDF: technical PDF smoke rendering.
- MapStruct: mapper support for future modules.
- Testcontainers PostgreSQL: integration tests without H2 when Docker is available.

Frontend responsibilities:

- React, TypeScript, Vite: application runtime and build.
- TanStack Router, Query, Table, Virtual: routing, remote state, future dense tables/virtualization.
- React Hook Form, Zod, `@hookform/resolvers`: login form validation.
- Zustand: local session/preferences only.
- Tailwind CSS, shadcn/ui pattern, Radix Slot, class-variance-authority, clsx, tailwind-merge, lucide-react: UI foundation.
- openapi-typescript, openapi-fetch, openapi-react-query: contract-driven API client.
- Sonner and cmdk: notifications and future command palette.
- Decimal.js: future financial calculations.
- Vitest, Testing Library, user-event, jsdom, coverage-v8, Playwright: unit/component/E2E tests.
- ESLint and Prettier: static quality and formatting.
