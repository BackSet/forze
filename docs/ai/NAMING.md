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

## Budgeting: termino funcional -> clase -> tabla

| Termino del diseno | Clase Java | Tabla |
| --- | --- | --- |
| Organizacion | `Organization` | `budgeting_organizations` |
| Unidad de medida | `UnitOfMeasure` | `budgeting_units_of_measure` |
| Categoria | `Category` | `budgeting_categories` |
| Impuesto configurable | `TaxConfig` | `budgeting_tax_configs` |
| Insumo | `Insumo` | `budgeting_insumos` |
| Rubro (catalogo) | `RubroMaestro` | `budgeting_rubros_maestros` |
| APU (catalogo) | `ApuMaestro` | `budgeting_apu_maestros` |
| Componente de APU | `ApuComponent` | `budgeting_apu_components` |
| Proveedor | `Supplier` | `budgeting_suppliers` |
| Cotizacion | `Quotation` | `budgeting_quotations` |
| Producto cotizado | `QuotationItem` | `budgeting_quotation_items` |
| Precio historico | `PriceHistory` | `budgeting_price_history` |
| Cliente | `Client` | `budgeting_clients` |
| Proyecto | `Project` | `budgeting_projects` |
| Equipo del proyecto | `ProjectTeamMember` | `budgeting_project_team` |
| Presupuesto | `Budget` | `budgeting_budgets` |
| Version de presupuesto | `BudgetVersion` | `budgeting_budget_versions` |
| Capitulo / subcapitulo | `Chapter` | `budgeting_chapters` |
| Rubro del presupuesto | `BudgetItem` | `budgeting_budget_items` |
| APU del rubro (snapshot) | `ItemApu` | `budgeting_item_apu` |
| Componente del APU (snapshot) | `ItemApuComponent` | `budgeting_item_apu_components` |
| Medicion | `Measurement` | `budgeting_measurements` |
| Escenario | `Scenario` | `budgeting_scenarios` |
| Variable de escenario | `ScenarioOverride` | `budgeting_scenario_overrides` |
| Solicitud de aprobacion | `ApprovalRequest` | `budgeting_approval_requests` |
| Comentario de aprobacion | `ApprovalComment` | `budgeting_approval_comments` |
| Documento de cliente | `BudgetDocument` | `budgeting_documents` |
| Linea base de obra | `ControlBaseline` | `budgeting_control_baselines` |
| Avance fisico | `ProgressEntry` | `budgeting_progress_entries` |
| Costo real | `RealCost` | `budgeting_real_costs` |
| Adicional | `Additional` | `budgeting_additionals` |
| Riesgo de presupuesto | `BudgetRisk` | `budgeting_budget_risks` |
| Auditoria | `AuditLogEntry` | `budgeting_audit_log` |

## Budgeting: estados (enum -> valores)

- `BudgetStatus`: `BORRADOR, EN_CALCULO, REQUIERE_AJUSTES, PENDIENTE_APROBACION, APROBADO, ENVIADO, ACEPTADO, RECHAZADO, ARCHIVADO`.
- `PriceStatus`: `BORRADOR, VIGENTE, POR_VERIFICAR, VENCIDO, BLOQUEADO`.
- `ViabilityStatus`: `VIABLE, VIABLE_CON_ALERTAS, NO_VIABLE`.
- `ApuStatus`: `BORRADOR, VALIDADO, VIGENTE, OBSOLETO, ARCHIVADO`.
- `InsumoType`: `MATERIAL, MANO_DE_OBRA, EQUIPO, TRANSPORTE, SUBCONTRATO, OTRO`.
- `ComponentSection`: `MATERIALES, MANO_DE_OBRA, EQUIPOS, TRANSPORTE, SUBCONTRATOS, OTROS`.
- `ScenarioType`: `BASE, ECONOMICO, RECOMENDADO, CONSERVADOR, PERSONALIZADO`.
- `RiskLevel`: `ALTO, MEDIO, BAJO`.
- `ApprovalStatus`: `PENDIENTE_APROBACION, OBSERVADO, APROBADO, RECHAZADO`.
- `DocumentType`: `COTIZACION, PRESUPUESTO_DETALLADO, RESUMEN_CAPITULOS, TERMINOS_CONDICIONES, CRONOGRAMA_VALORIZADO, PROPUESTA_ECONOMICA`.
- `DocumentFormat`: `PDF, EXCEL, CSV`.
- `RealCostType`: `COMPRA, MANO_DE_OBRA, EQUIPO, SUBCONTRATO, INDIRECTO, ADICIONAL`.
- `AdditionalStatus`: `PROPUESTO, EN_REVISION, APROBADO, RECHAZADO, EJECUTADO, FACTURADO`.
- Estados derivados de la accion "archivar"/alertas (no enumerados en el diseno):
  `CatalogStatus`/`ProjectStatus` = `ACTIVO, ARCHIVADO`; `SupplierStatus` = `ACTIVO, INACTIVO`;
  `QuotationStatus` = `VIGENTE, EXPIRADA`; `ItemValidationStatus` = `COMPLETO, INCOMPLETO`.

## Variables

- Backend: `SPRING_PROFILES_ACTIVE`, `APP_TIME_ZONE`, `SERVER_PORT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `FLYWAY_URL`, `FLYWAY_USER`, `FLYWAY_PASSWORD`, `JWT_ISSUER`, `JWT_SECRET`, `JWT_ACCESS_EXPIRATION`, `JWT_REFRESH_EXPIRATION`, `ADMIN_BOOTSTRAP_ENABLED`, `ADMIN_USERNAME`, `ADMIN_EMAIL`, `ADMIN_INITIAL_PASSWORD`, `CORS_ALLOWED_ORIGINS`, `OPENAPI_ENABLED`, `ACTUATOR_HEALTH_SHOW_DETAILS`, `TRACING_ENABLED`, `TRACING_SAMPLING_PROBABILITY`, `OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_SERVICE_NAME`.
- Frontend: `VITE_APP_NAME`, `VITE_APP_ENV`, `VITE_API_BASE_URL`, `VITE_REQUEST_TIMEOUT_MS`.

## Roles y Permisos

- Roles del sistema (canónicos, globales, `is_system=true`): `ADMINISTRADOR, PRESUPUESTISTA, APROBADOR, COMPRAS`.
  `ADMINISTRADOR` tiene `all_permissions=true` (siempre todos los permisos). Las organizaciones pueden crear
  roles personalizados (por organización; código en MAYUSCULAS_CON_GUION_BAJO).
- Permisos granulares (códigos reales en `ForzePermission` y tabla `budgeting_permissions`):
  - Proyectos: `PROYECTOS_READ, PROYECTOS_WRITE`.
  - Presupuestos: `PRESUPUESTOS_READ, PRESUPUESTOS_WRITE`.
  - Catálogos: `CATALOGOS_READ, CATALOGOS_WRITE`.
  - Proveedores: `PROVEEDORES_READ, PROVEEDORES_WRITE`.
  - Aprobaciones: `APROBACIONES_READ, APROBACIONES_WRITE`.
  - Documentos: `DOCUMENTOS_READ, DOCUMENTOS_WRITE`.
  - Administración: `ADMINISTRACION_READ, ADMINISTRACION_WRITE`.
  - Auditoría: `AUDITORIA_READ`.

## Formatos de código (generación backend)

`CodeGenerationService` sugiere el siguiente código por entidad (no lo reserva; la unicidad final la
garantizan las restricciones únicas al guardar). NAMING.md no definía formato previo, así que se adopta:

- Proyecto: `PRY-AAAA-0001` (único por organización; `uq_budgeting_projects_org_code`).
- Presupuesto: `PRE-AAAA-0001` (único **por proyecto**; `uq_budgeting_budgets_project_code`).
- Insumo: `INS-0001` · APU: `APU-0001` · Rubro: `RUB-0001` (únicos por organización).

Endpoints (permiso de escritura de la entidad; 403 sin permiso): `GET /api/projects/next-code`
(`PROYECTOS_WRITE`), `GET /api/projects/{projectId}/budgets/next-code` (`BUDGETS_WRITE`, igual que crear
presupuesto), `GET /api/insumos|apuses|rubros/next-code` (`CATALOGOS_WRITE`). El generador ignora códigos
que no calzan con su patrón (manuales o demo). Un código manual duplicado devuelve error claro (`400`); una
colisión concurrente al guardar devuelve `409` (handler de `DataIntegrityViolationException`).

## Evitar

- Paquete historico `com.forze.backend`.
- Roles, permisos o estados de negocio no confirmados.
- Endpoints de presupuestacion ficticios.
- Variantes prohibidas de budgeting: `presupuesto`/`budgets` sin prefijo `budgeting_`; `line_item`/`bom`
  para rubro (usar `BudgetItem`/`budgeting_budget_items`); `unit_price` en `double`/`float`;
  enum por `ordinal`; estados de presupuesto en ingles (`DRAFT`, `APPROVED`, ...): los estados son los
  del diseno en espanol/mayusculas; traducir terminos ya aprobados (Insumo, Rubro, APU se mantienen).
- Edicion manual de `routeTree.gen.ts`.
- Edicion manual de `schema.d.ts` cuando `/v3/api-docs` este disponible.
