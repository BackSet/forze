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
  `ItemApuComponent` (`budgeting_item_apu_components`), `Measurement` (`budgeting_measurements`), `BudgetRisk` (`budgeting_budget_risks`);
  enums `BudgetStatus`, `ViabilityStatus`, `ItemValidationStatus`.
- Domain `scenario`: `Scenario` (`budgeting_scenarios`), `ScenarioOverride` (`budgeting_scenario_overrides`);
  enums `ScenarioType`, `RiskLevel`.
- Domain `approval`: `ApprovalRequest` (`budgeting_approval_requests`),
  `ApprovalComment` (`budgeting_approval_comments`); enum `ApprovalStatus`.
- Domain `document`: `BudgetDocument` (`budgeting_documents`); enums `DocumentType`, `DocumentFormat`.
- Domain `control`: `ControlBaseline` (`budgeting_control_baselines`), `ProgressEntry` (`budgeting_progress_entries`),
  `RealCost` (`budgeting_real_costs`), `Additional` (`budgeting_additionals`);
  enums `RealCostType`, `AdditionalStatus`.
- Domain `audit`: `AuditLogEntry` (`budgeting_audit_log`). `AuditService.log(...)` registra cambios
  administrativos: alta/cambio/baja de membresia (`ASSIGN_ROLE`/`UPDATE_ROLE`/`REMOVE_MEMBER`),
  creacion/actualizacion/borrado de roles personalizados (`CREATE`/`UPDATE`/`DELETE` sobre `Role`) y
  alta/activacion de cuentas (`CREATE`/`TOGGLE` sobre `UserAccount`) y los eventos del ciclo de aprobacion
  (`SUBMIT_APPROVAL`/`APPROVE`/`OBSERVE`/`REJECT` sobre `BudgetVersion`, con estado previo y nuevo).
  Nunca registra contraseñas ni tokens.
- Relaciones clave: `project 1-N budget 1-N budget_version 1-N {chapter, budget_item}`;
  `budget_item 1-1 item_apu 1-N item_apu_component`; `budget_item 1-N measurement`;
  `supplier 1-N quotation 1-N quotation_item`; `insumo 1-N price_history`;
  `budget_version 1-N {scenario, approval_request, document}`; `project 1-1 control_baseline 1-N {progress, real_cost, additional}`.
- Eliminacion: hijos privados del agregado en `ON DELETE CASCADE` (versiones, capitulos, items, APU,
  componentes, mediciones, items de cotizacion, overrides, progreso, costos, adicionales, comentarios);
  catalogos/config en `RESTRICT`; enlaces a snapshots (`source_*`, `chapter_id`, `budget_item_id`) en `SET NULL`.
- Flujos soportados: crear proyecto con monto objetivo; presupuesto por capitulos/rubros; APU con snapshot
  de precios; reutilizacion (`rubro_relations`, `keywords`); escenarios; envio/aprobacion con version
  inmutable; documentos; control de obra con linea base; auditoria. La inmutabilidad de una version
  `APROBADO` se aplica tanto en el agregado `BudgetVersion` (`ensureEditable`) como en `BudgetService`
  para el arbol de snapshot que no pasa por el agregado (capitulos, rubros, APU de item, componentes,
  mediciones): toda mutacion valida `requireEditableVersion`. Solo `copyVersion` produce una copia editable.
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
- Servicios de calculo (BigDecimal, division-por-cero controlada). Redondeo centralizado en
  `BudgetRounding` (`MONEY_SCALE=2`, `UNIT_SCALE=4`, `HALF_UP`; helpers `money`/`unit`/`divideUnit`)
  usado por todo el pipeline (`ApuCalculationService`, `BudgetItemCalculationService`,
  `BudgetVersionCalculationService`, `ScenarioCalculationService`, `BudgetService`):
  `ApuCalculationService`, `BudgetItemCalculationService`, `BudgetVersionCalculationService` (directos,
  indirectos, contingencia, utilidad, impuestos, margen, diferencia vs objetivo), `ScenarioCalculationService`
  (recalcula desde version base aplicando overrides), `ViabilityEvaluationService`
  (`VIABLE`/`VIABLE_CON_ALERTAS`/`NO_VIABLE`), `AlertGenerationService`.
- Mediciones estructuradas alimentan la cantidad del rubro: al agregar/eliminar una medicion, `BudgetService`
  recomputa `BudgetItem.quantity` como suma de los resultados de las mediciones (computo metrico = fuente de la
  cantidad). El editor invalida la query de items tras cada mutacion de medicion para reflejar la cantidad.

### `shared.api`

- `ApiException`.
- `ApiExceptionHandler` con `ProblemDetail`.

## Frontend

### App Shell

- `src/main.tsx`, `components/providers.tsx`, `router.ts`, `routes/__root.tsx`.
- Tema: `lib/theme.ts` (preferencia `light`/`dark`/`system`, persistida en `localStorage` `forze-theme`,
  aplicada via `data-theme` en `:root`; `system` sigue `prefers-color-scheme`). El estado vive en el
  session-store (`theme`/`setTheme`); `main.tsx` aplica al cargar (sin FOUC) y escucha cambios del SO.
  `components/theme-toggle.tsx` cicla los 3 modos (icono + label accesible, no solo color).
- Shell responsive: sidebar `hidden md:flex`; en movil la navegacion va en `components/ui/drawer.tsx`
  (hamburguesa en el header, cierra con Esc/backdrop). El selector de proyecto se oculta en movil.

### Componentes UI reutilizables (canónicos, sin segunda librería)

- `components/ui/`: `button`, `input` (base), y los primitivos: `code-field` (input + "Generar" opcional
  via `onGenerate`, backend = fuente de unicidad), `form-section`, `page-header`, `empty-state`,
  `status-badge` (cva; significado por texto, no solo color), `quick-actions-bar`, `data-toolbar`
  (busqueda + acciones), `confirm-action` (confirmacion explicita sin librería de dialogos), `drawer`
  (slide-over general izquierda/derecha; la nav movil lo usa via hamburguesa `md:hidden`).
  Reglas: foco visible (`focus-visible:ring`), `motion-reduce`, tipado estricto, tokens de tema.
- Clientes y Proyectos (`app/clients-projects-tab.tsx`): formulario en `Drawer` por `FormSection`
  (identificacion/cliente/planificacion/financieros/descripcion); `CodeField` con "Generar" que llama
  `GET /api/projects/next-code` via el cliente tipado (`queryClient.fetchQuery`) y pide confirmacion antes de
  sobrescribir un codigo manual; alta rapida de cliente desde el formulario; archivar con `ConfirmAction`;
  error de codigo duplicado visible en el campo. Sin `document.getElementById` (selects controlados).

### Rutas

- `/`: `components/home-page.tsx`.
- `/login`: `features/auth/login-page.tsx`.
- `/app`: `app/app-page.tsx`. App Shell con menu lateral por permiso, selector de organizacion y
  selector de proyecto activo (header, gated `PROYECTOS_READ`, alimenta budget/editor/escenario/aprobacion).
  Command palette global (`app/command-palette.tsx`, `cmdk`, atajo ⌘K/Ctrl+K): navega a superficies legibles
  y ofrece creaciones rapidas, filtradas por permiso. Pestaña por defecto: Inicio.
  - Inicio (dashboard operativo): `src/app/home-tab.tsx`. KPIs por permiso (proyectos/catalogo/proveedores/
    miembros desde sus list endpoints), actividad reciente (`/api/audit-logs`), tareas pendientes (proyectos
    activos sin presupuesto, derivado de `/api/projects`), alertas criticas (puntero accionable: se evaluan por
    version de presupuesto) y creacion rapida. Sin KPIs decorativos: cada dato tiene endpoint fuente o estado
    vacio accionable.
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
  `BudgetVersionImmutabilityTests` (capitulos/rubros: rechazo de mutacion sobre version APROBADO en la capa de
  servicio, alta permitida en BORRADOR; sin Docker),
  `BudgetCalculationServiceTests` (calculos APU/version: valores normales, cero, redondeo BigDecimal; sin Docker),
  `ViabilityEvaluationServiceTests` (limites: sale==target, margen==minimo, costo==venta, venta cero; sin Docker),
  `ApprovalServiceTests` (audita SUBMIT/APPROVE/REJECT; rechaza enviar NO_VIABLE; sin Docker),
  `MembershipServiceTests` (proteccion de ultimo admin: no degradar ni eliminar; rol desconocido; sin Docker),
  `SecurityServiceTests` (ADMINISTRADOR concede todos incl. nuevos; rol normal solo mapeados; custom > sistema; sin Docker) y
  `BudgetingPersistenceTests` (Testcontainers: migraciones + `validate`, precision numerica, unique por
  organizacion, FK `RESTRICT`, inmutabilidad de snapshot ante cambio de catalogo, cascade del arbol de
  version, bloqueo optimista en `BudgetVersion`). Los Testcontainers se omiten sin Docker.
- Frontend: home, router, login validation, auth store, refresh single-flight.
- Playwright (red mockeada con `page.route`): `home.spec` (home, login, `/app`, reload/refresh, logout);
  `commercial-flow.spec` (login -> entrar a organizacion -> Inicio -> superficies de Aprobaciones y Documentos,
  gated por permiso). Las reglas de datos del ciclo (enviar solo viable, aprobar bloquea, auditoria, PDF sin
  costos) se fijan en tests backend; el E2E cubre la navegacion autenticada del ciclo.
