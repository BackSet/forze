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
- Template: `templates/technical-smoke.html`, `templates/cotizacion.html`, `templates/presupuesto-detallado.html`, `templates/resumen-capitulos.html`.
- API: `DocumentController` descarga cotización, presupuesto detallado y resumen de capítulos en PDF sin exponer datos confidenciales.

### `module.budgeting`

Paquetes: `domain.<area>` (entidades + enums + invariantes), `infrastructure` (repositories Spring Data + TenantContext), `api` y `application` (servicios de cálculo, control de acceso y APIs REST).
Implementadas todas las capas api/application por área. FKs desacopladas; integridad en BD. Migraciones `V2..V13` (`V13` = RBAC).

- Domain `admin`: `Organization` (`budgeting_organizations`), `Membership` (`budgeting_memberships`, rol como
  codigo string), `Role` (`budgeting_roles`, sistema/global o custom por org; `all_permissions` para ADMINISTRADOR),
  `Permission` (`budgeting_permissions`), mapeo `budgeting_role_permissions`; `UnitOfMeasure`
  (`budgeting_units_of_measure`), `Category` (`budgeting_categories`), `TaxConfig` (`budgeting_tax_configs`).
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
- Security (`security`): `ForzePermission` (permisos granulares por area: proyectos, presupuestos, catalogos,
  proveedores, aprobaciones, documentos, administracion, auditoria), `SecurityService` (resuelve organizacion
  activa via cabecera `X-Organization-Id`, valida membresia y permisos por rol), `TenantFilter` +
  `shared.TenantContext` (aislamiento por organizacion; los servicios filtran repositories por org activa).
- Bootstrap de organizacion: `POST /api/organizations` es el unico endpoint de negocio que NO exige
  `X-Organization-Id` (lo exime `TenantFilter`), para permitir crear la primera organizacion sin tenant.
  Requiere usuario autenticado (bearer), crea la `Organization` y registra al creador como `Membership`
  `ADMINISTRADOR` en una transaccion, y responde `OrganizationDto { id, name }`. CSRF esta deshabilitado
  (API stateless bearer; ver PROJECT_CONTEXT), por lo que no requiere token CSRF.
- Roles -> permisos: `ADMINISTRADOR` (todos), `PRESUPUESTISTA` (proyectos/presupuestos/catalogos/escenarios/
  documentos), `APROBADOR` (lectura + aprobar/observar/rechazar), `COMPRAS` (catalogos/proveedores/cotizaciones).
- API por area (REST, DTOs, validacion, sin exponer entidades JPA):
  `admin` (`OrganizationController`, `MembershipController` con proteccion de ultimo admin, `UserManagementController`,
  `CatalogConfigController`, `RoleController` -> `GET/POST/PUT/DELETE /api/roles` + `GET /api/permissions`,
  `AccessController` -> `GET /api/me/access` con rol y permisos efectivos),
  `project` (`ClientController`, `ProjectController`), `catalog` (`CatalogController`),
  `supplier` (`SupplierController`), `budget` (`BudgetController`), `scenario` (`ScenarioController`),
  `approval` (`ApprovalController`), `document` (`BudgetDocumentController`), `audit` (`AuditController`).
- Servicios de calculo (BigDecimal, scale/rounding centralizados, division-por-cero controlada):
  `ApuCalculationService`, `BudgetItemCalculationService`, `BudgetVersionCalculationService` (directos,
  indirectos, contingencia, utilidad, impuestos, margen, diferencia vs objetivo), `ScenarioCalculationService`
  (recalcula desde version base aplicando overrides), `ViabilityEvaluationService`
  (`VIABLE`/`VIABLE_CON_ALERTAS`/`NO_VIABLE`), `AlertGenerationService`.

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
  - Pestaña de Organización: `src/app/organization-tab.tsx`
  - Pestaña de Clientes y Proyectos: `src/app/clients-projects-tab.tsx`
  - Pestaña de Catálogo Técnico: `src/app/catalog-tab.tsx`
  - Pestaña de Proveedores: `src/app/suppliers-tab.tsx`
  - Pestaña de Versiones y Tasas: `src/app/budgets-tab.tsx`
  - Pestaña de Planilla Editor: `src/app/editor-tab.tsx`
  - Pestaña de Escenarios: `src/app/scenarios-tab.tsx`
  - Pestaña de Flujo Aprobación: `src/app/approvals-tab.tsx`
  - Pestaña de Documentos PDF: `src/app/documents-tab.tsx`
  - Pestaña de Auditoría: `src/app/audit-tab.tsx`

### Auth/API

- `lib/api/client.ts`: openapi-fetch + openapi-react-query; middleware `onRequest` (bearer + `X-Organization-Id`)
  y `onResponse` (sintetiza cuerpo JSON para respuestas de error vacias -> errores HTTP siempre disparan `onError`).
- `lib/api/errors.ts`: `normalizeApiError` (a `ApiError`) y `apiErrorMessage(error, fallback)`.
- `lib/auth/auth-api.ts`: login, refresh single-flight, `/me`, logout.
- `lib/auth/session-store.ts`: Zustand session/preferencias (incluye `role` y `permissions` efectivos de la org activa).
- `lib/auth/permissions.ts`: `usePermission`, `useHasAnyPermission`, `useIsAdministrator`, `useEffectiveAccess` (carga `/api/me/access`).
- `lib/auth/permission-gate.tsx`: `PermissionGate`. App Shell hace gating de menu por permiso e invalidacion de queries al cambiar organizacion.
- `app/access-state.ts`: `resolveAccessView`/`errorStatus` (funcion pura, testeada). El App Shell distingue
  estados de acceso del query `/api/me/access`: `loading` (pendiente o refetch tras cambiar org), `network-error`
  (fallo de red/CORS, recuperable con reintento), `stale-organization` (403 real -> volver al selector de
  organizacion), `forbidden` (permisos cargados sin acceso a la pestaña) y `allowed`. Un error de red/CORS nunca
  se muestra como 403.
- `lib/api/generated/schema.d.ts`: contrato TS de OpenAPI.

## Tests

- Backend: seguridad, contexto, Modulith, Testcontainers PostgreSQL, document renderer, JWT, token hashing, AuthService.
- Budgeting: `BudgetVersionInvariantTests` (invariante de inmutabilidad de version aprobada, sin Docker),
  `BudgetCalculationServiceTests` (calculos APU/version: valores normales, cero, redondeo BigDecimal; sin Docker),
  `MembershipServiceTests` (proteccion de ultimo admin: no degradar ni eliminar; rol desconocido; sin Docker),
  `SecurityServiceTests` (ADMINISTRADOR concede todos incl. nuevos; rol normal solo mapeados; custom > sistema; sin Docker) y
  `BudgetingPersistenceTests` (Testcontainers: migraciones + `validate`, precision numerica, unique por
  organizacion, FK `RESTRICT`, inmutabilidad de snapshot ante cambio de catalogo, cascade del arbol de
  version, bloqueo optimista en `BudgetVersion`). Los Testcontainers se omiten sin Docker.
- Frontend: home, router, login validation, auth store, refresh single-flight.
- Playwright: home, login, `/app`, reload/refresh y logout con red mockeada.
