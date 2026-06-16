# NAMING

## Alcance

- Fecha de auditoria: 2026-06-16.
- Proyecto: FORZE.
- Repositorio: BackSet/forze [verificado en Git].
- Rama analizada: `dev` [verificado en Git].
- Estado: no existen terminos de dominio funcional confirmados en codigo o documentacion actual [verificado en Git].

## Terminos canonicos confirmados

| Concepto | Nombre canonico | Contexto | Evidencia |
| --- | --- | --- | --- |
| Proyecto | FORZE | Documentacion de contexto IA | Solicitud de auditoria y nombre de proyecto indicado por el usuario [verificado en documentacion de tarea] |
| Repositorio | BackSet/forze | Git/GitHub | `origin` apunta a `https://github.com/BackSet/forze.git` [verificado en Git] |
| Backend | `backend` | Aplicacion Spring, carpeta raiz, artifactId | `backend/`, `spring.application.name=backend`, `<artifactId>backend</artifactId>` [verificado en Git] |
| Paquete base backend | `com.forze.backend` | Java | Declaracion `package com.forze.backend` [verificado en Git] |
| Clase de arranque | `BackendApplication` | Java/Spring Boot | `backend/src/main/java/com/forze/backend/BackendApplication.java` [verificado en Git] |
| Test de arranque | `BackendApplicationTests` | Java/Spring Boot Test | `backend/src/test/java/com/forze/backend/BackendApplicationTests.java` [verificado en Git] |

## Variantes prohibidas o desaconsejadas

- No hay variantes historicas confirmadas por el repositorio [verificado en Git].
- No introducir traducciones o variantes no existentes para nombres tecnicos confirmados. Ejemplos: mantener `backend`, `BackendApplication`, `com.forze.backend` y `FORZE` cuando se refieran a esos elementos especificos [inferido desde evidencia].
- No crear nombres de dominio, permisos, roles, estados, endpoints o tablas hasta que existan en codigo, contratos o documentacion canonica [regla de contexto].

## Diferencias por contexto

### UI

- No hay copy, rutas, componentes ni terminos UI confirmados [verificado en Git].

### API/backend

- Paquete Java confirmado: `com.forze.backend` [verificado en Git].
- Nombre de aplicacion Spring confirmado: `backend` [verificado en Git].
- No hay endpoints, DTOs, nombres de errores, controllers, services, repositories ni modelos confirmados [verificado en Git].

### Base de datos

- No hay tablas, columnas, enums, constraints, indices ni nombres de migraciones confirmados [verificado en Git].
- PostgreSQL aparece como dependencia runtime, pero no define por si mismo nomenclatura de dominio [verificado en Git].

### Documentacion

- La documentacion canonica de contexto tecnico vive en `docs/ai/` [verificado en Git].
- `backend/HELP.md` mantiene nombres propios de referencias Spring/Maven generadas [verificado en Git].

## Mapeo tecnico confirmado

| Concepto | Entidad o tipo | Tabla | Endpoint | Ruta frontend | Evidencia |
| --- | --- | --- | --- | --- | --- |
| Aplicacion backend | `BackendApplication` | No aplica | No confirmado | No aplica | Clase Java de arranque [verificado en Git] |
| Test de contexto backend | `BackendApplicationTests` | No aplica | No aplica | No aplica | Test `contextLoads` [verificado en Git] |

## Permisos, roles, enums, estados y constantes

- Permisos: ninguno confirmado [verificado en Git].
- Roles: ninguno confirmado [verificado en Git].
- Enums: ninguno confirmado [verificado en Git].
- Estados: ninguno confirmado [verificado en Git].
- Constantes de dominio: ninguna confirmada [verificado en Git].

## Casing y convenciones existentes

- Java packages: minusculas con puntos, ejemplo `com.forze.backend` [verificado en Git].
- Clases Java: PascalCase, ejemplo `BackendApplication` y `BackendApplicationTests` [verificado en Git].
- Metodo de test confirmado: camelCase, ejemplo `contextLoads` [verificado en Git].
- Propiedades Spring: kebab/dot notation, ejemplo `spring.application.name` [verificado en Git].
- Artifact Maven: minusculas, ejemplo `backend` [verificado en Git].
- Carpeta raiz de aplicacion: minusculas, ejemplo `backend/` [verificado en Git].

## Nombres historicos o deprecados

- No hay nombres historicos o deprecados confirmados [verificado en Git].

## Terminos pendientes de confirmar

- Terminos de dominio funcional de FORZE.
- Nombre canonico de usuarios, cuentas, sesiones, clientes, recursos o cualquier entidad de negocio.
- Convenciones de rutas API.
- Convenciones de rutas frontend.
- Nombres de tablas, columnas y migraciones.
- Roles, permisos, estados, enums y constantes.
- Formatos canonicos de errores.
