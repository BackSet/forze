# FORZE

FORZE is a monorepo for a professional construction budgeting product. It contains a Spring Boot backend and a React/TypeScript frontend prepared for future functional modules without adding demo domain data.

## Requirements

- Java 26
- Node.js 24 or compatible current LTS
- npm 11
- Docker with Compose for local PostgreSQL

Spring Boot 4.1.0 is used because its official system requirements state compatibility up to Java 26.

## Structure

- `backend/`: Java 26, Spring Boot, Maven Wrapper.
- `frontend/`: React, TypeScript, Vite, TanStack Router, TanStack Query and Tailwind CSS.
- `docs/ai/`: canonical technical context for future agents.
- `compose.yaml`: local PostgreSQL service.

## Local Database

```bash
docker compose up -d postgres
```

The service uses configurable variables with safe local defaults:

- `FORZE_DB_NAME`
- `FORZE_DB_USERNAME`
- `FORZE_DB_PASSWORD`
- `FORZE_DB_PORT`

## Backend

```bash
cd backend
./mvnw test
./mvnw verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Default backend configuration:

- `FORZE_DB_URL=jdbc:postgresql://localhost:5432/forze`
- `FORZE_DB_USERNAME=forze`
- `FORZE_DB_PASSWORD=forze`
- `FORZE_BACKEND_PORT=8080`
- `FORZE_FRONTEND_ORIGIN=http://localhost:5173`

Technical endpoints intentionally exposed without authentication:

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /v3/api-docs`

All other backend requests are denied by default until real authentication and domain endpoints are implemented.

## Frontend

```bash
cd frontend
npm install
npm run dev
npm run typecheck
npm run test
npm run build
npm run e2e
```

## OpenAPI Flow

The backend is the source of truth for the OpenAPI contract. Start PostgreSQL and the backend before generating frontend types:

```bash
docker compose up -d postgres
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

In another terminal:

```bash
cd frontend
npm run openapi:generate
npm run openapi:check
```

Generated OpenAPI types live under `frontend/src/lib/api/generated/` and must not be edited manually.
