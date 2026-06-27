package com.marxAI.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * LangChain4J AiServices interface for the Planner — MarxAI's primary conversational agent.
 *
 * <p>Backed by {@code gemini-2.0-flash} for both blocking and streaming variants. Per-session
 * conversation history is maintained via the {@code @MemoryId} annotation: each unique
 * {@code sessionId} string gets its own {@code MessageWindowChatMemory} instance (configured
 * in {@code LangChainConfig}), giving the model visibility into prior turns of the same session.
 *
 * <p>Specialist agents (DSA, System Design, Resume, Mock Interview, Study Plan) will be wired in
 * during Days 17–21. Until then this agent handles all intents using its general coaching prompt.
 * The {@code ChatService} injects the classified intent and RAG context into the user message
 * before it reaches this agent, so no structural changes are needed when specialists are added.
 */
@SystemMessage("""
        You are MarxAI, a world-class AI software engineering interview coach.
        You help engineers ace technical interviews through personalised, expert guidance across:
        DSA (Data Structures & Algorithms), System Design, Resume Review, Mock Interviews,
        and Study Planning.

        Coaching principles:
        • DSA — Socratic method. Ask probing questions and offer hints before revealing answers.
          Always analyse time and space complexity; encourage working through examples step by step.
        • System Design — Trade-off driven. Every design decision has a cost; discuss CAP theorem,
          scalability numbers, bottlenecks, and incremental improvements as a staff engineer would.
        • Resume — Direct, specific, and actionable. Give concrete rewrites, not vague suggestions.
          Be ATS-aware and hold answers to FAANG-caliber standards.
        • Mock Interview — Professional and probing. Ask follow-up questions to test depth;
          do not give away answers unless the user explicitly asks for the solution.
        • Study Plan — Data-driven. Base recommendations on the user's observed weak areas and
          their stated available time per day.

        When the user message starts with [CONTEXT FROM YOUR NOTES], that block contains excerpts
        from the user's own uploaded study materials. Prefer referencing those excerpts in your
        answer before drawing on general knowledge.
        """)
public interface PlannerAgent {

    /**
     * Blocking (non-streaming) chat turn. Used by the REST endpoint when a full response is needed
     * before replying to the client.
     *
     * @param sessionId unique session identifier used as the chat memory key
     * @param userMessage the user's message, optionally prefixed with RAG context
     * @return the assistant's full response text
     */
    String chat(@MemoryId String sessionId, @UserMessage String userMessage);

    /**
     * Streaming chat turn. Used by the SSE endpoint to push tokens to the client as they arrive
     * from Gemini, reducing perceived latency for longer responses.
     *
     * @param sessionId unique session identifier used as the chat memory key
     * @param userMessage the user's message, optionally prefixed with RAG context
     * @return a {@link TokenStream} — call {@code .onPartialResponse().onCompleteResponse().start()}
     */
    TokenStream streamChat(@MemoryId String sessionId, @UserMessage String userMessage);
}
