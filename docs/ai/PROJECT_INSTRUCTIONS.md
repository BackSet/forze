# PROJECT_INSTRUCTIONS

## Identidad fija

- Proyecto: FORZE.
- Repositorio: BackSet/forze.
- Rama base: `dev`.
- Entorno: local.
- Nivel: detallado.
- Ruta de contexto: `docs/ai`.
- Termino para implementacion: Agente.

## Rol del analista tecnico

- Inspeccionar el estado real de la rama activa antes de afirmar arquitectura, tecnologia, comandos, rutas, modulos o convenciones.
- Priorizar evidencia verificable del repositorio por encima de supuestos o descripciones auxiliares.
- Etiquetar informacion como `verificado en Git`, `verificado en documentacion`, `inferido` o `pendiente de confirmar` cuando sea relevante.
- Registrar contradicciones entre codigo, tests y documentacion sin resolverlas mediante cambios funcionales.

## Rol del Agente

- Implementar solo el alcance solicitado por la tarea vigente.
- Respetar la arquitectura, idioma, nombres y patrones existentes.
- Trabajar sobre la rama activa indicada por la tarea; para FORZE, la rama base canonica es `dev`.
- Mantener cambios aislados y revisables.
- No convertir los archivos de `docs/ai/` en changelog.

## Fuentes de verdad y prioridad

1. Codigo actual de la rama `dev`.
2. Configuracion ejecutable, migraciones y contratos.
3. Tests automatizados.
4. Documentacion canonica del repositorio.
5. Archivos de contexto IA existentes en `docs/ai/`.
6. Descripciones auxiliares.
7. Supuestos explicitos marcados como inferencias.

## Reglas de aislamiento

- No sobrescribir, revertir ni mezclar cambios locales ajenos.
- No modificar archivos fuera del alcance solicitado.
- En tareas de contexto IA, limitar cambios a `docs/ai/` salvo autorizacion explicita.
- No modificar codigo funcional durante auditorias de documentacion.
- No alterar manifiestos, workflows, configuracion ejecutable, migraciones o scripts si la tarea no lo pide expresamente.
- No hacer commit ni push salvo instruccion explicita.

## Restricciones permanentes

- No agregar dependencias sin autorizacion explicita.
- Respetar la arquitectura existente.
- No inventar archivos, rutas, endpoints, tablas, permisos, comandos, tecnologias, dependencias, modulos, roles, enums, estados ni convenciones.
- No implementar fuera del alcance.
- No realizar refactors no solicitados.
- No corregir bugs encontrados durante tareas de auditoria, salvo que la tarea lo autorice.
- No modificar migraciones historicas ya aplicadas.
- No generar migraciones nuevas sin una tarea funcional especifica.
- No duplicar documentacion extensa si ya existe una fuente canonica; enlazarla o resumirla.
- Respetar idioma, casing y nomenclatura existentes.

## Mantenimiento de contexto IA

### `PROJECT_CONTEXT.md`

Actualizar solo cuando cambien o se descubran stack, estructura, arquitectura, comandos, infraestructura, seguridad global, estrategia de datos o estrategia de pruebas.

### `MODULE_MAP.md`

Actualizar cuando cambien o se descubran modulos, rutas, APIs, entidades, tablas, permisos, relaciones, flujos o tests relacionados.

### `NAMING.md`

Actualizar cuando cambien o se descubran terminos de dominio, copy canonico, nombres tecnicos, permisos, roles, enums, estados o nombres historicos.

### `PROJECT_INSTRUCTIONS.md`

Actualizar cuando cambien identidad fija, repositorio, rama base, entorno, nivel, ruta de contexto, reglas operativas o restricciones permanentes.

## Revision obligatoria al finalizar implementaciones

Al finalizar cada implementacion, revisar:

- `docs/ai/PROJECT_CONTEXT.md`.
- `docs/ai/MODULE_MAP.md`.
- `docs/ai/NAMING.md`.
- `docs/ai/PROJECT_INSTRUCTIONS.md`.

Si existe impacto canonico real, actualizar solo los archivos afectados. Si se revisan y no requieren cambios, justificarlo expresamente en el reporte final.

## Formato obligatorio del reporte final

El reporte final de auditorias o implementaciones debe incluir:

1. Rama y repositorio verificados.
2. Estado inicial de Git.
3. Archivos creados.
4. Archivos actualizados.
5. Archivos revisados sin cambios.
6. Fuentes del repositorio inspeccionadas.
7. Stack, arquitectura y modulos confirmados.
8. Decisiones de documentacion importantes.
9. Contradicciones detectadas.
10. Informacion inferida.
11. Datos pendientes de confirmar.
12. Comandos ejecutados y resultados.
13. Pruebas o validaciones no ejecutadas y motivo.
14. Riesgos o limitaciones de la auditoria.
15. Estado final de Git.
16. Confirmacion explicita de que no se agregaron dependencias, se respeto la arquitectura existente, no se modifico codigo funcional, no se modificaron migraciones y no se hizo commit ni push.
