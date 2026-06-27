package com.marxAI.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.marxAI.agent.IntentClassifier;
import com.marxAI.agent.PlannerAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LangChainConfig}.
 *
 * <p>Verifies that the {@link IntentClassifier} and {@link PlannerAgent} beans are correctly
 * assembled from the underlying model beans without requiring a live Gemini API key.
 * The model beans are replaced with Mockito mocks, which is sufficient to confirm that
 * {@code AiServices.builder()} wires the dependencies and returns a non-null proxy.
 */
class LangChainConfigTest {

    private LangChainConfig langChainConfig;
    private ChatModel mockChatModel;
    private ChatModel mockFastModel;
    private StreamingChatModel mockStreamingModel;

    @BeforeEach
    void setUp() {
        langChainConfig = new LangChainConfig();
        mockChatModel = mock(ChatModel.class);
        mockFastModel = mock(ChatModel.class);
        mockStreamingModel = mock(StreamingChatModel.class);
    }

    @Test
    void intentClassifier_isCreatedWithFastModel() {
        IntentClassifier classifier = langChainConfig.intentClassifier(mockFastModel);

        // AiServices returns a proxy that implements the interface — verify it was created
        assertThat(classifier).isNotNull();
        assertThat(classifier).isInstanceOf(IntentClassifier.class);
    }

    @Test
    void plannerAgent_isCreatedWithMainAndStreamingModels() {
        PlannerAgent agent = langChainConfig.plannerAgent(mockChatModel, mockStreamingModel);

        assertThat(agent).isNotNull();
        assertThat(agent).isInstanceOf(PlannerAgent.class);
    }

    @Test
    void memoryWindowSize_isPositive() {
        assertThat(LangChainConfig.MEMORY_WINDOW_SIZE).isGreaterThan(0);
    }

    @Test
    void memoryWindowSize_isAtLeast10Messages() {
        // Ensure the window is large enough for a meaningful multi-turn session
        assertThat(LangChainConfig.MEMORY_WINDOW_SIZE).isGreaterThanOrEqualTo(10);
    }
}
