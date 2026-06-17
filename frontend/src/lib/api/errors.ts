export class ApiError extends Error {
  readonly status: number
  readonly detail: string

  constructor(status: number, detail: string) {
    super(detail)
    this.name = 'ApiError'
    this.status = status
    this.detail = detail
  }
}

type ProblemLike = {
  detail?: unknown
  title?: unknown
  status?: unknown
}

export function normalizeApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error
  }

  if (typeof error === 'object' && error !== null) {
    const problem = error as ProblemLike
    const status = typeof problem.status === 'number' ? problem.status : 500
    const detail = typeof problem.detail === 'string'
      ? problem.detail
      : typeof problem.title === 'string'
        ? problem.title
        : 'No se pudo completar la solicitud.'
    return new ApiError(status, detail)
  }

  return new ApiError(500, 'No se pudo completar la solicitud.')
}

/**
 * Extracts a human-readable message from an unknown error thrown by the
 * OpenAPI client (RFC 7807 problem details, a plain Error, or anything else),
 * falling back to the caller-provided message when none is present.
 */
export function apiErrorMessage(error: unknown, fallback: string): string {
  if (typeof error === 'object' && error !== null) {
    const problem = error as ProblemLike & { message?: unknown }
    if (typeof problem.detail === 'string' && problem.detail.length > 0) {
      return problem.detail
    }
    if (typeof problem.title === 'string' && problem.title.length > 0) {
      return problem.title
    }
    if (typeof problem.message === 'string' && problem.message.length > 0) {
      return problem.message
    }
  }
  return fallback
}
