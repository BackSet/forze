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
    return request
  },
})

export const api = createQueryClient(fetchClient)
