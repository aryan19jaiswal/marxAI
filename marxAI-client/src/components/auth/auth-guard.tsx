"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

import { useHasMounted } from "@/hooks/use-has-mounted";
import { useUserStore } from "@/store/user-store";

/**
 * Wraps pages that require a logged-in user. The JWT lives in
 * `localStorage` (via the persisted Zustand store), which is only readable
 * client-side, so this is a client-side gate rather than `proxy.ts` —
 * Proxy only sees cookies, not localStorage.
 *
 * Renders nothing until mounted (avoids a hydration mismatch, see
 * `useHasMounted`) and nothing while a redirect to `/login` is in flight.
 */
export function AuthGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const hasMounted = useHasMounted();
  const token = useUserStore((state) => state.token);

  useEffect(() => {
    if (hasMounted && !token) {
      router.replace("/login");
    }
  }, [hasMounted, token, router]);

  if (!hasMounted || !token) {
    return null;
  }

  return <>{children}</>;
}
