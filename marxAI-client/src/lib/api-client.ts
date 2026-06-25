import axios, { AxiosError } from "axios";
import type { ApiErrorResponse } from "@/types/auth";
import { useUserStore } from "@/store/user-store";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

/** Endpoints that don't require — and shouldn't redirect on — a 401. */
const PUBLIC_PATHS = ["/api/users/register", "/api/users/login"];

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { "Content-Type": "application/json" },
});

// Attach the JWT (if any) to every outgoing request.
apiClient.interceptors.request.use((config) => {
  const token = useUserStore.getState().token;
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

// A 401 on an authenticated request means the token is missing/expired —
// clear local auth state and bounce to /login. Login/register itself
// returning 401 (e.g. bad credentials) is a normal error, not a session
// expiry, so it's surfaced to the caller instead.
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorResponse>) => {
    const path = error.config?.url ?? "";
    const isPublicPath = PUBLIC_PATHS.some((p) => path.includes(p));

    if (error.response?.status === 401 && !isPublicPath) {
      useUserStore.getState().clearAuth();
      if (typeof window !== "undefined") {
        window.location.href = "/login";
      }
    }

    return Promise.reject(error);
  },
);

/** Extracts a human-readable message from an Axios error against this API. */
export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ApiErrorResponse | undefined;
    if (data?.message) return data.message;
  }
  return fallback;
}

/** Extracts per-field validation errors (400 responses), if present. */
export function getApiFieldErrors(
  error: unknown,
): Record<string, string> | null {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ApiErrorResponse | undefined;
    return data?.fieldErrors ?? null;
  }
  return null;
}
