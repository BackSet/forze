# PROJECT_CONTEXT

## Identidad

- Proyecto: FORZE.
- Repositorio: BackSet/forze.
- Rama canonica: `dev`.
- Fecha: 2026-06-16.
- Producto: herramienta profesional para presupuestacion de construccion.
- Timezone canonica: `America/Guayaquil`.

## Estructura

- `backend/`: Java 26, Spring Boot 4.1.0, Maven Wrapper.
- `frontend/`: React 19, TypeScript 5.9, Vite 8.
- `docker-compose.yml`: PostgreSQL y collector OTLP opcional.
- `.env.example`: variables locales sin secretos reales.
- `docs/ai/`: contexto canonico para agentes.

## Backend

- Paquete base: `com.backset.forze`.
- Modulos tecnicos bajo `com.backset.forze.module`:
  - `identity`: login, JWT access token, refresh token rotativo, logout, `/me`.
  - `document`: render HTML/PDF tecnico con Thymeleaf y OpenHTMLtoPDF.
- `configuration`: propiedades, seguridad, OpenAPI, CORS, Clock.
- `shared.api`: `ProblemDetail` y excepciones API.
- Persistencia: PostgreSQL + Flyway; Hibernate `ddl-auto=validate`.
- Migracion actual: `V1__identity_auth.sql`.
- Observabilidad: Actuator health/info, Micrometer Tracing, OTLP configurable, `traceId`/`spanId` en logs.
- OpenAPI: springdoc en `/v3/api-docs`.

## Autenticacion

- Endpoints:
  - `POST /api/auth/login`
  - `POST /api/auth/refresh`
  - `POST /api/auth/logout`
  - `GET /api/auth/me`
- Access token: JWT corto con issuer, subject, jti, issued-at, expiration y claim minimo `username`.
- Refresh token: aleatorio, guardado solo como hash SHA-256, con familia/sesion, expiracion, revocacion y rotacion.
- Refresh cookie: HttpOnly, path `/api/auth`, SameSite configurable, Secure en prod.
- CSRF: habilitado con cookie token repository; `/api/auth/**` queda ignorado explicitamente porque el flujo es stateless y usa SameSite/HttpOnly refresh cookie.
- Bootstrap admin: solo perfil `dev`, idempotente, password obligatorio por entorno.

## Frontend

- Rutas:
  - `/`: identidad FORZE y acceso a login, sin KPIs ni datos ficticios.
  - `/login`: React Hook Form + Zod, loading, errores y accesibilidad basica.
  - `/app`: ruta protegida que consulta `/api/auth/me`, muestra sesion tecnica y logout.
- API:
  - `openapi-fetch` con credentials.
  - `openapi-react-query` disponible como cliente principal para estado remoto.
  - Middleware de Authorization.
  - Timeout por request.
  - Refresh automatico single-flight.
  - Reintento maximo una vez tras 401 en `/me`.
  - Zustand guarda access token, usuario, estado de refresh y preferencias; no guarda refresh token.

## Comandos

- `docker compose up -d`
- `docker compose ps`
- `cd backend && ./mvnw test`
- `cd backend && ./mvnw verify`
- `cd backend && ./mvnw spring-boot:run`
- `cd frontend && npm ci`
- `cd frontend && npm run api:generate`
- `cd frontend && npm run typecheck`
- `cd frontend && npm run lint`
- `cd frontend && npm run test`
- `cd frontend && npm run build`
- `cd frontend && npm run test:e2e`

## Validacion Reciente

- Backend tests: 11 ejecutados, 1 omitido por Docker ausente.
- Frontend typecheck, lint, tests, build y E2E: exitosos.
- Docker/Compose: no disponible en el entorno actual (`docker: command not found`).

## Restricciones

- No copiar dominio, migraciones ni permisos de ECUBOX.
- No usar Axios, Ky, Redux, React Router, WebFlux, R2DBC, MongoDB, H2 ni Lombok.
- No inventar modulos funcionales de presupuestacion hasta que exista tarea concreta.
- No versionar secretos reales.
- No hacer commit ni push salvo instruccion explicita.
