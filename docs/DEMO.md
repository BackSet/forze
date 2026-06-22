# Datos demo (local / dev)

FORZE incluye un seeder de **datos demo** para explorar el sistema completo en
local sin cargar información real. Es **solo para entorno `dev`** y nunca se
ejecuta en producción.

## 1. Cargar los datos

El seeder (`DemoDataSeeder`) corre automáticamente al arrancar el backend en
perfil `dev`:

```bash
cd backend && ./run.sh        # o: mvn spring-boot:run  (perfil dev)
```

- Es **idempotente**: un segundo arranque no duplica datos (centinela:
  la cuenta `admin.demo@forze.local`).
- Está **bloqueado** si el perfil activo es `prod`.
- Toggle: `forze.demo.enabled` (encendido por defecto en dev).

Levanta el frontend en paralelo:

```bash
cd frontend && npm run dev
```

## 2. Iniciar sesión

Credenciales **locales ficticias** (contraseña común `Demo1234!`):

| Rol           | Usuario                              |
|---------------|--------------------------------------|
| Administrador | `admin.demo@forze.local`             |
| Presupuestista| `presupuestista.demo@forze.local`    |
| Aprobador     | `aprobador.demo@forze.local`         |
| Compras       | `compras.demo@forze.local`           |

En la pantalla de login (solo en dev) aparece un panel con estas cuentas;
al hacer clic se autocompleta el formulario. Tras entrar, selecciona la
organización **DEMO - Constructora Andina**.

## 3. Recorrido sugerido

La pantalla **Inicio** muestra una tarjeta *Modo demo* (solo dev) con accesos
directos. Qué revisar:

- **Proyectos** → "Edificio Residencial Demo" y "Bodega Industrial Demo".
- **Presupuestos** → `DEMO-PPTO-001` (**viable**, aprobado e inmutable) y
  `DEMO-PPTO-002` (**con alertas**: rubro en cantidad cero y rubro sin APU).
- **Editor** → APU por secciones, componentes y mediciones (cómputo métrico).
- **Proveedores** → cotización demo e **historial de precios** (comparador).
- **Escenarios** → escenario económico vs base.
- **Aprobaciones** → flujo enviado/aprobado de `DEMO-PPTO-001`.
- **Documentos** → cotización PDF generada.
- **Auditoría** → eventos de aprobación y administración.

## 4. Notas

- Todos los registros usan prefijo `DEMO-`; no chocan con datos reales.
- Las contraseñas nunca se registran en logs.
- Las ayudas demo del frontend (credenciales, tarjeta guía) se compilan solo
  bajo el dev server de Vite (`import.meta.env.DEV`) y **no** se incluyen en el
  build de producción.
