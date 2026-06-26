# MarxAI — High Level Architecture

## System Overview

MarxAI is an AI-powered Software Engineering Coach that acts as a personalized mentor for interview preparation. It combines RAG (Retrieval-Augmented Generation), Agentic AI, and LLM Orchestration to deliver context-aware coaching across DSA, System Design, and resume feedback.

---

## High Level Architecture Diagram

```mermaid
graph TB
    subgraph CLIENT["Client Layer"]
        UI["Next.js Frontend<br/>(Chat UI, Resume Upload,<br/>Study Plan Dashboard)"]
    end

    subgraph GATEWAY["API Gateway Layer"]
        GW["Spring Boot API Gateway<br/>Auth · Rate Limiting · Routing"]
    end

    subgraph ORCHESTRATION["Orchestration Layer"]
        PA["Planner Agent<br/>(LangChain4J)"]
        ROUTER["Intent Router<br/>(LLM-based classification)"]
    end

    subgraph AGENTS["Specialist Agent Layer"]
        RA["Resume Agent"]
        DA["DSA Agent"]
        SDA["System Design Agent"]
        MIA["Mock Interview Agent"]
        SPA["Study Plan Agent"]
    end

    subgraph RAG["RAG Layer"]
        EMB["Embedding Service<br/>(text-embedding-004)"]
        VDB["Vector Database<br/>(Qdrant)"]
        CHUNK["Document Chunker<br/>& Loader"]
    end

    subgraph STORAGE["Storage Layer"]
        PG["PostgreSQL<br/>(Users, Sessions,<br/>Progress, Conversations)"]
        S3["S3 / MinIO<br/>(Resume PDFs,<br/>Uploaded Notes)"]
        REDIS["Redis<br/>(Session Cache,<br/>Rate Limits)"]
    end

    subgraph LLM_LAYER["LLM Layer"]
        CLAUDE["Gemini gemini-2.0-flash<br/>(Primary LLM)"]
        FALLBACK["Gemini gemini-2.0-flash-lite<br/>(Fast / Cheap tasks)"]
    end

    subgraph MCP["MCP Tool Layer"]
        MCPS["MCP Server"]
        T1["Tool: Search Notes"]
        T2["Tool: Parse Resume"]
        T3["Tool: Question Generator"]
        T4["Tool: Progress Tracker"]
        T5["Tool: Web Search"]
    end

    UI -->|"HTTPS REST / WebSocket"| GW
    GW --> PA
    PA --> ROUTER
    ROUTER --> RA
    ROUTER --> DA
    ROUTER --> SDA
    ROUTER --> MIA
    ROUTER --> SPA

    RA --> MCPS
    DA --> MCPS
    SDA --> MCPS
    MIA --> MCPS
    SPA --> MCPS

    MCPS --> T1
    MCPS --> T2
    MCPS --> T3
    MCPS --> T4
    MCPS --> T5

    T1 --> VDB
    T2 --> S3
    T4 --> PG

    AGENTS -->|"Semantic Search"| EMB
    EMB --> VDB

    AGENTS -->|"LLM calls"| CLAUDE
    AGENTS -->|"Classification / summarize"| FALLBACK

    GW --> PG
    GW --> REDIS

    CHUNK -->|"Ingest pipeline"| EMB
    S3 -->|"Raw docs"| CHUNK
```

---

## User Journeys

```mermaid
sequenceDiagram
    actor User
    participant UI as Next.js UI
    participant GW as API Gateway
    participant PA as Planner Agent
    participant Agent as Specialist Agent
    participant RAG as RAG Layer
    participant LLM as Gemini LLM

    User->>UI: "Explain dynamic programming to me"
    UI->>GW: POST /chat {message, sessionId}
    GW->>PA: route(message, userContext)
    PA->>PA: classify intent → DSA
    PA->>Agent: DSA Agent invoke(message, context)
    Agent->>RAG: semanticSearch("dynamic programming")
    RAG-->>Agent: top-k relevant chunks
    Agent->>LLM: prompt(system_prompt + chunks + message)
    LLM-->>Agent: explanation + examples
    Agent-->>PA: response
    PA-->>GW: structured response
    GW-->>UI: streamed markdown
    UI-->>User: rendered explanation
```

---

## Data Flow — Document Ingestion

```mermaid
flowchart LR
    A["User Uploads<br/>PDF / Markdown"] --> B["S3 / MinIO<br/>Raw Storage"]
    B --> C["Document Processor<br/>(Apache Tika + LangChain4J)"]
    C --> D["Text Chunker<br/>(512 tokens, 50 overlap)"]
    D --> E["Embedding Model<br/>(text-embedding-3-small)"]
    E --> F["Qdrant<br/>Vector DB"]
    F --> G["Metadata Index<br/>(PostgreSQL)"]
```

---

## Agent Routing Logic

```mermaid
flowchart TD
    IN["User Message"] --> CLS["Intent Classifier<br/>(gemini-2.0-flash-lite)"]
    CLS --> |"DSA / algorithm / code"| DA["DSA Agent"]
    CLS --> |"system design / scalability"| SDA["System Design Agent"]
    CLS --> |"resume / feedback / ATS"| RA["Resume Agent"]
    CLS --> |"mock interview"| MIA["Mock Interview Agent"]
    CLS --> |"study plan / schedule"| SPA["Study Plan Agent"]
    CLS --> |"ambiguous"| PA["Planner asks clarifying Q"]

```

---

## Key Architectural Decisions

| Concern | Decision | Rationale |
|---|---|---|
| Orchestration | LangChain4J Planner Agent | Native Java, integrates with Spring Boot, supports tool calling |
| Vector DB | Qdrant | Self-hostable, fast, supports metadata filtering |
| LLM | Gemini `gemini-2.0-flash` | Best reasoning, long context for documents |
| Fast tasks | Gemini `gemini-2.0-flash-lite` | Classification & summarization at low cost |
| Streaming | WebSocket / SSE | Real-time chat UX |
| Auth | JWT + Spring Security | Stateless, scalable |
| Storage | MinIO (local) / S3 (prod) | Unified S3-compatible API |
