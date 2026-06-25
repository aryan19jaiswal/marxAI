"use client";

import { useEffect, useState } from "react";

import { authService } from "@/lib/auth-service";
import { getApiErrorMessage } from "@/lib/api-client";
import { useUserStore } from "@/store/user-store";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import type { UserResponse } from "@/types/auth";

export default function DashboardPage() {
  const storedUser = useUserStore((state) => state.user);
  const [profile, setProfile] = useState<UserResponse | null>(storedUser);
  const [error, setError] = useState<string | null>(null);

  // Confirms the token is actually accepted by a protected endpoint, rather
  // than just trusting whatever is cached in localStorage.
  useEffect(() => {
    authService
      .me()
      .then(setProfile)
      .catch((err) => setError(getApiErrorMessage(err, "Unable to load your profile.")));
  }, []);

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">
          Welcome{profile ? `, ${profile.name}` : ""}
        </h1>
        <p className="text-muted-foreground">
          Here&apos;s your MarxAI dashboard.
        </p>
      </div>

      {error && (
        <Card>
          <CardContent className="text-sm text-destructive">{error}</CardContent>
        </Card>
      )}

      <Card className="max-w-md">
        <CardHeader>
          <CardTitle>Account</CardTitle>
          <CardDescription>Your profile information.</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-2 text-sm">
          <div className="flex justify-between">
            <span className="text-muted-foreground">Name</span>
            <span>{profile?.name ?? "—"}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Email</span>
            <span>{profile?.email ?? "—"}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Joined</span>
            <span>
              {profile?.createdAt
                ? new Date(profile.createdAt).toLocaleDateString()
                : "—"}
            </span>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
