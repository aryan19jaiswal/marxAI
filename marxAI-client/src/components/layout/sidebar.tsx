"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  CalendarCheck,
  FileText,
  LayoutDashboard,
  Mic,
  NotebookPen,
} from "lucide-react";

import { cn } from "@/lib/utils";

interface NavItem {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  /** Pages not yet built in this phase of the roadmap; shown but not clickable. */
  comingSoon?: boolean;
}

const NAV_ITEMS: NavItem[] = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { href: "/chat", label: "Chat", icon: NotebookPen, comingSoon: true },
  { href: "/documents", label: "Documents", icon: FileText },
  { href: "/mock-interview", label: "Mock Interview", icon: Mic, comingSoon: true },
  { href: "/study-plan", label: "Study Plan", icon: CalendarCheck, comingSoon: true },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="hidden w-56 flex-col border-r border-border bg-sidebar px-3 py-4 text-sidebar-foreground sm:flex">
      <Link href="/dashboard" className="px-2 pb-4 text-lg font-semibold">
        MarxAI
      </Link>

      <nav className="flex flex-col gap-1">
        {NAV_ITEMS.map(({ href, label, icon: Icon, comingSoon }) => {
          const isActive = pathname === href;

          if (comingSoon) {
            return (
              <span
                key={href}
                className="flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm text-muted-foreground/60"
                title="Coming soon"
              >
                <Icon className="size-4" />
                {label}
              </span>
            );
          }

          return (
            <Link
              key={href}
              href={href}
              className={cn(
                "flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm font-medium transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
                isActive && "bg-sidebar-accent text-sidebar-accent-foreground",
              )}
            >
              <Icon className="size-4" />
              {label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
