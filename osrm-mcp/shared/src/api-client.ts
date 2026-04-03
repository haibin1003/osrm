import axios, { AxiosInstance, AxiosError } from 'axios';
import { getAccessToken, getTokenForUser } from './auth.js';

export class OsrmApiError extends Error {
  constructor(
    message: string,
    public readonly code: number
  ) {
    super(message);
    this.name = 'OsrmApiError';
  }
}

/** Attach response interceptor: unwrap ApiResponse<T>, throw OsrmApiError on errors */
export function attachResponseInterceptor(instance: AxiosInstance): void {
  instance.interceptors.response.use(
    (res) => {
      const body = res.data;
      if (body && typeof body === 'object' && 'code' in body) {
        if (body.code !== 200 && body.code !== 0) {
          throw new OsrmApiError(body.message ?? 'API error', body.code);
        }
        return body.data ?? body;
      }
      return res.data;
    },
    (err: AxiosError) => {
      const msg =
        (err.response?.data as any)?.message ??
        err.message ??
        'Network error';
      throw new OsrmApiError(msg, err.response?.status ?? 0);
    }
  );
}

/**
 * Create an API client using env-configured single account (portal / admin).
 * requireAuth=false → no Authorization header (public portal APIs)
 * requireAuth=true  → reads OSRM_USERNAME/OSRM_PASSWORD from env
 */
export function createClient(baseUrl: string, requireAuth: boolean): AxiosInstance {
  const instance = axios.create({
    baseURL: baseUrl,
    timeout: 15_000,
    headers: { 'Content-Type': 'application/json' },
  });

  if (requireAuth) {
    instance.interceptors.request.use(async (config) => {
      const token = await getAccessToken();
      config.headers.Authorization = `Bearer ${token}`;
      return config;
    });
  }

  attachResponseInterceptor(instance);
  return instance;
}

/**
 * Create an API client for a specific user (workflow multi-user scenario).
 * Credentials are passed per-call; tokens are cached and auto-refreshed per user.
 */
export function createClientForUser(
  baseUrl: string,
  username: string,
  password: string
): AxiosInstance {
  const instance = axios.create({
    baseURL: baseUrl,
    timeout: 15_000,
    headers: { 'Content-Type': 'application/json' },
  });

  instance.interceptors.request.use(async (config) => {
    const token = await getTokenForUser(baseUrl, username, password);
    config.headers.Authorization = `Bearer ${token}`;
    return config;
  });

  attachResponseInterceptor(instance);
  return instance;
}

/** @deprecated use createClient() or createClientForUser() */
export function getClient(): AxiosInstance {
  throw new Error('getClient() is deprecated. Use createClient() or createClientForUser().');
}
