import { create } from "zustand";
import { createJSONStorage, persist } from "zustand/middleware";
import type { UserResponse } from "@/types/auth";

interface UserState {
  token: string | null;
  user: UserResponse | null;
  /** Persists the JWT + profile returned by /register or /login. */
  setAuth: (token: string, user: UserResponse) => void;
  /** Clears auth state, e.g. on logout or a 401 from the API. */
  clearAuth: () => void;
}

/**
 * `localStorage` is read synchronously, so this store already holds the
 * persisted token by the time the first client render happens. Components
 * that redirect based on auth state should still gate on a post-mount flag
 * (see `useHasMounted`) so the server-rendered markup — which has no
 * `localStorage` — matches the client's first hydration pass.
 */
const noopStorage = {
  getItem: () => null,
  setItem: () => {},
  removeItem: () => {},
};

export const useUserStore = create<UserState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      setAuth: (token, user) => set({ token, user }),
      clearAuth: () => set({ token: null, user: null }),
    }),
    {
      name: "marxai-auth",
      storage: createJSONStorage(() =>
        typeof window === "undefined" ? noopStorage : window.localStorage,
      ),
      partialize: (state) => ({ token: state.token, user: state.user }),
    },
  ),
);
