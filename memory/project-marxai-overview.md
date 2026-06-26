---
name: project-marxai-overview
description: MarxAI project — 30-day AI interview prep tool. Tech stack, progress, and key design decisions.
metadata:
  type: project
---

MarxAI is a 30-day AI-powered interview prep tool built in Java/Spring Boot + Next.js.

**Tech stack:**
- Backend: Spring Boot 3.5, Java 21, Gradle, PostgreSQL + Flyway, MinIO (S3), Qdrant (vector store), LangChain4J, Gemini AI
- Frontend: Next.js 16 (App Router), React 19, TypeScript, Tailwind v4, shadcn/ui (base-nova style via @base-ui/react), Zustand, TanStack Query v5, Axios
- Auth: JWT (JJWT) with stateless Spring Security

**Why:** User is building this iteratively over 30 days; each session implements the next day(s) from the roadmap.

**Progress as of Day 14 (2026-06-26):**
- Days 1–7: Full foundation — Docker, DB schema (Flyway), JWT auth, Next.js scaffold with login/register
- Days 8–12: Full RAG pipeline — MinIO upload, Tika parsing, Qdrant embeddings, context assembly
- Days 13–14: Document Management UI — GET/DELETE /api/docs backend endpoints + /documents Next.js page

**How to apply:** When asked to implement the next day, read the ROADMAP_30_DAYS.md (design/) to understand exactly what's expected.
