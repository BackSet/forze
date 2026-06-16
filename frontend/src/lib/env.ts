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
}
