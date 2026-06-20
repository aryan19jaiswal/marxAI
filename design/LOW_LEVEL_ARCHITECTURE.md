# MarxAI — Low Level Architecture

## Backend Service Breakdown

```mermaid
graph TB
    subgraph SPRINGBOOT["Spring Boot Application (marxAI-server)"]
        direction TB

        subgraph CONTROLLERS["REST / WebSocket Controllers"]
            CC["ChatController<br/>/api/chat"]
            RC["ResumeController<br/>/api/resume"]
            DC["DocumentController<br/>/api/docs"]
            SC["StudyPlanController<br/>/api/study-plan"]
            UC["UserController<br/>/api/users"]
            WS["ChatWebSocketHandler<br/>/ws/chat"]
        end

        subgraph SERVICES["Service Layer"]
            CS["ChatService"]
            RS["ResumeService"]
            DS["DocumentService"]
            SS["StudyPlanService"]
            IS["IngestionService"]
            PS["ProgressService"]
        end

        subgraph AGENTS_IMPL["Agent Implementations (LangChain4J)"]
            PAI["PlannerAgent<br/>- classifyIntent()<br/>- routeToAgent()<br/>- assembleContext()"]
            DAI["DSAAgent<br/>- explainConcept()<br/>- generateProblem()<br/>- reviewSolution()"]
            SDAI["SystemDesignAgent<br/>- explainPattern()<br/>- designSystem()<br/>- reviewDesign()"]
            RAI["ResumeAgent<br/>- parseResume()<br/>- scoreResume()<br/>- suggestImprovements()"]
            MIAI["MockInterviewAgent<br/>- startSession()<br/>- generateQuestion()<br/>- evaluateAnswer()<br/>- provideFeedback()"]
            SPAI["StudyPlanAgent<br/>- assessLevel()<br/>- buildPlan()<br/>- adjustPlan()"]
        end

        subgraph TOOLS["LangChain4J Tools (MCP-compatible)"]
            T_SEARCH["@Tool searchNotes(query, topic)<br/>→ Qdrant semantic search"]
            T_RESUME["@Tool parseResume(fileId)<br/>→ Apache Tika PDF extraction"]
            T_QGEN["@Tool generateQuestion(topic, difficulty)<br/>→ LLM question factory"]
            T_PROGRESS["@Tool getUserProgress(userId)<br/>→ PostgreSQL progress fetch"]
            T_WEB["@Tool webSearch(query)<br/>→ Tavily/Serper API"]
            T_CODE["@Tool runCode(code, lang)<br/>→ Judge0 sandbox"]
        end

        subgraph RAG_IMPL["RAG Implementation"]
            EMB_SVC["EmbeddingService<br/>(OpenAI/Voyage embeddings)"]
            QDRANT_SVC["QdrantService<br/>- upsertVectors()<br/>- similaritySearch()<br/>- filterByMetadata()"]
            CHUNK_SVC["ChunkingService<br/>- splitByToken()<br/>- overlapChunking()"]
            CTX_SVC["ContextAssembler<br/>- rerank()<br/>- buildPromptContext()"]
        end

        subgraph MEMORY["Memory / Session Layer"]
            CONV_MEM["ConversationMemory<br/>(LangChain4J MessageWindowMemory)"]
            USER_CTX["UserContextStore<br/>(Redis — session scoped)"]
            PROG_TRACK["ProgressTracker<br/>(PostgreSQL — persistent)"]
        end

        subgraph INFRA["Infrastructure"]
            SEC["Spring Security<br/>JWT Filter Chain"]
            CACHE["Redis Cache<br/>(@Cacheable)"]
            ASYNC["@Async Executor<br/>(ThreadPoolTaskExecutor)"]
            STREAM["SSE Emitter<br/>(Streaming responses)"]
        end
    end

    CC --> CS
    RC --> RS
    DC --> DS
    SC --> SS
    WS --> CS

    CS --> PAI
    RS --> RAI
    DS --> IS
    SS --> SPAI

    PAI --> DAI
    PAI --> SDAI
    PAI --> RAI
    PAI --> MIAI
    PAI --> SPAI

    DAI --> T_SEARCH
    DAI --> T_QGEN
    DAI --> T_CODE
    SDAI --> T_SEARCH
    RAI --> T_RESUME
    RAI --> T_WEB
    MIAI --> T_QGEN
    MIAI --> T_PROGRESS
    SPAI --> T_PROGRESS

    T_SEARCH --> QDRANT_SVC
    T_RESUME --> CHUNK_SVC

    DAI --> EMB_SVC
    SDAI --> EMB_SVC
    EMB_SVC --> QDRANT_SVC
    QDRANT_SVC --> CTX_SVC
    CTX_SVC --> PAI

    CS --> CONV_MEM
    CS --> USER_CTX
    PAI --> PROG_TRACK
```

---

## Database Schema

```mermaid
erDiagram
    USERS {
        uuid id PK
        varchar email UK
        varchar name
        varchar password_hash
        jsonb preferences
        timestamp created_at
        timestamp updated_at
    }

    SESSIONS {
        uuid id PK
        uuid user_id FK
        varchar agent_type
        jsonb metadata
        timestamp started_at
        timestamp ended_at
    }

    CONVERSATIONS {
        uuid id PK
        uuid session_id FK
        varchar role
        text content
        jsonb tool_calls
        int tokens_used
        timestamp created_at
    }

    DOCUMENTS {
        uuid id PK
        uuid user_id FK
        varchar filename
        varchar s3_key
        varchar doc_type
        varchar status
        int chunk_count
        jsonb metadata
        timestamp uploaded_at
    }

    CHUNKS {
        uuid id PK
        uuid document_id FK
        text content
        varchar qdrant_id
        int chunk_index
        jsonb metadata
    }

    PROGRESS {
        uuid id PK
        uuid user_id FK
        varchar topic
        varchar subtopic
        int score
        varchar status
        int attempts
        timestamp last_practiced
    }

    STUDY_PLANS {
        uuid id PK
        uuid user_id FK
        date start_date
        date end_date
        jsonb plan_json
        varchar status
        timestamp created_at
    }

    RESUME {
        uuid id PK
        uuid user_id FK
        varchar s3_key
        jsonb parsed_data
        int ats_score
        jsonb feedback
        timestamp uploaded_at
    }

    USERS ||--o{ SESSIONS : "has"
    SESSIONS ||--o{ CONVERSATIONS : "contains"
    USERS ||--o{ DOCUMENTS : "uploads"
    DOCUMENTS ||--o{ CHUNKS : "split into"
    USERS ||--o{ PROGRESS : "tracks"
    USERS ||--o{ STUDY_PLANS : "has"
    USERS ||--o{ RESUME : "uploads"
```

---

## Planner Agent — Internal Flow

```mermaid
sequenceDiagram
    participant CS as ChatService
    participant PA as PlannerAgent
    participant IC as IntentClassifier
    participant CM as ConversationMemory
    participant AG as SpecialistAgent
    participant TOOL as Tools
    participant VDB as Qdrant
    participant LLM as Claude

    CS->>PA: chat(userId, message, sessionId)
    PA->>CM: loadHistory(sessionId)
    CM-->>PA: last N messages
    PA->>IC: classify(message + history)
    IC->>LLM: system_prompt + classify_template
    LLM-->>IC: {intent, confidence, entities}
    IC-->>PA: intent=DSA, topic=DP, difficulty=medium

    PA->>AG: invoke(intent, message, context)
    AG->>TOOL: searchNotes("dynamic programming")
    TOOL->>VDB: similaritySearch(embedding, filter={topic:DSA})
    VDB-->>TOOL: [{chunk, score}, ...]
    TOOL-->>AG: relevant chunks

    AG->>LLM: buildPrompt(system + chunks + history + message)
    LLM-->>AG: stream(response tokens)
    AG-->>PA: streamed response
    PA->>CM: saveMessage(user + assistant)
    PA-->>CS: SSE stream
```

---

## RAG Pipeline — Document Ingestion

```mermaid
sequenceDiagram
    participant API as DocumentController
    participant IS as IngestionService
    participant S3 as MinIO/S3
    participant TIKA as Apache Tika
    participant CHUNK as ChunkingService
    participant EMB as EmbeddingService
    participant QD as Qdrant
    participant PG as PostgreSQL

    API->>IS: ingestDocument(file, userId, docType)
    IS->>S3: upload(file) → s3Key
    IS->>PG: insertDocument(userId, s3Key, status=PROCESSING)
    IS->>TIKA: extractText(file)
    TIKA-->>IS: rawText
    IS->>CHUNK: split(rawText, chunkSize=512, overlap=50)
    CHUNK-->>IS: [chunk1, chunk2, ...]
    loop For each chunk
        IS->>EMB: embed(chunk.text)
        EMB-->>IS: float[] vector (1536-dim)
        IS->>QD: upsert(id, vector, payload={docId,topic,chunkIdx})
        IS->>PG: insertChunk(docId, qdrantId, chunkIdx)
    end
    IS->>PG: updateDocument(status=READY, chunkCount=N)
```

---

## Mock Interview Agent — State Machine

```mermaid
stateDiagram-v2
    [*] --> IDLE
    IDLE --> SETUP: startInterview(topic, difficulty)
    SETUP --> QUESTION_ASKED: generateFirstQuestion()
    QUESTION_ASKED --> EVALUATING: userAnswers()
    EVALUATING --> HINT_GIVEN: requestHint()
    HINT_GIVEN --> EVALUATING: userAnswers()
    EVALUATING --> FOLLOW_UP: partialAnswer()
    FOLLOW_UP --> EVALUATING: userAnswers()
    EVALUATING --> FEEDBACK_GIVEN: completeAnswer()
    FEEDBACK_GIVEN --> QUESTION_ASKED: nextQuestion()
    FEEDBACK_GIVEN --> REPORT_GENERATED: allQuestionsAsked()
    REPORT_GENERATED --> [*]

    note right of EVALUATING
        Tools used:
        - scoreAnswer(answer, rubric)
        - getUserProgress(userId)
    end note

    note right of REPORT_GENERATED
        Persists:
        - scores per question
        - topics to revisit
        - updates ProgressTracker
    end note
```

---

## Frontend Component Architecture

```mermaid
graph TB
    subgraph NEXTJS["Next.js App (marxAI-client)"]
        subgraph PAGES["Pages / App Router"]
            HOME["/"]
            CHAT["/chat/[sessionId]"]
            RESUME["/resume"]
            DOCS["/documents"]
            PLAN["/study-plan"]
            MOCK["/mock-interview"]
        end

        subgraph COMPONENTS["Shared Components"]
            CHATUI["ChatWindow<br/>- MessageList<br/>- MessageInput<br/>- StreamRenderer"]
            SIDEBAR["Sidebar<br/>- SessionHistory<br/>- QuickActions"]
            UPLOAD["FileUploader<br/>(react-dropzone)"]
            MD["MarkdownRenderer<br/>(react-markdown + shiki)"]
            PROG["ProgressChart<br/>(recharts)"]
        end

        subgraph STATE["State Management"]
            ZUSTAND["Zustand Store<br/>- chatStore<br/>- userStore<br/>- sessionStore"]
            REACT_QUERY["TanStack Query<br/>- document queries<br/>- progress queries"]
        end

        subgraph API_LAYER["API Layer"]
            AXIOS["Axios Client<br/>(JWT interceptor)"]
            WS_CLIENT["WebSocket Client<br/>(socket.io-client)"]
        end
    end

    CHAT --> CHATUI
    CHAT --> SIDEBAR
    RESUME --> UPLOAD
    CHATUI --> MD
    PLAN --> PROG
    CHATUI --> STATE
    STATE --> AXIOS
    CHATUI --> WS_CLIENT
```

---

## Prompt Engineering Templates

```mermaid
graph TD
    subgraph PROMPTS["System Prompts per Agent"]
        P1["DSA Mentor Prompt<br/>Role: Expert CS tutor<br/>Tone: Socratic, step-by-step<br/>Tools: searchNotes, generateProblem"]
        P2["System Design Prompt<br/>Role: Staff engineer coach<br/>Tone: Trade-off focused<br/>Tools: searchNotes, webSearch"]
        P3["Resume Reviewer Prompt<br/>Role: FAANG recruiter<br/>Tone: Direct, ATS-aware<br/>Tools: parseResume, webSearch"]
        P4["Mock Interviewer Prompt<br/>Role: Technical interviewer<br/>Tone: Neutral, probing<br/>Tools: generateQuestion, runCode, scoreAnswer"]
        P5["Study Plan Prompt<br/>Role: Learning coach<br/>Tone: Motivating, adaptive<br/>Tools: getUserProgress, buildPlan"]
    end
```
