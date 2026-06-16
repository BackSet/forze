# NAMING

## Canonico

| Concepto | Nombre |
| --- | --- |
| Producto | FORZE |
| Repositorio | BackSet/forze |
| Rama base | `dev` |
| Backend Maven group | `com.backset.forze` |
| Backend artifact | `backend` |
| Paquete base backend | `com.backset.forze` |
| Clase de arranque | `ForzeApplication` |
| Aplicacion Spring | `forze-backend` |
| Frontend package | `frontend` |
| Base de datos local | `forze` |

## Convenciones Tecnicas

- Java packages: lowercase con puntos, bajo `com.backset.forze`.
- Clases Java: PascalCase.
- Tests Java: sufijo `Tests`.
- Propiedades Spring: kebab/dot notation.
- Rutas frontend: TanStack Router file-based routes bajo `frontend/src/routes`.
- Componentes React: PascalCase.
- Helpers TypeScript: camelCase.
- Variables de entorno FORZE: prefijo `FORZE_`.

## Terminos De Producto Confirmados

- Presupuesto de obra.
- Proyecto.
- Cliente.
- Ubicacion.
- Capitulo.
- Rubro.
- APU.
- Cantidad.
- Rendimiento.
- Materiales.
- Mano de obra.
- Equipos.
- Costos directos.
- Costos indirectos.
- Proveedor.
- Version aprobada.
- Cotizacion final.
- Trazabilidad.

Estos terminos estan confirmados como direccion de producto en `PRODUCT.md`, pero aun no deben convertirse en tablas, endpoints, entidades o rutas hasta que exista una tarea funcional especifica.

## Evitar

- No usar el paquete historico `com.forze.backend`; fue reemplazado por `com.backset.forze`.
- No introducir usuarios, roles, permisos, estados, codigos de error o entidades sin respaldo en una tarea funcional.
- No editar manualmente archivos generados como `frontend/src/routeTree.gen.ts` o `frontend/src/lib/api/generated/schema.d.ts`.
