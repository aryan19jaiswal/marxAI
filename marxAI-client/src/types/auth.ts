/**
 * Mirrors the DTOs exposed by `UserController` in marxAI-server
 * (com.marxAI.model.dto.*). Keep these in sync with the backend.
 */

/** POST /api/users/register body. */
export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

/** POST /api/users/login body. */
export interface LoginRequest {
  email: string;
  password: string;
}

/** Public-facing user view, returned by /register, /login, and /me. */
export interface UserResponse {
  id: string;
  email: string;
  name: string;
  createdAt: string;
}

/** Response body for /register and /login. */
export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInMs: number;
  user: UserResponse;
}

/** Uniform error body produced by GlobalExceptionHandler. */
export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string> | null;
}
