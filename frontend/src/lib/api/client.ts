import createFetchClient from 'openapi-fetch'
import createQueryClient from 'openapi-react-query'

import type { paths } from '@/lib/api/generated/schema'

const baseUrl = import.meta.env.VITE_FORZE_API_URL ?? 'http://localhost:8080'

export const fetchClient = createFetchClient<paths>({
  baseUrl,
})

export const api = createQueryClient(fetchClient)
