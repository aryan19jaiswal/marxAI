"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";

import { authService } from "@/lib/auth-service";
import { getApiErrorMessage, getApiFieldErrors } from "@/lib/api-client";
import { flattenZodErrors, registerSchema } from "@/lib/validation";
import { useHasMounted } from "@/hooks/use-has-mounted";
import { useUserStore } from "@/store/user-store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export default function RegisterPage() {
  const router = useRouter();
  const hasMounted = useHasMounted();
  const token = useUserStore((state) => state.token);
  const setAuth = useUserStore((state) => state.setAuth);

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Already logged in — bounce to the dashboard instead of showing the form.
  useEffect(() => {
    if (hasMounted && token) {
      router.replace("/dashboard");
    }
  }, [hasMounted, token, router]);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);

    const result = registerSchema.safeParse({ name, email, password });
    if (!result.success) {
      setFieldErrors(flattenZodErrors(result.error));
      return;
    }
    setFieldErrors({});

    setIsSubmitting(true);
    try {
      const { token, user } = await authService.register(result.data);
      setAuth(token, user);
      router.replace("/dashboard");
    } catch (error) {
      setFieldErrors(getApiFieldErrors(error) ?? {});
      setFormError(getApiErrorMessage(error, "Unable to register. Please try again."));
    } finally {
      setIsSubmitting(false);
    }
  }

  if (!hasMounted || token) {
    return null;
  }

  return (
    <div className="flex flex-1 items-center justify-center bg-background px-4 py-16">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="text-2xl">Create an account</CardTitle>
          <CardDescription>Start your MarxAI study journey.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
            <div className="flex flex-col gap-2">
              <Label htmlFor="name">Name</Label>
              <Input
                id="name"
                autoComplete="name"
                value={name}
                onChange={(event) => setName(event.target.value)}
                aria-invalid={Boolean(fieldErrors.name)}
              />
              {fieldErrors.name && (
                <p className="text-sm text-destructive">{fieldErrors.name}</p>
              )}
            </div>

            <div className="flex flex-col gap-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                aria-invalid={Boolean(fieldErrors.email)}
              />
              {fieldErrors.email && (
                <p className="text-sm text-destructive">{fieldErrors.email}</p>
              )}
            </div>

            <div className="flex flex-col gap-2">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                autoComplete="new-password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                aria-invalid={Boolean(fieldErrors.password)}
              />
              {fieldErrors.password ? (
                <p className="text-sm text-destructive">{fieldErrors.password}</p>
              ) : (
                <p className="text-sm text-muted-foreground">
                  Must be 8-72 characters.
                </p>
              )}
            </div>

            {formError && <p className="text-sm text-destructive">{formError}</p>}

            <Button type="submit" disabled={isSubmitting} className="mt-2 w-full">
              {isSubmitting ? "Creating account..." : "Register"}
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-muted-foreground">
            Already have an account?{" "}
            <Link href="/login" className="font-medium text-foreground underline-offset-4 hover:underline">
              Log in
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
