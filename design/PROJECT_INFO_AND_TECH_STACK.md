# MarxAI — Project Information & Tech Stack

## Project Summary

| Field | Details |
|---|---|
| **Project Name** | MarxAI |
| **Tagline** | Your AI Software Engineering Interview Coach |
| **Goal** | Eliminate scattered prep by giving engineers a single AI-powered mentor for DSA, System Design, Resume, and Mock Interviews |
| **Target Users** | CS students, bootcamp grads, engineers switching jobs |
| **Core Value Prop** | Personalized, context-aware coaching that knows *your* notes, *your* resume, and *your* weak spots |

---

## Problem Statement

Engineers preparing for technical interviews waste hours:
- Re-searching the same DSA patterns acro# Problem Statement: Online Code Judge

**Objective:** Design a platform where users solve programming problems by submitting code that is compiled, executed against test cases, and scored.

**Basic Requirements**

1. Users register and log in.
2. Browse problems with description, examples, difficulty.
3. Submit code in one of several languages.
4. System compiles + runs against hidden test cases; returns pass/fail with runtime.
5. Users see submission history.
6. Leaderboard per problem (by runtime/memory).

**Admin**

1. Add/edit problems and test cases.
2. View submissions; ban users for cheating.
3. Stats: submissions/day, acceptance rate per problem.

**Discussion Guide - Level 1**

- Schema for Problem, Test Case, Submission.
- What happens between "user clicks submit" and "user sees verdict"? Sync or async?
- Where do you store the user's code?
- How do you compute "average runtime" for the leaderboard?

**Discussion Guide - Level 2 - unique angle: sandboxing + worker queue + adversarial code**

- The submitted code is untrusted. It could fork() bomb, read /etc/passwd, or mine bitcoin. How do you isolate it? (Containers? gVisor? Firecracker? seccomp?)
- 50k submissions in 5 minutes after a contest starts. What's the queue + worker design? How do you auto scale?
- **Time-limit enforcement** — Python is naturally 3x slower than C++. How is "TLE" defined fairly?
- Memory limit enforcement — ulimit? cgroup? What happens at the boundary?
- A user's code reads from /proc/self to print the test inputs into its own output. How would you even detect that?ss blogs and YouTube
- Getting generic resume feedback that doesn't match their target role
- Practicing mock interviews with no real-time feedback
- Building study plans that don't adapt to their progress

MarxAI solves this by combining RAG (your notes → relevant context), Agentic AI (right specialist for each question), and persistent memory (tracks what you've mastered).

---

## Tech Stack

### Backend

| Layer | Technology | Version | Reason |
|---|---|---|---|
| Framework | Spring Boot | 3.3.x | Industry standard, strong ecosystem, fits Java interview prep domain |
| AI Orchestration | LangChain4J | 0.35.x | Native Java LLM framework, agent + tool support, RAG pipelines |
| LLM | Claude claude-sonnet-4-6 (Anthropic) | Latest | Best reasoning + 200K context for long documents |
| LLM Fast | Claude Haiku 4.5 | Latest | Classification, summarization, cheap tasks |
| Embedding | text-embedding-004 (Google Gemini) | — | 768-dim, fast, cost-effective |
| ORM | Spring Data JPA + Hibernate | — | Type-safe DB access |
| Migration | Flyway | — | Version-controlled DB migrations |
| Security | Spring Security + JJWT | — | JWT-based stateless auth |
| Validation | Hibernate Validator (Jakarta) | — | Request DTO validation |
| Async | Spring @Async + Virtual Threads | JDK 21 | Non-blocking LLM calls |
| Streaming | Server-Sent Events (SSE) | — | Real-time token streaming to frontend |
| PDF Parsing | Apache Tika | 2.x | Robust PDF/DOCX text extraction |
| Build | Maven | 3.9.x | Dependency management |

### Databases & Storage

| Technology | Use Case | Reason |
|---|---|---|
| PostgreSQL 16 | Users, sessions, conversations, progress, metadata | ACID, JSONB support, battle-tested |
| Qdrant | Vector embeddings for RAG | Self-hostable, fast ANN, metadata filtering, REST API |
| Redis 7 | Session cache, rate limiting, conversation window cache | Sub-ms reads, TTL support |
| MinIO (local) / AWS S3 (prod) | Raw PDFs, uploaded notes | S3-compatible, swap with no code change |

### Frontend

| Technology | Version | Reason |
|---|---|---|
| Next.js | 16.x (App Router) | SSR + streaming UI, file-based routing |
| TypeScript | 5.x | Type safety |
| Tailwind CSS | 4.x | Rapid UI development |
| shadcn/ui | Latest | Accessible component library |
| TanStack Query | v5 | Server state management, caching |
| Zustand | 4.x | Lightweight client state (chat, sessions) |
| Axios | 1.x | HTTP client with interceptors |
| socket.io-client | 4.x | WebSocket for real-time chat |
| react-markdown | — | Render LLM markdown responses |
| shiki | — | Syntax-highlighted code blocks |
| react-dropzone | — | Resume and document upload |
| recharts | — | Progress visualization charts |

### DevOps & Infrastructure

| Technology | Use Case |
|---|---|
| Docker + Docker Compose | Local dev orchestration (all services in one command) |
| GitHub Actions | CI/CD pipeline |
| Nginx | Reverse proxy in production |
| AWS EC2 / Railway | Backend deployment |
| Vercel | Frontend deployment |
| AWS RDS | Managed PostgreSQL in production |
| AWS ElastiCache | Managed Redis in production |
| Qdrant Cloud | Managed vector DB in production |

### External APIs & Services

| Service | Use Case |
|---|---|
| Anthropic API | Claude LLM calls |
| Google Gemini API | Embeddings only (text-embedding-004) |
| Judge0 | Code execution sandbox for coding problems |
| Tavily / Serper | Web search tool for agents |

---

## AI Concepts Implemented

| Concept | Implementation |
|---|---|
| **Prompt Engineering** | Distinct system prompts per agent (Mentor / Interviewer / Reviewer / Planner) stored as templates |
| **RAG** | LangChain4J `EmbeddingStoreRetriever` → Qdrant similarity search → context injection |
| **Agentic AI** | `AiServices` Planner Agent with tool calling, intent classification, and multi-step reasoning |
| **MCP (Model Context Protocol)** | Custom MCP server exposing tools: `searchNotes`, `parseResume`, `generateQuestion`, `trackProgress`, `webSearch`, `runCode` |
| **LLM Orchestration** | PlannerAgent routes to specialist agents; each agent has its own prompt, tools, and memory scope |
| **Memory** | `MessageWindowChatMemory` (short-term, per session) + PostgreSQL progress store (long-term, cross-session) |
| **Tool Calling** | LangChain4J `@Tool` annotations on service methods; agents auto-select tools based on intent |
| **Streaming** | SSE / WebSocket streaming of LLM tokens for real-time chat UX |

---

## Project Structure

```
marxAI/
├── design/                          ← Architecture & planning docs
│   ├── HIGH_LEVEL_ARCHITECTURE.md
│   ├── LOW_LEVEL_ARCHITECTURE.md
│   ├── PROJECT_INFO_AND_TECH_STACK.md
│   └── ROADMAP_30_DAYS.md
│
├── marxAI-server/                   ← Spring Boot backend
│   ├── src/main/java/com/marxai/
│   │   ├── agent/                   ← LangChain4J agent implementations
│   │   │   ├── PlannerAgent.java
│   │   │   ├── DSAAgent.java
│   │   │   ├── SystemDesignAgent.java
│   │   │   ├── ResumeAgent.java
│   │   │   ├── MockInterviewAgent.java
│   │   │   └── StudyPlanAgent.java
│   │   ├── tool/                    ← @Tool methods (MCP-compatible)
│   │   │   ├── NoteSearchTool.java
│   │   │   ├── ResumeParseTool.java
│   │   │   ├── QuestionGeneratorTool.java
│   │   │   ├── ProgressTrackerTool.java
│   │   │   ├── WebSearchTool.java
│   │   │   └── CodeRunnerTool.java
│   │   ├── rag/                     ← RAG pipeline
│   │   │   ├── IngestionService.java
│   │   │   ├── EmbeddingService.java
│   │   │   ├── QdrantService.java
│   │   │   └── ChunkingService.java
│   │   ├── controller/              ← REST + WebSocket
│   │   ├── service/                 ← Business logic
│   │   ├── repository/              ← Spring Data JPA
│   │   ├── model/entity/            ← JPA entities
│   │   ├── model/dto/               ← Request/Response DTOs
│   │   ├── config/                  ← Spring beans, security, LangChain4J
│   │   ├── prompt/                  ← Prompt templates
│   │   └── exception/               ← Error handling
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   └── db/migration/            ← Flyway SQL scripts
│   └── pom.xml
│
└── marxAI-client/                   ← Next.js frontend
    ├── src/
    │   ├── app/                     ← Next.js App Router pages
    │   ├── components/              ← Reusable UI components
    │   ├── hooks/                   ← Custom React hooks
    │   ├── store/                   ← Zustand stores
    │   ├── lib/                     ← API clients, utils
    │   └── types/                   ← TypeScript interfaces
    └── package.json
```

---

## Key Design Principles

1. **Separation of Concerns** — Each agent owns a single responsibility. The Planner never directly calls LLMs; specialist agents do.
2. **Tool-first Architecture** — Agents don't hardcode knowledge. All data access (notes, resume, progress) goes through declared tools, making them composable and testable.
3. **Prompt as Config** — System prompts are externalized to template files, not hardcoded in Java. Changing agent behavior = changing a template.
4. **Progressive Context** — Short-term memory (MessageWindow) for conversation flow; long-term memory (PostgreSQL) for cross-session progress. Both injected into prompts.
5. **Dev-friendly Docker Compose** — Single `docker-compose up` spins up Postgres, Qdrant, Redis, MinIO, and the Spring Boot app locally.
6. **Stream Everything** — All LLM responses stream via SSE. No waiting for full responses.
