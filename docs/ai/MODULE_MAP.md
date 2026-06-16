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

### `module.budgeting`

Paquetes: `domain.<area>` (entidades + enums + invariantes) e `infrastructure` (repositories Spring Data).
Sin `api`/`application` todavia (esta tarea no incluye endpoints). Todas las FK se guardan como `UUID`
planos para desacoplar agregados; la integridad se aplica en el esquema. Migraciones `V2..V11`.

- Domain `admin`: `Organization` (`budgeting_organizations`), `UnitOfMeasure` (`budgeting_units_of_measure`),
  `Category` (`budgeting_categories`), `TaxConfig` (`budgeting_tax_configs`).
- Domain `catalog`: `Insumo` (`budgeting_insumos`), `RubroMaestro` (`budgeting_rubros_maestros`),
  `ApuMaestro` (`budgeting_apu_maestros`), `ApuComponent` (`budgeting_apu_components`);
  enums `CatalogStatus`, `InsumoType`, `ApuStatus`, `ComponentSection`; join `budgeting_rubro_relations`.
- Domain `supplier`: `Supplier` (`budgeting_suppliers`), `Quotation` (`budgeting_quotations`),
  `QuotationItem` (`budgeting_quotation_items`), `PriceHistory` (`budgeting_price_history`);
  enums `SupplierStatus`, `QuotationStatus`.
- Domain `project`: `Client` (`budgeting_clients`), `Project` (`budgeting_projects`),
  `ProjectTeamMember` (`budgeting_project_team`); enum `ProjectStatus`.
- Domain `budget`: `Budget` (`budgeting_budgets`), `BudgetVersion` (`budgeting_budget_versions`),
  `Chapter` (`budgeting_chapters`), `BudgetItem` (`budgeting_budget_items`), `ItemApu` (`budgeting_item_apu`),
  `ItemApuComponent` (`budgeting_item_apu_components`), `Measurement` (`budgeting_measurements`);
  enums `BudgetStatus`, `ViabilityStatus`, `ItemValidationStatus`.
- Domain `scenario`: `Scenario` (`budgeting_scenarios`), `ScenarioOverride` (`budgeting_scenario_overrides`);
  enums `ScenarioType`, `RiskLevel`.
- Domain `approval`: `ApprovalRequest` (`budgeting_approval_requests`),
  `ApprovalComment` (`budgeting_approval_comments`); enum `ApprovalStatus`.
- Domain `document`: `BudgetDocument` (`budgeting_documents`); enums `DocumentType`, `DocumentFormat`.
- Domain `control`: `ControlBaseline` (`budgeting_control_baselines`), `ProgressEntry` (`budgeting_progress_entries`),
  `RealCost` (`budgeting_real_costs`), `Additional` (`budgeting_additionals`);
  enums `RealCostType`, `AdditionalStatus`.
- Domain `audit`: `AuditLogEntry` (`budgeting_audit_log`).
- Relaciones clave: `project 1-N budget 1-N budget_version 1-N {chapter, budget_item}`;
  `budget_item 1-1 item_apu 1-N item_apu_component`; `budget_item 1-N measurement`;
  `supplier 1-N quotation 1-N quotation_item`; `insumo 1-N price_history`;
  `budget_version 1-N {scenario, approval_request, document}`; `project 1-1 control_baseline 1-N {progress, real_cost, additional}`.
- Eliminacion: hijos privados del agregado en `ON DELETE CASCADE` (versiones, capitulos, items, APU,
  componentes, mediciones, items de cotizacion, overrides, progreso, costos, adicionales, comentarios);
  catalogos/config en `RESTRICT`; enlaces a snapshots (`source_*`, `chapter_id`, `budget_item_id`) en `SET NULL`.
- Flujos soportados: crear proyecto con monto objetivo; presupuesto por capitulos/rubros; APU con snapshot
  de precios; reutilizacion (`rubro_relations`, `keywords`); escenarios; envio/aprobacion con version
  inmutable; documentos; control de obra con linea base; auditoria.

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
- Budgeting: `BudgetVersionInvariantTests` (invariante de inmutabilidad de version aprobada, sin Docker) y
  `BudgetingPersistenceTests` (Testcontainers: migraciones + `validate`, precision numerica, unique por
  organizacion, FK `RESTRICT`, inmutabilidad de snapshot ante cambio de catalogo, cascade del arbol de
  version, bloqueo optimista en `BudgetVersion`). Los Testcontainers se omiten sin Docker.
- Frontend: home, router, login validation, auth store, refresh single-flight.
- Playwright: home, login, `/app`, reload/refresh y logout con red mockeada.
