export const THEME_STORAGE_KEY = "marxai-theme";

/**
 * Inlined into a blocking `<script>` in the root `<head>` so the `dark`
 * class is applied before first paint — avoids a flash of the wrong theme
 * that a client-side effect (which runs after hydration) would cause.
 */
export const themeInitScript = `(function() {
  try {
    var stored = localStorage.getItem("${THEME_STORAGE_KEY}");
    var isDark = stored === "dark" || (!stored && window.matchMedia("(prefers-color-scheme: dark)").matches);
    document.documentElement.classList.toggle("dark", isDark);
  } catch (e) {}
})();`;

type Listener = () => void;
const listeners = new Set<Listener>();

/** Reads the `dark` class applied by `themeInitScript` (or a prior `setTheme` call). */
export function getThemeSnapshot(): boolean {
  return document.documentElement.classList.contains("dark");
}

/** SSR has no DOM; assume light so the server-rendered markup is deterministic. */
export function getServerThemeSnapshot(): boolean {
  return false;
}

export function subscribeTheme(listener: Listener): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/** Applies the theme to the DOM, persists it, and notifies subscribers (e.g. `useSyncExternalStore`). */
export function setTheme(isDark: boolean): void {
  document.documentElement.classList.toggle("dark", isDark);
  localStorage.setItem(THEME_STORAGE_KEY, isDark ? "dark" : "light");
  listeners.forEach((listener) => listener());
}
