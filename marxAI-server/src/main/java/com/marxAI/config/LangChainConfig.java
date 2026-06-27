package com.marxAI.config;

import com.marxAI.agent.IntentClassifier;
import com.marxAI.agent.PlannerAgent;
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
 * Registers all LangChain4J beans needed for Day 15-16 agent infrastructure:
 * two {@link ChatLanguageModel}s (main reasoning model + fast classifier model),
 * one {@link StreamingChatLanguageModel} for SSE, and the two AiServices-backed agents.
 *
 * <p>Model assignments:
 * <ul>
 *   <li>{@code gemini-2.0-flash} — primary model for the {@link PlannerAgent}; high-quality
 *       multi-turn coaching responses.</li>
 *   <li>{@code gemini-2.0-flash-lite} — fast, low-cost model for the {@link IntentClassifier};
 *       runs on every message so latency and token cost matter more than output quality.</li>
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
     * AiServices-backed {@link PlannerAgent} using both the blocking and streaming models.
     * Each unique {@code @MemoryId} (session ID string) gets its own
     * {@link MessageWindowChatMemory} with a {@value #MEMORY_WINDOW_SIZE}-message sliding window,
     * so multi-turn sessions remain coherent without unbounded memory growth.
     *
     * <p>Note: memory is in-process only. A server restart clears all session histories.
     * Persistent memory backed by Redis is planned for a later sprint.
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
}
