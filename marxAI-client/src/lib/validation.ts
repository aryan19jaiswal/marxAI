import { z } from "zod";

/**
 * Mirrors the `@Valid` constraints on `RegisterRequest`/`LoginRequest` in
 * marxAI-server so the client can reject obviously-invalid input before
 * making a round trip. Keep in sync with the backend DTOs.
 */
export const registerSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, "Name is required")
    .max(255, "Name must be at most 255 characters"),
  email: z
    .string()
    .trim()
    .min(1, "Email is required")
    .max(255, "Email must be at most 255 characters")
    .email("Email must be valid"),
  password: z
    .string()
    .min(8, "Password must be between 8 and 72 characters")
    .max(72, "Password must be between 8 and 72 characters"),
});

export const loginSchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, "Email is required")
    .email("Email must be valid"),
  password: z.string().min(1, "Password is required"),
});

export type RegisterFormValues = z.infer<typeof registerSchema>;
export type LoginFormValues = z.infer<typeof loginSchema>;

/** Flattens a ZodError into the same `{field: message}` shape as the API's `fieldErrors`. */
export function flattenZodErrors(error: z.ZodError): Record<string, string> {
  const fieldErrors: Record<string, string> = {};
  for (const issue of error.issues) {
    const key = issue.path[0];
    if (typeof key === "string" && !(key in fieldErrors)) {
      fieldErrors[key] = issue.message;
    }
  }
  return fieldErrors;
}
