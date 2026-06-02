export const API_BASE_URL =
  (process.env.NEXT_PUBLIC_API_BASE_URL ?? "") +
  (process.env.NEXT_PUBLIC_APP_API_PREFIX ?? "/app-api");
export const API_TENANT_ID = process.env.NEXT_PUBLIC_TENANT_ID ?? "1";
export const API_TERMINAL = process.env.NEXT_PUBLIC_TERMINAL ?? "20";

interface ApiResponse<T = unknown> {
  code: number;
  data: T;
  msg: string;
}

const AUTH_EXPIRED_EVENT = "copse:auth-expired";

let accessToken: string | null = null;
let refreshToken: string | null = null;
let isRefreshing = false;
let refreshQueue: Array<{
  resolve: (token: string) => void;
  reject: (err: unknown) => void;
}> = [];

async function readApiResponse<T>(res: Response): Promise<ApiResponse<T>> {
  const text = await res.text();
  if (!text) {
    return {
      code: res.ok ? 0 : res.status,
      data: undefined as T,
      msg: res.ok ? "" : res.statusText || `HTTP ${res.status}`,
    };
  }
  try {
    return JSON.parse(text) as ApiResponse<T>;
  } catch {
    return {
      code: res.ok ? 500 : res.status,
      data: undefined as T,
      msg: text || res.statusText || `HTTP ${res.status}`,
    };
  }
}

export function setTokens(access: string, refresh: string) {
  accessToken = access;
  refreshToken = refresh;
  if (typeof window !== "undefined") {
    localStorage.setItem("access_token", access);
    localStorage.setItem("refresh_token", refresh);
  }
}

export function getAccessToken(): string | null {
  if (accessToken) return accessToken;
  if (typeof window !== "undefined") {
    accessToken = localStorage.getItem("access_token");
  }
  return accessToken;
}

export function getRefreshToken(): string | null {
  if (refreshToken) return refreshToken;
  if (typeof window !== "undefined") {
    refreshToken = localStorage.getItem("refresh_token");
  }
  return refreshToken;
}

export function clearTokens() {
  accessToken = null;
  refreshToken = null;
  if (typeof window !== "undefined") {
    localStorage.removeItem("access_token");
    localStorage.removeItem("refresh_token");
  }
}

export async function refreshAccessToken(): Promise<string> {
  const rt =
    refreshToken ??
    (typeof window !== "undefined"
      ? localStorage.getItem("refresh_token")
      : null);
  if (!rt) throw new Error("No refresh token");

  const res = await fetch(`${API_BASE_URL}/member/auth/refresh-token?refreshToken=${rt}`, {
    method: "POST",
    headers: {
      "tenant-id": API_TENANT_ID,
      terminal: API_TERMINAL,
    },
  });
  const body = await readApiResponse<{ accessToken: string; refreshToken: string }>(res);
  if (body.code !== 0) throw new Error(body.msg);

  setTokens(body.data.accessToken, body.data.refreshToken);
  return body.data.accessToken;
}

async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getAccessToken();
  const headers: Record<string, string> = {
    Accept: "*/*",
    "Content-Type": "application/json;charset=UTF-8",
    "tenant-id": API_TENANT_ID,
    terminal: API_TERMINAL,
    ...(options.headers as Record<string, string>),
  };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  let res = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
  let body = await readApiResponse<T>(res);

  if (body.code === 401) {
    if (isRefreshing) {
      return new Promise<T>((resolve, reject) => {
        refreshQueue.push({
          resolve: (newToken: string) => {
            headers["Authorization"] = `Bearer ${newToken}`;
            fetch(`${API_BASE_URL}${path}`, { ...options, headers })
              .then((r) => readApiResponse<T>(r))
              .then((b) => {
                if (b.code === 0) resolve(b.data);
                else reject(new Error(b.msg));
              })
              .catch(reject);
          },
          reject,
        });
      });
    }

    isRefreshing = true;
    try {
      const newToken = await refreshAccessToken();
      isRefreshing = false;
      refreshQueue.forEach((q) => q.resolve(newToken));
      refreshQueue = [];

      headers["Authorization"] = `Bearer ${newToken}`;
      res = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
      body = await readApiResponse<T>(res);
    } catch (err) {
      isRefreshing = false;
      refreshQueue.forEach((q) => q.reject(err));
      refreshQueue = [];
      clearTokens();
      if (typeof window !== "undefined") {
        window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
      }
      throw err;
    }
  }

  if (body.code !== 0) {
    throw new Error(body.msg || `API error ${body.code}`);
  }
  return body.data;
}

export const api = {
  get: <T>(path: string) => request<T>(path),

  post: <T>(path: string, data?: unknown) =>
    request<T>(path, {
      method: "POST",
      body: data ? JSON.stringify(data) : undefined,
    }),

  put: <T>(path: string, data?: unknown) =>
    request<T>(path, {
      method: "PUT",
      body: data ? JSON.stringify(data) : undefined,
    }),

  delete: <T>(path: string) => request<T>(path, { method: "DELETE" }),
};

export function onAuthExpired(handler: () => void) {
  if (typeof window === "undefined") return () => undefined;
  window.addEventListener(AUTH_EXPIRED_EVENT, handler);
  return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handler);
}
