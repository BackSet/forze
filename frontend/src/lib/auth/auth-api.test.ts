import { beforeEach, describe, expect, it, vi } from 'vitest'

import { refreshAccessToken } from '@/lib/auth/auth-api'
import { useSessionStore } from '@/lib/auth/session-store'

const { post } = vi.hoisted(() => ({
  post: vi.fn(),
}))

vi.mock('@/lib/api/client', () => ({
  fetchClient: {
    POST: post,
    GET: vi.fn(),
  },
}))

describe('auth api', () => {
  beforeEach(() => {
    post.mockReset()
    useSessionStore.getState().clearSession()
  })

  it('uses single-flight refresh', async () => {
    post.mockResolvedValue({
      data: { accessToken: 'next-access', tokenType: 'Bearer' },
      error: undefined,
    })

    const [first, second] = await Promise.all([refreshAccessToken(), refreshAccessToken()])

    expect(first).toBe('next-access')
    expect(second).toBe('next-access')
    expect(post).toHaveBeenCalledTimes(1)
    expect(useSessionStore.getState().accessToken).toBe('next-access')
  })
})
