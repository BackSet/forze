const DEFAULT_TIMEOUT_MS = 15_000

function numberFromEnv(value: string | undefined, fallback: number) {
  if (!value) {
    return fallback
  }

  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback
}

export const env = {
  appName: import.meta.env.VITE_APP_NAME ?? 'FORZE',
  appEnv: import.meta.env.VITE_APP_ENV ?? 'development',
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  requestTimeoutMs: numberFromEnv(import.meta.env.VITE_REQUEST_TIMEOUT_MS, DEFAULT_TIMEOUT_MS),
  // True only under the Vite dev server; false in production builds. Gates the
  // local demo aids (credentials, demo guide) so they never ship to production.
  isDev: import.meta.env.DEV,
}

/** The fictitious, local-only demo accounts created by the backend dev seeder. */
export const DEMO_ACCOUNTS = [
  { role: 'Administrador', username: 'admin.demo@forze.local' },
  { role: 'Presupuestista', username: 'presupuestista.demo@forze.local' },
  { role: 'Aprobador', username: 'aprobador.demo@forze.local' },
  { role: 'Compras', username: 'compras.demo@forze.local' },
] as const

/** Shared local-only demo password (matches the backend seeder default). */
export const DEMO_PASSWORD = 'Demo1234!'
