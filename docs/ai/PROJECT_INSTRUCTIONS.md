# PROJECT_INSTRUCTIONS

## Identidad Fija

- Proyecto: FORZE.
- Repositorio: BackSet/forze.
- Rama base: `dev`.
- Entorno principal: local.
- Ruta de contexto: `docs/ai`.

## Rol Del Agente

- Implementar solo el alcance solicitado.
- Verificar el estado real de la rama antes de afirmar arquitectura, comandos o convenciones.
- Mantener cambios pequenos, revisables y alineados con el stack existente.
- Actualizar `docs/ai/` cuando cambien estructura, stack, comandos, modulos, seguridad, datos o convenciones.
- No hacer commit ni push salvo instruccion explicita del usuario.

## Fuentes De Verdad

1. Codigo y configuracion de la rama activa.
2. Tests automatizados y contratos generados.
3. `PRODUCT.md` para direccion de producto.
4. `README.md` para uso local.
5. `docs/ai/` para contexto tecnico sintetico.

## Reglas Permanentes

- No inventar dominio: sin endpoints, tablas, datos seed, roles, estados o pantallas funcionales sin tarea concreta.
- Mantener el backend como fuente de verdad de OpenAPI.
- Regenerar tipos frontend con `npm run openapi:generate`; no editar `schema.d.ts` manualmente.
- Regenerar rutas con `npm run routes:generate`; no editar `routeTree.gen.ts` manualmente.
- Mantener `hibernate.ddl-auto=validate`; no usar schema generation como sustituto de migraciones.
- No crear migraciones sin historia funcional clara.
- Mantener seguridad deny-by-default hasta que exista autenticacion y autorizacion reales.
- Si Docker no esta disponible, documentar que los tests Testcontainers se omiten por entorno.

## Validacion Esperada

- Backend: `cd backend && ./mvnw verify`.
- Frontend: `cd frontend && npm run typecheck && npm run test && npm run lint && npm run build`.
- E2E: `cd frontend && npm run e2e`.
- OpenAPI: levantar backend y ejecutar `cd frontend && npm run openapi:generate`.

## Reporte Final

Todo reporte de implementacion debe incluir:

- Rama y repositorio verificados.
- Cambios principales.
- Comandos ejecutados y resultado.
- Validaciones no ejecutadas o limitadas por entorno.
- Estado final de Git.
