import { ServiceError } from './emergencyService.ts'

export interface BackendClientOptions {
  fetchImpl?: typeof fetch
  timeoutMs?: number
}

export interface BackendClient {
  get<T>(path: string, params?: Record<string, string | number>): Promise<T>
  post<T>(path: string, params?: Record<string, string | number>): Promise<T>
}

/** Cliente HTTP mínimo hacia la API Java. No lleva credenciales ni secretos. */
export function createBackendClient(baseUrl: string, options: BackendClientOptions = {}): BackendClient {
  const base = baseUrl.trim().replace(/\/+$/, '')
  if (!base) {
    throw new ServiceError('configuration', 'Falta la URL del backend (VITE_SALIDASEGURA_API_URL).')
  }
  const doFetch = options.fetchImpl ?? ((input: RequestInfo | URL, init?: RequestInit) => fetch(input, init))
  const timeoutMs = options.timeoutMs ?? 8000

  async function request<T>(method: 'GET' | 'POST', path: string, params?: Record<string, string | number>): Promise<T> {
    const query = params
      ? '?' + Object.entries(params).map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`).join('&')
      : ''
    const controller = new AbortController()
    let timedOut = false
    const timer = setTimeout(() => { timedOut = true; controller.abort() }, timeoutMs)
    let response: Response
    try {
      response = await doFetch(base + path + query, { method, signal: controller.signal })
    } catch {
      throw new ServiceError('connection', timedOut
        ? 'El backend no respondió a tiempo.'
        : 'No se pudo conectar con el backend.')
    } finally {
      clearTimeout(timer)
    }
    let body: unknown
    try {
      body = await response.json()
    } catch {
      if (!response.ok) throw new ServiceError('api', `El backend respondió con error ${response.status}.`, response.status)
      throw new ServiceError('response', 'El backend devolvió una respuesta que no es JSON válido.')
    }
    if (!response.ok) {
      const message = (body as { error?: unknown } | null)?.error
      throw new ServiceError('api', typeof message === 'string' ? message : `El backend respondió con error ${response.status}.`, response.status)
    }
    return body as T
  }

  return {
    get: (path, params) => request('GET', path, params),
    post: (path, params) => request('POST', path, params),
  }
}
