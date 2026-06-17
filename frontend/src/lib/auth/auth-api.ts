import { fetchClient } from '@/lib/api/client'
import { ApiError, normalizeApiError } from '@/lib/api/errors'
import { env } from '@/lib/env'
import { useSessionStore, type SessionUser } from '@/lib/auth/session-store'

type LoginInput = {
  username: string
  password: string
}

let refreshPromise: Promise<string | null> | null = null

function timeoutSignal(signal?: AbortSignal) {
  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), env.requestTimeoutMs)

  if (signal) {
    signal.addEventListener('abort', () => controller.abort(), { once: true })
  }

  return {
    signal: controller.signal,
    clear: () => window.clearTimeout(timeout),
  }
}

async function requestLoginAccessToken(body: LoginInput, signal?: AbortSignal) {
  const timeout = timeoutSignal(signal)
  try {
    const response = await fetchClient.POST('/api/auth/login', { body, signal: timeout.signal })
    const { data, error } = response as { data?: { accessToken?: string }; error?: unknown }

    if (error) {
      throw normalizeApiError(error)
    }

    const accessToken = data?.accessToken
    if (!accessToken) {
      throw new ApiError(500, 'La respuesta de autenticacion no incluyo access token.')
    }

    useSessionStore.getState().setAccessToken(accessToken)
    return accessToken
  }
  finally {
    timeout.clear()
  }
}

async function requestRefreshAccessToken() {
  const timeout = timeoutSignal()
  try {
    const response = await fetchClient.POST('/api/auth/refresh', { signal: timeout.signal })
    const { data, error } = response as { data?: { accessToken?: string }; error?: unknown }

    if (error) {
      throw normalizeApiError(error)
    }

    const accessToken = data?.accessToken
    if (!accessToken) {
      throw new ApiError(500, 'La respuesta de autenticacion no incluyo access token.')
    }

    useSessionStore.getState().setAccessToken(accessToken)
    return accessToken
  }
  finally {
    timeout.clear()
  }
}

export async function login(input: LoginInput, signal?: AbortSignal) {
  await requestLoginAccessToken(input, signal)
  return loadCurrentUser(signal)
}

export async function refreshAccessToken() {
  if (!refreshPromise) {
    useSessionStore.getState().setRefreshing(true)
    refreshPromise = requestRefreshAccessToken()
      .catch((error: unknown) => {
        useSessionStore.getState().clearSession()
        throw normalizeApiError(error)
      })
      .finally(() => {
        refreshPromise = null
        useSessionStore.getState().setRefreshing(false)
      })
  }

  return refreshPromise
}

export async function loadCurrentUser(signal?: AbortSignal): Promise<SessionUser> {
  const timeout = timeoutSignal(signal)
  try {
    let response = await fetchClient.GET('/api/auth/me', { signal: timeout.signal })

    if (response.response.status === 401) {
      const refreshed = await refreshAccessToken()
      if (!refreshed) {
        throw new ApiError(401, 'La sesion expiro.')
      }
      response = await fetchClient.GET('/api/auth/me', { signal: timeout.signal })
    }

    const { data, error } = response as { data?: SessionUser; error?: unknown }

    if (error) {
      throw normalizeApiError(error)
    }

    if (!data) {
      throw new ApiError(500, 'La respuesta de sesion esta vacia.')
    }

    useSessionStore.getState().setUser(data)
    return data
  }
  finally {
    timeout.clear()
  }
}

export async function logout() {
  await fetchClient.POST('/api/auth/logout')
  useSessionStore.getState().clearSession()
}
