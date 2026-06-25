"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";

import { useHasMounted } from "@/hooks/use-has-mounted";
import { useUserStore } from "@/store/user-store";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/theme/theme-toggle";

export default function Home() {
  const router = useRouter();
  const hasMounted = useHasMounted();
  const token = useUserStore((state) => state.token);

  useEffect(() => {
    if (hasMounted && token) {
      router.replace("/dashboard");
    }
  }, [hasMounted, token, router]);

  return (
    <div className="flex flex-1 flex-col bg-background">
      <header className="flex items-center justify-between px-6 py-4">
        <span className="text-lg font-semibold">MarxAI</span>
        <ThemeToggle />
      </header>

      <main className="flex flex-1 flex-col items-center justify-center gap-6 px-6 text-center">
        <h1 className="max-w-xl text-4xl font-semibold tracking-tight">
          Your AI-powered interview prep mentor
        </h1>
        <p className="max-w-md text-lg text-muted-foreground">
          DSA practice, system design reviews, resume feedback, and mock
          interviews — all in one place.
        </p>
        <div className="flex gap-3">
          <Button size="lg" nativeButton={false} render={<Link href="/register" />}>
            Get started
          </Button>
          <Button
            variant="outline"
            size="lg"
            nativeButton={false}
            render={<Link href="/login" />}
          >
            Log in
          </Button>
        </div>
      </main>
    </div>
  );
}
