# MODULE_MAP

## Backend

### `configuration`

- Propiedades: `AppProperties`, `BootstrapProperties`, `CorsProperties`, `JwtProperties`, `SecurityProperties`.
- Seguridad: `SecurityConfiguration`.
- OpenAPI: `OpenApiConfiguration`.
- Tiempo: `TimeConfiguration`.

### `module.identity`

- API: `AuthController`, `LoginRequest`, `AuthTokenResponse`, `MeResponse`.
- Application: `AuthService`, `AdminBootstrapper`, `AuthTokens`, `AuthenticatedUser`.
- Domain: `UserAccount`, `RefreshToken`.
- Infrastructure: repositories JPA, `JwtService`, `JwtAuthenticationFilter`, `RefreshTokenGenerator`, `TokenHashing`, `UserPrincipal`.
- Persistencia:
  - `identity_users`
  - `identity_refresh_tokens`
- Flujos:
  - login valida credenciales sin revelar existencia de usuario;
  - refresh rota token y revoca el anterior;
  - reuse detection marca familia;
  - logout revoca refresh actual;
  - `/me` requiere access token.

### `module.document`

- Application: `DocumentRenderer`, `TechnicalSmokeDocument`.
- Domain: `DocumentRenderException`.
- Infrastructure: `ThymeleafPdfDocumentRenderer`.
- Template: `templates/technical-smoke.html`.
- No hay endpoint de negocio PDF.

### `shared.api`

- `ApiException`.
- `ApiExceptionHandler` con `ProblemDetail`.

## Frontend

### App Shell

- `src/main.tsx`, `components/providers.tsx`, `router.ts`, `routes/__root.tsx`.

### Rutas

- `/`: `components/home-page.tsx`.
- `/login`: `features/auth/login-page.tsx`.
- `/app`: `app/app-page.tsx`.

### Auth/API

- `lib/api/client.ts`: openapi-fetch + openapi-react-query.
- `lib/api/errors.ts`: normalizacion de errores.
- `lib/auth/auth-api.ts`: login, refresh single-flight, `/me`, logout.
- `lib/auth/session-store.ts`: Zustand session/preferencias.
- `lib/api/generated/schema.d.ts`: contrato TS de OpenAPI.

## Tests

- Backend: seguridad, contexto, Modulith, Testcontainers PostgreSQL, document renderer, JWT, token hashing, AuthService.
- Frontend: home, router, login validation, auth store, refresh single-flight.
- Playwright: home, login, `/app`, reload/refresh y logout con red mockeada.
