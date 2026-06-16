# PROJECT_CONTEXT

## Identidad y alcance

- Fecha de auditoria: 2026-06-16.
- Proyecto: FORZE.
- Repositorio: BackSet/forze (`origin` apunta a `https://github.com/BackSet/forze.git`) [verificado en Git].
- Rama principal de analisis: `dev` [verificado en Git].
- Entorno relevante: local.
- Ruta de contexto IA: `docs/ai`.
- Tipo de repositorio: repositorio con estructura potencialmente monorepo por carpetas raiz `backend/`, `frontend/` y `docs/`; solo `backend/` contiene archivos de aplicacion rastreados actualmente [verificado en Git, inferido].
- Dominio funcional: pendiente de confirmar. El codigo y la documentacion actuales no describen reglas de negocio, usuarios, procesos ni casos de uso del producto.

## Estructura principal

- `backend/`: aplicacion Java Spring Boot inicial [verificado en Git].
- `backend/src/main/java/com/forze/backend/BackendApplication.java`: punto de entrada de Spring Boot [verificado en Git].
- `backend/src/main/resources/application.properties`: configuracion local minima con `spring.application.name=backend` [verificado en Git].
- `backend/src/test/java/com/forze/backend/BackendApplicationTests.java`: prueba `contextLoads` con `@SpringBootTest` [verificado en Git].
- `backend/pom.xml`: manifiesto Maven del backend [verificado en Git].
- `backend/mvnw` y `backend/mvnw.cmd`: Maven Wrapper [verificado en Git].
- `backend/.mvn/wrapper/maven-wrapper.properties`: wrapper Maven `3.9.16`, wrapper version `3.3.4` [verificado en Git].
- `backend/HELP.md`: documentacion generada de referencia Spring Initializr [verificado en Git].
- `frontend/`: carpeta existente sin archivos rastreados ni implementacion confirmada [verificado en Git].
- `docs/`: carpeta existente; antes de esta auditoria no contenia archivos rastreados [verificado en Git].
- `docs/ai/`: ruta de contexto tecnico IA creada/auditada por esta tarea [verificado en Git].
- Infraestructura: no hay `Dockerfile`, Compose, workflows de GitHub Actions, Makefile ni archivos de despliegue confirmados [verificado en Git].
- Migraciones: no hay archivos de migracion Flyway confirmados bajo `backend/src/main/resources` [verificado en Git].

## Stack confirmado

### Backend

- Java `26`, configurado en `backend/pom.xml` como `<java.version>26</java.version>` [verificado en Git].
- Maven con Maven Wrapper [verificado en Git].
- Maven Wrapper descarga Apache Maven `3.9.16` [verificado en Git].
- Spring Boot `4.1.0`, heredado de `spring-boot-starter-parent` [verificado en Git].
- Paquete base Java: `com.forze.backend` [verificado en Git].
- Dependencias productivas declaradas:
  - `spring-boot-starter-data-jpa` [verificado en Git].
  - `spring-boot-starter-flyway` [verificado en Git].
  - `spring-boot-starter-security` [verificado en Git].
  - `spring-boot-starter-validation` [verificado en Git].
  - `spring-boot-starter-webmvc` [verificado en Git].
  - `flyway-database-postgresql` [verificado en Git].
  - `spring-boot-devtools` con scope `runtime` y `optional=true` [verificado en Git].
  - `postgresql` con scope `runtime` [verificado en Git].
  - `lombok` con `optional=true` [verificado en Git].
- Dependencias de prueba declaradas:
  - `spring-boot-starter-data-jpa-test` [verificado en Git].
  - `spring-boot-starter-flyway-test` [verificado en Git].
  - `spring-boot-starter-security-test` [verificado en Git].
  - `spring-boot-starter-validation-test` [verificado en Git].
  - `spring-boot-starter-webmvc-test` [verificado en Git].

### Frontend

- No hay stack frontend confirmado. La carpeta `frontend/` existe pero no contiene archivos rastreados [verificado en Git].

### Base de datos y persistencia

- Persistencia prevista por dependencias: Spring Data JPA, Flyway y driver PostgreSQL estan declarados [verificado en Git, inferido].
- No hay datasource configurado, entidades JPA, repositorios, migraciones, seeds, constraints ni indices confirmados [verificado en Git].
- Base de datos efectiva: pendiente de confirmar. PostgreSQL esta respaldado por dependencia runtime, pero no hay configuracion de conexion documentada o ejecutable.

## Arquitectura actual

### Backend

- Estado confirmado: aplicacion Spring Boot minima con clase de arranque `BackendApplication` [verificado en Git].
- No hay controllers, handlers, services, casos de uso, repositories, entidades, DTOs, mappers, enums, validaciones personalizadas, manejo de errores propio ni configuracion de seguridad propia [verificado en Git].
- Seguridad: Spring Security esta declarada como dependencia, pero no existe configuracion local de autenticacion, autorizacion, roles, permisos ni filtros personalizados [verificado en Git].
- Contratos API: no hay endpoints, OpenAPI, controladores ni formatos de error implementados [verificado en Git].

### Frontend

- No hay rutas, paginas, componentes, UI components, hooks, stores, servicios API, validaciones, estilos, tokens ni tests frontend confirmados [verificado en Git].

## Infraestructura y CI/CD

- No hay workflows `.github/`, Docker, Compose, scripts de despliegue, configuracion de observabilidad ni servicios externos configurados en archivos rastreados [verificado en Git].
- Despliegue: pendiente de confirmar.
- Observabilidad: pendiente de confirmar.

## Configuracion local

- Configuracion disponible: `backend/src/main/resources/application.properties` con `spring.application.name=backend` [verificado en Git].
- Variables de entorno documentadas: ninguna confirmada [verificado en Git].
- Puerto local: pendiente de confirmar; no hay `server.port` configurado [verificado en Git].

## Comandos confirmados

Los comandos siguientes estan respaldados por `backend/mvnw`, `backend/pom.xml` y plugins/lifecycle Maven. No se ejecutaron builds, tests ni migraciones durante esta auditoria.

- Instalacion de dependencias: no hay comando de instalacion documentado o script especifico confirmado. Maven resolvera dependencias al ejecutar goals respaldados por el wrapper [verificado en Git, inferido].
- Desarrollo backend: `cd backend && ./mvnw spring-boot:run` [verificado en Git por `spring-boot-maven-plugin`].
- Build backend: `cd backend && ./mvnw package` [verificado en Git por proyecto Maven].
- Lint: no hay comando de lint confirmado [verificado en Git].
- Typecheck: no hay comando independiente de typecheck confirmado; la compilacion Java queda cubierta por goals Maven como `compile` o `package` [verificado en Git].
- Pruebas backend: `cd backend && ./mvnw test` [verificado en Git por proyecto Maven y test JUnit/Spring].
- Migraciones: no hay comando especifico de migraciones confirmado ni archivos de migracion existentes [verificado en Git].
- Frontend: no hay comandos confirmados [verificado en Git].

## Fuentes canonicas inspeccionadas

- `git remote -v`, `git remote get-url origin`, `git branch --show-current`, `git status --short`, `git ls-files`, `git log --oneline --decorate --all --max-count=10` [verificado en Git].
- `backend/pom.xml` [verificado en Git].
- `backend/.mvn/wrapper/maven-wrapper.properties` [verificado en Git].
- `backend/HELP.md` [verificado en Git].
- `backend/src/main/java/com/forze/backend/BackendApplication.java` [verificado en Git].
- `backend/src/main/resources/application.properties` [verificado en Git].
- `backend/src/test/java/com/forze/backend/BackendApplicationTests.java` [verificado en Git].
- Busquedas locales de frontend, docs, workflows, infraestructura, controllers, entidades, migraciones y configuracion [verificado en Git].

## Reglas criticas para futuros Agentes

- Trabajar sobre la rama activa indicada por la tarea; para este contexto canonico la rama base es `dev`.
- No inventar tecnologias, endpoints, tablas, permisos, comandos, rutas, variables ni modulos.
- No agregar dependencias sin autorizacion explicita.
- No modificar codigo funcional cuando la tarea sea de contexto o auditoria.
- Mantener `docs/ai/` como contexto tecnico verificable, no como changelog.
- Registrar como `pendiente de confirmar` cualquier afirmacion sin evidencia directa.

## Pendientes de confirmar

- Dominio funcional de FORZE.
- Arquitectura objetivo del producto.
- Stack frontend, si se implementara.
- Configuracion real de base de datos y variables de entorno.
- Estrategia de autenticacion, autorizacion, roles y permisos.
- Contratos API y formatos de error.
- Migraciones, esquema, seeds e historial de datos.
- Infraestructura, despliegue, CI/CD y observabilidad.
- Convenciones de naming de dominio.
