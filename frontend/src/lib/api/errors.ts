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
