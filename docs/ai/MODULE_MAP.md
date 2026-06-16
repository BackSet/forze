# MODULE_MAP

## Alcance de este mapa

- Fecha de auditoria: 2026-06-16.
- Proyecto: FORZE.
- Repositorio: BackSet/forze [verificado en Git].
- Rama analizada: `dev` [verificado en Git].
- Estado del producto: backend Spring Boot inicial sin modulos funcionales de negocio confirmados [verificado en Git].

## Inventario de modulos confirmados

### Backend Application

- Nombre canonico: Backend Application [inferido desde `BackendApplication` y `spring.application.name=backend`].
- Proposito: arrancar la aplicacion Spring Boot del backend [verificado en Git].
- Rutas frontend: ninguna confirmada [verificado en Git].
- Paginas o componentes de entrada: no aplica; no hay frontend implementado [verificado en Git].
- Endpoints, controllers o handlers: ninguno confirmado [verificado en Git].
- Services o casos de uso: ninguno confirmado [verificado en Git].
- Entidades, modelos, tablas o persistencia: ninguno confirmado [verificado en Git].
- Permisos o roles: ninguno confirmado [verificado en Git].
- Dependencias con otros modulos: usa auto-configuracion y dependencias declaradas en `backend/pom.xml` [verificado en Git].
- Tests relacionados: `backend/src/test/java/com/forze/backend/BackendApplicationTests.java`, prueba `contextLoads` [verificado en Git].
- Documentacion relacionada: `backend/HELP.md` contiene referencias generadas a Spring Boot, Maven, Flyway, Security, JPA, Web y Validation [verificado en Git].

## Modulos tecnicos declarados por dependencia

Estos elementos estan presentes como capacidades declaradas en el manifiesto Maven, pero no tienen implementacion propia confirmada en codigo del proyecto.

### Web MVC

- Evidencia: `spring-boot-starter-webmvc` en `backend/pom.xml` [verificado en Git].
- Endpoints/controllers/handlers: ninguno confirmado [verificado en Git].
- Flujos criticos: pendiente de confirmar.

### Data JPA

- Evidencia: `spring-boot-starter-data-jpa` en `backend/pom.xml` [verificado en Git].
- Repositories: ninguno confirmado [verificado en Git].
- Entidades/tablas: ninguna confirmada [verificado en Git].
- Persistencia: pendiente de confirmar.

### Flyway

- Evidencia: `spring-boot-starter-flyway` y `flyway-database-postgresql` en `backend/pom.xml` [verificado en Git].
- Migraciones: ninguna confirmada [verificado en Git].
- Flujos de migracion: pendiente de confirmar.

### Security

- Evidencia: `spring-boot-starter-security` y `spring-boot-starter-security-test` en `backend/pom.xml` [verificado en Git].
- Configuracion propia: ninguna confirmada [verificado en Git].
- Roles/permisos: ninguno confirmado [verificado en Git].
- Autenticacion/autorizacion: pendiente de confirmar.

### Validation

- Evidencia: `spring-boot-starter-validation` en `backend/pom.xml` [verificado en Git].
- DTOs o validaciones personalizadas: ninguna confirmada [verificado en Git].

### PostgreSQL Driver

- Evidencia: dependencia `org.postgresql:postgresql` con scope `runtime` en `backend/pom.xml` [verificado en Git].
- Datasource: no configurado en archivos rastreados [verificado en Git].
- Tablas/indices/constraints: ninguno confirmado [verificado en Git].

## Relaciones y dependencias entre modulos

- `BackendApplication` depende del paquete base `com.forze.backend` y de la auto-configuracion de Spring Boot [verificado en Git].
- No hay relaciones funcionales entre modulos de dominio porque no existen modulos de dominio confirmados [verificado en Git].
- Las capacidades Web, JPA, Flyway, Security, Validation y PostgreSQL estan declaradas en Maven, pero su uso funcional esta pendiente de confirmar [verificado en Git].

## Flujos criticos confirmados

- Arranque de aplicacion Spring Boot desde `BackendApplication.main` [verificado en Git].
- Carga de contexto Spring en test `BackendApplicationTests.contextLoads` [verificado en Git].
- No hay flujos de negocio, autenticacion, persistencia, API, UI, pagos, notificaciones u otros flujos criticos confirmados [verificado en Git].

## Zonas de busqueda

### Frontend

- `frontend/`: carpeta existente sin archivos rastreados [verificado en Git].

### Backend

- `backend/src/main/java/com/forze/backend/`: codigo Java productivo [verificado en Git].
- `backend/src/main/resources/`: configuracion y futuros recursos de aplicacion [verificado en Git].
- `backend/src/test/java/com/forze/backend/`: tests Java [verificado en Git].
- `backend/pom.xml`: dependencias, version de Java y plugins Maven [verificado en Git].

### Base de datos

- No hay carpeta de migraciones confirmada. Si se usa Flyway con convenciones Spring Boot, la ubicacion esperada seria `backend/src/main/resources/db/migration`, pero esa ruta no existe actualmente [verificado en Git, inferido].

### Documentacion

- `backend/HELP.md`: referencias generadas por Spring Initializr [verificado en Git].
- `docs/ai/`: contexto tecnico canonico para Agentes [verificado en Git].

## Integraciones externas

- PostgreSQL: dependencia runtime declarada, sin configuracion de conexion confirmada [verificado en Git].
- Servicios externos adicionales: ninguno confirmado [verificado en Git].

## Pendientes de confirmar

- Modulos funcionales del dominio.
- Rutas frontend y paginas.
- API publica o privada.
- Controllers, handlers, services, repositories, entidades, DTOs y mappers.
- Permisos, roles, estados, enums y constantes de dominio.
- Esquema de base de datos, migraciones y seeds.
- Integraciones externas reales.
- Flujos criticos del negocio.
