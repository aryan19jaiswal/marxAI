package com.marxAI.config;

import com.marxAI.agent.DsaAgent;
import com.marxAI.agent.IntentClassifier;
import com.marxAI.agent.PlannerAgent;
import com.marxAI.agent.tool.CodeRunnerTool;
import com.marxAI.agent.tool.NoteSearchTool;
import com.marxAI.agent.tool.QuestionGeneratorTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Registers all LangChain4J beans: two {@link ChatModel}s (primary + fast classifier),
 * one {@link StreamingChatModel} for SSE, and the AiServices-backed agents.
 *
 * <p>Model assignments:
 * <ul>
 *   <li>{@code gemini-2.0-flash} — primary model for {@link PlannerAgent} and {@link DsaAgent}.</li>
 *   <li>{@code gemini-2.0-flash-lite} — fast, low-cost model for {@link IntentClassifier} and
 *       {@link QuestionGeneratorTool}; runs on every message so cost and latency matter.</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class LangChainConfig {

    /**
     * Maximum number of message turns kept in each session's sliding in-memory window.
     * At 20 turns (10 user + 10 assistant), a typical coding session fits comfortably within
     * Gemini's context window without unbounded memory growth per session.
     */
    static final int MEMORY_WINDOW_SIZE = 20;

    /**
     * Primary {@link ChatModel} backed by {@code gemini-2.0-flash}.
     * Used by the {@link PlannerAgent} for full-quality coaching responses.
     * Marked {@code @Primary} so this bean is injected when no {@code @Qualifier} is specified.
     */
    @Bean
    @Primary
    public ChatModel chatModel(GeminiProperties properties) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(properties.apiKey())
                .modelName(properties.chatModel())
                .build();
    }

    /**
     * Fast {@link ChatModel} backed by {@code gemini-2.0-flash-lite}.
     * Used exclusively by the {@link IntentClassifier} to keep classification latency low.
     * Qualified as {@code "fast"} to avoid ambiguity with the primary model.
     */
    @Bean
    @Qualifier("fast")
    public ChatModel fastChatModel(GeminiProperties properties) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(properties.apiKey())
                .modelName(properties.chatModelFast())
                .build();
    }

    /**
     * {@link StreamingChatModel} backed by {@code gemini-2.0-flash}.
     * Used by the {@link PlannerAgent}'s {@code streamChat()} method to push tokens to the SSE
     * endpoint incrementally rather than waiting for the full response.
     */
    @Bean
    public StreamingChatModel streamingChatModel(GeminiProperties properties) {
        return GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(properties.apiKey())
                .modelName(properties.chatModel())
                .build();
    }

    /**
     * AiServices-backed {@link IntentClassifier} using the fast model.
     * No chat memory is configured — classification is stateless and always based on the
     * single user message being classified.
     */
    @Bean
    public IntentClassifier intentClassifier(@Qualifier("fast") ChatModel fastChatModel) {
        return AiServices.builder(IntentClassifier.class)
                .chatModel(fastChatModel)
                .build();
    }

    /**
     * AiServices-backed {@link PlannerAgent} for all non-DSA intents.
     * Each unique {@code @MemoryId} (session ID) gets its own sliding
     * {@link MessageWindowChatMemory} with {@value #MEMORY_WINDOW_SIZE} messages.
     *
     * <p>Memory is in-process only; a restart clears session histories.
     */
    @Bean
    public PlannerAgent plannerAgent(ChatModel chatModel, StreamingChatModel streamingChatModel) {
        return AiServices.builder(PlannerAgent.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(
                        memoryId -> MessageWindowChatMemory.withMaxMessages(MEMORY_WINDOW_SIZE))
                .build();
    }

    /**
     * AiServices-backed {@link DsaAgent} equipped with the three DSA specialist tools.
     *
     * <p>LangChain4J introspects each tool object for {@code @Tool}-annotated methods and
     * registers them as callable functions the model can invoke mid-conversation.
     * The same {@link MessageWindowChatMemory} window is used as for the planner,
     * keeping per-session context coherent across DSA and non-DSA turns when the user
     * switches topics within a session.
     */
    @Bean
    public DsaAgent dsaAgent(
            ChatModel chatModel,
            StreamingChatModel streamingChatModel,
            NoteSearchTool noteSearchTool,
            QuestionGeneratorTool questionGeneratorTool,
            CodeRunnerTool codeRunnerTool) {
        return AiServices.builder(DsaAgent.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(
                        memoryId -> MessageWindowChatMemory.withMaxMessages(MEMORY_WINDOW_SIZE))
                .tools(noteSearchTool, questionGeneratorTool, codeRunnerTool)
                .build();
    }
}
