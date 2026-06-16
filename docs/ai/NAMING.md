# NAMING

## Canonico

| Concepto | Nombre |
| --- | --- |
| Producto | FORZE |
| Repositorio | BackSet/forze |
| Rama base | `dev` |
| Paquete base | `com.backset.forze` |
| Backend app | `forze-backend` |
| Timezone | `America/Guayaquil` |
| DB local | `forze` |
| Access token | `access token` |
| Refresh token | `refresh token` |
| Cookie refresh | `forze_refresh` |

## Endpoints

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `GET /actuator/health`
- `GET /actuator/info`
- `GET /v3/api-docs`

## Entidades Tecnicas

- `UserAccount` -> tabla `identity_users`.
- `RefreshToken` -> tabla `identity_refresh_tokens`.
- `TechnicalSmokeDocument` -> DTO tecnico sin persistencia.

## Variables

- Backend: `SPRING_PROFILES_ACTIVE`, `APP_TIME_ZONE`, `SERVER_PORT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `FLYWAY_URL`, `FLYWAY_USER`, `FLYWAY_PASSWORD`, `JWT_ISSUER`, `JWT_SECRET`, `JWT_ACCESS_EXPIRATION`, `JWT_REFRESH_EXPIRATION`, `ADMIN_BOOTSTRAP_ENABLED`, `ADMIN_USERNAME`, `ADMIN_EMAIL`, `ADMIN_INITIAL_PASSWORD`, `CORS_ALLOWED_ORIGINS`, `OPENAPI_ENABLED`, `ACTUATOR_HEALTH_SHOW_DETAILS`, `TRACING_ENABLED`, `TRACING_SAMPLING_PROBABILITY`, `OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_SERVICE_NAME`.
- Frontend: `VITE_APP_NAME`, `VITE_APP_ENV`, `VITE_API_BASE_URL`, `VITE_REQUEST_TIMEOUT_MS`.

## Evitar

- Paquete historico `com.forze.backend`.
- Roles, permisos o estados de negocio no confirmados.
- Endpoints de presupuestacion ficticios.
- Edicion manual de `routeTree.gen.ts`.
- Edicion manual de `schema.d.ts` cuando `/v3/api-docs` este disponible.
