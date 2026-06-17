import createFetchClient from 'openapi-fetch'
import createQueryClient from 'openapi-react-query'

import type { paths } from '@/lib/api/generated/schema'
import { env } from '@/lib/env'
import { useSessionStore } from '@/lib/auth/session-store'

export const fetchClient = createFetchClient<paths>({
  baseUrl: env.apiBaseUrl,
  credentials: 'include',
})

fetchClient.use({
  onRequest({ request }) {
    const token = useSessionStore.getState().accessToken
    if (token) {
      request.headers.set('Authorization', `Bearer ${token}`)
    }
    const orgId = useSessionStore.getState().activeOrganizationId
    if (orgId) {
      request.headers.set('X-Organization-Id', orgId)
    }
    return request
  },
  // Guarantee that any non-2xx response carries a body so openapi-fetch
  // populates `error` (and openapi-react-query rejects). Without this, a
  // backend error with an empty body (e.g. a Spring 401/403 with no payload)
  // would surface as `{ data: undefined, error: undefined }`, which the
  // mutation would treat as success and run `onSuccess(undefined)`.
  async onResponse({ response }) {
    if (response.ok) {
      return undefined
    }
    const body = await response.clone().text()
    if (body.trim().length > 0) {
      // Real payload present (e.g. our RFC 7807 `{ detail }`): leave untouched.
      return undefined
    }
    const detail =
      response.status === 401
        ? 'Sesión no válida o expirada.'
        : response.status === 403
          ? 'No tiene permisos para realizar esta acción.'
          : 'No se pudo completar la solicitud.'
    return new Response(JSON.stringify({ status: response.status, detail }), {
      status: response.status,
      statusText: response.statusText,
      headers: { 'Content-Type': 'application/json' },
    })
  },
})

export const api = createQueryClient(fetchClient)
