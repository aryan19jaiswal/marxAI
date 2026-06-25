import { useSyncExternalStore } from "react";

const emptySubscribe = () => () => {};

/**
 * Returns `false` on the server and during the first client render, then
 * `true` after mount. Use this to gate any UI that depends on browser-only
 * state (e.g. the Zustand auth store) so the client's first hydration pass
 * matches the server-rendered markup, avoiding a React hydration mismatch.
 *
 * Implemented with `useSyncExternalStore` rather than `useEffect`+`setState`
 * — "is this the client yet" is exactly the case React recommends it for,
 * and it avoids triggering `react-hooks/set-state-in-effect`.
 */
export function useHasMounted(): boolean {
  return useSyncExternalStore(
    emptySubscribe,
    () => true,
    () => false,
  );
}
