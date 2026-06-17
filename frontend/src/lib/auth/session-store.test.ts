import { describe, expect, it } from 'vitest'

import { useSessionStore } from '@/lib/auth/session-store'

describe('session store', () => {
  it('keeps session data local and clears it', () => {
    useSessionStore.getState().setAccessToken('access')
    useSessionStore.getState().setUser({ id: '1', username: 'admin' })

    expect(useSessionStore.getState().accessToken).toBe('access')
    expect(useSessionStore.getState().user?.username).toBe('admin')

    useSessionStore.getState().clearSession()

    expect(useSessionStore.getState().accessToken).toBeNull()
    expect(useSessionStore.getState().user).toBeNull()
  })
})
