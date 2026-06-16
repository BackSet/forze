import { describe, expect, it } from 'vitest'

import { router } from '@/router'

describe('router', () => {
  it('registers the initial route', () => {
    expect(router.routesByPath['/']).toBeDefined()
  })
})
