# MODULE_MAP

## Alcance

- Proyecto: FORZE.
- Rama: `dev`.
- Estado: monorepo inicial con cimientos tecnicos. No hay modulos funcionales de dominio implementados.

## Modulos Tecnicos

### Backend Application

- Entrada: `backend/src/main/java/com/backset/forze/ForzeApplication.java`.
- Proposito: arrancar la aplicacion Spring Boot y escanear propiedades.
- Tests: `ForzeApplicationTests`, `ModulithArchitectureTests`, `PostgresContainerTests`.

### Backend Config

- Paquete: `com.backset.forze.config`.
- `CorsProperties`: origenes permitidos para CORS.
- `SecurityConfig`: permite health/info/OpenAPI y deniega todo lo demas.
- `OpenApiConfig`: metadatos del contrato OpenAPI.

### Backend Web

- Paquete: `com.backset.forze.web`.
- `ApiExceptionHandler`: manejo global de errores de validacion con `ProblemDetail`.
- Endpoints de dominio: ninguno confirmado.

### Frontend App Shell

- Entrada: `frontend/src/main.tsx`.
- Providers: `frontend/src/components/providers.tsx`.
- Router: `frontend/src/router.ts`, `frontend/src/routes/__root.tsx`, `frontend/src/routes/index.tsx`.
- Pantalla inicial: `frontend/src/components/home-page.tsx`.

### Frontend API Client

- Cliente: `frontend/src/lib/api/client.ts`.
- Contrato generado: `frontend/src/lib/api/generated/schema.d.ts`.
- Fuente de verdad: backend `/v3/api-docs`.

### Frontend UI Foundation

- Estilos globales y tokens: `frontend/src/index.css`.
- Utilidades: `frontend/src/lib/utils.ts`.
- Componentes confirmados:
  - `components/ui/button.tsx`
  - `components/theme-toggle.tsx`
  - `components/home-page.tsx`

## Flujos Confirmados

- Health backend: `GET /actuator/health` responde `UP`.
- OpenAPI backend: `GET /v3/api-docs` responde contrato OpenAPI 3.1.0 sin paths de dominio.
- Generacion de tipos: `npm run openapi:generate` crea `schema.d.ts` desde el backend activo.
- Frontend inicial: ruta `/` muestra los cimientos tecnicos de FORZE.

## Modulos De Dominio Pendientes

- Proyectos, clientes, presupuestos, capitulos, rubros, APU, insumos, proveedores, costos historicos, versiones, aprobaciones, exportaciones y ejecucion son conceptos de producto confirmados en `PRODUCT.md`, pero no tienen implementacion en codigo.
- No hay entidades JPA, tablas, migraciones, controllers, services, repositories, permisos o rutas frontend de dominio.
