"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";

/**
 * Wraps the app with a TanStack Query client.
 * Must be a client component so the QueryClient is created per-browser-session,
 * not shared across SSR renders.
 */
export function QueryProvider({ children }: { children: React.ReactNode }) {
  // Create the client once per component mount (not at module level) to avoid
  // sharing state between server renders.
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            // Don't retry on mount if data is fresh within 30s.
            staleTime: 30_000,
          },
        },
      }),
  );

  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}
