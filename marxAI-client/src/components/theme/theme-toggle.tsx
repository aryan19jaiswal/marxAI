"use client";

import { useSyncExternalStore } from "react";
import { Moon, Sun } from "lucide-react";

import {
  getServerThemeSnapshot,
  getThemeSnapshot,
  setTheme,
  subscribeTheme,
} from "@/lib/theme";
import { Button } from "@/components/ui/button";

export function ThemeToggle() {
  const isDark = useSyncExternalStore(
    subscribeTheme,
    getThemeSnapshot,
    getServerThemeSnapshot,
  );

  return (
    <Button
      variant="ghost"
      size="icon"
      aria-label="Toggle dark mode"
      onClick={() => setTheme(!isDark)}
    >
      {isDark ? <Sun /> : <Moon />}
    </Button>
  );
}
