import axios from 'axios';

interface TokenPair {
  accessToken: string;
  refreshToken: string;
  expiresAt: number; // ms timestamp
}

// ── Per-user token cache (key: "username@baseUrl") ────────────────────────────
const tokenCache = new Map<string, TokenPair>();

// ── Single-account state (for admin/portal using env vars) ────────────────────
let singleBaseUrl: string = '';

export function configureAuth(url: string) {
  singleBaseUrl = url;
}

/** For single-account servers (admin): reads OSRM_USERNAME / OSRM_PASSWORD from env */
export async function getAccessToken(): Promise<string> {
  const username = process.env.OSRM_USERNAME;
  const password = process.env.OSRM_PASSWORD;
  if (!username || !password) {
    throw new Error('OSRM_USERNAME and OSRM_PASSWORD environment variables are required');
  }
  return getTokenForUser(singleBaseUrl, username, password);
}

/** For multi-user servers (workflow): per-user token cache with auto-refresh */
export async function getTokenForUser(
  baseUrl: string,
  username: string,
  password: string
): Promise<string> {
  const key = `${username}@${baseUrl}`;
  const cached = tokenCache.get(key);
  if (cached && Date.now() < cached.expiresAt - 60_000) {
    return cached.accessToken;
  }
  const pair = await loginOrRefresh(baseUrl, username, password, cached?.refreshToken);
  tokenCache.set(key, pair);
  return pair.accessToken;
}

async function loginOrRefresh(
  baseUrl: string,
  username: string,
  password: string,
  refreshToken?: string
): Promise<TokenPair> {
  if (refreshToken) {
    try {
      const res = await axios.post(`${baseUrl}/auth/refresh`, { refreshToken });
      return buildPair(res.data.data);
    } catch {
      // refresh failed, fall through to login
    }
  }
  const res = await axios.post(`${baseUrl}/auth/login`, { username, password });
  return buildPair(res.data.data);
}

function buildPair(data: {
  accessToken: string;
  refreshToken: string;
  expiresIn?: number;
}): TokenPair {
  return {
    accessToken: data.accessToken,
    refreshToken: data.refreshToken,
    expiresAt: Date.now() + (data.expiresIn ?? 7200) * 1000,
  };
}
