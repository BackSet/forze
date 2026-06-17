import { describe, expect, it } from 'vitest'

import { errorStatus, resolveAccessView } from '@/app/access-state'

const base = {
  isPending: false,
  isFetching: false,
  isError: false,
  errorStatus: undefined as number | undefined,
  permissionsCount: 0,
  hasRequiredPermission: false,
}

describe('resolveAccessView', () => {
  it('shows loading while the access query is pending (never a 403)', () => {
    expect(resolveAccessView({ ...base, isPending: true })).toBe('loading')
  })

  it('shows loading while refetching after an organization switch cleared permissions', () => {
    expect(resolveAccessView({ ...base, isFetching: true, permissionsCount: 0 })).toBe('loading')
  })

  it('shows a recoverable network error (not a 403) on a network/CORS failure', () => {
    expect(resolveAccessView({ ...base, isError: true, errorStatus: undefined })).toBe('network-error')
    expect(resolveAccessView({ ...base, isError: true, errorStatus: 500 })).toBe('network-error')
  })

  it('treats a real HTTP 403 as a stale organization (back to selector)', () => {
    expect(resolveAccessView({ ...base, isError: true, errorStatus: 403 })).toBe('stale-organization')
  })

  it('shows forbidden when access loaded but the surface permission is missing', () => {
    expect(resolveAccessView({ ...base, permissionsCount: 5, hasRequiredPermission: false })).toBe('forbidden')
  })

  it('allows the surface when the required permission is present', () => {
    expect(resolveAccessView({ ...base, permissionsCount: 5, hasRequiredPermission: true })).toBe('allowed')
  })
})

describe('errorStatus', () => {
  it('extracts the status from a normalized API error', () => {
    expect(errorStatus({ status: 403, detail: 'x' })).toBe(403)
  })

  it('returns undefined for non-status errors', () => {
    expect(errorStatus(new Error('boom'))).toBeUndefined()
    expect(errorStatus(null)).toBeUndefined()
    expect(errorStatus('nope')).toBeUndefined()
  })
})
