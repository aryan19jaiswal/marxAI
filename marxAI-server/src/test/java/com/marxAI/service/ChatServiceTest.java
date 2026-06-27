package com.marxAI.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ChatService}, with all collaborators mocked.
 *
 * <p>Covers:
 * <ul>
 *   <li>New-session creation when no sessionId is supplied.</li>
 *   <li>Session resumption when a valid sessionId is supplied.</li>
 *   <li>{@link SessionNotFoundException} when an unknown sessionId is supplied.</li>
 *   <li>Intent classification is always delegated to {@link IntentClassifier}.</li>
 *   <li>RAG context is correctly scoped via {@link DocumentType} for domain-specific intents.</li>
 *   <li>RAG context is injected into the prompt only when chunks are present.</li>
 *   <li>Both user and assistant turns are persisted to the conversations table.</li>
 *   <li>The correct {@link ChatResponse} fields are returned.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private IntentClassifier intentClassifier;
    @Mock private PlannerAgent plannerAgent;
    @Mock private RetrievalService retrievalService;
    @Mock private SessionRepository sessionRepository;
    @Mock private ConversationRepository conversationRepository;
    @Mock private UserRepository userRepository;

    private ChatService chatService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                intentClassifier,
                plannerAgent,
                retrievalService,
                sessionRepository,
                conversationRepository,
                userRepository);
    }

    // ---------------------------------------------------------------------------
    // Happy-path — new session
    // ---------------------------------------------------------------------------

    @Test
    void chat_noSessionId_createsNewSession() {
        ChatRequest request = new ChatRequest(null, "explain binary search");
        setupDsaFlowWithNewSession(request.message(), "Binary search is O(log n)...");

        chatService.chat(USER_ID, request);

        verify(sessionRepository).save(any(Session.class));
        verify(sessionRepository, never()).findById(any());
    }

    @Test
    void chat_withValidSessionId_resumesExistingSession() {
        Session existingSession = buildSession(SESSION_ID);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(existingSession));
        ChatRequest request = new ChatRequest(SESSION_ID, "explain binary search");
        // Do NOT stub userRepository/sessionRepository.save — this path never calls them
        stubChatFlow(request.message(), intentResult(AgentIntent.DSA, "binary search"),
                AssembledContext.empty(), "Binary search is O(log n)...");

        chatService.chat(USER_ID, request);

        verify(sessionRepository).findById(SESSION_ID);
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void chat_withUnknownSessionId_throwsSessionNotFoundException() {
        UUID unknown = UUID.randomUUID();
        when(sessionRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.chat(USER_ID, new ChatRequest(unknown, "hi")))
                .isInstanceOf(SessionNotFoundException.class)
                .hasMessageContaining(unknown.toString());
    }

    // ---------------------------------------------------------------------------
    // Intent classification and RAG scoping
    // ---------------------------------------------------------------------------

    @Test
    void chat_dsaIntent_scopesRetrievalToDsaDocType() {
        ChatRequest request = new ChatRequest(null, "what is a heap?");
        stubNewSessionCreation();
        stubChatFlow(request.message(), intentResult(AgentIntent.DSA, "heap"), AssembledContext.empty(),
                "A heap is a complete binary tree...");

        chatService.chat(USER_ID, request);

        verify(retrievalService).retrieve(eq("what is a heap?"), eq(DocumentType.DSA));
    }

    @Test
    void chat_systemDesignIntent_scopesRetrievalToSystemDesignDocType() {
        ChatRequest request = new ChatRequest(null, "design a URL shortener");
        IntentClassificationResult intent = intentResult(AgentIntent.SYSTEM_DESIGN, "URL shortener");
        stubNewSessionCreation();
        stubChatFlow(request.message(), intent, AssembledContext.empty(), "Here is a design...");

        chatService.chat(USER_ID, request);

        verify(retrievalService).retrieve(eq("design a URL shortener"), eq(DocumentType.SYSTEM_DESIGN));
    }

    @Test
    void chat_generalIntent_usesUnscopedRetrieval() {
        ChatRequest request = new ChatRequest(null, "hello");
        IntentClassificationResult intent = intentResult(AgentIntent.GENERAL, "greeting");
        stubNewSessionCreation();
        stubChatFlow(request.message(), intent, AssembledContext.empty(), "Hi there!");

        chatService.chat(USER_ID, request);

        // GENERAL has no DocumentType → falls back to unscoped retrieve(query)
        verify(retrievalService).retrieve(eq("hello"));
        verify(retrievalService, never()).retrieve(anyString(), any(DocumentType.class));
    }

    @Test
    void chat_mockInterviewIntent_usesUnscopedRetrieval() {
        ChatRequest request = new ChatRequest(null, "start a mock interview");
        IntentClassificationResult intent = intentResult(AgentIntent.MOCK_INTERVIEW, "interview");
        stubNewSessionCreation();
        stubChatFlow(request.message(), intent, AssembledContext.empty(), "Sure, let's start!");

        chatService.chat(USER_ID, request);

        verify(retrievalService).retrieve(eq("start a mock interview"));
        verify(retrievalService, never()).retrieve(anyString(), any(DocumentType.class));
    }

    // ---------------------------------------------------------------------------
    // RAG context injection
    // ---------------------------------------------------------------------------

    @Test
    void chat_withRagContext_prependsContextBlockToPrompt() {
        ChatRequest request = new ChatRequest(null, "explain quick sort");
        String contextText = "### Source 1:\nQuick sort uses pivot partitioning...\n";
        AssembledContext context = new AssembledContext(contextText, 1);
        IntentClassificationResult intent = intentResult(AgentIntent.DSA, "quick sort");
        stubNewSessionCreation();
        stubChatFlow(request.message(), intent, context, "Quick sort is a divide-and-conquer algorithm...");

        chatService.chat(USER_ID, request);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(plannerAgent).chat(anyString(), messageCaptor.capture());
        String prompt = messageCaptor.getValue();
        assertThat(prompt).startsWith("[CONTEXT FROM YOUR NOTES]\n");
        assertThat(prompt).contains(contextText);
        assertThat(prompt).endsWith("explain quick sort");
    }

    @Test
    void chat_withoutRagContext_passesThroughUserMessageUnchanged() {
        ChatRequest request = new ChatRequest(null, "what is recursion?");
        IntentClassificationResult intent = intentResult(AgentIntent.DSA, "recursion");
        stubNewSessionCreation();
        stubChatFlow(request.message(), intent, AssembledContext.empty(), "Recursion is...");

        chatService.chat(USER_ID, request);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(plannerAgent).chat(anyString(), messageCaptor.capture());
        assertThat(messageCaptor.getValue()).isEqualTo("what is recursion?");
    }

    // ---------------------------------------------------------------------------
    // Conversation persistence
    // ---------------------------------------------------------------------------

    @Test
    void chat_persistsBothUserAndAssistantTurns() {
        ChatRequest request = new ChatRequest(null, "explain merge sort");
        setupDsaFlowWithNewSession(request.message(), "Merge sort is O(n log n)...");

        chatService.chat(USER_ID, request);

        ArgumentCaptor<Conversation> convCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository, times(2)).save(convCaptor.capture());
        assertThat(convCaptor.getAllValues().get(0).getRole()).isEqualTo(ChatService.ROLE_USER);
        assertThat(convCaptor.getAllValues().get(0).getContent()).isEqualTo("explain merge sort");
        assertThat(convCaptor.getAllValues().get(1).getRole()).isEqualTo(ChatService.ROLE_ASSISTANT);
        assertThat(convCaptor.getAllValues().get(1).getContent()).isEqualTo("Merge sort is O(n log n)...");
    }

    // ---------------------------------------------------------------------------
    // Response structure
    // ---------------------------------------------------------------------------

    @Test
    void chat_returnsCorrectResponseFields() {
        ChatRequest request = new ChatRequest(null, "explain binary search");
        String agentReply = "Binary search is O(log n)...";
        AssembledContext context = new AssembledContext("### Source 1:\nNotes on binary search\n", 1);
        IntentClassificationResult intent = intentResult(AgentIntent.DSA, "binary search");
        Session newSession = buildSession(SESSION_ID);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(new User());
        when(sessionRepository.save(any())).thenReturn(newSession);
        when(intentClassifier.classify(request.message())).thenReturn(intent);
        when(retrievalService.retrieve(request.message(), DocumentType.DSA)).thenReturn(context);
        when(plannerAgent.chat(anyString(), anyString())).thenReturn(agentReply);
        when(conversationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChatResponse response = chatService.chat(USER_ID, request);

        assertThat(response.sessionId()).isEqualTo(SESSION_ID);
        assertThat(response.response()).isEqualTo(agentReply);
        assertThat(response.intent()).isEqualTo(AgentIntent.DSA);
        assertThat(response.sourceCount()).isEqualTo(1);
    }

    // ---------------------------------------------------------------------------
    // buildPromptMessage helper (package-private for direct unit testing)
    // ---------------------------------------------------------------------------

    @Test
    void buildPromptMessage_withZeroSources_returnsUnmodifiedMessage() {
        String result = chatService.buildPromptMessage("hello", AssembledContext.empty());
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void buildPromptMessage_withSources_wrapsContextAroundMessage() {
        AssembledContext ctx = new AssembledContext("### Source 1:\nsome note\n", 1);
        String result = chatService.buildPromptMessage("my question", ctx);
        assertThat(result).startsWith("[CONTEXT FROM YOUR NOTES]\n");
        assertThat(result).contains("### Source 1:\nsome note\n");
        assertThat(result).contains("[END CONTEXT]\n\n");
        assertThat(result).endsWith("my question");
    }

    // ---------------------------------------------------------------------------
    // Fixture helpers
    // ---------------------------------------------------------------------------

    /**
     * Stubs the full new-session path for a DSA chat turn. Includes session creation stubs
     * (userRepository + sessionRepository.save), which are only needed when sessionId is null.
     */
    private void setupDsaFlowWithNewSession(String message, String agentReply) {
        stubNewSessionCreation();
        stubChatFlow(message, intentResult(AgentIntent.DSA, "binary search"),
                AssembledContext.empty(), agentReply);
    }

    /**
     * Stubs only the core chat flow stubs (intent, RAG, planner, conversation).
     * Use when the session already exists — do NOT call this with session-creation stubs
     * on the same test to avoid UnnecessaryStubbingException.
     */
    private void stubChatFlow(
            String message,
            IntentClassificationResult intent,
            AssembledContext context,
            String agentReply) {
        when(intentClassifier.classify(message)).thenReturn(intent);
        DocumentType docType = intent.intent().toDocumentType().orElse(null);
        if (docType != null) {
            when(retrievalService.retrieve(message, docType)).thenReturn(context);
        } else {
            when(retrievalService.retrieve(message)).thenReturn(context);
        }
        when(plannerAgent.chat(anyString(), anyString())).thenReturn(agentReply);
        when(conversationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubNewSessionCreation() {
        when(userRepository.getReferenceById(any())).thenReturn(new User());
        when(sessionRepository.save(any())).thenReturn(buildSession(SESSION_ID));
    }

    private static Session buildSession(UUID id) {
        Session s = new Session();
        s.setId(id);
        s.setAgentType("PLANNER");
        return s;
    }

    private static IntentClassificationResult intentResult(AgentIntent intent, String topic) {
        return new IntentClassificationResult(intent, 0.95, topic, "medium", "");
    }
}
