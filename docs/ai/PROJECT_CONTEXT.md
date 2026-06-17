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
- Variables de entorno por componente (sin config general en la raiz):
  - `backend/.env.example` -> `backend/.env` (variables del backend; lo carga `backend/run.sh`).
  - `frontend/.env.example` -> `frontend/.env` (variables `VITE_*`; las carga Vite automaticamente).
  - Los `*.env` son locales e ignorados por Git; solo se versionan los `*.env.example`.
- `docs/ai/`: contexto canonico para agentes.

## Backend

- Paquete base: `com.backset.forze`.
- Modulos tecnicos bajo `com.backset.forze.module`:
  - `identity`: login, JWT access token, refresh token rotativo, logout, `/me`.
  - `document`: render HTML/PDF tecnico con Thymeleaf y OpenHTMLtoPDF.
  - `budgeting`: dominio de presupuestacion de obras civiles (proyectos, presupuestos, versiones,
    capitulos, rubros, APU, catalogo tecnico, proveedores, precios, escenarios, aprobaciones,
    documentos, control de obra, auditoria). Capas api/application por área, controllers, servicios y repositorios completamente funcionales.
- `configuration`: propiedades, seguridad, OpenAPI, CORS, Clock.
- `shared.api`: `ProblemDetail` y excepciones API.
- Persistencia: PostgreSQL + Flyway; Hibernate `ddl-auto=validate`.
- Migraciones: `V1__identity_auth.sql` (identidad) y `V2..V12` (modulo `budgeting`).
- Aislamiento por organización implementado mediante validación de membresías en filtro/interceptor.

## Estrategia de datos de presupuestacion

- Prefijo de tablas del modulo: `budgeting_`.
- IDs: UUID asignados por la aplicacion (sin secuencias expuestas).
- Dinero: `BigDecimal`/`numeric`. Costos y precios unitarios `numeric(18,4)`; totales `numeric(18,2)`
  redondeados `HALF_UP` al calcular. Porcentajes (margen, utilidad, indirectos, contingencia, tasa de
  impuesto) `numeric(7,4)`. Cantidades/mediciones `numeric(18,4)`; rendimientos y desperdicio `numeric(18,6)`.
- Moneda: codigo ISO-4217 `varchar(3)` por presupuesto; un presupuesto opera en una sola moneda; sin tipos de cambio.
- Fechas: `timestamptz` para instantes, `date` para fechas civiles; timezone canonica `America/Guayaquil`.
- Enums tecnicos estables: `varchar` + `CHECK` (nunca ordinal). Estados derivados de acciones del diseno
  (p.ej. `ACTIVO/ARCHIVADO` por la accion "archivar") quedan documentados en NAMING.
- Versionado/historial: una version de presupuesto `APROBADO` es inmutable (invariante de dominio);
  cualquier cambio crea una nueva version. Cada `budget_version` posee su propio arbol de capitulos/rubros/APU.
  Los precios usados se congelan como snapshot en `budgeting_item_apu_components.unit_price`, de modo que
  una version historica no cambia aunque cambie el catalogo o el historial de precios.
- Totales financieros almacenados (`total_cost`, `sale_price`, `margin`) son snapshots congelados por
  necesidad historica; el resto de derivados se calcula (proyeccion de obra y % de avance no se persisten).
- Concurrencia: `@Version` (`version bigint`) en raices editables (organizacion, catalogo, proyecto,
  presupuesto, version, escenario, aprobacion, baseline, adicional).
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

- `docker compose --env-file backend/.env up -d` (Postgres toma `DB_USERNAME`/`DB_PASSWORD` del backend)
- `docker compose ps`
- `cd backend && ./mvnw test`
- `cd backend && ./mvnw verify`
- `cd backend && mvn spring-boot:run` (perfil `dev` importa `backend/.env` de forma nativa via
  `spring.config.import=optional:file:.env[.properties]`; no requiere exportar variables a mano)
- `./backend/run.sh` (equivalente: tambien carga `backend/.env`, fija Java 25 y ejecuta `mvn spring-boot:run`)
- `cd frontend && npm ci`
- `cd frontend && npm run api:generate`
- `cd frontend && npm run typecheck`
- `cd frontend && npm run lint`
- `cd frontend && npm run test`
- `cd frontend && npm run build`
- `cd frontend && npm run test:e2e`

## Validacion Reciente

- Backend `./mvnw test`: 25 tests, 0 fallos, 8 omitidos por Docker ausente (Testcontainers).
- `ModulithArchitectureTests` verde: el modulo `budgeting` respeta los limites de modulo.
- Schema budgeting (V2..V12) + Hibernate `validate`: VALIDADO.
- `BudgetingPersistenceTests` (Testcontainers) siguen omitidos sin Docker; cubren ademas snapshot/cascade/lock.
- Frontend typecheck, lint, tests, build y E2E: 100% exitosos con todos los tests de vitest y Playwright pasando.
- Docker/Compose: no disponible en el entorno actual (`docker: command not found`).

## Restricciones

- No copiar dominio, migraciones ni permisos de ECUBOX.
- No usar Axios, Ky, Redux, React Router, WebFlux, R2DBC, MongoDB, H2 ni Lombok.
- No reconstruir historicos de presupuesto usando catalogos o precios actuales; usar los snapshots de la version.
- No persistir totales calculables salvo necesidad historica explicita (ya definida en la version y el item).
- No versionar secretos reales.
- No hacer commit ni push salvo instruccion explicita.
