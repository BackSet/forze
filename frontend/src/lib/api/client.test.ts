import { beforeEach, describe, expect, it, vi } from 'vitest'

import { fetchClient } from '@/lib/api/client'
import { useSessionStore } from '@/lib/auth/session-store'

// openapi-fetch resolves `globalThis.fetch` at client-creation time, so we
// override the fetch implementation per-request to drive the real client
// (and its registered middleware) deterministically.
function fetchReturning(response: Response) {
  return vi.fn(async () => response)
}

describe('api fetch client error normalization', () => {
  beforeEach(() => {
    useSessionStore.getState().clearSession()
  })

  it('turns an empty (bodyless) error response into a populated error so it never resolves as success', async () => {
    const { data, error } = await fetchClient.POST('/api/organizations', {
      body: { name: 'Acme Corp' },
      fetch: fetchReturning(new Response('', { status: 403, headers: { 'Content-Length': '0' } })),
    })

    expect(data).toBeUndefined()
    expect(error).toBeDefined()
    expect((error as unknown as { detail?: string }).detail).toMatch(/permiso/i)
  })

  it('preserves an existing JSON error body (RFC 7807 detail)', async () => {
    const { error } = await fetchClient.POST('/api/organizations', {
      body: { name: 'Acme Corp' },
      fetch: fetchReturning(
        new Response(JSON.stringify({ detail: 'El encabezado X-Organization-Id es requerido.' }), {
          status: 400,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    })

    expect((error as unknown as { detail?: string }).detail).toBe('El encabezado X-Organization-Id es requerido.')
  })

  it('returns parsed data on a successful response', async () => {
    const { data, error } = await fetchClient.POST('/api/organizations', {
      body: { name: 'Acme Corp' },
      fetch: fetchReturning(
        new Response(JSON.stringify({ id: '11111111-1111-1111-1111-111111111111', name: 'Acme Corp' }), {
          status: 201,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    })

    expect(error).toBeUndefined()
    expect(data).toEqual({ id: '11111111-1111-1111-1111-111111111111', name: 'Acme Corp' })
  })
})
