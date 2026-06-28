package com.marxAI.service;

import com.marxAI.agent.DsaAgent;
import com.marxAI.agent.IntentClassifier;
import com.marxAI.agent.PlannerAgent;
import com.marxAI.exception.SessionNotFoundException;
import com.marxAI.model.dto.AssembledContext;
import com.marxAI.model.dto.ChatRequest;
import com.marxAI.model.dto.ChatResponse;
import com.marxAI.model.dto.IntentClassificationResult;
import com.marxAI.model.entity.Conversation;
import com.marxAI.model.entity.Session;
import com.marxAI.model.entity.User;
import com.marxAI.model.enums.AgentIntent;
import com.marxAI.model.enums.DocumentType;
import com.marxAI.repository.ConversationRepository;
import com.marxAI.repository.SessionRepository;
import com.marxAI.repository.UserRepository;
import dev.langchain4j.service.TokenStream;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Orchestrates the full chat pipeline for both blocking and streaming variants.
 *
 * <p>Every chat turn follows this sequence:
 * <ol>
 *   <li>Resolve or create a {@link Session} (new when {@code sessionId} is null).</li>
 *   <li>Classify the user's message via {@link IntentClassifier} using the fast model.</li>
 *   <li>Retrieve relevant RAG context from Qdrant, scoped by the intent's {@link DocumentType}.</li>
 *   <li>Prepend the RAG context block to the user message when chunks are available.</li>
 *   <li>Route to the appropriate specialist agent based on intent:
 *       {@link DsaAgent} for {@code DSA}, {@link PlannerAgent} for all other intents.</li>
 *   <li>Persist both the user turn and assistant turn to the {@code conversations} table.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    /** Role string persisted in {@code conversations.role} for user turns. */
    static final String ROLE_USER = "user";

    /** Role string persisted in {@code conversations.role} for assistant turns. */
    static final String ROLE_ASSISTANT = "assistant";

    /**
     * Header injected before RAG context chunks in the user message so the model knows these
     * are the user's own notes rather than instructions.
     */
    private static final String CONTEXT_HEADER = "[CONTEXT FROM YOUR NOTES]\n";
    private static final String CONTEXT_FOOTER = "[END CONTEXT]\n\n";

    private final IntentClassifier intentClassifier;
    private final PlannerAgent plannerAgent;
    private final DsaAgent dsaAgent;
    private final RetrievalService retrievalService;
    private final SessionRepository sessionRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    /**
     * Blocking chat turn — waits for the model to finish before returning.
     *
     * @param userId  authenticated user's ID
     * @param request chat payload with optional session ID and the user's message
     * @return full {@link ChatResponse} with assistant text, session ID, intent, and RAG source count
     */
    @Transactional
    public ChatResponse chat(UUID userId, ChatRequest request) {
        Session session = resolveSession(userId, request.sessionId());
        IntentClassificationResult intentResult = intentClassifier.classify(request.message());
        AssembledContext context = retrieveContext(request.message(), intentResult.intent());

        String formattedMessage = buildPromptMessage(request.message(), context);
        persistConversationTurn(session, ROLE_USER, request.message());

        String response = routeBlocking(
                intentResult.intent(), session.getId().toString(), formattedMessage);

        persistConversationTurn(session, ROLE_ASSISTANT, response);
        return new ChatResponse(
                session.getId(), response, intentResult.intent(), context.sourceCount());
    }

    /**
     * Streaming chat turn — tokens are pushed to {@code emitter} as they arrive from Gemini.
     *
     * <p>The setup phase (session resolution, intent classification, RAG retrieval, user message
     * persistence) is transactional. The streaming phase is async: each {@code onPartialResponse}
     * callback runs in LangChain4J's streaming thread. The full assembled response is persisted
     * inside {@code onCompleteResponse} using Spring Data JPA's own per-save transaction.
     *
     * @param userId  authenticated user's ID
     * @param request chat payload
     * @param emitter SSE emitter to push tokens to; completed or failed on stream end
     */
    public void streamChat(UUID userId, ChatRequest request, SseEmitter emitter) {
        StreamSetup setup = initStreamSetup(userId, request);

        StringBuilder fullResponse = new StringBuilder();
        TokenStream tokenStream = routeStreaming(
                setup.intentResult().intent(), setup.sessionId(), setup.formattedMessage());

        tokenStream
                .onPartialResponse(token -> {
                    fullResponse.append(token);
                    try {
                        emitter.send(SseEmitter.event().data(token));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                })
                .onCompleteResponse(response -> {
                    // Spring Data JPA's save() is @Transactional internally — safe to call here
                    // from the LangChain4J streaming thread without an outer transaction.
                    conversationRepository.save(Conversation.builder()
                            .session(setup.session())
                            .role(ROLE_ASSISTANT)
                            .content(fullResponse.toString())
                            .build());
                    emitter.complete();
                })
                .onError(emitter::completeWithError)
                .start();
    }

    // ---------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------

    /**
     * Transactional setup phase for a streaming chat turn: resolves the session, classifies intent,
     * retrieves RAG context, and persists the user's message. Returns a lightweight context record
     * that can safely cross the transaction boundary into the streaming thread.
     */
    @Transactional
    StreamSetup initStreamSetup(UUID userId, ChatRequest request) {
        Session session = resolveSession(userId, request.sessionId());
        IntentClassificationResult intentResult = intentClassifier.classify(request.message());
        AssembledContext context = retrieveContext(request.message(), intentResult.intent());
        String formattedMessage = buildPromptMessage(request.message(), context);
        persistConversationTurn(session, ROLE_USER, request.message());
        return new StreamSetup(session, session.getId().toString(), formattedMessage, intentResult);
    }

    private Session resolveSession(UUID userId, UUID sessionId) {
        if (sessionId == null) {
            return createSession(userId);
        }
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
    }

    private Session createSession(UUID userId) {
        User user = userRepository.getReferenceById(userId);
        return sessionRepository.save(Session.builder()
                .user(user)
                .agentType("PLANNER")
                .build());
    }

    private AssembledContext retrieveContext(String message, AgentIntent intent) {
        Optional<DocumentType> docType = intent.toDocumentType();
        return docType.isPresent()
                ? retrievalService.retrieve(message, docType.get())
                : retrievalService.retrieve(message);
    }

    /**
     * Prepends a formatted RAG context block to the user message so the model can reference it.
     * When no context is available the message is passed through unchanged to avoid wasting tokens.
     */
    String buildPromptMessage(String userMessage, AssembledContext context) {
        if (context.sourceCount() == 0) {
            return userMessage;
        }
        return CONTEXT_HEADER + context.context() + CONTEXT_FOOTER + userMessage;
    }

    private void persistConversationTurn(Session session, String role, String content) {
        conversationRepository.save(Conversation.builder()
                .session(session)
                .role(role)
                .content(content)
                .build());
    }

    /**
     * Routes a blocking chat call to {@link DsaAgent} for DSA intents or
     * {@link PlannerAgent} for everything else.
     */
    private String routeBlocking(AgentIntent intent, String sessionId, String message) {
        return intent == AgentIntent.DSA
                ? dsaAgent.chat(sessionId, message)
                : plannerAgent.chat(sessionId, message);
    }

    /**
     * Routes a streaming chat call to {@link DsaAgent} for DSA intents or
     * {@link PlannerAgent} for everything else.
     */
    private TokenStream routeStreaming(AgentIntent intent, String sessionId, String message) {
        return intent == AgentIntent.DSA
                ? dsaAgent.streamChat(sessionId, message)
                : plannerAgent.streamChat(sessionId, message);
    }

    /**
     * Lightweight context record passed from the transactional setup phase to the async
     * streaming phase, avoiding detached-entity issues by carrying only serialisable values.
     */
    record StreamSetup(
            Session session,
            String sessionId,
            String formattedMessage,
            IntentClassificationResult intentResult) {}
}
