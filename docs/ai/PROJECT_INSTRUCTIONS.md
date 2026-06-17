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
- Flyway es propietario del esquema; Hibernate solo valida. No editar migraciones historicas `V1..V13`; nuevas migraciones desde `V14`.
- Autorizacion RBAC persistente: permisos y roles son datos (`budgeting_permissions`, `budgeting_roles`,
  `budgeting_role_permissions`); autorizar por permiso (no por rol) via `@PreAuthorize("@securityService.hasPermission('...')")`.
  El rol de sistema `ADMINISTRADOR` (`all_permissions=true`) siempre tiene todos los permisos registrados, no es editable
  ni eliminable, y no se puede degradar ni eliminar al ultimo `ADMINISTRADOR` de una organizacion.
- Budgeting: dinero en `BigDecimal`/`numeric` (unitarios `18,4`, totales `18,2` HALF_UP, porcentajes `7,4`,
  cantidades `18,4`, rendimientos/desperdicio `18,6`); moneda ISO-4217 por presupuesto sin tipos de cambio;
  enums via `varchar`+`CHECK`; tablas con prefijo `budgeting_`; FK como `UUID` planos.
- Budgeting: una version de presupuesto `APROBADO` es inmutable; los precios se congelan como snapshot en
  la version y no se reconstruyen desde catalogos/precios actuales.
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
