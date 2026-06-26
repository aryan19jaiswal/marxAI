---
name: project-conventions
description: MarxAI coding conventions — patterns used consistently across the codebase.
metadata:
  type: project
---

**Backend (Java):**
- Lombok: @Builder, @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor, @RequiredArgsConstructor on all entities/services
- DTOs are Java records; entities use Lombok builders
- Services have Javadoc with @param/@throws; controllers have brief endpoint-level Javadoc
- Exceptions thrown from services; GlobalExceptionHandler translates them to ErrorResponse
- Integration tests use @SpringBootTest + TestRestTemplate (RANDOM_PORT), require Docker
- Unit tests use Mockito (@ExtendWith(MockitoExtension.class))
- Repository naming: findBy{Field}OrderBy{Field}Desc, deleteBy{Field}

**Frontend (Next.js/React):**
- "use client" only on components that use hooks/interactivity
- API calls go through apiClient (Axios) with JWT interceptor in lib/api-client.ts
- Service layer: one *-service.ts per domain that wraps apiClient
- Types in src/types/*.ts, mirroring backend DTOs
- Zustand for auth state (user-store.ts)
- TanStack Query for server state (documents, etc.)
- @base-ui/react for UI primitives (Button uses it; shadcn "base-nova" style)
- Sidebar has comingSoon flag to grey out unimplemented routes
