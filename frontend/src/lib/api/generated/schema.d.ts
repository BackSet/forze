/**
 * This file is generated from the FORZE OpenAPI contract.
 * Do not make direct changes when a backend /v3/api-docs endpoint is available.
 */

export interface paths {
  '/api/auth/login': {
    post: operations['login']
  }
  '/api/auth/refresh': {
    post: operations['refresh']
  }
  '/api/auth/logout': {
    post: operations['logout']
  }
  '/api/auth/me': {
    get: operations['me']
  }
  '/actuator/health': {
    get: operations['health']
  }
}

export type webhooks = Record<string, never>

export interface components {
  schemas: {
    LoginRequest: {
      username: string
      password: string
    }
    AuthTokenResponse: {
      accessToken: string
      tokenType: string
    }
    MeResponse: {
      id: string
      username: string
      email?: string | null
    }
    ProblemDetail: {
      type?: string
      title?: string
      status?: number
      detail?: string
      instance?: string
      [key: string]: unknown
    }
  }
  responses: never
  parameters: never
  requestBodies: never
  headers: never
  pathItems: never
}

export interface operations {
  login: {
    requestBody: {
      content: {
        'application/json': components['schemas']['LoginRequest']
      }
    }
    responses: {
      200: {
        content: {
          'application/json': components['schemas']['AuthTokenResponse']
        }
      }
      400: { content: { 'application/problem+json': components['schemas']['ProblemDetail'] } }
      401: { content: { 'application/problem+json': components['schemas']['ProblemDetail'] } }
    }
  }
  refresh: {
    responses: {
      200: {
        content: {
          'application/json': components['schemas']['AuthTokenResponse']
        }
      }
      401: { content: { 'application/problem+json': components['schemas']['ProblemDetail'] } }
    }
  }
  logout: {
    responses: {
      204: never
    }
  }
  me: {
    responses: {
      200: {
        content: {
          'application/json': components['schemas']['MeResponse']
        }
      }
      401: { content: { 'application/problem+json': components['schemas']['ProblemDetail'] } }
    }
  }
  health: {
    responses: {
      200: {
        content: {
          'application/json': {
            status: string
          }
        }
      }
    }
  }
}

export type $defs = Record<string, never>
