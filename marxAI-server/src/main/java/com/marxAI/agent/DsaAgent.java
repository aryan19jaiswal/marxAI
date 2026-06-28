package com.marxAI.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * LangChain4J AiServices interface for the DSA specialist agent.
 *
 * <p>Backed by {@code gemini-2.0-flash} and equipped with three tools:
 * <ul>
 *   <li>{@link com.marxAI.agent.tool.NoteSearchTool} — semantic search over the user's
 *       uploaded DSA notes, grounding explanations in their own materials.</li>
 *   <li>{@link com.marxAI.agent.tool.QuestionGeneratorTool} — on-demand LeetCode-style
 *       practice problem generation for any topic and difficulty.</li>
 *   <li>{@link com.marxAI.agent.tool.CodeRunnerTool} — real code execution via Judge0
 *       to verify solutions and show concrete output.</li>
 * </ul>
 *
 * <p>The {@link com.marxAI.service.ChatService} routes messages with {@code AgentIntent.DSA}
 * to this agent. All other intents continue to the {@link PlannerAgent}.
 *
 * <p>Per-session memory is maintained via the {@code @MemoryId} annotation using the same
 * {@code MessageWindowChatMemory} mechanism as {@link PlannerAgent}.
 */
@SystemMessage("""
        You are the DSA specialist for MarxAI — a world-class Data Structures & Algorithms coach.
        Your mission is to help engineers master DSA for technical interviews.

        Coaching principles:
        • Socratic method — ask probing questions and offer graduated hints before revealing answers.
          Never hand over a full solution unless the user explicitly asks for it.
        • Complexity first — always analyse time and space complexity; ask the user to derive it.
        • Examples before code — guide the user to trace through examples before writing code.
        • Reference personal notes — call searchNotes before explaining any concept to see if the
          user has their own study materials on the topic; prefer those over generic knowledge.
        • Practice on demand — call generateQuestion when the user asks for a practice problem;
          always specify topic and difficulty clearly.
        • Verify solutions — use runCode to execute the user's solution and give concrete
          feedback on correctness and performance.

        Typical session workflow:
        1. Clarify the problem — constraints, edge cases, expected I/O.
        2. Ask for the user's initial approach — do not give hints until they attempt one.
        3. Guide toward the optimal solution through targeted questions.
        4. Review time/space complexity together once a solution is reached.
        5. Optionally run the code, inspect the output, and discuss edge cases.
        """)
public interface DsaAgent {

    /**
     * Blocking chat turn. Used when a full response is needed before replying to the client.
     *
     * @param sessionId   unique session identifier used as the chat memory key
     * @param userMessage the user's message, optionally prefixed with RAG context
     * @return the assistant's full response text
     */
    String chat(@MemoryId String sessionId, @UserMessage String userMessage);

    /**
     * Streaming chat turn. Used by the SSE endpoint to push tokens as they arrive from Gemini.
     *
     * @param sessionId   unique session identifier used as the chat memory key
     * @param userMessage the user's message, optionally prefixed with RAG context
     * @return a {@link TokenStream} — call {@code .onPartialResponse().onCompleteResponse().start()}
     */
    TokenStream streamChat(@MemoryId String sessionId, @UserMessage String userMessage);
}
