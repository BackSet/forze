# PROJECT_INSTRUCTIONS

## Identidad

- Proyecto: FORZE.
- Repositorio: BackSet/forze.
- Rama base: `dev`.
- Entorno principal: local.
- Contexto IA: `docs/ai`.

## Reglas

- Trabajar sobre `dev` salvo instruccion explicita.
- No hacer commit ni push salvo instruccion explicita.
- No sobrescribir cambios locales ajenos.
- No copiar dominio, migraciones, permisos ni entidades de ECUBOX.
- Mantener `backend/` y `frontend/` como raices.
- Mantener timezone `America/Guayaquil`.
- No usar Axios, Ky, Redux, React Router, Lombok, WebFlux, R2DBC, H2 o MongoDB.
- No versionar secretos reales.

## Arquitectura

- Backend modular bajo `com.backset.forze.module`.
- Submodulos actuales: `identity` y `document`.
- Cada modulo mantiene `api`, `application`, `domain`, `infrastructure` cuando aplica.
- No crear carpetas globales de controllers/services/repositories.
- Flyway es propietario del esquema; Hibernate solo valida.
- Access JWT + refresh cookie HttpOnly.
- Refresh tokens siempre hasheados.
- Actuator solo health/info.
- OpenAPI desde backend.
- Frontend usa TanStack Router, TanStack Query, openapi-fetch/openapi-react-query, Zustand solo para sesion/preferencias.

## Validacion Esperada

- `cd backend && ./mvnw verify`
- `cd frontend && npm ci`
- `cd frontend && npm run routes:generate`
- `cd frontend && npm run typecheck`
- `cd frontend && npm run lint`
- `cd frontend && npm run test`
- `cd frontend && npm run build`
- `cd frontend && npm run test:e2e`

## Mantenimiento

Actualizar estos archivos cuando cambien stack, rutas, endpoints, persistencia, seguridad, comandos o estructura:

- `docs/ai/PROJECT_CONTEXT.md`
- `docs/ai/MODULE_MAP.md`
- `docs/ai/NAMING.md`
- `docs/ai/PROJECT_INSTRUCTIONS.md`
