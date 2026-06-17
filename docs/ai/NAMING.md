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
| Auditoria | `AuditLogEntry` | `budgeting_audit_log` |

## Budgeting: estados (enum -> valores)

- `BudgetStatus`: `BORRADOR, EN_CALCULO, REQUIERE_AJUSTES, PENDIENTE_APROBACION, APROBADO, ENVIADO, ACEPTADO, RECHAZADO, ARCHIVADO`.
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

- Roles de membresía: `ADMINISTRADOR, PRESUPUESTISTA, APROBADOR, COMPRAS`.
- Permisos granulares:
  - Proyectos: `READ_PROJECTS, WRITE_PROJECTS`.
  - Presupuestos: `READ_BUDGETS, WRITE_BUDGETS`.
  - Catálogo: `READ_CATALOG, WRITE_CATALOG`.
  - Proveedores: `READ_SUPPLIERS, WRITE_SUPPLIERS`.
  - Aprobaciones: `READ_APPROVALS, WRITE_APPROVALS`.
  - Documentos: `READ_DOCUMENTS, WRITE_DOCUMENTS`.
  - Administración: `READ_ADMIN, WRITE_ADMIN`.
  - Auditoría: `READ_AUDIT`.

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
