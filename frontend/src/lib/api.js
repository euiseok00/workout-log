const AUTH_KEY = 'workout_auth'

export function readAuth() {
  try {
    return JSON.parse(localStorage.getItem(AUTH_KEY))
  } catch {
    return null
  }
}

export function saveAuth(auth) {
  localStorage.setItem(AUTH_KEY, JSON.stringify(auth))
}

export function clearAuth() {
  localStorage.removeItem(AUTH_KEY)
}

export class ApiError extends Error {
  constructor(status, problem) {
    super(problem?.detail || '요청을 처리하지 못했습니다.')
    this.status = status
    this.code = problem?.code
    this.errors = problem?.errors || []
  }
}

export async function api(path, options = {}) {
  const auth = readAuth()
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || ''}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(auth?.accessToken
        ? { Authorization: `${auth.tokenType || 'Bearer'} ${auth.accessToken}` }
        : {}),
      ...options.headers,
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
  })

  if (!response.ok) {
    let problem
    try {
      problem = await response.json()
    } catch {
      problem = { detail: '서버와 통신하지 못했습니다.' }
    }
    if (response.status === 401 && auth) {
      clearAuth()
      window.dispatchEvent(new Event('auth-expired'))
    }
    throw new ApiError(response.status, problem)
  }

  if (response.status === 204) return null
  return response.json()
}

export function query(params) {
  const values = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') values.set(key, value)
  })
  const result = values.toString()
  return result ? `?${result}` : ''
}
