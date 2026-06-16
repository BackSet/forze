# PROJECT_CONTEXT

## Identidad

- Proyecto: FORZE.
- Repositorio: BackSet/forze (`https://github.com/BackSet/forze.git`) [verificado en Git].
- Rama canonica de trabajo: `dev` [verificado en Git].
- Fecha de contexto: 2026-06-16.
- Tipo de producto: aplicacion profesional para presupuestacion de construccion [verificado en `PRODUCT.md`].
- Principio de producto: hacer que presupuestos complejos sean controlables, verificables y rapidos de construir [verificado en `PRODUCT.md`].

## Estructura

- `backend/`: backend Java 26 con Spring Boot 4.1.0 y Maven Wrapper.
- `frontend/`: frontend React/TypeScript/Vite.
- `compose.yaml`: PostgreSQL local para desarrollo.
- `docs/ai/`: contexto tecnico canonico para agentes.
- `PRODUCT.md`: contexto de producto y direccion visual.

## Stack Confirmado

### Backend

- Java 26 [verificado con `java -version` y `backend/pom.xml`].
- Maven Wrapper con Apache Maven 3.9.16 [verificado con `./mvnw --version`].
- Spring Boot 4.1.0 [verificado en `backend/pom.xml`].
- Paquete base: `com.backset.forze`.
- Spring Web MVC, Validation, Security, Actuator, Data JPA y Flyway.
- PostgreSQL driver y `flyway-database-postgresql`.
- Spring Modulith 2.1.0 para verificacion de arquitectura.
- MapStruct 1.6.3 preparado para mapeos futuros.
- springdoc OpenAPI 3.0.3, endpoint `/v3/api-docs`.
- Testcontainers 1.21.4 para pruebas PostgreSQL cuando Docker este disponible.

### Frontend

- Node.js 24.16.0 y npm 11.13.0 [verificado localmente].
- React 19, TypeScript 5.9, Vite 8.
- TanStack Router, TanStack Query, TanStack Table y TanStack Virtual.
- Tailwind CSS 4, shadcn/ui pattern, Radix Slot, lucide-react.
- React Hook Form, Zod, Zustand, Decimal.js.
- openapi-typescript, openapi-fetch y openapi-react-query.
- Vitest, Testing Library y Playwright.

## Backend

- Clase de arranque: `backend/src/main/java/com/backset/forze/ForzeApplication.java`.
- Seguridad global: CSRF deshabilitado para API stateless inicial, CORS configurable, endpoints tecnicos permitidos y todo lo demas denegado por defecto.
- Endpoints permitidos sin autenticacion:
  - `GET /actuator/health`
  - `GET /actuator/info`
  - `GET /v3/api-docs`
  - `GET /v3/api-docs/**`
- No hay endpoints, entidades, servicios, repositorios, migraciones o reglas de dominio implementadas todavia.
- JPA usa `hibernate.ddl-auto=validate`; Flyway esta habilitado.
- Errores de validacion usan `ProblemDetail` mediante `ApiExceptionHandler`.

## Frontend

- Entrada: `frontend/src/main.tsx`.
- Router: `frontend/src/router.ts` y rutas en `frontend/src/routes/`.
- Pantalla inicial: `frontend/src/components/home-page.tsx`.
- Providers globales: `frontend/src/components/providers.tsx`.
- Cliente API tipado: `frontend/src/lib/api/client.ts`.
- Tipos OpenAPI generados: `frontend/src/lib/api/generated/schema.d.ts`.
- La UI inicial es una pantalla tecnica de base de trabajo, sin KPIs, datos de muestra ni modulos de dominio inventados.

## Configuracion Local

- Base de datos local esperada: PostgreSQL via `compose.yaml`.
- Variables backend:
  - `FORZE_DB_URL` (`jdbc:postgresql://localhost:5432/forze` por defecto).
  - `FORZE_DB_USERNAME` (`forze` por defecto).
  - `FORZE_DB_PASSWORD` (`forze` por defecto).
  - `FORZE_BACKEND_PORT` (`8080` por defecto).
  - `FORZE_FRONTEND_ORIGIN` (`http://localhost:5173` por defecto).

## Comandos

- Backend test: `cd backend && ./mvnw test`.
- Backend verify/build: `cd backend && ./mvnw verify`.
- Backend dev: `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`.
- PostgreSQL local: `docker compose up -d postgres`.
- Frontend install: `cd frontend && npm install`.
- Frontend dev: `cd frontend && npm run dev`.
- Frontend typecheck: `cd frontend && npm run typecheck`.
- Frontend unit tests: `cd frontend && npm run test`.
- Frontend lint: `cd frontend && npm run lint`.
- Frontend build: `cd frontend && npm run build`.
- Frontend E2E: `cd frontend && npm run e2e`.
- Generar tipos OpenAPI: `cd frontend && npm run openapi:generate` con backend activo.

## Validaciones Recientes

- `backend/./mvnw verify`: exitoso, 6 tests, 1 omitido porque Docker no esta disponible.
- `frontend/npm run typecheck`: exitoso.
- `frontend/npm run test`: exitoso, 2 tests.
- `frontend/npm run lint`: exitoso.
- `frontend/npm run build`: exitoso.
- `frontend/npm run e2e`: exitoso luego de instalar Chromium de Playwright.
- `docker compose up -d postgres`: no ejecutable en este entorno porque `docker` no existe en PATH.

## Reglas Para Futuros Agentes

- No inventar modulos, endpoints, tablas, roles, estados, datos demo ni reglas de negocio.
- El backend es la fuente de verdad del contrato OpenAPI.
- Los tipos generados bajo `frontend/src/lib/api/generated/` no se editan manualmente.
- Mantener `docs/ai/` como contexto canonico verificable, no como changelog.
- No hacer commit ni push salvo instruccion explicita del usuario.
