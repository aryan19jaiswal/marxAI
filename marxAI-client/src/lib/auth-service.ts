import { apiClient } from "@/lib/api-client";
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  UserResponse,
} from "@/types/auth";

/** Wraps the `/api/users` endpoints exposed by UserController. */
export const authService = {
  register(request: RegisterRequest) {
    return apiClient
      .post<AuthResponse>("/api/users/register", request)
      .then((res) => res.data);
  },

  login(request: LoginRequest) {
    return apiClient
      .post<AuthResponse>("/api/users/login", request)
      .then((res) => res.data);
  },

  me() {
    return apiClient.get<UserResponse>("/api/users/me").then((res) => res.data);
  },
};
