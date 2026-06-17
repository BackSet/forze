/**
 * Decides how the app shell should render the active surface based on the state
 * of the effective-access query. A pending or failed access query must never be
 * shown as a permission denial; the backend remains the authority.
 */
export type AccessView =
  | 'loading'
  | 'stale-organization' // real HTTP 403: not a member of the active organization
  | 'network-error' // network/CORS/API failure: recoverable, offer retry
  | 'forbidden' // access loaded, but no permission for the requested surface
  | 'allowed'

export interface AccessInput {
  isPending: boolean
  isFetching: boolean
  isError: boolean
  errorStatus?: number | undefined
  permissionsCount: number
  hasRequiredPermission: boolean
}

export function resolveAccessView(input: AccessInput): AccessView {
  const { isPending, isFetching, isError, errorStatus, permissionsCount, hasRequiredPermission } = input

  // Loading: first fetch, or a refetch (e.g. after switching organization clears
  // the previously resolved permissions) that has not yet failed.
  if (isPending || (isFetching && permissionsCount === 0 && !isError)) {
    return 'loading'
  }
  if (isError) {
    return errorStatus === 403 ? 'stale-organization' : 'network-error'
  }
  if (!hasRequiredPermission) {
    return 'forbidden'
  }
  return 'allowed'
}

/** Pulls an HTTP status out of the normalized API error ({ status, detail }). */
export function errorStatus(error: unknown): number | undefined {
  if (error && typeof error === 'object' && 'status' in error) {
    const status = (error as { status?: unknown }).status
    if (typeof status === 'number') return status
  }
  return undefined
}
