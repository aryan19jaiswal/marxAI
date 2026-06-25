"use client";

import { useRouter } from "next/navigation";
import { LogOut } from "lucide-react";

import { useUserStore } from "@/store/user-store";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/theme/theme-toggle";

export function Header() {
  const router = useRouter();
  const user = useUserStore((state) => state.user);
  const clearAuth = useUserStore((state) => state.clearAuth);

  function handleLogout() {
    clearAuth();
    router.replace("/login");
  }

  return (
    <header className="flex items-center justify-between border-b border-border px-4 py-3 sm:px-6">
      <span className="text-sm font-medium text-muted-foreground sm:hidden">
        MarxAI
      </span>
      <div className="flex flex-1 items-center justify-end gap-3">
        {user && (
          <span className="text-sm text-muted-foreground">{user.name}</span>
        )}
        <ThemeToggle />
        <Button variant="ghost" size="icon" aria-label="Log out" onClick={handleLogout}>
          <LogOut />
        </Button>
      </div>
    </header>
  );
}
