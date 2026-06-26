# MarxAI

**Your AI Software Engineering Interview Coach**

MarxAI is a full-stack AI application that gives engineers a single, personalized mentor for DSA, System Design, Resume Review, and Mock Interviews — powered by RAG, agentic AI, and persistent memory.

---

## What It Does

- **DSA Agent** — Socratic coaching, problem generation, code execution via Judge0, feedback on your solution
- **System Design Agent** — Trade-off-focused explanations grounded in your notes + live web search
- **Resume Agent** — ATS scoring, section-by-section feedback, job-description tailoring
- **Mock Interview Agent** — Full interview sessions with per-question scoring and a final performance report
- **Study Plan Agent** — Adaptive day-by-day plans built from your weak topics and progress history
- **RAG over your own notes** — Upload PDFs, markdown, and text files; every agent searches them before answering

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.3, Java 21, LangChain4J |
| LLM | Google Gemini `gemini-2.0-flash` (reasoning) + `gemini-2.0-flash-lite` (classification) |
| Embeddings | Google Gemini `text-embedding-004` (768-dim) |
| Vector DB | Qdrant |
| Relational DB | PostgreSQL 16 |
| Cache | Redis 7 |
| Object Storage | MinIO (local) / AWS S3 (prod) |
| Frontend | Next.js 16 (App Router), TypeScript, Tailwind CSS, shadcn/ui |
| Code Execution | Judge0 |

---

## Project Structure

```
marxAI/
├── design/                  # Architecture and planning docs
├── marxAI-server/           # Spring Boot backend
│   └── src/main/java/com/marxAI/
│       ├── agent/           # LangChain4J agent implementations
│       ├── tool/            # @Tool methods (MCP-compatible)
│       ├── rag/             # RAG pipeline (ingest, embed, retrieve)
│       ├── controller/      # REST + WebSocket endpoints
│       ├── service/         # Business logic
│       ├── config/          # Spring beans, security, LangChain4J
│       └── prompt/          # Prompt templates
└── marxAI-client/           # Next.js frontend
    └── src/
        ├── app/             # App Router pages
        ├── components/      # UI components
        ├── store/           # Zustand state (auth, chat)
        └── lib/             # API clients, utils
```

---

## Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 21
- Node.js 20+
- A Google Gemini API key

### 1. Clone and configure

```bash
git clone <repo-url>
cd marxAI
cp .env.example .env
# Fill in GEMINI_API_KEY, JWT_SECRET in .env
```

### 2. Start all services

```bash
docker-compose up -d
```

This spins up PostgreSQL, Qdrant, Redis, and MinIO.

### 3. Run the backend

```bash
cd marxAI-server
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 4. Run the frontend

```bash
cd marxAI-client
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

---

## How the RAG Pipeline Works

1. **Upload** — PDF/MD/TXT uploaded via `POST /api/docs/upload`, stored in MinIO
2. **Parse** — Apache Tika extracts raw text
3. **Chunk** — 512-token chunks with 50-token overlap, tagged with metadata
4. **Embed** — Gemini `text-embedding-004` generates 768-dim vectors
5. **Store** — Vectors + metadata upserted into Qdrant
6. **Retrieve** — At chat time, the user query is embedded and top-k chunks are fetched
7. **Inject** — Chunks are assembled into the agent's prompt as grounded context

---

## AI Concepts Implemented

| Concept | Implementation |
|---|---|
| RAG | LangChain4J `EmbeddingStoreRetriever` → Qdrant similarity search |
| Agentic AI | `AiServices`-based agents with `@Tool` methods and intent routing |
| Prompt Engineering | Externalized system prompts per agent (Mentor / Interviewer / Reviewer / Planner) |
| Memory | `MessageWindowChatMemory` (per session) + PostgreSQL progress (cross-session) |
| Streaming | Server-Sent Events for real-time token delivery |
| MCP | Custom MCP server exposing tools: `searchNotes`, `parseResume`, `runCode`, and more |

---

## Build Roadmap

See [`design/ROADMAP_30_DAYS.md`](design/ROADMAP_30_DAYS.md) for the full 30-day build plan.

**Current progress (Day 11):**
- [x] Auth (JWT), database schema (Flyway), Docker Compose
- [x] Document upload (MinIO), Apache Tika parsing, chunking
- [x] Qdrant integration, Gemini embeddings, full ingestion pipeline
- [ ] RAG retrieval & context assembly
- [ ] Agents (DSA, System Design, Resume, Mock Interview, Study Plan)
- [ ] Chat UI with streaming

---

## Environment Variables

| Variable | Description |
|---|---|
| `GEMINI_API_KEY` | Google Gemini API key (LLM + embeddings) |
| `JWT_SECRET` | Secret for signing JWTs |
| `POSTGRES_*` | PostgreSQL connection details |
| `QDRANT_HOST` | Qdrant host (default: `localhost`) |
| `MINIO_*` | MinIO endpoint and credentials |

See `.env.example` for the full list.
